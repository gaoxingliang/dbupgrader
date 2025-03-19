package io.github.codeed.dbupgrader;

import com.mysql.cj.jdbc.*;
import org.junit.jupiter.api.*;

public class DbUpgradeExample {

    @Test
    public void test() throws Exception {
// Initialize MySQL DataSource
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://localhost:13306/testdb");
        dataSource.setUser("root");
        dataSource.setPassword("root123");
        dataSource.setConnectTimeout(3000);

        // Configure the upgrade process
        UpgradeConfiguration configuration = UpgradeConfiguration.builder()
                .upgradeClassPackage("io.github.codeed.dbupgrader.upgradescripts")
                .targetVersion(3)
                .application("server")
                .build();

        // Create and run the upgrader
        DbUpgrader upgrader = new DbUpgrader("example", dataSource, configuration);
        upgrader.upgrade();
    }
}
