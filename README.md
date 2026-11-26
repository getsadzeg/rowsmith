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

## When it can't decide

If a value can't be generated safely, Rowsmith stops and tells you exactly what to set:

```
Rowsmith: the generated values of notification#1 do not satisfy check notification_check: (num_nonnulls(email, slack_channel) = 1); set values that satisfy it with .set(...) in the test or with RowsmithSetup.value(...) (NotificationTest:15)
```

## Good to know

- PostgreSQL 14–18. Java 21.
- Works inside a rolled-back test transaction, or without one (then it cleans up after each test).
- Tested against 476 real-world CHECK constraints and schemas like Keycloak and Pagila.

More: [API](docs/api.md) · [Errors](docs/errors.md) · [Benchmark](benchmark/) · [Claude Code skill](skills/rowsmith)

## License

[MIT](LICENSE)
