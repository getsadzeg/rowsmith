package io.github.rowsmith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class Pg {

    private static final PostgreSQLContainer CONTAINER;
    private static final Map<String, PostgreSQLContainer> EXTRA = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNTER = new AtomicInteger();

    static {
        String image = System.getProperty("rowsmith.test.pgImage", "postgres:17-alpine");
        CONTAINER = new PostgreSQLContainer(image);
        CONTAINER.start();
    }

    private Pg() {}

    public static Clock clockAt(LocalDate day) {
        return Clock.fixed(day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    public static String image() {
        return CONTAINER.getDockerImageName();
    }

    public static DataSource freshDatabaseOn(String image) {
        PostgreSQLContainer c = EXTRA.computeIfAbsent(image, i -> {
            PostgreSQLContainer pc = new PostgreSQLContainer(DockerImageName.parse(i).asCompatibleSubstituteFor("postgres"));
            pc.start();
            return pc;
        });
        return freshDatabase(c);
    }

    public static DataSource freshDatabase() {
        return freshDatabase(CONTAINER);
    }

    private static DataSource freshDatabase(PostgreSQLContainer CONTAINER) {
        String name = "t" + ProcessHandle.current().pid() + "_" + COUNTER.incrementAndGet();
        try (Connection c = DriverManager.getConnection(CONTAINER.getJdbcUrl(), CONTAINER.getUsername(), CONTAINER.getPassword());
             Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(CONTAINER.getJdbcUrl().replaceAll("/[^/?]+(\\?|$)", "/" + name + "$1"));
        ds.setUser(CONTAINER.getUsername());
        ds.setPassword(CONTAINER.getPassword());
        return ds;
    }

    public static DataSource withSchema(String sql) {
        DataSource ds = freshDatabase();
        exec(ds, sql);
        return ds;
    }

    public static DataSource withResource(String resource) {
        try (var in = Pg.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IllegalArgumentException("missing resource " + resource);
            return withSchema(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void exec(DataSource ds, String sql) {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static int serverMajorVersion(DataSource ds) {
        try (Connection c = ds.getConnection()) {
            return c.getMetaData().getDatabaseMajorVersion();
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static long count(DataSource ds, String table) {
        return queryLong(ds, "SELECT count(*) FROM " + table);
    }

    public static long queryLong(DataSource ds, String sql) {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement(); var rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static String queryString(DataSource ds, String sql) {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement(); var rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
