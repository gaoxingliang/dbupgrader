package io.github.codeed.dbupgrader.starter;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;

import javax.sql.*;

@SpringBootApplication
public class SpringApplicationDemo {
    public static void main(String[] args) {
        SpringApplication.run(SpringApplicationDemo.class, args);
    }

    @Bean
    public DataSource dataSource() throws Exception {
        ReflectionUtils.getClasses("io.github.codeed.dbupgrader.starter.test.upgrades");
        return null;
    }
}
