package io.github.codeed.dbupgrader.starter;

import lombok.*;
import org.springframework.boot.context.properties.*;

import javax.sql.*;
import java.util.*;

@Data
@ConfigurationProperties(prefix = "dbupgrader")
public class DbUpgraderProperties {
    /**
     * Multiple datasource configurations
     * Key: datasource name, Value: datasource specific configuration
     */
    private Map<String, DataSourceConfig> dataSources;

    /**
     * Application name should be unique in your organization.
     * To avoid issues when different projects use same database.
     */
    private String application;

    @Data
    public static class DataSourceConfig {
        /**
         * Whether to enable dbupgrader for this datasource
         */
        private boolean enabled = false;

        /**
         * Package path where upgrade classes are located
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

        /**
         * Target version for this specific datasource. It can be set in external configurer.
         * {@link DbUpgraderConfigurer#configureUpgradeProperties(String, DataSource, DataSourceConfig)}
         */
        private Integer targetVersion;

        /**
         * skipped upgrade class names {@link Class#getCanonicalName()}
         */
        private List<String> skipClasses;
    }
} 