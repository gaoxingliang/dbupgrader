package io.github.gaoxingliang.dbupgrader;

public class Main {
    public static void main(String[] args) {
        String x = "CREATE TABLE %s (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "key_name VARCHAR(100) NOT NULL, " +
                "value VARCHAR(500) NOT NULL, " +
                "gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "UNIQUE KEY uk_key_name (key_name)" +
                ")";
        System.out.println(x);
    }
}
