package io.github.gaoxingliang.dbupgrader.upgradescripts;

import io.github.gaoxingliang.dbupgrader.*;
import io.github.gaoxingliang.dbupgrader.utils.*;

import java.sql.*;

@DbUpgrade(version = 1, after = "V1AddTableUser")
public class V1AddAdminRecord implements UpgradeProcess{
    @Override
    public void upgrade(DbUpgrader migrator, Connection connection) throws SQLException {
        SqlHelperUtils.executeUpdate(connection,
                "insert into test_user values (123)");
    }
}
