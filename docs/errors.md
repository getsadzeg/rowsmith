# Rowsmith error codes

Every failure Rowsmith raises on purpose is a `RowsmithException`. Its message starts with `Rowsmith: `, names
the table, column and constraint involved, says what is allowed, and ends with the line in your test that made
the call, e.g. `(OverdueReminderTest:14)`. `e.code()` returns one of the codes below. Messages may improve
between versions; codes stay. Invalid arguments, such as a negative count, throw `IllegalArgumentException`.

Values set in the test and by `RowsmithSetup` are checked against the schema before anything is sent to the
database. A failed `create(...)` leaves nothing behind: its inserts run in one transaction, or under a
savepoint inside yours.

| Code | Meaning | Fix |
|---|---|---|
| `UNKNOWN_TABLE` | No such table on the search_path; the message lists the tables. | Use a listed name, or `schema.table`. |
| `UNKNOWN_COLUMN` | No such column on the table (the message lists its columns), or a typed column of another table was used. | Use a listed column; use the column constant of the table you create. |
| `INVALID_VALUE` | A value cannot be converted to the column type (`"abc"` for an integer), a GENERATED column was set, a relative time such as `"today-3x"` does not parse, or a value read back does not match the generated Java type. | Fix the value; leave GENERATED columns to the database; regenerate the code after a schema change. |
| `VALUE_OUT_OF_RANGE` | Too long for `varchar(n)`/`char(n)`, or overflows `numeric(p,s)`, an integer type or a date. | Shorten or scale the value. |
| `NOT_IN_ENUM` | Not a label of the Postgres enum; the message lists the labels. Also raised when a value read back is not a constant of the generated Java enum. | Use a listed label; regenerate the code after a schema change. |
| `CHECK_VIOLATION` | The values set (by the test or a `RowsmithSetup` pattern) violate a CHECK of the table or a domain. Single-column checks list the allowed values or bounds. | Change the named values. |
| `NOT_NULL_VIOLATION` | A NOT NULL column was set to `null`. | Remove the `setNull`/`set`, or give a value. |
| `UNIQUE_VIOLATION` | Rows would collide on a primary key, unique constraint or (partial or expression) unique index; also `with(child, n > 1)` over a unique, one-to-one FK. The message says where the values came from. | Set different values, or use a different parent with `use(...)`. |
| `FOREIGN_KEY_VIOLATION` | A raw id does not reference an existing row (reported by the database, translated). | Pass the parent `Row`: `set(TASK.PROJECT_ID, project)` or `use(project)`. |
| `REQUIRED_FK_CYCLE` | NOT NULL, non-deferrable foreign keys form a cycle, so no insert order exists. | Make one FK nullable or `DEFERRABLE INITIALLY DEFERRED`, or create one side first and `use(...)` it. |
| `INVALID_RELATIONSHIP` | `with(child, ...)` where the child has no FK, or several FKs, to this table; `parent(...)`/`child(...)` for a row that was not linked in the same call, or `child(...)` with zero or several children. | Follow the FKs listed in the message; name one with `with(child, viaColumn, n, spec)`; use `children(...)` for several. |
| `NO_SINGLE_PRIMARY_KEY` | `row.id()` or `find(table, id)` on a table without a single-column primary key. | Read key columns with `row.get(...)`. |
| `ROW_NOT_FOUND` | `find(table, id)` found no row with that primary key. | Check the id, or create the row first. |
| `CANNOT_GENERATE` | Rowsmith cannot generate a value that satisfies the constraints: a CHECK linking columns (`done_at` required when `status = 'done'`, `ends_on >= starts_on`, `num_nonnulls(a, b) = 1`), contradictory checks, a unique column whose allowed values ran out, or a partitioned table without a DEFAULT partition that needs its partition key. The message names the columns. | Set the named columns in the test, or with `RowsmithSetup.value(...)` for every test. |
| `UNSUPPORTED_TYPE` | A NOT NULL column without a default has a type Rowsmith cannot generate (e.g. `point`, ranges). Nullable ones are left NULL. | Set it, or give the column a DEFAULT. |
| `INVALID_CONFIG` | A `RowsmithSetup` value pattern targets a foreign key or GENERATED column, or the Spring test context has several `Clock` beans and none is `@Primary`. | Use `use(...)` for foreign keys; drop the pattern; mark one `Clock` `@Primary`. |
| `DATABASE_ERROR` | The database failed in a way Rowsmith did not predict: an insert (the message includes Postgres' error and the values), the catalog read, or a cleanup statement. | Read the Postgres message; report it if Rowsmith should have caught it. |
| `CLEANUP_BLOCKED` | Cleanup refused and deleted nothing: rows Rowsmith did not create still reference rows it created, a table has no primary key or NOT NULL unique key to identify its rows by, or `cleanup()` was called while a transaction is open on the thread. | Delete those rows first, run the test in a rolled-back `@Transactional` transaction, or call `cleanup()` after the transaction ends. |
| `NO_DATASOURCE` | `@RowsmithTest` found no DataSource (or several DataSource beans with none `@Primary`), a Testcontainers container field is not started, or `Rowsmith.of(null)`. | Use a Spring test context with a DataSource bean, a `DataSource` field, or a static, started `PostgreSQLContainer` field. |
