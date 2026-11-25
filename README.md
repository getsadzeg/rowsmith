# Rowsmith

**Valid, connected PostgreSQL rows for integration tests, in one line.**

Every database test starts the same way. Before you can check one thing, you insert a pile of related rows:
the user, the workspace it owns, the project inside it, and finally the task you actually care about. Each
needs every required column filled, every CHECK satisfied, every unique value distinct. Then a migration adds
a NOT NULL column or a new foreign key, and dozens of tests break even though nothing they test has changed.

Before, a test of one overdue task:

```java
Long owner = jdbc.queryForObject("INSERT INTO app_user (email, display_name) VALUES ('owner@example.com', 'Owner') RETURNING id", Long.class);
Long workspace = jdbc.queryForObject("INSERT INTO workspace (owner_id, slug, name, plan) VALUES (?, 'acme', 'Acme', 'free') RETURNING id", Long.class, owner);
UUID project = jdbc.queryForObject("INSERT INTO project (workspace_id, name, starts_on) VALUES (?, 'Launch', '2026-01-01') RETURNING id", UUID.class, workspace);
jdbc.update("INSERT INTO task (project_id, reporter_id, title, status, due_on) VALUES (?, ?, 'Write docs', 'todo', '2026-01-01')", project, owner);
```

With Rowsmith:

```java
Row task = rowsmith.create(TASK, t -> t.set(TASK.STATUS, TaskStatus.TODO).set(TASK.DUE_ON, today().minusDays(3)));
```

The test states only the values it depends on. Rowsmith:

- **fills in the rest**: required columns, values that pass every CHECK, unique keys, and the parent rows
  (project, workspace, owner) in foreign-key order;
- **reads your live schema**, so tests keep passing when a migration adds a column or a foreign key;
- **fails before inserting** when it cannot, with an error that names the column and tells you what to set.

## Quick start

Java 21, PostgreSQL 14 to 18. Not on Maven Central yet: run `./gradlew publishToMavenLocal` in this repository,
then add the dependency and the Gradle plugin:

```kotlin
// settings.gradle.kts
pluginManagement { repositories { mavenLocal(); gradlePluginPortal() } }

// build.gradle.kts
plugins { id("io.github.getsadzeg.rowsmith") version "0.1.0-SNAPSHOT" }
repositories { mavenLocal(); mavenCentral() }
dependencies { testImplementation("io.github.getsadzeg:rowsmith-junit:0.1.0-SNAPSHOT") }

rowsmith {
    packageName = "com.example.db"   // generates com.example.db.Db into the test source set
}
```

The plugin reads your Flyway migrations and generates `Db`, with a constant per table (`TASK`) and a typed
column per column (`TASK.DUE_ON` is a `Column<LocalDate>`). Your first test:

```java
@SpringBootTest
@Transactional                      // Rowsmith joins the test transaction; everything is rolled back
@RowsmithTest
class OverdueTaskTest {
    @Autowired TaskService tasks;
    Rowsmith rowsmith;              // injected for each test

    @Test
    void findsTasksPastTheirDueDate() {
        Row task = rowsmith.create(TASK, t -> t.set(TASK.STATUS, TaskStatus.TODO).set(TASK.DUE_ON, today().minusDays(3)));

        assertThat(tasks.overdueIds()).containsExactly(task.get(TASK.ID));
    }
}
```

No Spring or JUnit? Use `Rowsmith.of(dataSource)`. No code generation? Every method also takes plain names:
`rowsmith.create("task", t -> t.set("status", "todo").set("due_on", "today-3d"))`.

## API

| Call | Effect |
|---|---|
| `create(TASK, spec)`, `createMany(TASK, n, spec)` | rows plus their required parents; parents are shared within one call |
| `.set(TASK.STATUS, TaskStatus.TODO)` | a value of the column's Java type |
| `.set(TASK.DUE_ON, today().minusDays(3))` | dates relative to Rowsmith's clock (`RelativeTime.today()`, `now()`) |
| `.set(TASK.PROJECT_ID, project)`, `.use(TASK.ASSIGNEE_ID, alice)`, `.use(project)` | point foreign keys at existing rows |
| `.setNull(TASK.ASSIGNEE_ID)` | NULL for a nullable column |
| `.with(TASK, 3, spec)`, `.with(TASK, TASK.REPORTER_ID, 1, spec)` | children of this row (name the FK when there are several) |
| `.apply(TaskData::overdue)` | a preset; later calls win |
| `row.get(TASK.DUE_ON)`, `row.id()`, `row.parent(TASK.PROJECT_ID)`, `row.child(TASK)`, `row.children(TASK)` | values from `INSERT ... RETURNING *`, navigation |
| `plan(TASK, spec)`, `describe(TASK)`, `find(TASK, id)`, `tables()` | dry run with SQL and value sources, schema view, load a row |

