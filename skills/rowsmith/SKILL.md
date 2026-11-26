---
name: rowsmith
description: Writes and fixes PostgreSQL test data in Java integration tests with the Rowsmith library, where a test sets only the values its behavior depends on and Rowsmith creates the required parent rows, NOT NULL columns and CHECK-valid, unique values from the live schema. Use whenever writing, fixing or reviewing JUnit or Spring tests that need database rows in a project that uses Rowsmith (io.github.rowsmith imports, @RowsmithTest, rowsmith-junit, the io.github.getsadzeg.rowsmith Gradle plugin), when a test fails with a "Rowsmith:" error, or when a migration broke test setup, even if the request only says "add a test for this service" or "the suite is red after the migration".
---

# Rowsmith test data

Rowsmith reads the Postgres catalog and inserts complete rows: a test sets only the values its behavior depends
on, and Rowsmith fills every other column and creates required parents in foreign-key order. Such tests survive
migrations that add columns, CHECKs or foreign keys, so never hand-write parent rows, filler columns or raw
`INSERT`s next to Rowsmith.

## Find the table constants

`rowsmith { packageName = "com.example.db" }` in `build.gradle(.kts)` makes the Gradle plugin generate
`com.example.db.Db` (name from `className`, default `Db`) into `build/generated/rowsmith/java` on
`compileTestJava`. Read it for table constants (`TASK`), typed columns (`TASK.DUE_ON` is `Column<LocalDate>`) and
value enums (`Db.TaskStatus.DONE`). Without the plugin, use database names: `create("task", t -> t.set("status", "todo"))`.

## Write a test

```java
import static com.example.db.Db.*;
import static io.github.rowsmith.RelativeTime.today;

@SpringBootTest
@Transactional                      // rows join the test transaction and are rolled back
@RowsmithTest                       // injects a Rowsmith per test
class OverdueReminderTest {
    @Autowired ReminderService reminders;
    Rowsmith rowsmith;

    @Test
    void remindsTheAssigneeOfAnOverdueTask() {
        Row alice = rowsmith.create(APP_USER);
        Row task = rowsmith.create(TASK, t -> t
            .set(TASK.STATUS, TaskStatus.TODO)
            .set(TASK.DUE_ON, today().minusDays(3))
            .use(TASK.ASSIGNEE_ID, alice));

        reminders.sendDaily();
        // assert on task.get(TASK.ID), alice.get(APP_USER.EMAIL), ...
    }
}
```

Imports: `io.github.rowsmith.{Row, Rowsmith, RowSpec}`, `io.github.rowsmith.junit.RowsmithTest`.

| Call | Effect |
|---|---|
| `create(TASK, spec)`, `createMany(TASK, n, spec)` | rows plus required parents |
| `.set(col, value)`, `.setNull(col)` | a typed value; `today()`/`now()` for dates relative to the app clock |
| `.set(TASK.PROJECT_ID, projectRow)`, `.use(TASK.ASSIGNEE_ID, row)`, `.use(row)` | point FKs at existing rows |
| `.with(TASK, n, spec)`, `.with(TASK, TASK.REPORTER_ID, n, spec)` | children of this row; name the FK when there are several |
| `.apply(TaskData::overdue)` | a preset, later calls win |
| `row.get(col)`, `row.parent(TASK.PROJECT_ID)`, `row.child(TASK)`, `row.children(TASK)` | read values, navigate rows of the same call |
| `find(TASK, id)` | load an existing row from the database, e.g. to `use` it or assert on it |
| `plan(TASK, spec)`, `describe(TASK)` | dry run with SQL, schema as Rowsmith sees it |

Parents are shared within one `create` call and new for each call. For two tasks in one project, create the
project first and `use(project)`, or `create(PROJECT, p -> p.with(TASK, 2))`.

Setup several tests need goes in a plain method (look for an existing presets class first):
`static void overdue(RowSpec t) { t.set(TASK.STATUS, TaskStatus.TODO).set(TASK.DUE_ON, today().minusDays(3)); }`,
then `create(TASK, TaskData::overdue)`.

Columns left alone: identity, generated and DEFAULT columns go to the database; nullable columns and optional FKs
stay NULL. Arrays take a `List`, json/jsonb take JSON text. Project-wide value patterns go in a `RowsmithSetup`
(public no-arg constructor) used with `@RowsmithTest(setup = TrackerSetup.class)`; test values win:
`public void configure(Config c) { c.value(APP_USER.EMAIL, n -> "user" + n + "@example.com"); }`.

## Workflow

1. Read the code under test to learn which columns its behavior depends on; set exactly those.
2. Run only the test class. On a `Rowsmith:` error, fix the spec from the message and rerun.
3. After a migration, rerun the suite before editing tests: usually nothing needs to change. If generated code
   no longer compiles, a column was renamed or removed; follow the compiler.

## Reading errors

Errors start with `Rowsmith:`, name the table, column and constraint, end with the test line, and are raised
before anything reaches the database; a failed `create` leaves nothing behind.

```
Rowsmith: task.status = "doing" violates check task_status_check; allowed: todo, in progress, done, won't do (OverdueReminderTest:14)
Rowsmith: the generated values of task#1 do not satisfy check task_check: ((status = 'done'::text) = (done_at IS NOT NULL)); set done_at with .set(...) in the test or with RowsmithSetup.value(...) (OverdueReminderTest:21)
```

- "allowed: ..." or "must be ...": pick a value from the message.
- "set X with .set(...)" (`CANNOT_GENERATE`): a CHECK links columns; set the named columns, in the test if the
  value matters there, else with a `RowsmithSetup.value(...)` pattern.
- "set values that satisfy it" (`CANNOT_GENERATE`): a CHECK spans several columns; set values that satisfy the check
  shown, the same way.
- Unknown table or column: the message lists the valid names.

Fix the spec; do not catch the exception or fall back to raw SQL, which breaks again on the next migration.
Each `ErrorCode` constant's Javadoc explains its code.

## Transactions

- `@Transactional` test: Rowsmith joins the transaction. The code under test must run in it to see the rows;
  another thread or `REQUIRES_NEW` cannot.
- No test transaction: Rowsmith commits and deletes its rows after each test. Rows the code under test created
  that reference them must be deleted first, or cleanup refuses (`CLEANUP_BLOCKED`).
- Unflushed JPA entities are invisible to Rowsmith's SQL: `entityManager.flush()` first, or create those rows
  with Rowsmith.
- Without Spring, `@RowsmithTest` uses a `DataSource` field or a static Testcontainers `PostgreSQLContainer`
  field; outside JUnit, `Rowsmith.of(dataSource)` plus `cleanup()` for committed rows.
- The schema is read once per DataSource: migrate before the first test. Parallelize with Gradle forks, not
  JUnit's in-JVM parallel mode.
