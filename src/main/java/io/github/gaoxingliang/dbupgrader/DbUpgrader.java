package io.github.gaoxingliang.dbupgrader;

import io.github.gaoxingliang.dbupgrader.utils.*;
import lombok.extern.java.*;
import org.apache.commons.lang3.*;

import javax.sql.*;
import java.sql.*;
import java.util.*;

@Log
public class DbUpgrader {
    private final String name;
    private final DataSource dataSource;
    private final UpgradeConfiguration upgradeConfiguration;

    public DbUpgrader(String name, DataSource ds, UpgradeConfiguration upgradeConfiguration) {
        this.name = name;
        this.dataSource = ds;
        this.upgradeConfiguration = upgradeConfiguration;
    }

    public void upgrade() throws Exception {
        log.info("Upgrade started for " + name);
        // 1 init setup stuffs
        List<Class> classList = ReflectionUtils.getClasses(upgradeConfiguration.getUpgradeClassPackage());
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        createConfigurationTableIfNotExists(conn, upgradeConfiguration.getUpgradeConfigurationTable());
        createUpgradeHistoryTableIfNotExists(conn, upgradeConfiguration.getUpgradeHistoryTable());
        int currentVer = getCurrentVersion(conn);
        int targetVer = upgradeConfiguration.getTargetVersion();
        TreeMap<Integer, Map<Class, DbUpgrade>> upgradeList = new TreeMap<>();
        for (Class c : classList) {
            DbUpgrade upgrade = (DbUpgrade) c.getDeclaredAnnotation(DbUpgrade.class);
            if (upgrade != null) {
                upgradeList.computeIfAbsent(upgrade.version(), k -> new HashMap<>()).put(c, upgrade);
            }
        }
        conn.commit();
        SqlHelperUtils.closeQuietly(conn);
        log.info("Scanned total versions: " + upgradeList.size());
        if (upgradeConfiguration.getPotentialMissVersionCount() > 0) {
            checkPotentialMissedUpgrade(currentVer, upgradeList);
        }

        log.info("Will try to upgrade from " + currentVer + " to " + targetVer);

        // 2 check and do upgrades
        // any version left and is not processed?
        while (currentVer <= targetVer) {
            Map<Class, DbUpgrade> needUpgradeOfVersion = upgradeList.get(currentVer);
            if (needUpgradeOfVersion != null && !needUpgradeOfVersion.isEmpty()) {
                executeUpgrades(needUpgradeOfVersion, currentVer, true);
            }
            currentVer++;
        }
        log.info("Upgrade finished for " + name);
    }

    private void checkPotentialMissedUpgrade(int currentVer, TreeMap<Integer, Map<Class, DbUpgrade>> upgradeList) throws Exception {
        // Check for missed upgrades in recent versions

        log.info("Checking for missed upgrades in recent " + upgradeConfiguration.getPotentialMissVersionCount() + " versions");
        Connection checkConn = dataSource.getConnection();
        checkConn.setAutoCommit(false);
        try {
            // Get versions less than currentVer in descending order
            NavigableMap<Integer, Map<Class, DbUpgrade>> recentVersions = upgradeList.headMap(currentVer, false).descendingMap();

            int checkedVersions = 0;
            for (Map.Entry<Integer, Map<Class, DbUpgrade>> versionEntry : recentVersions.entrySet()) {
                if (checkedVersions >= upgradeConfiguration.getPotentialMissVersionCount()) {
                    break;
                }

                int ver = versionEntry.getKey();
                Map<Class, DbUpgrade> upgradesForVersion = versionEntry.getValue();

                // Check if any upgrade in this version was missed
                boolean hasMissedUpgrades = false;
                for (Class upgradeClass : upgradesForVersion.keySet()) {
                    if (!isUpgradeExecuted(checkConn, upgradeClass.getName())) {
                        log.warning("Found missed upgrade: " + upgradeClass.getName() + " for version " + ver);
                        hasMissedUpgrades = true;
                        break;
                    }
                }

                // If we found missed upgrades, execute all upgrades for this version
                if (hasMissedUpgrades) {
                    executeUpgrades(upgradesForVersion, ver, false);
                }

                checkedVersions++;
            }
            checkConn.commit();
        } catch (Exception e) {
            checkConn.rollback();
            throw e;
        }
        finally {
            SqlHelperUtils.closeQuietly(checkConn);
        }
    }

