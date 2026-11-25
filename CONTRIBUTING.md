# Contributing

Rowsmith is small on purpose: correct rows and clear errors matter more than features.

## Build and test

Requirements: Java 21 and a running Docker (tests and code generation use Testcontainers).

```bash
./gradlew build              # all modules, PostgreSQL 17 by default
./gradlew build -Ppg=16      # another PostgreSQL major (14 to 18); CI runs all five
./gradlew :rowsmith-core:test --tests '*TrickySchemaTest'
```

`rowsmith-core/build/reports/rowsmith/realworld.md` holds the real-world schema coverage table.

## Where things live

- `rowsmith-core`: public API in `io.github.rowsmith`; catalog reader (`internal/schema`), CHECK expression
  parser and evaluator (`internal/sql`), value generation (`internal/gen`), planner and executor (`internal/plan`).
- `rowsmith-codegen`: generates the typed `Db` class from a schema. `rowsmith-gradle-plugin`: the `rowsmith { }`
  block and the `rowsmithGenerate` task.
- `rowsmith-junit`: the `@RowsmithTest` extension.
- `examples/lending-demo`: a Spring Boot app whose tests use Rowsmith. `benchmark/`: the coding-agent benchmark.
- `skills/rowsmith`: the Claude Code skill; keep `examples/lending-demo/.claude/skills/rowsmith` identical.

## Adding support for a schema feature

1. Add a SQL file to `rowsmith-core/src/test/resources/tricky/`. Tables that must fail declare it in a header
   line: `-- expect <table>: <ERROR_CODE>`.
2. Run `TrickySchemaTest`: every table must seed or fail with the declared, documented error.
3. For real-world CHECKs, add a case to `rowsmith-core/src/test/resources/checks/` with its `-- case:`,
   `-- source:` and `-- license:` header, plus `-- expect: CLEAR_ERROR` when Rowsmith should ask for values and
   `-- requires: PostgreSQL <major>` when the CHECK behaves differently on older servers.
4. If plan output changes on purpose, delete `rowsmith-core/src/test/resources/snapshots/lending-plan.sql` and
   rerun `DeterminismTest`, which writes it again.
5. If generated code changes on purpose, refresh the golden `Db.java` files: `./gradlew :rowsmith-codegen:test -PupdateGolden`.

## Error messages

Every new failure path throws `RowsmithException` with an `ErrorCode`, names the table, column and constraint,
says what is allowed, and is covered by a test. Document new codes in `docs/errors.md`.

## Pull requests

Keep changes focused, add tests, and describe the user-visible effect in `CHANGELOG.md`.

## License

By contributing you agree that your contributions are licensed under the [MIT License](LICENSE).
