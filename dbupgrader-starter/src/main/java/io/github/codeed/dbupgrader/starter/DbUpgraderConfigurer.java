package io.github.codeed.dbupgrader.starter;

import javax.sql.*;

/**
 * Interface for configuring DbUpgrader before it's used
 */
public interface DbUpgraderConfigurer {
    /**
     * Configure DbUpgrader properties  for a datasource.
     * @param dataSourceName
     * @param dataSource
     * @param dataSourceConfig
     */
    void configureUpgradeProperties(String dataSourceName, DataSource dataSource, DbUpgraderProperties.DataSourceConfig dataSourceConfig);
} 