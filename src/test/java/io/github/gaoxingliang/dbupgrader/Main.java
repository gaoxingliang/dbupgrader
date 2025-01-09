package io.github.gaoxingliang.dbupgrader;

import io.github.gaoxingliang.dbupgrader.utils.*;

public class Main {
    public static void main(String[] args) throws Exception {
        SqlHelperUtils.smartInsertWithPrimaryKeySet(null, "insert into students (id, name) values (1, 'Tom'),(2, '3')");
    }
}
