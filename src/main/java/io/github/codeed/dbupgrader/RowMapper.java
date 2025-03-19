package io.github.codeed.dbupgrader;

import java.sql.*;

/**
 * Interface for mapping a ResultSet row to an object
 */
@FunctionalInterface
public interface RowMapper<T> {
    T mapRow(ResultSet rs) throws SQLException;
}