# Database Upgrader

A lightweight database version management tool that helps you manage database schema changes easily and safely.

## Features

- Simple and intuitive API
- Automatic database version upgrades
- Annotation-based version definition
- Version dependency management
- Automatic upgrade sequence handling
- SQL execution tracking and version control

## Quick Start

### 1. Add Dependency

```groovy
dependencies {
    implementation 'io.github.gaoxingliang:db-upgrader:1.0.0'
}
```

### 2. Define Your Upgrades

```java
@DbUpgrade(version = 1)
public class InitTableUpgrade {
    @UpgradeStep
    public void createTable(Connection connection) {
        String sql = "CREATE TABLE users (" +
                    "id INT PRIMARY KEY," +
                    "name VARCHAR(255)" +
                    ")";
        // Execute your SQL
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

DbUpgrader.upgrade(config);
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

## Documentation

For more detailed information, please check our [Wiki](https://github.com/yourusername/db-upgrader/wiki).

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

# 数据库升级管理工具

一个轻量级的数据库版本管理工具，帮助您轻松安全地管理数据库架构变更。

## 特性

- 简单直观的API
- 自动数据库版本升级
- 基于注解的版本定义
- 版本依赖关系管理
- 自动处理升级顺序
- SQL执行跟踪和版本控制

## 快速开始

### 1. 添加依赖

```groovy
dependencies {
    implementation 'io.github.gaoxingliang:db-upgrader:1.0.0'
}
```

### 2. 定义升级步骤

```java
@DbUpgrade(version = 1)
public class InitTableUpgrade {
    @UpgradeStep
    public void createTable(Connection connection) {
        String sql = "CREATE TABLE users (" +
                    "id INT PRIMARY KEY," +
                    "name VARCHAR(255)" +
                    ")";
        // 执行SQL
    }
}
```

### 3. 执行升级

```java
UpgradeConfiguration config = UpgradeConfiguration.builder()
    .upgradeClassPackage("com.example.upgrades")
    .jdbcUrl("jdbc:mysql://localhost:3306/yourdb")
    .user("username")
    .password("password")
    .targetVersion(1)
    .build();

DbUpgrader.upgrade(config);
```

## 开发环境设置

### 测试数据库

使用Docker设置测试数据库：

```bash
docker run --name test-mysql \
    -e MYSQL_ROOT_PASSWORD=root123 \
    -e MYSQL_DATABASE=testdb \
    -p 13306:3306 \
    -d mysql:8.0
```

## 文档

更详细的信息请查看我们的 [Wiki](https://github.com/yourusername/db-upgrader/wiki)。

## 许可证

本项目采用 MIT 许可证 - 详情请见 LICENSE 文件。