package io.github.gaoxingliang.dbupgrader.upgradescripts;

import io.github.gaoxingliang.dbupgrader.*;
import io.github.gaoxingliang.dbupgrader.utils.*;

import java.sql.*;

// we set the maxAffectRecords to 5, so if the script update more than 5 records, it will rollback and throw an exception
@DbUpgrade(version = 2, maxAffectRecords = 50 /*change this to make it pass*/)
public class V2AddTableStudent implements UpgradeProcess{
    @Override
    public void upgrade(DbUpgrader migrator, Connection connection) throws SQLException {
        System.out.println("Add a new table dummy for V2AddTableStudent ");
        int maxInsert = 10;
        for (int i = 0; i < maxInsert; i++) {
            SqlHelperUtils.executeUpdate(connection,
                    String.format("insert into test_user values (%d)", 123 + i));
        }
    }
}
