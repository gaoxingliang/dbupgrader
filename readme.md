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
full example: [DbUpgradeExample](./src/main/test/java/io/github/gaoxingliang/dbupgrader/DbUpgradeExample.java)

### 1. Add Dependency
see latest version:
```groovy
dependencies {
    implementation 'io.gitee.gaoxingliang:db-upgrader:0.0.1'
}
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