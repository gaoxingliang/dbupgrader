package io.github.gaoxingliang.dbupgrader.upgradescripts;

import io.github.gaoxingliang.dbupgrader.*;

import java.sql.*;

@DbUpgrade(version = 2)
public class V2AddTableStudent implements UpgradeProcess{
    @Override
    public void upgrade(DbUpgrader migrator, Connection connection) throws SQLException {
        System.out.println("Add a new table dummy for V2AddTableStudent ");
    }
}
