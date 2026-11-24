package io.github.rowsmith.codegen;

import com.palantir.javapoet.JavaFile;
import io.github.rowsmith.internal.schema.CatalogReader;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Generates one Java source file with typed table and column constants for Rowsmith, from a
 * schema built by a {@link SchemaSource} in a temporary Postgres (Testcontainers) or read from an
 * existing database.
 *
 * <pre>{@code
 * new Codegen("com.example.db", "Db")
 *     .generate(new FlywaySource(List.of("filesystem:src/main/resources/db/migration")), "postgres:17-alpine", outputDir);
 * }</pre>
 */
public final class Codegen {

    private final String packageName;
    private final String className;

    public Codegen(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

    /** The Java source for the schema the connection sees. */
    public String source(Connection connection) throws SQLException {
        return javaFile(connection).toString();
    }

    /** Writes {@code <package dirs>/<className>.java} under {@code outputDir} and returns its path. */
    public Path write(Connection connection, Path outputDir) throws SQLException, IOException {
        return javaFile(connection).writeToPath(outputDir);
    }

    /** Builds the schema with {@code source} in a temporary Postgres of {@code postgresImage} and writes the source file. */
    public Path generate(SchemaSource source, String postgresImage, Path outputDir) throws Exception {
        DockerImageName image = DockerImageName.parse(postgresImage).asCompatibleSubstituteFor("postgres");
        try (PostgreSQLContainer pg = new PostgreSQLContainer(image)) {
            pg.start();
            DataSource ds = dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
            source.apply(ds);
            try (Connection c = ds.getConnection()) {
                return write(c, outputDir);
            }
        }
    }

    /** Reads the schema of an existing database and writes the source file. */
    public Path generate(String jdbcUrl, String user, String password, Path outputDir) throws SQLException, IOException {
        try (Connection c = dataSource(jdbcUrl, user, password).getConnection()) {
            return write(c, outputDir);
        }
    }

    private JavaFile javaFile(Connection connection) throws SQLException {
        return new DbClassGenerator(CatalogReader.read(connection), connection, packageName, className).generate();
    }

    private static DataSource dataSource(String url, String user, String password) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setUser(user);
        ds.setPassword(password);
        return ds;
    }
}
