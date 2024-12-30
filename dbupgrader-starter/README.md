# DbUpgrader Spring Boot Starter

This Spring Boot starter provides auto-configuration for DbUpgrader, supporting both Spring Boot 2 and 3. It enables automatic database schema management with support for multiple datasources.

## Features

- Auto-configuration for DbUpgrader
- Support for Spring Boot 2.x and 3.x
- Multiple datasource support
- Flexible configuration options
- Automatic version management

## Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>io.github.gaoxingliang</groupId>
    <artifactId>dbupgrader-starter</artifactId>
    <version>${version}</version>
</dependency>
```

Or with Gradle:

```groovy
implementation 'io.github.gaoxingliang:dbupgrader-starter:${version}'
```

## Configuration

### Basic Configuration

Add the following to your `application.yml` or `application.properties`:

```yaml
dbupgrader:
  enabled: true
  upgrade-class-package: com.example.upgrades
  target-version: 1
  upgrade-history-table: db_upgrade_history
  upgrade-configuration-table: db_upgrade_configuration
  dry-run: false
  potential-miss-version-count: 10
```

### Multiple Datasource Configuration

For multiple datasources, configure them like this:

```yaml
dbupgrader:
  data-sources:
    primary:
      enabled: true
      target-version: 2
      upgrade-class-package: com.example.upgrades.primary
    secondary:
      enabled: true
      target-version: 1
      upgrade-class-package: com.example.upgrades.secondary
```

## Usage

1. Create your upgrade classes:

```java
@DbUpgrade(version = 1)
public class V1CreateUserTable implements UpgradeProcess {
    @Override
    public void upgrade(DbUpgrader migrator, Connection connection) throws SQLException {
        SqlHelperUtils.executeUpdate(connection,
            "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100))");
    }
}
```

2. The starter will automatically detect and run your upgrades on application startup.

## Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `dbupgrader.enabled` | Enable/disable auto-configuration | true |
| `dbupgrader.upgrade-class-package` | Package containing upgrade classes | - |
| `dbupgrader.target-version` | Target version to upgrade to | - |
| `dbupgrader.upgrade-history-table` | Table name for upgrade history | db_upgrade_history |
| `dbupgrader.upgrade-configuration-table` | Table name for upgrade configuration | db_upgrade_configuration |
| `dbupgrader.dry-run` | Simulate upgrades without executing | false |
| `dbupgrader.potential-miss-version-count` | Number of recent versions to check for missed upgrades | 10 |

### Datasource-specific Properties

For each datasource under `dbupgrader.data-sources.<name>`:

| Property | Description | Default |
|----------|-------------|---------|
| `enabled` | Enable/disable for this datasource | true |
| `target-version` | Override global target version | - |
| `upgrade-class-package` | Override global upgrade class package | - |

## Examples

### Single Datasource

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: user
    password: pass

dbupgrader:
  enabled: true
  upgrade-class-package: com.example.upgrades
  target-version: 1
```

### Multiple Datasources

```yaml
spring:
  datasource:
    primary:
      url: jdbc:mysql://localhost:3306/db1
      username: user1
      password: pass1
    secondary:
      url: jdbc:mysql://localhost:3306/db2
      username: user2
      password: pass2

dbupgrader:
  enabled: true
  upgrade-class-package: com.example.upgrades
  target-version: 1
  data-sources:
    primary:
      enabled: true
      target-version: 2
      upgrade-class-package: com.example.upgrades.primary
    secondary:
      enabled: true
      target-version: 1
      upgrade-class-package: com.example.upgrades.secondary
``` 