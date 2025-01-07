package io.github.gaoxingliang.dbupgrader.stats;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;

public class SqlStatementInvocationHandler implements InvocationHandler {
    private final Object target;
    private final SqlExecutionStats stats;
    private final Object [] args;
    public SqlStatementInvocationHandler(Object target, SqlExecutionStats stats, Object [] args) {
        this.target = target;
        this.stats = stats;
        this.args = args;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(target, args);
        String sql = "";
        if (args != null && args.length > 0) {
            sql = args[0].toString();
        } else if (this.args != null && this.args.length > 0) {
            sql =  this.args[0].toString();
        }
        // Track statistics for executeUpdate and execute methods
        if (method.getName().equals("executeUpdate")) {
            int count = (Integer) result;
            updateStats(sql, count);
        } else if (method.getName().equals("execute")) {
            boolean isResultSet = (Boolean) result;
            if (!isResultSet) {
                int count = ((Statement)target).getUpdateCount();
                if (count >= 0) {
                    updateStats(sql, count);
                }
            }
        }
        
        return result;
    }

    private void updateStats(String sql, int count) {
        String upperSql = sql.trim().toUpperCase();
        if (upperSql.startsWith("INSERT")) {
            stats.addInsertedRecords(count);
        } else if (upperSql.startsWith("UPDATE")) {
            stats.addUpdatedRecords(count);
        } else if (upperSql.startsWith("DELETE")) {
            stats.addDeletedRecords(count);
        }
    }
} 