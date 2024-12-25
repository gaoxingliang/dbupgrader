package io.github.gaoxingliang.dbupgrader;

import com.mysql.cj.jdbc.*;

public class DbUpgradeExample {
    public static void main(String[] args) throws Exception {
        // Initialize MySQL DataSource
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://localhost:13306/testdb");
        dataSource.setUser("root");
        dataSource.setPassword("root123");
        
        // Configure the upgrade process
        UpgradeConfiguration configuration = UpgradeConfiguration.builder()
                .upgradeClassPackage("io.github.gaoxingliang.dbupgrader.upgradescripts")
                .targetVersion(2)
                .build();
        
        // Create and run the upgrader
        DbUpgrader upgrader = new DbUpgrader("example", dataSource, configuration);
        upgrader.upgrade();
    }
}
