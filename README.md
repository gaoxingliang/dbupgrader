# Database Upgrader

A lightweight database version management tool that helps you manage database schema changes easily and safely.

## Features

- Simple and intuitive API
- Give the developer full control of upgrade process
- Automatic database version upgrades
- Annotation-based version definition
- Version dependency management
- Automatic upgrade sequence handling
- SQL execution tracking and version control

## Transaction management
For each version, different upgrade processes will execute in same transaction. Note in mysql, some ALTER, CREATE sql will trigger a transaction commit automatically.

## Quick Start
full example: [DbUpgradeExample](./src/test/java/io/github/codeed/dbupgrader/DbUpgradeExample.java)

### 1. Add Dependency
see latest version: [maven central](https://mvnrepository.com/artifact/io.gitee.codeed/dbupgrader)

Gradle:

```groovy
dependencies {
	implementation group: 'io.gitee.codeed', name: 'dbupgrader', version: '${version}'
}
```

Mvn:

```xml
<!-- https://mvnrepository.com/artifact/io.gitee.codeed/dbupgrader -->
<dependency>
    <groupId>io.gitee.codeed</groupId>
    <artifactId>dbupgrader</artifactId>
    <version>${version}</version>
</dependency>
```



### 2. Define Your Upgrades

```java
@DbUpgrade(version = 1, after = "V1AddTableUser")
public class V1AddAdminRecord implements UpgradeProcess{
    @Override
    public void upgrade(DbUpgrader migrator, Connection connection) throws SQLException {
        SqlHelperUtils.executeUpdate(connection,
                "insert into test_user values (123)");
    }
}
```

### 3. Run the Upgrade

```java
UpgradeConfiguration config = UpgradeConfiguration.builder()
    .upgradeClassPackage("com.example.upgrades")
    .jdbcUrl("jdbc:mysql://localhost:3306/yourdb")
    .user("username")
    .password("password")
    .targetVersion(1)
    .build();

DbUpgrader upgrader = new DbUpgrader("example", dataSource, config);
upgrader.upgrade();
```

## Tricky snippets for mysql
Use `SqlHelperUtils.executeUpdate` to run the sql.
### create table if not exists
```sql
CREATE TABLE IF NOT EXISTS XX (id int);
```
or java code `SqlHelperUtils#createTableIfNotExists`

### add column if not exists

<font color='red'>note if you are using alibaba druid, the below sql will cause an error. please use the java code WITHOUT IF NOT EXISTS </font> see tracking issue: https://github.com/alibaba/druid/issues/6067

```sql
ALTER TABLE XX ADD COLUMN IF NOT EXISTS name VARCHAR(100);
```

Or java code `SqlHelperUtils#smartAddColumn("ALTER TABLE XX ADD COLUMN YYY VARCHAR(100)")`

### insert records if not exists

```sql
INSERT IGNORE INTO xxx (id, name) values (xx, yy)
```

or java code:  `SqlHelperUtils#smartInsertWithPrimaryKeySet`

## Quick start for springboot

1、import the springboot starter:

see latest version: [maven central](https://mvnrepository.com/artifact/io.gitee.codeed/dbupgrader-starter)

Gradle:

```groovy
dependencies {
   // https://mvnrepository.com/artifact/io.gitee.codeed/dbupgrader
	implementation group: 'io.gitee.codeed', name: 'dbupgrader-starter', version: '0.0.1'
}
```

Mvn:

```xml
<!-- https://mvnrepository.com/artifact/io.gitee.codeed/dbupgrader -->
<dependency>
    <groupId>io.gitee.codeed</groupId>
    <artifactId>dbupgrader-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

2、application.yml

Option a, set the targetVersion in yaml:

```yaml
dbupgrader:
  enabled: true
  application: server
  dataSources:
    default:
      enabled: true
      targetVersion: 1
      upgradeClassPackage: com.example.upgrades.master
```

Option b, set the targetVersion in code:
create a bean DbUpgraderConfigurer:
```java
    @Bean
    public DbUpgraderConfigurer dbUpgraderConfigurer() {
        return new DbUpgraderConfigurer() {
            @Override
            public void configureUpgradeProperties(String dataSourceName, DataSource dataSource,
                                                   DbUpgraderProperties.DataSourceConfig dataSourceConfig) {
                if (dataSourceName.equals("default")) {
                    dataSourceConfig.setTargetVersion(DatabaseSchemaVersion.VERSION);
                }
            }
        };
    }
```


## UpgradeConfiguration

source code [UpgradeConfiguration](./src/main/java/io/github/codeed/dbupgrader/UpgradeConfiguration.java)

| Name | Required | Default value | Comment |
| ---- | -------- | ------------- | ------- |
| upgradeClassPackage | Yes | - | Package path where upgrade classes are located |
| targetVersion | Yes | - | Target version number to upgrade to (must be > 0) |
| application | Yes | - | Set a reasonable application name eg `user-server`. This will help when different services share same database. |
| upgradeHistoryTable | No | db_upgrade_history | Table name for storing upgrade history |
| upgradeConfigurationTable | No | db_upgrade_configuration | Table name for storing upgrade configuration |
| createHistoryTableSql | No | `CREATE TABLE %s (id BIGINT AUTO_INCREMENT PRIMARY KEY, application VARCHAR(100) NOT NULL,class_name VARCHAR(200) NOT NULL, gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_class_name (class_name))` | SQL for creating history table if not exists. It has a placeholder for the table name if needed. |
| createConfigurationTableSql | No | `CREATE TABLE %s (id BIGINT AUTO_INCREMENT PRIMARY KEY, key_name VARCHAR(100) NOT NULL, value VARCHAR(500) NOT NULL, gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP, gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, UNIQUE KEY uk_key_name (key_name))` | SQL for creating configuration table if not exists. It has a placeholder for the table name if needed. |
| dryRun | No | false | If true, will only simulate the upgrade without executing |
| potentialMissVersionCount | No | 10 | In case of we missed some upgrade process, we will recheck recent version records and execute it if missed. for example, two branch may share a same target version and someone merged the branch to master, and upgrade it. while some other still use the old target version, and the upgrade process is missed. Recommendation: if you may have a long-running project/epic/feature, you may want to set this to a larger number.  If <=0, we won't check that. |


## Development Setup

### Test Database

To set up a test database using Docker:

```bash
docker run --name test-mysql \
    -e MYSQL_ROOT_PASSWORD=root123 \
    -e MYSQL_DATABASE=testdb \
    -p 13306:3306 \
    -d mysql:8.0
```

### Internal tables
- `db_upgrade_history` records all executed scripts
- `db_upgrade_configuration` records the current version and etc