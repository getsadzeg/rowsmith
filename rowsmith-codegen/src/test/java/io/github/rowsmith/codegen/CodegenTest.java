package io.github.rowsmith.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.postgresql.ds.PGSimpleDataSource;

class CodegenTest {

    static final String IMAGE = System.getProperty("rowsmith.test.pgImage", "postgres:17-alpine");
    static final boolean UPDATE = Boolean.getBoolean("rowsmith.test.updateGolden");
    static final Path CORE_TESTS = Path.of(System.getProperty("rowsmith.test.coreTests"));

    @TempDir
    Path tmp;

    @ParameterizedTest
    @CsvSource({
        "tracker.sql, io.github.rowsmith.tracker",
        "lending.sql, io.github.rowsmith.lending",
        "codegen-tricky.sql, io.github.rowsmith.tricky"
    })
    void coreTestSchemasMatchTheirCheckedInDbClasses(String schema, String packageName) throws Exception {
        Path generated = new Codegen(packageName, "Db").generate(new SqlScriptsSource(List.of(coreResource(schema))), IMAGE, tmp.resolve("src"));
        assertGolden(generated, golden(packageName));
        assertCompiles(generated);
    }

    @Test
    void flywayMigrationsBuildTheSchemaInOrder() throws Exception {
        Path migrations = Files.createDirectories(tmp.resolve("db/migration"));
        Files.writeString(migrations.resolve("V1__workspace.sql"), "CREATE TABLE workspace (id bigint PRIMARY KEY, name text NOT NULL);");
        Files.writeString(migrations.resolve("V2__rename.sql"), "ALTER TABLE workspace RENAME COLUMN name TO title;");
        Path generated = new Codegen("com.example", "Schema")
                .generate(new FlywaySource(List.of("filesystem:" + migrations)), IMAGE, tmp.resolve("src"));
        assertThat(generated).hasFileName("Schema.java");
        assertThat(Files.readString(generated))
                .contains("public static final Workspace WORKSPACE = new Workspace();")
                .contains("public final Column<String> TITLE = column(\"title\", String.class);")
                .doesNotContain("FlywaySchemaHistory")
                .doesNotContain("NAME =");
    }

    @Test
    void customSourceAndExistingDatabaseGiveTheSameCode() throws Exception {
        Codegen codegen = new Codegen("io.github.rowsmith.tricky", "Db");
        Path golden = golden("io.github.rowsmith.tricky");
        SchemaSource alsoReadByUrlWhileUp = ds -> {
            new SqlScriptsSource(List.of(coreResource("codegen-tricky.sql"))).apply(ds);
            PGSimpleDataSource pg = (PGSimpleDataSource) ds;
            assertGolden(codegen.generate(pg.getUrl(), pg.getUser(), pg.getPassword(), tmp.resolve("url")), golden);
        };
        assertGolden(codegen.generate(alsoReadByUrlWhileUp, IMAGE, tmp.resolve("custom")), golden);
    }

    @Test
    void aConstantPostgresCannotComputeStillGenerates() throws Exception {
        Path schema = Files.writeString(tmp.resolve("tenant.sql"), """
                CREATE TABLE note (id serial PRIMARY KEY,
                  tenant int NOT NULL DEFAULT current_setting('app.tenant')::int CHECK (tenant <> current_setting('app.tenant')::int));
                """);
        Path generated = new Codegen("com.example", "Db").generate(new SqlScriptsSource(List.of(schema)), IMAGE, tmp.resolve("src"));
        assertThat(Files.readString(generated)).contains("public final Column<Integer> TENANT = column(\"tenant\", Integer.class);");
    }

    private static Path coreResource(String name) {
        return CORE_TESTS.resolve("resources").resolve(name);
    }

    private static Path golden(String packageName) {
        return CORE_TESTS.resolve("java").resolve(packageName.replace('.', '/')).resolve("Db.java");
    }

    private static void assertGolden(Path generated, Path golden) throws IOException {
        String actual = Files.readString(generated);
        if (UPDATE) {
            Files.createDirectories(golden.getParent());
            Files.writeString(golden, actual);
        }
        assertThat(actual).isEqualTo(Files.readString(golden));
    }

    private void assertCompiles(Path source) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        Path out = Files.createDirectories(tmp.resolve("classes"));
        int status = javac.run(null, null, null, "-Xlint:all", "-Werror", "-classpath", System.getProperty("java.class.path"),
                "-d", out.toString(), source.toString());
        assertThat(status).isZero();
    }
}
