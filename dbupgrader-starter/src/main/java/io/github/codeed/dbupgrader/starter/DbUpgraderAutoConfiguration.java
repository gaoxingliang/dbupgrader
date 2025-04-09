package io.github.codeed.dbupgrader.starter;

import io.github.codeed.dbupgrader.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.jdbc.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.lookup.*;

import javax.sql.*;
import java.util.*;

@Slf4j
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({DbUpgrader.class, DataSource.class})
@EnableConfigurationProperties(DbUpgraderProperties.class)
@ConditionalOnProperty(prefix = "dbupgrader", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DbUpgraderAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    public DbUpgraderInitializer dbUpgraderInitializer(ObjectProvider<DataSource> dataSourceProvider, DbUpgraderProperties properties,
                                                       DbUpgraderConfigurer configurer) {
        return new DbUpgraderInitializer(dataSourceProvider, properties, configurer);
    }

    @Bean
    @ConditionalOnBean(AbstractRoutingDataSource.class)
    public MultiDataSourceDbUpgraderInitializer multiDataSourceDbUpgraderInitializer(ObjectProvider<AbstractRoutingDataSource> routingDataSourceProvider, DbUpgraderProperties properties, DbUpgraderConfigurer configurer) {
        return new MultiDataSourceDbUpgraderInitializer(routingDataSourceProvider, properties, configurer);
    }

    @Bean
    @ConditionalOnMissingBean(DbUpgraderConfigurer.class)
    public DbUpgraderConfigurer dbUpgraderConfigurer() {
        return new DbUpgraderConfigurer() {
            @Override
            public void configureUpgradeProperties(String dataSourceName, DataSource dataSource, DbUpgraderProperties.DataSourceConfig dataSourceConfig) {
            }
        };
    }

    private static class DbUpgraderInitializer {
        private final ObjectProvider<DataSource> dataSourceProvider;
        private final DbUpgraderConfigurer configurer;
        private final DbUpgraderProperties properties;

        public DbUpgraderInitializer(ObjectProvider<DataSource> dataSourceProvider, DbUpgraderProperties properties,
                                     DbUpgraderConfigurer configurer) {
            this.dataSourceProvider = dataSourceProvider;
            this.configurer = configurer;
            this.properties = properties;
            initialize();
        }

        private void initialize() {
            DataSource dataSource = dataSourceProvider.getIfAvailable();
            if (dataSource == null) {
                log.warn("No DataSource found, skipping DbUpgrader initialization");
                return;
            }

            DbUpgraderProperties.DataSourceConfig config = properties.getDataSources().get("default");
            if (config == null) {
                log.warn("No default DataSourceConfig found, skipping DbUpgrader initialization");
                return;
            }

            if (!config.isEnabled()) {
                log.info("Default datasource dbupgrader is disable");
                return;
            }

            configurer.configureUpgradeProperties("default", dataSource, config);

            try {
                UpgradeConfiguration upgradeConfig = fromConfig(config, properties);
                DbUpgrader upgrader = new DbUpgrader("default", dataSource, upgradeConfig);
                upgrader.upgrade();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize DbUpgrader", e);
            }
        }
    }

    private static class MultiDataSourceDbUpgraderInitializer {
        private final ObjectProvider<AbstractRoutingDataSource> routingDataSourceProvider;
        private final DbUpgraderConfigurer configurer;
        private final DbUpgraderProperties properties;

        public MultiDataSourceDbUpgraderInitializer(ObjectProvider<AbstractRoutingDataSource> routingDataSourceProvider,
                                                    DbUpgraderProperties properties, DbUpgraderConfigurer configurer) {
            this.routingDataSourceProvider = routingDataSourceProvider;
            this.configurer = configurer;
            this.properties = properties;
            initialize();
        }

        private void initialize() {
            AbstractRoutingDataSource routingDataSource = routingDataSourceProvider.getIfAvailable();
            if (routingDataSource == null) {
                return;
            }

            Map<Object, DataSource> targetDataSources = new HashMap<>();
            try {
                java.lang.reflect.Field field = AbstractRoutingDataSource.class.getDeclaredField("targetDataSources");
                field.setAccessible(true);
                targetDataSources = (Map<Object, DataSource>) field.get(routingDataSource);
            } catch (Exception e) {
                log.error("Failed to get targetDataSources from AbstractRoutingDataSource", e);
                return;
            }

            Map<String, DataSource> dataSources = new HashMap<>();
            for (Map.Entry<Object, DataSource> entry : targetDataSources.entrySet()) {
                dataSources.put(entry.getKey().toString(), entry.getValue());
            }


            for (Map.Entry<String, DbUpgraderProperties.DataSourceConfig> entry : properties.getDataSources().entrySet()) {
                String dataSourceName = entry.getKey();
                DbUpgraderProperties.DataSourceConfig config = entry.getValue();

                if (!config.isEnabled()) {
                    log.info("DataSource {} dbupgrader is disabled", dataSourceName);
                    continue;
                }

                DataSource targetDataSource = targetDataSources.get(dataSourceName);
                if (targetDataSource == null) {
                    log.warn("DataSource not found for name: {}", dataSourceName);
                    continue;
                }

                configurer.configureUpgradeProperties(dataSourceName, targetDataSource, config);

                try {
                    UpgradeConfiguration upgradeConfig = fromConfig(config, properties);
                    DbUpgrader upgrader = new DbUpgrader(dataSourceName, targetDataSource, upgradeConfig);
                    upgrader.upgrade();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize DbUpgrader for datasource: " + dataSourceName, e);
                }
            }
        }
    }

    private static UpgradeConfiguration fromConfig(DbUpgraderProperties.DataSourceConfig config, DbUpgraderProperties props) {
        UpgradeConfiguration.Builder builder = UpgradeConfiguration.builder()
                        .application(props.getApplication())
                        .upgradeClassPackage(config.getUpgradeClassPackage())
                        .targetVersion(config.getTargetVersion())
                        .upgradeHistoryTable(config.getUpgradeHistoryTable())
                        .upgradeConfigurationTable(config.getUpgradeConfigurationTable())
                        .dryRun(config.isDryRun())
                        .potentialMissVersionCount(config.getPotentialMissVersionCount());
        if (config.getSkipClasses() != null && !config.getSkipClasses().isEmpty()) {
            config.getSkipClasses().forEach(builder::addSkipClass);
        }

        return builder.build();
    }
}