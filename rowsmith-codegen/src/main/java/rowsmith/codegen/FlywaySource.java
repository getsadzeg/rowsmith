package rowsmith.codegen;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.postgresql.core.Utils;

/**
 * Runs Flyway migrations, then drops Flyway's schema history table so only the application's
 * tables are generated.
 *
 * @param locations Flyway locations, e.g. {@code filesystem:src/main/resources/db/migration} or {@code classpath:db/migration}
 */
public record FlywaySource(List<String> locations) implements SchemaSource {

    public FlywaySource {
        locations = List.copyOf(locations);
    }

    @Override
    public void apply(DataSource dataSource) throws SQLException {
        // Flyway finds its Postgres support with this class loader (not the thread's, which a build tool may set).
        Flyway flyway = Flyway.configure(FlywaySource.class.getClassLoader())
                .dataSource(dataSource)
                .locations(locations.toArray(String[]::new))
                .load();
        flyway.migrate();
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP TABLE " + Utils.escapeIdentifier(null, flyway.getConfiguration().getTable()));
        }
    }
}
