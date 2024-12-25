package io.github.gaoxingliang.dbupgrader;

import com.google.common.base.Preconditions;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

@Getter
public class UpgradeConfiguration {
    /**
     * required
     */
    private String upgradeClassPackage;
    /**
     * should > 0
     */
    private Integer targetVersion;

    /**
     * optional
     */
    private String upgradeHistoryTable = "db_upgrade_history";
    private String upgradeConfigurationTable = "db_upgrade_configuration";
    private String createHistoryTableSql = "CREATE TABLE %s (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "class_name VARCHAR(100) NOT NULL, " +
            "gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "UNIQUE KEY uk_class_name (class_name)" +
            ")";
    private String createConfigurationTableSql = "CREATE TABLE %s (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "key_name VARCHAR(100) NOT NULL, " +
            "value VARCHAR(500) NOT NULL, " +
            "gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
            "UNIQUE KEY uk_key_name (key_name)" +
            ")";
    private boolean dryRun = false;

    public static final String CONFIG_CURRENT_VERSION = "current_version";

    private UpgradeConfiguration() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UpgradeConfiguration config = new UpgradeConfiguration();

        public Builder upgradeClassPackage(String upgradeClassPackage) {
            config.upgradeClassPackage = upgradeClassPackage;
            return this;
        }

        public Builder upgradeHistoryTable(String upgradeHistoryTable) {
            config.upgradeHistoryTable = upgradeHistoryTable;
            return this;
        }

        public Builder upgradeConfigurationTable(String upgradeConfigurationTable) {
            config.upgradeConfigurationTable = upgradeConfigurationTable;
            return this;
        }

        public Builder targetVersion(Integer targetVersion) {
            config.targetVersion = targetVersion;
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            config.dryRun = dryRun;
            return this;
        }

        public Builder createHistoryTableSql(String createHistoryTableSql) {
            config.createHistoryTableSql = createHistoryTableSql;
            return this;
        }

        public Builder createConfigurationTableSql(String createConfigurationTableSql) {
            config.createConfigurationTableSql = createConfigurationTableSql;
            return this;
        }

        public UpgradeConfiguration build() {
            // Validate required fields
            Preconditions.checkArgument(StringUtils.isNotEmpty(config.upgradeClassPackage), 
                "upgradeClassPackage must be set");
            Preconditions.checkArgument(config.targetVersion != null, 
                "targetVersion must be set");
            Preconditions.checkArgument(config.targetVersion > 0,
                "targetVersion must be > 0");
            Preconditions.checkArgument(StringUtils.isNotEmpty(config.upgradeHistoryTable), 
                "upgradeHistoryTable must not be empty");
            Preconditions.checkArgument(StringUtils.isNotEmpty(config.upgradeConfigurationTable), 
                "upgradeConfigurationTable must not be empty");
            Preconditions.checkArgument(StringUtils.isNotEmpty(config.createConfigurationTableSql),
                    "createConfigurationTableSql must not be empty");
            Preconditions.checkArgument(StringUtils.isNotEmpty(config.createHistoryTableSql),
                    "createHistoryTableSql must not be empty");


            return config;
        }
    }
}
