package io.github.gaoxingliang.dbupgrader.starter.test.upgrades;

import io.github.gaoxingliang.dbupgrader.*;
import io.github.gaoxingliang.dbupgrader.utils.*;

import java.sql.*;

@DbUpgrade(version = 1)
public class V1AddTableUser implements UpgradeProcess{
    @Override
    public void upgrade(DbUpgrader migrator, Connection connection) throws SQLException {
        SqlHelperUtils.createTableIfNotExists(
                connection,
                "test_user",
                "create table test_user (id int)"
        );
    }
}
