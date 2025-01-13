package io.github.gaoxingliang.dbupgrader.upgradescripts;

import io.github.gaoxingliang.dbupgrader.*;
import io.github.gaoxingliang.dbupgrader.utils.*;

import java.sql.*;

@DbUpgrade(version = 3)
public class V3SmartInsert implements UpgradeProcess{
    @Override
    public void upgrade(DbUpgrader migrator, Connection connection) throws SQLException {
        SqlHelperUtils.createTableIfNotExists(connection, "students", "create table students (id int primary key, name varchar(100), gmt_created timestamp default now())");
        SqlHelperUtils.smartInsertWithPrimaryKeySet(connection, "insert into students (id, name, gmt_created) values (1, 'Tom', now())");
        // should no error.
        SqlHelperUtils.smartInsertWithPrimaryKeySet(connection, "insert into `testdb`.students (`id`, name) values (1, 'Tom')");
        SqlHelperUtils.smartInsertWithPrimaryKeySet(connection, "insert into students (`id`, name) values (2, 'Tom2'), (3, 'Tom3')");
    }
}
