package io.github.rowsmith.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RowsmithPluginTest {

    static final String CORE_JAR = System.getProperty("rowsmith.test.coreJar");
    static final String CODEGEN_JAR = System.getProperty("rowsmith.test.codegenJar");
    static final String IMAGE = System.getProperty("rowsmith.test.pgImage", "postgres:17-alpine");

    @TempDir
    Path dir;

    @Test
    void generatesFromFlywayMigrationsBeforeCompilingTestsAndOnlyWhenMigrationsChange() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'io.github.getsadzeg.rowsmith'
                }
                dependencies { testImplementation files('%s') }
                rowsmith {
                    packageName = 'com.example.db'
                    postgresImage = '%s'
                    flyway { locations 'db/migration' }
                }
                """.formatted(CORE_JAR, IMAGE));
        write("db/migration/V1__tasks.sql", """
                CREATE TABLE project (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name text NOT NULL);
                CREATE TABLE task (
                  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  project_id bigint NOT NULL REFERENCES project(id),
                  status text NOT NULL CHECK (status IN ('todo', 'done'))
                );
                """);
        write("src/test/java/com/example/UsesDb.java", """
                package com.example;

                import static com.example.db.Db.TASK;

                import com.example.db.Db.TaskStatus;
                import io.github.rowsmith.RowSpec;

                class UsesDb {
                    static void done(RowSpec t) {
                        t.set(TASK.STATUS, TaskStatus.DONE);
                    }
                }
                """);

        BuildResult first = build("compileTestJava");
        assertThat(first.task(":rowsmithGenerate").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(first.task(":compileTestJava").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(first.getTasks()).extracting(t -> t.getPath()).containsSubsequence(":rowsmithGenerate", ":compileTestJava");
        Path generated = dir.resolve("build/generated/rowsmith/java/com/example/db/Db.java");
        assertThat(Files.readString(generated)).contains("public static final Task TASK = new Task();").doesNotContain("FlywaySchemaHistory");

        BuildResult second = build("compileTestJava");
        assertThat(second.task(":rowsmithGenerate").getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);

        write("db/migration/V2__due_date.sql", "ALTER TABLE task ADD COLUMN due_on date;");
        BuildResult third = build("compileTestJava");
        assertThat(third.task(":rowsmithGenerate").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(Files.readString(generated)).contains("public final Column<LocalDate> DUE_ON = column(\"due_on\", LocalDate.class);");
    }

    @Test
    void runsSqlScriptsInOrder() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'io.github.getsadzeg.rowsmith'
                }
                rowsmith {
                    sql 'db/2_tasks.sql', 'db/1_projects.sql'
                    packageName = 'com.example'
                    className = 'Tables'
                    postgresImage = '%s'
                }
                """.formatted(IMAGE));
        write("db/1_projects.sql", "CREATE TABLE project (id bigint PRIMARY KEY);");
        write("db/2_tasks.sql", "CREATE TABLE task (id bigint PRIMARY KEY);");

        BuildResult result = build("rowsmithGenerate");
        assertThat(result.task(":rowsmithGenerate").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(Files.readString(dir.resolve("build/generated/rowsmith/java/com/example/Tables.java")))
                .contains("public static final Project PROJECT", "public static final Task TASK");
    }

    @Test
    void aSqlDirectoryRunsOnlyItsSqlFilesSortedByPath() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'io.github.getsadzeg.rowsmith'
                }
                rowsmith {
                    sql 'db'
                    packageName = 'com.example'
                    className = 'Tables'
                    postgresImage = '%s'
                }
                """.formatted(IMAGE));
        write("db/2_tasks.sql", "CREATE TABLE task (id bigint PRIMARY KEY, project_id bigint REFERENCES project);");
        write("db/1_projects.sql", "CREATE TABLE project (id bigint PRIMARY KEY);");
        write("db/README.md", "# Schema scripts");
        write("db/.DS_Store", "\u0000\u0001binary");

        BuildResult result = build("rowsmithGenerate");
        assertThat(result.task(":rowsmithGenerate").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(Files.readString(dir.resolve("build/generated/rowsmith/java/com/example/Tables.java")))
                .contains("public static final Project PROJECT", "public static final Task TASK");
    }

    @Test
    void usesTheDefaultFlywayLocationWithoutASchemaBlock() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'io.github.getsadzeg.rowsmith'
                }
                rowsmith {
                    packageName = 'com.example.db'
                    postgresImage = '%s'
                }
                """.formatted(IMAGE));
        write("src/main/resources/db/migration/V1__project.sql", "CREATE TABLE project (id bigint PRIMARY KEY);");

        build("rowsmithGenerate");
        assertThat(Files.readString(dir.resolve("build/generated/rowsmith/java/com/example/db/Db.java")))
                .contains("public static final Project PROJECT");
    }

    @Test
    void runsACustomSchemaSourceFromTheMainClasspath() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'io.github.getsadzeg.rowsmith'
                }
                dependencies { compileOnly files('%s', '%s') }
                rowsmith {
                    custom 'com.example.BuildSchema', sourceSets.main.runtimeClasspath
                    packageName = 'com.example.db'
                    postgresImage = '%s'
                }
                """.formatted(CODEGEN_JAR, CORE_JAR, IMAGE));
        write("src/main/java/com/example/BuildSchema.java", """
                package com.example;

                import io.github.rowsmith.codegen.SchemaSource;
                import javax.sql.DataSource;

                public class BuildSchema implements SchemaSource {
                    @Override
                    public void apply(DataSource ds) throws Exception {
                        try (var c = ds.getConnection(); var st = c.createStatement()) {
                            st.execute("CREATE TABLE workspace (id bigint PRIMARY KEY, slug text NOT NULL)");
                        }
                    }
                }
                """);

        BuildResult result = build("rowsmithGenerate");
        assertThat(result.task(":compileJava").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(Files.readString(dir.resolve("build/generated/rowsmith/java/com/example/db/Db.java")))
                .contains("public final Column<String> SLUG = column(\"slug\", String.class);");
    }

    @Test
    void aMissingJdbcUrlNamesTheArgument() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'io.github.getsadzeg.rowsmith'
                }
                rowsmith {
                    packageName = 'com.example.db'
                    url null, 'app', 'secret'
                }
                """);
        BuildResult result = runner("help").buildAndFail();
        assertThat(result.getOutput()).contains("rowsmith.url(jdbcUrl, user, password): jdbcUrl");
    }

    private BuildResult build(String task) {
        return runner(task).build();
    }

    private GradleRunner runner(String task) {
        if (!Files.exists(dir.resolve("settings.gradle"))) {
            try {
                write("settings.gradle", "rootProject.name = 'demo'");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return GradleRunner.create()
                .withProjectDir(dir.toFile())
                .withPluginClasspath()
                .withArguments(task, "--configuration-cache", "--warning-mode=fail", "--stacktrace")
                .forwardOutput();
    }

    private void write(String path, String content) throws IOException {
        Path p = dir.resolve(path);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
    }
}
