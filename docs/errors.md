# Rowsmith error codes

Every deliberate failure is a `RowsmithException` whose message starts with `Rowsmith: `, names the table,
column and constraint, and ends with your test line, e.g. `(TaskServiceTest:14)`. `e.code()` returns one of the
codes below; messages may improve between versions, codes stay. Invalid arguments (a negative count) throw
`IllegalArgumentException`. Values are checked before anything reaches the database, and a failed `create`
leaves nothing behind (one transaction, or a savepoint inside yours).

- **`UNKNOWN_TABLE`**: no such table on the search_path; the message lists the tables.
  Fix: use a listed name, or `schema.table`.
- **`UNKNOWN_COLUMN`**: no such column on the table (the message lists them), or a typed column of another table.
  Fix: use a listed column, or the constant of the table you create.
- **`INVALID_VALUE`**: a value does not convert to the column type, a GENERATED column was set, a relative time
  (`"today-3x"`) does not parse, or a value read back does not match the generated Java type.
  Fix: correct the value; leave GENERATED columns alone; regenerate the code after a schema change.
- **`VALUE_OUT_OF_RANGE`**: too long for `varchar(n)`/`char(n)`, or overflows `numeric(p,s)`, an integer or a date.
  Fix: shorten or scale the value.
- **`NOT_IN_ENUM`**: not a label of the Postgres enum (listed), or a value read back is not a generated enum constant.
  Fix: use a listed label; regenerate the code after a schema change.
- **`CHECK_VIOLATION`**: values set by the test or a `RowsmithSetup` violate a table or domain CHECK; single-column
  checks list the allowed values or bounds.
  Fix: change the named values.
- **`NOT_NULL_VIOLATION`**: a NOT NULL column was set to `null`.
  Fix: remove the `setNull`/`set`, or give a value.
- **`UNIQUE_VIOLATION`**: rows would collide on a primary key, unique constraint or (partial or expression) unique
  index, also via `with(child, n > 1)` over a one-to-one FK; the message says where the values came from.
  Fix: set different values, or use another parent with `use(...)`.
- **`FOREIGN_KEY_VIOLATION`**: a raw id references no row (reported by the database, translated).
  Fix: pass the parent `Row`: `set(TASK.PROJECT_ID, project)` or `use(project)`.
- **`REQUIRED_FK_CYCLE`**: NOT NULL, non-deferrable foreign keys form a cycle, so no insert order exists.
  Fix: make one FK nullable or `DEFERRABLE INITIALLY DEFERRED`, or create one side first and `use(...)` it.
- **`INVALID_RELATIONSHIP`**: `with(child, ...)` where the child has no or several FKs to this table;
  `parent`/`child` for a row not linked in the same call; `child` with zero or several children.
  Fix: name the FK with `with(child, viaColumn, n, spec)`, or use `children(...)`.
- **`NO_SINGLE_PRIMARY_KEY`**: `row.id()` or `find(table, id)` on a table without a single-column primary key.
  Fix: read the key columns with `row.get(...)`.
- **`ROW_NOT_FOUND`**: `find(table, id)` found no row with that primary key.
  Fix: check the id, or create the row first.
- **`CANNOT_GENERATE`**: no generated value satisfies the constraints: a CHECK linking columns (`ends_on >= starts_on`,
  `num_nonnulls(a, b) = 1`), contradictory checks, exhausted unique values, or a partition key with no DEFAULT
  partition. The message names the columns.
  Fix: set them in the test, or with `RowsmithSetup.value(...)` for every test.
- **`UNSUPPORTED_TYPE`**: a NOT NULL column without a default has a type Rowsmith cannot generate (`point`, ranges);
  nullable ones stay NULL.
  Fix: set it, or give the column a DEFAULT.
- **`INVALID_CONFIG`**: a `RowsmithSetup` pattern targets a foreign key or GENERATED column, or the Spring context
  has several `Clock` beans and none is `@Primary`.
  Fix: use `use(...)` for foreign keys; drop the pattern; mark one `Clock` `@Primary`.
- **`DATABASE_ERROR`**: the database failed in a way Rowsmith did not predict (an insert, the catalog read or a
  cleanup statement); the message includes Postgres' error and the values.
  Fix: read the Postgres message; report it if Rowsmith should have caught it.
- **`CLEANUP_BLOCKED`**: cleanup deleted nothing: rows Rowsmith did not create reference its rows, a table has no
  primary key or NOT NULL unique key, or `cleanup()` ran inside an open transaction.
  Fix: delete those rows first, use a rolled-back `@Transactional` test, or call `cleanup()` after the transaction.
- **`NO_DATASOURCE`**: `@RowsmithTest` found no DataSource (or several beans, none `@Primary`), a container field is
  not started, or `Rowsmith.of(null)`.
  Fix: provide a Spring DataSource bean, a `DataSource` field, or a static, started `PostgreSQLContainer` field.
