package io.github.gaoxingliang.dbupgrader.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "dbupgrader")
public class DbUpgraderProperties {
    /**
     * Multiple datasource configurations
     * Key: datasource name, Value: datasource specific configuration
     */
    private Map<String, DataSourceConfig> dataSources;

    @Data
    public static class DataSourceConfig {
        /**
         * Whether to enable dbupgrader for this datasource
         */
        private boolean enabled = true;

        /**
         * Target version for this specific datasource
         * If not set, will use the global targetVersion
         */
        private Integer targetVersion;

        /**
         * Upgrade class package for this specific datasource
         */
        private String upgradeClassPackage;

        /**
         * Table name for storing upgrade history
         */
        private String upgradeHistoryTable = "db_upgrade_history";

        /**
         * Table name for storing upgrade configuration
         */
        private String upgradeConfigurationTable = "db_upgrade_configuration";

        /**
         * If true, will only simulate the upgrade without executing
         */
        private boolean dryRun = false;

        /**
         * In case of missed upgrade process, recheck recent version records
         */
        private int potentialMissVersionCount = 10;
    }
} 