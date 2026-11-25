# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses semantic versioning.

## [0.1.0] - unreleased

### Added
- `rowsmith-core`: reads the live PostgreSQL catalog (tables, columns, identity and generated columns, defaults,
  enums, domains, arrays, CHECKs, unique constraints and partial or expression unique indexes, foreign keys
  including composite and deferrable ones, partitioned tables) once per DataSource and creates valid, connected
  rows: `create`, `createMany`, `set`, `setNull`, `with`, `use`, `apply`, dry-run `plan`, `find`, `describe`,
  `tables`, `created`, `cleanup`.
- Typed API: `Table` and `Column<T>` constants, Java enums for Postgres enums and CHECK value lists (`PgEnum`),
  typed `Row.get(Column)`, `Row.parent(...)`/`children(...)` by column or table. Every method also accepts
  table and column names as strings.
- Presets as plain `Consumer<RowSpec>` methods, passed as the spec or applied with `RowSpec.apply(...)`.
- `RelativeTime.today()`/`now()` (and `"today-3d"` strings) relative to Rowsmith's clock.
- `RowsmithSetup`: per-column value patterns by row number for every generated row.
- Values are checked against the schema before insert; CHECK expressions are evaluated, and when generated
  values cannot satisfy one the error names the columns to set. A failed `create` undoes only its own inserts,
  also inside the caller's transaction.
- `RowsmithException` with stable `ErrorCode`s and messages that end at the caller's line (`docs/errors.md`).
- `Plan` shows the SQL and where each column's value comes from (`ValueSource`).
- `rowsmith-codegen`: generates the typed `Db` class from Flyway migrations, SQL scripts, a JDBC URL or a custom
  `SchemaSource`, run in a temporary Postgres.
- `rowsmith-gradle-plugin` (`io.github.getsadzeg.rowsmith`): the `rowsmith { }` block and the
  `rowsmithGenerate` task, which adds the generated class to the test source set.
- `rowsmith-junit`: `@RowsmithTest(setup = ..., cleanup = ...)` injects a Rowsmith per test (field, method or
  constructor parameter) bound to the Spring context's DataSource, a `DataSource` field or a Testcontainers
  container field; joins an actual Spring test transaction, otherwise deletes the rows it created after each
  test; uses the application's `Clock` bean.
- Claude Code skill in `skills/rowsmith`.
