package io.github.codeed.dbupgrader;

import java.sql.*;

/**
 * {@link DbUpgrade} use this with class
 */
public interface UpgradeProcess {
    void upgrade(DbUpgrader migrator, Connection connection) throws SQLException;
}
