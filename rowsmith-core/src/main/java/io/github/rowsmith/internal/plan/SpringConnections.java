package io.github.rowsmith.internal.plan;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class SpringConnections implements Connections {
    private final DataSource ds;

    SpringConnections(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Connection get() throws SQLException {
        try {
            return DataSourceUtils.getConnection(ds);
        } catch (CannotGetJdbcConnectionException e) {
            if (e.getCause() instanceof SQLException s) throw s;
            throw new SQLException(e.getMessage(), e);
        }
    }

    @Override
    public void release(Connection c) {
        DataSourceUtils.releaseConnection(c, ds);
    }

    @Override
    public boolean isExternalTransaction(Connection c) {
        return TransactionSynchronizationManager.isActualTransactionActive() && DataSourceUtils.isConnectionTransactional(c, ds);
    }
}
