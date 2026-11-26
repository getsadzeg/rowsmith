# Rowsmith

**Test data for PostgreSQL, without the setup.**

To test one thing, you usually insert a pile of related rows first: parents, required columns, values that pass
every constraint. And every migration breaks that setup.

Rowsmith reads your database schema and creates everything for you. You only write what your test is about.

```java
// before: insert a user, a workspace, a project, then the task
// after:
Row task = rowsmith.create(TASK, t -> t.set(TASK.STATUS, TaskStatus.TODO));
```

The workspace, project and every required column are filled in, valid and connected. When a migration adds a
column or a foreign key, this line keeps working.

## Quick start

Not on Maven Central yet: run `./gradlew publishToMavenLocal` in this repository, then:

```kotlin
// settings.gradle.kts
pluginManagement { repositories { mavenLocal(); gradlePluginPortal() } }

// build.gradle.kts
plugins { id("io.github.getsadzeg.rowsmith") version "0.1.0-SNAPSHOT" }
repositories { mavenLocal(); mavenCentral() }
dependencies { testImplementation("io.github.getsadzeg:rowsmith-junit:0.1.0-SNAPSHOT") }

rowsmith {
    packageName = "com.example.db"   // generates com.example.db.Db (TASK, TASK.STATUS, ...) from your Flyway migrations
}
```

```java
@SpringBootTest
@Transactional          // Rowsmith joins the test transaction; everything is rolled back
@RowsmithTest
class TaskServiceTest {
    @Autowired TaskService tasks;
    Rowsmith rowsmith;  // injected for each test

    @Test
    void listsOpenTasks() {
        Row task = rowsmith.create(TASK, t -> t.set(TASK.STATUS, TaskStatus.TODO));

        assertThat(tasks.openTaskIds()).containsExactly(task.get(TASK.ID));
    }
}
```

No Spring? Give the test a static Testcontainers `PostgreSQLContainer` field, or use `Rowsmith.of(dataSource)`.

## Usage

**Only overdue tasks show up in the report**
```java
Row late = rowsmith.create(TASK, TaskData::overdue);
rowsmith.create(TASK, t -> t.set(TASK.DUE_ON, today().plusDays(3)));

assertThat(reports.overdueTaskIds()).containsExactly(late.get(TASK.ID));
```
**A project's progress counts only its done tasks**
```java
Row project = rowsmith.create(PROJECT, p -> p
        .with(TASK, 3, t -> t.set(TASK.STATUS, TaskStatus.DONE).set(TASK.DONE_AT, now()))
        .with(TASK, 1, t -> t.set(TASK.STATUS, TaskStatus.TODO)));

assertThat(projects.progress(project.get(PROJECT.ID))).isEqualTo(75);
```
**Unassigning keeps the task but clears the assignee**
```java
Row alice = rowsmith.create(APP_USER);
Row task = rowsmith.create(TASK, t -> t.use(TASK.ASSIGNEE_ID, alice));

tasks.unassign(task.get(TASK.ID));

assertThat(rowsmith.find(TASK, task.id()).get(TASK.ASSIGNEE_ID)).isNull();
```

A preset like `TaskData::overdue` is a plain static method:

```java
static void overdue(RowSpec t) {
    t.set(TASK.STATUS, TaskStatus.TODO).set(TASK.DUE_ON, today().minusDays(3));
}
```

Columns you don't set stay NULL if nullable, are left to database defaults and identity, or are generated to fit
your constraints. Project-wide patterns go in a `RowsmithSetup`: `c.value(APP_USER.EMAIL, n -> "user" + n + "@example.com")`.

`plan(TASK, spec)` shows the SQL and where each value comes from, without inserting.

## When it can't decide

If a value can't be generated safely, Rowsmith stops and tells you what to fix:

```
Rowsmith: the generated values of notification#1 do not satisfy check notification_check: (num_nonnulls(email, slack_channel) = 1); set values that satisfy it with .set(...) in the test or with RowsmithSetup.value(...) (NotificationTest:15)
```

## Good to know

- PostgreSQL 14–18. Java 21.
- Works inside a rolled-back `@Transactional` test, or without one (then it cleans up after each test). A fresh
  `Rowsmith` is injected per test method; `@Nested` classes work. Run in parallel with Gradle forks.
- The plugin reads Flyway migrations by default, or `sql(...)`, `url(...)` or `custom(...)`.
- Tested against 476 real-world CHECK constraints and schemas like Keycloak and Pagila.

More: [Claude Code skill](skills/rowsmith) · [Benchmark](benchmark/) · error codes in the `ErrorCode` Javadoc

Build: `./gradlew build` (needs Docker).

## License

[MIT](LICENSE)
