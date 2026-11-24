package io.github.rowsmith.internal.plan;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

interface Connections {

    Connection get() throws SQLException;

    void release(Connection c);

    boolean isExternalTransaction(Connection c);

    static void rollbackQuietly(Connection c) {
        quietly(c::rollback);
    }

    static void enableAutoCommitQuietly(Connection c) {
        quietly(() -> c.setAutoCommit(true));
    }

    static void closeQuietly(Connection c) {
        quietly(c::close);
    }

    @SuppressWarnings("EmptyCatch")
    private static void quietly(SqlAction action) {
        try {
            action.run();
        } catch (SQLException ignored) {
        }
    }

    @FunctionalInterface
    interface SqlAction {
        void run() throws SQLException;
    }

    static Connections of(DataSource ds) {
        try {
            Class.forName("org.springframework.jdbc.datasource.DataSourceUtils", false, Connections.class.getClassLoader());
            return new SpringConnections(ds);
        } catch (ClassNotFoundException | LinkageError e) {
            return new Plain(ds);
        }
    }

    record Plain(DataSource ds) implements Connections {
        @Override public Connection get() throws SQLException { return ds.getConnection(); }

        @Override public void release(Connection c) { closeQuietly(c); }

        @Override public boolean isExternalTransaction(Connection c) { return false; }
    }
}
