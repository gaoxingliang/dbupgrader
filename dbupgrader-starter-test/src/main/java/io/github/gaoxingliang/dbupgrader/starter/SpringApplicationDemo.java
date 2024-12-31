package io.github.gaoxingliang.dbupgrader.starter;

import io.github.gaoxingliang.dbupgrader.utils.*;
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
        ReflectionUtils.getClasses("io.github.gaoxingliang.dbupgrader.starter.test.upgrades");
        return null;
    }
}
