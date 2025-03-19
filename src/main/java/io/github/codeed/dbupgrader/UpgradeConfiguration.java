package io.github.codeed.dbupgrader;

import com.google.common.base.Preconditions;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

@Setter
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
     * Application name should be unique in your organization.
     * To avoid issues when different projects use same database.
     */
    private String application;

    /**
     * optional
     */
    private String upgradeHistoryTable = "db_upgrade_history";
    private String upgradeConfigurationTable = "db_upgrade_configuration";
    private String createHistoryTableSql = "CREATE TABLE %s (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "application VARCHAR(100) NOT NULL, " +
            "class_name VARCHAR(200) NOT NULL, " +
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

    /**
     * In case of we missed some upgrade process, we will recheck recent version records and execute it if missed.
     * for example, two branch may share a same target version and someone merged the branch to master, and upgrade it.
     * while some other still use the old target version, and the upgrade process is missed.
     * Recommendation: if you may have a long-running project/epic/feature, you may want to set this to a larger number.
     * If <=0, we won't check that.
     */
    private int potentialMissVersionCount = 10;

    public static final String CONFIG_CURRENT_VERSION = "current_version";

    public UpgradeConfiguration() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UpgradeConfiguration config = new UpgradeConfiguration();

        public Builder upgradeClassPackage(String upgradeClassPackage) {
            if (upgradeClassPackage != null) {
                config.upgradeClassPackage = upgradeClassPackage;
            }
            return this;
        }

        public Builder application(String application) {
            config.application = application;
            return this;
        }

        public Builder upgradeHistoryTable(String upgradeHistoryTable) {
            if (upgradeHistoryTable != null) {
                config.upgradeHistoryTable = upgradeHistoryTable;
            }
            return this;
        }

        public Builder upgradeConfigurationTable(String upgradeConfigurationTable) {
            if (upgradeConfigurationTable != null) {
                config.upgradeConfigurationTable = upgradeConfigurationTable;
            }
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
            if (createHistoryTableSql != null) {
                config.createHistoryTableSql = createHistoryTableSql;
            }
            return this;
        }

        public Builder createConfigurationTableSql(String createConfigurationTableSql) {
            if (createConfigurationTableSql != null) {
                config.createConfigurationTableSql = createConfigurationTableSql;
            }
            return this;
        }

        public Builder potentialMissVersionCount(int potentialMissVersionCount) {
            config.potentialMissVersionCount = potentialMissVersionCount;
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
            Preconditions.checkArgument(StringUtils.isNotEmpty(config.application), "application should be set. eg: server");
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
