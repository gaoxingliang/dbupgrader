package io.github.codeed.dbupgrader.upgradescripts;

import io.github.codeed.dbupgrader.*;
import io.github.codeed.dbupgrader.utils.*;

import java.sql.*;

@DbUpgrade(version = 1)
public class V1AddTableUser implements UpgradeProcess{
    @Override
    public void upgrade(DbUpgrader migrator, Connection connection) throws SQLException {
        SqlHelperUtils.createTableIfNotExists(
                connection,
                "test_user",
                "create table test_user (id int primary key)"
        );
    }
}
