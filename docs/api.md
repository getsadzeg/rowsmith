# Rowsmith API

Examples use the generated `Db` of a task tracker (`TASK`, `PROJECT`, `APP_USER`). Every method that takes a
`Table` or `Column` also takes table and column names as strings:
`rowsmith.create("task", t -> t.set("status", "todo").set("due_on", "today-3d"))`.

## Calls

| Call | Effect |
|---|---|
| `create(TASK, spec)`, `createMany(TASK, n, spec)` | rows plus their required parents; parents are shared within one call, new for each call |
| `.set(TASK.STATUS, TaskStatus.TODO)`, `.setNull(TASK.ASSIGNEE_ID)` | a value of the column's Java type; NULL for a nullable column |
| `.set(TASK.DUE_ON, today().minusDays(3))` | dates relative to Rowsmith's clock (`RelativeTime.today()`, `now()`) |
| `.set(TASK.PROJECT_ID, project)`, `.use(TASK.ASSIGNEE_ID, alice)`, `.use(project)` | point foreign keys at existing rows |
| `.with(TASK, 3, spec)`, `.with(TASK, TASK.REPORTER_ID, 1, spec)` | children of this row; name the FK when there are several |
| `.apply(TaskData::overdue)` | a preset; later calls win |
| `plan(TASK, spec)`, `planMany(TASK, n, spec)` | dry run: SQL and where each value comes from (`ValueSource`), nothing inserted |
| `describe(TASK)`, `tables()` | the schema as Rowsmith sees it |
| `find(TASK, id)` | load an existing row, e.g. to `use` it |
| `created()`, `cleanup()` | rows created so far; delete them (only for committed rows) |

`Row`: `get(TASK.DUE_ON)` (typed), `get("due_on")`, `getLong/getString/getLocalDate/getUuid/...(name)`, `id()`,
`values()`, `parent(TASK.PROJECT_ID)`, `child(TASK)` (exactly one), `children(TASK)`. Values come from
`INSERT ... RETURNING *`; navigation covers rows of the same call or passed with `use`/`set`.

A Postgres enum, or a text column whose CHECK lists its values, becomes a Java enum (`TaskStatus.DONE` writes
`'done'`). Identity, generated and DEFAULT columns are left to the database; nullable columns and optional FKs
stay NULL unless set.

## Presets

Plain `Consumer<RowSpec>` methods, so they compose, take parameters and are found by your IDE:

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

## RowsmithSetup

Project-wide value patterns for every generated row, by row number (1, 2, ... per table). Values the test sets
win; a pattern that breaks a constraint is an error. Needs a public no-arg constructor.

```java
public class TrackerSetup implements RowsmithSetup {
    @Override
    public void configure(Config c) {
        c.value(APP_USER.EMAIL, n -> "user" + n + "@example.com");
    }
}
```

Use it with `@RowsmithTest(setup = TrackerSetup.class)` or `Rowsmith.builder(dataSource).setup(new TrackerSetup()).build()`.

## Gradle plugin

```kotlin
rowsmith {
    packageName = "com.example.db"                    // required
    className = "Db"                                  // default
    postgresImage = "postgres:17-alpine"              // default; use the version production runs
    flyway { locations("src/main/resources/db/migration") }        // default source
    // or: sql("schema.sql", "src/test/resources/fixtures")         scripts, in this order
    // or: url("jdbc:postgresql://localhost/app", "app", "secret")  an existing database
    // or: custom("com.example.BuildSchema", sourceSets.main.get().runtimeClasspath)
}
```

The plugin builds the schema in a temporary Postgres (Docker) and writes `Db.java` to
`build/generated/rowsmith/java` before `compileTestJava`, again whenever the sources change (`./gradlew
rowsmithGenerate` runs it alone). `custom` takes a class implementing `io.github.rowsmith.codegen.SchemaSource`,
e.g. for Liquibase.

## Supported usage

- **Transactions.** In a `@Transactional` test Rowsmith joins the transaction and everything is rolled back.
  Without one it commits and, after each test, deletes exactly the rows it created, children first; it refuses
  rather than deleting half when other rows still reference them. A failed `create` undoes only its own inserts.
  `@RowsmithTest(cleanup = false)` keeps committed rows.
- **Injection.** `@RowsmithTest` injects a Rowsmith per test into a field, a test method parameter or a
  constructor parameter, bound to the Spring context's DataSource, a `DataSource` field, or a static
  Testcontainers `PostgreSQLContainer` field. In Spring tests it also uses the application's `Clock` bean.
  Without the extension: `Rowsmith.of(dataSource)` or `Rowsmith.builder(dataSource).clock(clock).build()`.
- **Parallelism.** A Rowsmith is not thread-safe: run tests in parallel with Gradle forks (`maxParallelForks`),
  not JUnit's in-JVM parallel mode.
- **Schema.** Read once per DataSource: migrate before the first test and do not change the schema during the run.
- **Database.** PostgreSQL 14 to 18; CI runs the build on all five.

## Evidence

- **CHECK constraints.** Of 476 CHECKs from real schemas (GitLab, Synapse, Supabase Auth, Rails, Django and
  Flyway apps, the PostgreSQL regression tests and others), Rowsmith generates rows for 430, fails with an error
  naming the columns to set for 46, and produces an invalid row for none (`RealWorldCheckTest`).
- **Real schemas.** Every table of Pagila (with one `RowsmithSetup` line for its partition key), Chinook, Spring
  PetClinic and Keycloak seeds, plus 13 edge-case schemas and property-based tests over random schemas.
- **Schema changes.** Four migrations broke 47 of 52 hand-written tests and 0 of 52 Rowsmith tests; see
  [`benchmark/`](../benchmark/).
