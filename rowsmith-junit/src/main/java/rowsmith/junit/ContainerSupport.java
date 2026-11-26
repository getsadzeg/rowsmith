package rowsmith.junit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import rowsmith.ErrorCode;
import rowsmith.RowsmithException;

final class ContainerSupport {

    private static final Map<String, DataSource> DATASOURCES = new ConcurrentHashMap<>();

    private ContainerSupport() {}

    static boolean available() {
        try {
            Class.forName("org.testcontainers.containers.JdbcDatabaseContainer", false, ContainerSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    static boolean isContainer(Class<?> type) {
        return JdbcDatabaseContainer.class.isAssignableFrom(type);
    }

    static DataSource dataSource(Object container) {
        JdbcDatabaseContainer<?> c = (JdbcDatabaseContainer<?>) container;
        if (!c.isRunning()) {
            throw new RowsmithException(ErrorCode.NO_DATASOURCE, c.getClass().getSimpleName()
                    + " is not started; start it before the tests, e.g. a static @Container field");
        }
        return DATASOURCES.computeIfAbsent(c.getJdbcUrl() + "|" + c.getUsername(), k -> {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setURL(c.getJdbcUrl());
            ds.setUser(c.getUsername());
            ds.setPassword(c.getPassword());
            return ds;
        });
    }
}
