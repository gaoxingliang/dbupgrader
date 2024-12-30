package io.github.gaoxingliang.dbupgrader.starter;

import io.github.gaoxingliang.dbupgrader.DbUpgrader;
import io.github.gaoxingliang.dbupgrader.UpgradeConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({DbUpgrader.class, DataSource.class})
@EnableConfigurationProperties(DbUpgraderProperties.class)
@ConditionalOnProperty(prefix = "dbupgrader", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DbUpgraderAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    public DbUpgraderInitializer dbUpgraderInitializer(
            ObjectProvider<DataSource> dataSourceProvider,
            DbUpgraderProperties properties) {
        
        return new DbUpgraderInitializer(dataSourceProvider, properties);
    }

    @Bean
    @ConditionalOnBean(AbstractRoutingDataSource.class)
    public MultiDataSourceDbUpgraderInitializer multiDataSourceDbUpgraderInitializer(
            ObjectProvider<AbstractRoutingDataSource> routingDataSourceProvider,
            DbUpgraderProperties properties) {
            
        return new MultiDataSourceDbUpgraderInitializer(routingDataSourceProvider, properties);
    }

    private static class DbUpgraderInitializer {
        private final ObjectProvider<DataSource> dataSourceProvider;
        private final DbUpgraderProperties properties;

        public DbUpgraderInitializer(ObjectProvider<DataSource> dataSourceProvider,
                                   DbUpgraderProperties properties) {
            this.dataSourceProvider = dataSourceProvider;
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

            try {
                UpgradeConfiguration upgradeConfig = fromYamlConfig(config);
                DbUpgrader upgrader = new DbUpgrader("default", dataSource, upgradeConfig);
                upgrader.upgrade();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize DbUpgrader", e);
            }
        }
    }

    private static class MultiDataSourceDbUpgraderInitializer {
        private final ObjectProvider<AbstractRoutingDataSource> routingDataSourceProvider;
        private final DbUpgraderProperties properties;

        public MultiDataSourceDbUpgraderInitializer(
                ObjectProvider<AbstractRoutingDataSource> routingDataSourceProvider,
                DbUpgraderProperties properties) {
            this.routingDataSourceProvider = routingDataSourceProvider;
            this.properties = properties;
            initialize();
        }

        private void initialize() {
            AbstractRoutingDataSource routingDataSource = routingDataSourceProvider.getIfAvailable();
            if (routingDataSource == null || properties.getDataSources() == null) {
                return;
            }

            Map<Object, DataSource> targetDataSources = new HashMap<>();
            try {
                // Use reflection to get targetDataSources from AbstractRoutingDataSource
                java.lang.reflect.Field field = AbstractRoutingDataSource.class
                        .getDeclaredField("targetDataSources");
                field.setAccessible(true);
                targetDataSources = (Map<Object, DataSource>) field.get(routingDataSource);
            } catch (Exception e) {
                log.error("Failed to get targetDataSources from AbstractRoutingDataSource", e);
                return;
            }

            for (Map.Entry<String, DbUpgraderProperties.DataSourceConfig> entry : 
                    properties.getDataSources().entrySet()) {
                String dataSourceName = entry.getKey();
                DbUpgraderProperties.DataSourceConfig config = entry.getValue();

                if (!config.isEnabled()) {
                    log.info("DataSource {} dbupgrader is disable", dataSourceName);
                    continue;
                }

                DataSource targetDataSource = targetDataSources.get(dataSourceName);
                if (targetDataSource == null) {
                    log.warn("DataSource not found for name: {}", dataSourceName);
                    continue;
                }

                try {
                    UpgradeConfiguration upgradeConfig = fromYamlConfig(config);
                    DbUpgrader upgrader = new DbUpgrader(dataSourceName, targetDataSource, upgradeConfig);
                    upgrader.upgrade();
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to initialize DbUpgrader for datasource: " + dataSourceName, e);
                }
            }
        }
    }

    static UpgradeConfiguration fromYamlConfig(DbUpgraderProperties.DataSourceConfig config) {
        return  UpgradeConfiguration.builder()
                .upgradeClassPackage(config.getUpgradeClassPackage())
                .targetVersion(config.getTargetVersion())
                .upgradeHistoryTable(config.getUpgradeHistoryTable())
                .upgradeConfigurationTable(config.getUpgradeConfigurationTable())
                .dryRun(config.isDryRun())
                .potentialMissVersionCount(config.getPotentialMissVersionCount())
                .build();
    }
} 