    private void executeUpgrades(Map<Class, DbUpgrade> needUpgradeOfVersion, int currentVer, boolean updateVersion) throws Exception {
        // Build dependency graph
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Class> classMap = new HashMap<>();

        // Initialize graph with all classes
        for (Map.Entry<Class, DbUpgrade> entry : needUpgradeOfVersion.entrySet()) {
            String className = entry.getKey().getName();
            DbUpgrade upgrade = entry.getValue();
            classMap.put(className, entry.getKey());
            graph.put(className, new HashSet<>());

            // Add dependencies based on after() annotation
            String afterClass = upgrade.after();
            if (StringUtils.isNotEmpty(afterClass)) {
                if (needUpgradeOfVersion.containsKey(Class.forName(upgradeConfiguration.getUpgradeClassPackage() + "." + afterClass))) {
                    graph.get(className).add(afterClass);
                }
            }
        }

        // Topological sort
        List<String> sortedClasses = TopologicalSort.sort(graph);
        if (sortedClasses == null) {
            throw new RuntimeException("Circular dependency detected in upgrade classes for version " + currentVer);
        }

        // Execute upgrades in order
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            for (String className : sortedClasses) {
                Class clazz = classMap.get(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (instance instanceof UpgradeProcess) {
                    if (upgradeConfiguration.isDryRun()) {
                        log.info("Execute class " + className + " for version " + currentVer);
                        continue;
                    }
                    if (isUpgradeExecuted(conn, clazz.getName())) {
                        continue;
                    }
                    UpgradeProcess upgrade = (UpgradeProcess) instance;
                    try {
                        upgrade.upgrade(this, conn);
                        // Record successful upgrade
                        SqlHelperUtils.executeUpdate(conn, "insert into " + upgradeConfiguration.getUpgradeHistoryTable() + "(class_name)" +
                                " values (?)", clazz.getName());
                        log.info("Executed a new class " + className);
                    } catch (Exception e) {
                        log.severe("Failed to execute upgrade for class: " + className);
                        throw e;
                    }
                } else {
                    log.warning("The class " + className + " doesn't implement " + UpgradeProcess.class);
                }
            }

            if (updateVersion) {
                // Update current version
                updateCurrentVersion(conn, currentVer);
            }
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        }
        finally {
            SqlHelperUtils.closeQuietly(conn);
        }
    }

    private void updateCurrentVersion(Connection conn, int ver) throws SQLException {
        if (upgradeConfiguration.isDryRun()) {
            log.info("Will tick version to " + ver);
        } else {
            SqlHelperUtils.executeUpdate(conn, "update " + upgradeConfiguration.getUpgradeConfigurationTable() + " set value = ? where " +
                    "key_name=?", ver + "", UpgradeConfiguration.CONFIG_CURRENT_VERSION);
            log.info("Tick version to " + ver);
        }
    }

    private void createUpgradeHistoryTableIfNotExists(Connection conn, String tableName) throws SQLException {
        String createTableSql = String.format(upgradeConfiguration.getCreateHistoryTableSql(), tableName);
        SqlHelperUtils.createTableIfNotExists(conn, tableName, createTableSql);
    }

    private boolean isUpgradeExecuted(Connection conn, String className) throws SQLException {
        String executed = SqlHelperUtils.query(conn, "select class_name from " + upgradeConfiguration.getUpgradeHistoryTable() + " where " +
                "class_name = ?", rs -> rs.getString(1), className);

        return executed != null;
    }

    private void createConfigurationTableIfNotExists(Connection conn, String tableName) throws SQLException {
        String createTableSql = String.format(upgradeConfiguration.getCreateConfigurationTableSql(), tableName);
        SqlHelperUtils.createTableIfNotExists(conn, tableName, createTableSql);
    }


    private int getCurrentVersion(Connection conn) throws SQLException {
        String ver = SqlHelperUtils.query(conn, "select value from " + upgradeConfiguration.getUpgradeConfigurationTable() + " where " +
                "key_name = ?", rs -> rs.getString(1), UpgradeConfiguration.CONFIG_CURRENT_VERSION);
        int version = 0;
        if (ver == null) {
            version = 0;
            SqlHelperUtils.insertWithIdReturned(conn, "insert into " + upgradeConfiguration.getUpgradeConfigurationTable() + "(key_name, " +
                    "value) values (?, ?)", UpgradeConfiguration.CONFIG_CURRENT_VERSION, version + "");
        } else {
            version = Integer.parseInt(ver);
        }

        return version;
    }
}
