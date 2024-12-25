package io.github.gaoxingliang.dbupgrader.utils;

import io.github.gaoxingliang.dbupgrader.*;
import lombok.experimental.*;

import java.sql.*;
import java.util.*;

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
}
