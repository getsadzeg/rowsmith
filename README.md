# Rowsmith

**Test data for PostgreSQL, without the setup.**

Rowsmith is a Java library for integration tests. You say which row you need and which values matter to the test.
It creates that row and every row it depends on by reading your schema. Your tests keep working when the schema
changes.

## The problem

Say your app tracks tasks. A task belongs to a project, a project to a workspace, and a workspace has an owner. To
test anything about one task, you first write this:

```java
Long alice = jdbc.queryForObject("INSERT INTO app_user (email, display_name) VALUES ('alice@example.com', 'Alice') RETURNING id", Long.class);
Long acme = jdbc.queryForObject("INSERT INTO workspace (owner_id, slug, name, plan) VALUES (?, 'acme', 'Acme', 'free') RETURNING id", Long.class, alice);
UUID web = jdbc.queryForObject("INSERT INTO project (workspace_id, name, starts_on) VALUES (?, 'Website', current_date) RETURNING id", UUID.class, acme);
jdbc.update("INSERT INTO task (project_id, reporter_id, title, status) VALUES (?, ?, 'Fix login', 'todo')", web, alice);
```

The test only cares about `status`. Everything else is there to satisfy foreign keys, NOT NULL, UNIQUE and CHECK
constraints, and the next migration that adds a required column breaks every one of these setups.

With Rowsmith:

```java
Row task = rowsmith.create(TASK, t -> t.set(TASK.STATUS, TaskStatus.TODO));
```

`TASK` and `TaskStatus` are generated from your migrations.

## Features

- **Creates the parents for you.** Required foreign keys are followed and their rows created automatically.
- **Values that pass your constraints.** NOT NULL, UNIQUE, CHECK, enums, lengths and numeric precision, with CHECKs
  verified by Postgres itself.
- **Survives migrations.** New required columns and foreign keys are filled in without touching the test.
- **Typed.** Tables, columns and enum values are generated as Java constants, so a renamed column fails to compile
  instead of failing at runtime.
- **Clear errors.** When it can't pick a safe value, it stops and names the constraint, what to set and the test line.
- **Dry run.** `plan()` shows the SQL and where each value comes from, without inserting anything.
- **JUnit 5 and Spring.** It joins a rolled-back `@Transactional` test, or cleans up after itself.
- **Built for coding agents.** It ships a Claude Code skill, and an agent can act on its errors directly. In the
  [benchmark](benchmark/), migrations broke 47 of 52 hand-written test setups and 0 of 52 Rowsmith ones.

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
- Rowsmith follows database constraints, not JPA annotations; rules that live only in your entities need a CHECK
  or a value set in the test.
- Tested against 476 real-world CHECK constraints and schemas like Keycloak and Pagila.

More: [Claude Code skill](skills/rowsmith) · [Benchmark](benchmark/) · error codes in the `ErrorCode` Javadoc

Build: `./gradlew build` (needs Docker).

## License

[MIT](LICENSE)
