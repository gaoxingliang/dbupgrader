package io.github.gaoxingliang.dbupgrader.utils;

import io.github.gaoxingliang.dbupgrader.*;
import lombok.experimental.*;
import lombok.extern.java.*;
import net.sf.jsqlparser.*;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.*;
import net.sf.jsqlparser.statement.insert.*;

import java.sql.*;
import java.util.*;
import java.util.stream.*;

@Log
@UtilityClass
public class SqlHelperUtils {
    public void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    public void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    public void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    public boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            while (rs.next()) {
                if (rs.getString("TABLE_NAME").equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void createTableIfNotExists(Connection conn, String tableName, String createTableSql) throws SQLException {
        if (!tableExists(conn, tableName)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSql);
            }
        }
    }

    /**
     * Execute update with variable arguments
     */
    public int executeUpdate(Connection conn, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps.executeUpdate();
        }
    }

    /**
     * Insert and return the generated id
     */
    public long insertWithIdReturned(Connection conn, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Failed to get generated ID " + sql + " args: " + Arrays.toString(args));
            }
        }
    }

    /**
     * Execute query with a RowMapper
     *
     * @return null or T
     */
    public <T> T query(Connection conn, String sql, RowMapper<T> rowMapper, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowMapper.mapRow(rs);
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Query for a list of objects
     */
    public <T> List<T> queryForList(Connection conn, String sql, RowMapper<T> rowMapper, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
            }
            List<T> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.add(rowMapper.mapRow(rs));
                }
            }
            return result;
        }
    }

    /**
     * Automatically insert if the record doesn't exist.
     * Parse the sql, check the primary key and automatically check whether the record exists.
     *
     * @param connection Database connection
     * @param sql        INSERT SQL statement
     * @throws SQLException if a database access error occurs
     * @return whether the record is inserted
     */
    public boolean smartInsertWithPrimaryKeySet(Connection connection, String sql) throws SQLException {
        try {
            // Parse the INSERT statement
            net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Insert)) {
                throw new SQLException("This method only support insert sql");
            }
            Insert insert = (Insert) statement;
            String tableName = insert.getTable().getName();

            // Get primary key columns for the table
            List<String> pkColumns = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getPrimaryKeys(null, null, tableName)) {
                while (rs.next()) {
                    pkColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            if (pkColumns.isEmpty()) {
                throw new SQLException(String.format("No primary key found for table: {}, consider use other methods to insert records",
                        tableName));
            }

            return insertIfNotExists(connection, sql, pkColumns.toArray(new String[0]), insert);
        } catch (JSQLParserException e) {
            throw new SQLException("Failed to parse SQL statement: " + sql, e);
        }
    }

    /**
     * Automatically insert if the record doesn't exist.
     * Parse the sql, automatically check whether the record exists by usingg the input uniqueColumns
     * @param connection
     * @param sql
     * @param uniqueColumns
     * @return whether the record is inserted
     * @throws SQLException
     */
    public boolean smartInsertWithUniqueColumns(Connection connection, String sql, String ...uniqueColumns) throws SQLException {
        try {
            // Parse the INSERT statement
            net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Insert)) {
                throw new SQLException("This method only support insert sql");
            }
            if (uniqueColumns == null || uniqueColumns.length == 0) {
                throw new SQLException("The unique columns is empty");
            }
            Insert insert = (Insert) statement;
            return insertIfNotExists(connection, sql, uniqueColumns, insert);
        } catch (JSQLParserException e) {
            throw new SQLException("Failed to parse SQL statement: " + sql, e);
        }
    }

    private static boolean insertIfNotExists(Connection connection, String sql, String[] uniqueColumns, Insert insert) throws SQLException {
        // Get columns and values from the INSERT statement
        List<String> columns =
                insert.getColumns().stream().map(column -> column.getColumnName().toLowerCase()).collect(Collectors.toList());
        ExpressionList<?> values = insert.getValues().getExpressions();
        String tableName = insert.getTable().getName();
        // Build WHERE clause for checking existence
        StringBuilder existenceCheck = new StringBuilder("SELECT count(1) FROM " + tableName + " WHERE ");
        List<String> whereClauses = new ArrayList<>();
        List<Object> whereArgs = new ArrayList<>();
        for (String uniqueColumn : uniqueColumns) {
            int valueIndex = columns.indexOf(uniqueColumn.toLowerCase());
            if (valueIndex == -1) {
                throw new SQLException("Column " + uniqueColumn + " value not found in INSERT statement");
            }
            whereClauses.add(uniqueColumn + " = ? ");
            whereArgs.add(map2Value(values.get(valueIndex)));
        }
        existenceCheck.append(String.join(" AND ", whereClauses));

        // Check if record exists
        int count = query(connection, existenceCheck.toString(), rs -> rs.getInt(1), whereArgs.toArray());
        if (count == 0) {
            executeUpdate(connection, sql);
            return true;
        } else {
            log.info("Record already exists, skipping insert: " + sql);
            return false;
        }
    }

    private Object map2Value(Expression expr) throws SQLException {
        if (expr instanceof StringValue) {
            return ((StringValue) expr).getValue();
        } else if (expr instanceof LongValue) {
            return ((LongValue) expr).getValue();
        } else if (expr instanceof DoubleValue) {
            return ((DoubleValue) expr).getValue();
        } else if (expr instanceof DateValue) {
            return ((DateValue) expr).getValue();
        } else if (expr instanceof TimeValue) {
            return ((TimeValue) expr).getValue();
        } else if (expr instanceof TimestampValue) {
            return ((TimestampValue) expr).getValue();
        } else if (expr instanceof NullValue) {
            throw new SQLException("The value is null for the primary key " + expr);
        } else {
            throw new SQLException("Unsupported expression type: " + expr.getClass().getName() + " for primary key " + expr);
        }
    }
}
