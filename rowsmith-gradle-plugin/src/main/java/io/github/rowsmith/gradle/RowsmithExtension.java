package io.github.rowsmith.gradle;

import java.util.Objects;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Property;

/**
 * The {@code rowsmith { }} block. The schema comes from Flyway migrations in
 * {@code src/main/resources/db/migration} unless one of {@link #flyway}, {@link #sql}, {@link #url} or
 * {@link #custom} says otherwise.
 *
 * <pre>
 * rowsmith {
 *     packageName = "com.example.db"           // required
 *     flyway { locations("src/main/resources/db/migration", "src/test/resources/db/testdata") }
 * }
 * </pre>
 */
public abstract class RowsmithExtension {

    /** Package of the generated class. */
    public abstract Property<String> getPackageName();

    /** Name of the generated class. Default {@code Db}. */
    public abstract Property<String> getClassName();

    /** Docker image of the temporary Postgres. Use the version production runs. Default {@code postgres:17-alpine}. */
    public abstract Property<String> getPostgresImage();

    /** Flyway migrations, run in a temporary Postgres. */
    public void flyway(Action<? super FlywaySpec> configure) {
        getSource().set(SchemaSourceType.FLYWAY);
        getMigrations().setFrom(defaultMigrations());
        configure.execute(new FlywaySpec(getMigrations()));
    }

    /** SQL scripts, run in the given order in a temporary Postgres; a directory runs its {@code *.sql} files sorted by path. */
    public void sql(Object... scripts) {
        getSource().set(SchemaSourceType.SQL);
        getMigrations().setFrom(scripts);
    }

    /** An existing database; nothing is built. Generation always runs, since Gradle cannot see the database change. */
    public void url(String jdbcUrl, String user, String password) {
        Objects.requireNonNull(jdbcUrl, "rowsmith.url(jdbcUrl, user, password): jdbcUrl");
        getSource().set(SchemaSourceType.URL);
        getUrl().set(jdbcUrl);
        getUser().set(user);
        getPassword().set(password);
    }

    /**
     * A {@code io.github.rowsmith.codegen.SchemaSource} class, loaded from {@code classpath} (e.g.
     * {@code sourceSets.main.runtimeClasspath}) and run in a temporary Postgres.
     */
    public void custom(String className, Object classpath) {
        Objects.requireNonNull(className, "rowsmith.custom(className, classpath): className");
        Objects.requireNonNull(classpath, "rowsmith.custom(className, classpath): classpath");
        getSource().set(SchemaSourceType.CUSTOM);
        getCustomSource().set(className);
        getCustomClasspath().setFrom(classpath);
    }

    /** The {@code flyway { }} block. */
    public static final class FlywaySpec {
        private final ConfigurableFileCollection locations;

        FlywaySpec(ConfigurableFileCollection locations) {
            this.locations = locations;
        }

        /** Migration directories. Default {@code src/main/resources/db/migration}. */
        public void locations(Object... directories) {
            locations.setFrom(directories);
        }
    }

    @Inject
    protected abstract ProjectLayout getLayout();

    Directory defaultMigrations() {
        return getLayout().getProjectDirectory().dir("src/main/resources/db/migration");
    }

    abstract Property<SchemaSourceType> getSource();
    abstract ConfigurableFileCollection getMigrations();
    abstract Property<String> getUrl();
    abstract Property<String> getUser();
    abstract Property<String> getPassword();
    abstract Property<String> getCustomSource();
    abstract ConfigurableFileCollection getCustomClasspath();
}
