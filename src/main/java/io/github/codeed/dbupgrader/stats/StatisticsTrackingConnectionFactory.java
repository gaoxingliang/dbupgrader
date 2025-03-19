package io.github.codeed.dbupgrader.stats;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class StatisticsTrackingConnectionFactory {
    
    public static Connection createConnection(Connection connection) {
        SqlExecutionStats stats = new SqlExecutionStats();
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] { Connection.class },
            new ConnectionInvocationHandler(connection, stats)
        );
    }

    private static class ConnectionInvocationHandler implements java.lang.reflect.InvocationHandler {
        private final Connection target;
        private final SqlExecutionStats stats;

        public ConnectionInvocationHandler(Connection target, SqlExecutionStats stats) {
            this.target = target;
            this.stats = stats;
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
            Object result = method.invoke(target, args);
            
            // Intercept statement creation methods
            if (result instanceof Statement) {
                if (method.getName().equals("createStatement")) {
                    return Proxy.newProxyInstance(
                        Statement.class.getClassLoader(),
                        new Class<?>[] { Statement.class },
                        new SqlStatementInvocationHandler(result, stats, args)
                    );
                } else if (method.getName().equals("prepareStatement")) {
                    return Proxy.newProxyInstance(
                        PreparedStatement.class.getClassLoader(),
                        new Class<?>[] { PreparedStatement.class },
                        new SqlStatementInvocationHandler(result, stats, args)
                    );
                }
            }
            
            return result;
        }
    }

    public static SqlExecutionStats getStats(Connection connection) {
        if (Proxy.isProxyClass(connection.getClass())) {
            ConnectionInvocationHandler handler = (ConnectionInvocationHandler) Proxy.getInvocationHandler(connection);
            return handler.stats;
        }
        return null;
    }
} 