A Postgres enum, or a text column whose CHECK lists its values, becomes a Java enum (`TaskStatus.DONE` writes
`'done'`). A column renamed by a migration is a compile error, not a failing test.

## Presets and setup

Presets are plain methods taking a `RowSpec`, so they compose, take parameters and are found by your IDE:

```java
final class TaskData {
    static void overdue(RowSpec t) {
        t.set(TASK.STATUS, TaskStatus.TODO).set(TASK.DUE_ON, today().minusDays(3));
    }
}

rowsmith.create(TASK, TaskData::overdue);
rowsmith.create(PROJECT, p -> p.with(TASK, 3, TaskData::overdue));
rowsmith.create(TASK, t -> t.apply(TaskData::overdue).set(TASK.PRIORITY, Priority.URGENT));
```

Project-wide value patterns go in a `RowsmithSetup`, referenced from `@RowsmithTest(setup = ...)` or
`Rowsmith.builder(dataSource).setup(...)`. Values the test sets win:

```java
public class TrackerSetup implements RowsmithSetup {
    @Override
    public void configure(Config c) {
        c.value(APP_USER.EMAIL, n -> "user" + n + "@example.com");   // n = 1, 2, ... per table
    }
}
```

Where the schema comes from (the default is Flyway migrations in `src/main/resources/db/migration`):

```kotlin
rowsmith {
    packageName = "com.example.db"
    className = "Db"                                  // default
    postgresImage = "postgres:17-alpine"              // default; use the version production runs
    flyway { locations("src/main/resources/db/migration", "src/test/resources/db/testdata") }
    // or: sql("schema.sql", "src/test/resources/fixtures")         scripts, in this order
    // or: url("jdbc:postgresql://localhost/app", "app", "secret")  an existing database
    // or: custom("com.example.BuildSchema", sourceSets.main.get().runtimeClasspath)
}
```

The plugin builds the schema in a temporary Postgres (Docker) and writes `Db.java` to
`build/generated/rowsmith/java` before `compileTestJava`, again whenever the migrations or scripts change.
`custom` takes a class implementing `io.github.rowsmith.codegen.SchemaSource`, for Liquibase or your own setup.

## Supported usage

- **Transactions.** Either the test runs in a `@Transactional` test transaction (Rowsmith joins it, everything
  is rolled back), or in no transaction (Rowsmith commits and deletes exactly the rows it created after each
  test, children first, refusing rather than deleting half when other rows still reference them). A failed
  `create` undoes only its own inserts.
- **DataSource.** `@RowsmithTest` takes the Spring context's DataSource, a `DataSource` field, or a static
  Testcontainers `PostgreSQLContainer` field. In Spring tests it also uses the application's `Clock` bean.
- **Parallelism.** One Rowsmith per test, not thread-safe: run tests in parallel with Gradle forks
  (`maxParallelForks`), not JUnit's in-JVM parallel mode.
- **Schema.** Read once per DataSource; migrate before the first test and do not change the schema during the run.
- **Database.** PostgreSQL 14 to 18 only. CI runs the build on all five.

## Errors

Values are checked against the schema before anything is sent to the database. Every error is a
`RowsmithException` with a stable `ErrorCode`, names the table, column and constraint, and ends with your line:

```
Rowsmith: task.status = "doing" violates check task_status_check; allowed: todo, in progress, done, won't do (OverdueTaskTest:14)
Rowsmith: the generated values of task#1 do not satisfy check task_check: ((status = 'done'::text) = (done_at IS NOT NULL)); set done_at with .set(...) in the test or with RowsmithSetup.value(...) (OverdueTaskTest:21)
```

Every code, what it means and how to fix it: [docs/errors.md](docs/errors.md).

## Evidence

- **CHECK constraints.** 476 CHECKs collected from real schemas (GitLab, Synapse, Supabase Auth, Rails, Django
  and Flyway apps, the PostgreSQL regression tests and others): Rowsmith generates rows for 430 automatically, the
  other 46 fail with a clear error naming the columns to set, and none produce an invalid row
  (`RealWorldCheckTest`).
- **Schema changes.** In the benchmark, four migrations broke 47 of 52 hand-written tests and 0 of 52
  Rowsmith tests.
- **Coding agents.** In 16 Claude Code runs (Sonnet, 2026-11-25) agents made the suite pass in every run of both
  conditions, at a median $0.14 and 34 s per task with hand-written setup vs $0.11 and 25 s with Rowsmith; see
  [`benchmark/`](benchmark/).
- **Real schemas.** Every table of Pagila, Chinook, Spring PetClinic and Keycloak seeds (Pagila with one
  `RowsmithSetup` line for its partition key), plus 13 edge-case schemas and property-based tests over random schemas.

## Coding agents

[`skills/rowsmith`](skills/rowsmith/SKILL.md) is a Claude Code skill: copy it to `.claude/skills/rowsmith/`.

## License

[MIT](LICENSE). Contributions: [CONTRIBUTING.md](CONTRIBUTING.md).
