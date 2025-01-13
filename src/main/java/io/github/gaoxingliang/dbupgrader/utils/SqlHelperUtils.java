package io.github.gaoxingliang.dbupgrader.utils;

import com.google.common.base.*;
import io.github.gaoxingliang.dbupgrader.*;
import lombok.experimental.*;
import lombok.extern.java.*;
import net.sf.jsqlparser.*;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.*;
import net.sf.jsqlparser.statement.alter.*;
import net.sf.jsqlparser.statement.insert.*;
import org.apache.commons.lang3.*;

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

    public boolean smartAddColumn(Connection conn, String sql) throws SQLException {
        // Parse the INSERT statement
        net.sf.jsqlparser.statement.Statement statement = null;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new RuntimeException(e);
        }
        if (!(statement instanceof Alter)) {
            throw new SQLException("This method only support alter sql");
        }
        Alter alter = (Alter) statement;
        String tableName = alter.getTable().getName();
        Preconditions.checkArgument(alter.getAlterExpressions().size() == 1, "Only support one alter expression");
        AlterExpression alterExpr = alter.getAlterExpressions().get(0);
        AlterOperation alterOps = alterExpr.getOperation();
        Preconditions.checkArgument(alterOps.name().equalsIgnoreCase("ADD"), "Only support ADD COLUMN operation");
        // allown add one column each time.
        List<AlterExpression.ColumnDataType> columns = alterExpr.getColDataTypeList();
        Preconditions.checkArgument(columns.size() == 1, "Only support add one column each time");
        AlterExpression.ColumnDataType colDataType =  columns.get(0);
        String column = colDataType.getColumnName();
        // check whether the column exists
        try (ResultSet rs = conn.getMetaData().getColumns(ObjectUtils.firstNonNull(alter.getTable().getSchemaName(),
                conn.getCatalog()), null, tableName, column)) {
            if (rs.next()) {
                log.info("Column already exists, skipping add column: " + sql);
                return false;
            }
        }

        executeUpdate(conn, sql);
        return true;
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
     * @return whether the record is inserted
     * @throws SQLException if a database access error occurs
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
            try (ResultSet rs = connection.getMetaData().getPrimaryKeys(ObjectUtils.firstNonNull(insert.getTable().getSchemaName(),
                    connection.getCatalog()), null, tableName)) {
                while (rs.next()) {
                    pkColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            if (pkColumns.isEmpty()) {
                throw new SQLException(String.format("No primary key found for table: {}, consider use other methods to insert records",
                        tableName));
            }

            return insertIfNotExists(connection, sql, pkColumns.toArray(new String[0]), insert) > 0;
        } catch (JSQLParserException e) {
            throw new SQLException("Failed to parse SQL statement: " + sql, e);
        }
    }

    /**
     * Automatically insert if the record doesn't exist.
     * Parse the sql, automatically check whether the record exists by usingg the input uniqueColumns
     *
     * @param connection
     * @param sql
     * @param uniqueColumns
     * @return whether the record is inserted
     * @throws SQLException
     */
    public boolean smartInsertWithUniqueColumns(Connection connection, String sql, String... uniqueColumns) throws SQLException {
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
            return insertIfNotExists(connection, sql, uniqueColumns, insert) > 0;
        } catch (JSQLParserException e) {
            throw new SQLException("Failed to parse SQL statement: " + sql, e);
        }
    }

    private static int insertIfNotExists(Connection connection, String sql, String[] uniqueColumns, Insert insert) throws SQLException {
        // Get columns and values from the INSERT statement
        List<String> columns =
                insert.getColumns().stream().map(column -> column.getColumnName().toLowerCase()).collect(Collectors.toList());
        ExpressionList<?> values = insert.getValues().getExpressions();
        if (values.isEmpty()) {
            throw new SQLException("No insert values found " + sql);
        }

        boolean multipleInsert = values.get(0) instanceof ParenthesedExpressionList;
        List<ExpressionList> multipleInsertValues = new ArrayList<>();
        if (multipleInsert) {
            for (int i = 0; i < values.size(); i++) {
                ParenthesedExpressionList value = (ParenthesedExpressionList) values.get(i);
                if (value.isEmpty()) {
                    throw new SQLException("No insert values found " + sql);
                }
                multipleInsertValues.add(value);
            }
        } else {
            multipleInsertValues.add(values);
        }
        String tableName = insert.getTable().getName();
        // Build WHERE clause for checking existence
        StringBuilder existenceCheck = new StringBuilder("SELECT count(1) FROM " + tableName + " WHERE ");
        List<String> whereClauses = new ArrayList<>();
        for (String uniqueColumn : uniqueColumns) {
            whereClauses.add(uniqueColumn + " = ? ");
        }
        existenceCheck.append(String.join(" AND ", whereClauses));
        String existenceCheckSql = existenceCheck.toString();
        String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", insert.getTable().getName(), String.join(",", columns),
                String.join(",", Collections.nCopies(columns.size(), "?")));
        int insertCount = 0;
        for (ExpressionList exprList : multipleInsertValues) {
            // compose args
            List<Object> whereArgs = new ArrayList<>();
            for (String uniqueColumn : uniqueColumns) {
                int valueIndex = searchColumnIndex(columns, uniqueColumn);
                if (valueIndex == -1) {
                    throw new SQLException("Column " + uniqueColumn + " value not found in INSERT statement columns " + columns);
                }
                whereArgs.add(map2Value((Expression) exprList.get(valueIndex), false));
            }
            // Check if record exists
            if (recordExists(connection, existenceCheckSql, whereArgs)) {
                log.info("Record already exists, skipping insert: " + sql + "with args:" + whereArgs);
            } else {
                // insert args
                List<Object> insertArgs = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    insertArgs.add(map2Value((Expression) exprList.get(i), true));
                }
                executeUpdate(connection, insertSql, insertArgs.toArray());
                insertCount++;
            }
        }

        return insertCount;
    }

    private static boolean recordExists(Connection connection, String sql, List<Object> whereArgs) throws SQLException {
        return query(connection, sql, rs -> rs.getInt(1), whereArgs.toArray()) > 0;
    }

    private Object map2Value(Expression expr, boolean possibleNull) throws SQLException {
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
            if (!possibleNull) {
                throw new SQLException("The value is null for the primary key " + expr);
            } else {
                return null;
            }
        } else {
            throw new SQLException("Unsupported expression type: " + expr.getClass().getName() + " for primary key " + expr);
        }
    }

    private int searchColumnIndex(List<String> columns, String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            String currentCol = columns.get(i);
            if (currentCol.equalsIgnoreCase(columnName) ||
                    currentCol.equalsIgnoreCase(String.format("`%s`", columnName))
                    || columnName.equalsIgnoreCase(String.format("`%s`", currentCol))) {
                return i;
            }
        }

        return -1;
    }
}
