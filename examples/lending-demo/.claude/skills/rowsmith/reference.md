# Rowsmith reference

## Contents
- Rowsmith, RowSpec, Row
- Values and dates
- RowsmithSetup
- Setup without Spring or JUnit
- Gradle plugin
- Errors

All methods taking `Table`/`Column` also take table and column names as strings.

## Rowsmith, RowSpec, Row

`Rowsmith`: `create(table[, spec])`, `createMany(table, n[, spec])`, `plan(table[, spec])`,
`planMany(table, n, spec)`, `find(table, id)` (load an existing row, e.g. for `use`), `describe(table)`,
`tables()`, `created()`, `cleanup()`. Not thread-safe: one per test (`@RowsmithTest` does this).

`RowSpec`: `set(col, value)`, `set(fkCol, row)`, `set(dateCol, RelativeTime)`, `set(Map)`, `setNull(col)`,
`apply(preset)`, `with(child, n)`, `with(child, spec)`, `with(child, n, spec)`, `with(child, viaFkCol, n, spec)`,
`use(row)` (every required FK to that row's table), `use(fkCol, row)`.

`Row`: `get(col)` (typed for a `Column<T>`, raw for a name), `id()` (single-column PK, raw type such as `Long`),
`getLong/getInt/getString/getBoolean/getBigDecimal/getLocalDate/getLocalDateTime/getOffsetDateTime/getUuid(name)`,
`values()`, `parent(fkCol | table)`, `child(table)` (exactly one), `children(table)`. Navigation covers rows
created in the same call or passed with `use`/`set`.

`Plan`: `rows()` (each with `values()`, `sources()` as `ValueSource`: USER, SETUP, PARENT_ROW,
DATABASE_GENERATED, DATABASE_DEFAULT, LEFT_NULL, ROWSMITH), `sql()`, `counts()`; `toString()` prints all of it.

## Values and dates

- Generated code maps Postgres enums and text columns whose CHECK lists values to Java enums; `TaskStatus.DONE`
  writes its label (`'done'`). With string names, pass the label or the enum constant.
- `RelativeTime.today()`, `now()`, with `plusDays/minusDays/plusMonths/minusMonths/plusHours/minusHours`,
  `plus/minus(Period | Duration)`; as strings `"today-3d"`, `"now+2h"`. They follow Rowsmith's clock: the
  application's `Clock` bean in Spring tests, else the system clock.
- Arrays take a `List`; json/jsonb take JSON text; an FK column takes a `Row`.
- Left alone: identity, generated and DEFAULT columns go to the database; nullable columns and optional FKs
  stay NULL. Set them when the behavior depends on them.

## RowsmithSetup

Value patterns for a column of every generated row, by row number (1, 2, ... per table and Rowsmith). Test values
win; a pattern that breaks a constraint is an error. Needs a public no-arg constructor.

```java
public class TrackerSetup implements RowsmithSetup {
    @Override
    public void configure(Config c) {
        c.value(APP_USER.EMAIL, n -> "user" + n + "@example.com");
        c.value("workspace", "name", n -> "Workspace " + n);
    }
}
```

Use it with `@RowsmithTest(setup = TrackerSetup.class)` or `Rowsmith.builder(ds).setup(new TrackerSetup())`.
`@RowsmithTest(cleanup = false)` keeps committed rows.

## Setup without Spring or JUnit

`@RowsmithTest` uses the Spring context's DataSource, a `DataSource` field, or a static Testcontainers
`PostgreSQLContainer` field, and injects the Rowsmith into a field, test method parameter or constructor parameter.
Without it: `Rowsmith.of(dataSource)` or `Rowsmith.builder(dataSource).clock(clock).build()`, plus `cleanup()`
when the rows were committed.

The schema is read once per DataSource: migrate before the first test, do not change it during the run. Run tests
in parallel with Gradle forks, not JUnit's in-JVM parallel mode.

## Gradle plugin

```kotlin
plugins { id("io.github.getsadzeg.rowsmith") version "0.1.0-SNAPSHOT" }
rowsmith {
    packageName = "com.example.db"                    // required
    className = "Db"                                  // default
    postgresImage = "postgres:17-alpine"              // default
    flyway { locations("src/main/resources/db/migration") }   // default source
    // or sql("schema.sql") / url(jdbcUrl, user, password) / custom("com.example.BuildSchema", classpath)
}
```

`./gradlew rowsmithGenerate` (or any test compile) regenerates the class after a migration; needs Docker.

## Errors

| Code | Meaning | Fix |
|---|---|---|
| UNKNOWN_TABLE / UNKNOWN_COLUMN | name not found, or a column of another table | use a name the message lists |
| INVALID_VALUE | value not convertible, GENERATED column set, bad relative time | fix the value; leave GENERATED columns alone |
| VALUE_OUT_OF_RANGE | too long for varchar(n), numeric overflow | shorten or scale |
| NOT_IN_ENUM | not a label of the enum | use a listed label; regenerate after a schema change |
| CHECK_VIOLATION | set values violate a CHECK | use a value from "allowed:" / "must be" |
| NOT_NULL_VIOLATION | NOT NULL column set to null | remove the setNull or give a value |
| UNIQUE_VIOLATION | rows would collide on a unique key (incl. partial) | different values, or another parent via `use` |
| FOREIGN_KEY_VIOLATION | a raw id references nothing | pass the `Row` instead of an id |
| REQUIRED_FK_CYCLE | NOT NULL FKs form a cycle | create one side first and `use(...)` it |
| INVALID_RELATIONSHIP | `with` finds no or several FKs; `parent`/`child` not linked | follow the FKs listed; `with(child, viaFkCol, n, spec)` |
| NO_SINGLE_PRIMARY_KEY | `id()`/`find` without a single-column PK | `row.get(...)` the key columns |
| ROW_NOT_FOUND | `find` found nothing | check the id |
| CANNOT_GENERATE | a CHECK links columns, values ran out, or a partition key is needed | set the named columns, or values that satisfy the shown CHECK (test or `RowsmithSetup`) |
| UNSUPPORTED_TYPE | NOT NULL column of a type Rowsmith cannot generate | set it |
| INVALID_CONFIG | setup pattern on an FK or GENERATED column; several `Clock` beans, none `@Primary` | fix the setup; mark one `@Primary` |
| DATABASE_ERROR | database failed unexpectedly (Postgres message included) | read the Postgres message |
| CLEANUP_BLOCKED | other rows reference Rowsmith's rows, or a transaction is open | delete them first, or use `@Transactional` |
| NO_DATASOURCE | no DataSource found for the test | Spring DataSource bean, `DataSource` field or started container field |
