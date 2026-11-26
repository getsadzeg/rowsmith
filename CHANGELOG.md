# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); semantic versioning.

## [0.1.0] - unreleased

### Added
- `rowsmith-core`: reads the PostgreSQL catalog once per DataSource (identity and generated columns, defaults,
  enums, domains, arrays, CHECKs, unique constraints and partial or expression unique indexes, composite and
  deferrable foreign keys, partitioned tables) and creates valid, connected rows: `create`, `createMany`, `set`,
  `setNull`, `with`, `use`, `apply`, `plan`, `find`, `describe`, `tables`, `created`, `cleanup`.
- Typed API: `Table` and `Column<T>` constants, Java enums for Postgres enums and CHECK value lists, typed
  `Row.get`, `Row.parent`/`children`; every method also takes names as strings.
- Presets as plain `Consumer<RowSpec>` methods; `RelativeTime.today()`/`now()` (and `"today-3d"`) on Rowsmith's
  clock; `RowsmithSetup` value patterns by row number.
- Validation before insert, including CHECK evaluation; when generated values cannot satisfy a CHECK, the error
  names the columns to set. A failed `create` undoes only its own inserts, also inside the caller's transaction.
- `RowsmithException` with stable `ErrorCode`s, messages ending at the caller's line (`docs/errors.md`).
- `Plan`: the SQL and each value's `ValueSource`.
- `rowsmith-codegen` and `rowsmith-gradle-plugin` (`io.github.getsadzeg.rowsmith`): generate the typed `Db` class
  into the test source set from Flyway migrations, SQL scripts, a JDBC URL or a custom `SchemaSource`.
- `rowsmith-junit`: `@RowsmithTest(setup, cleanup)` injects a Rowsmith per test (field, method or constructor
  parameter) from the Spring DataSource, a `DataSource` field or a Testcontainers container field; joins a Spring
  test transaction or deletes its rows after each test; uses the application's `Clock` bean.
- Claude Code skill in `skills/rowsmith`.
