# Contributing

Rowsmith is small on purpose: correct rows and clear errors matter more than features.

## Build and test

Needs Java 21 and a running Docker (tests and code generation use Testcontainers).

```bash
./gradlew build              # all modules, PostgreSQL 17
./gradlew build -Ppg=16      # another major, 14 to 18; CI runs all five
./gradlew :rowsmith-core:test --tests '*TrickySchemaTest'
```

Real-world schema coverage: `rowsmith-core/build/reports/rowsmith/realworld.md`.

## Layout

- `rowsmith-core`: public API in `io.github.rowsmith`; under `internal/`: catalog reader (`schema`), CHECK
  parser and evaluator (`sql`), value generation (`gen`), planner and executor (`plan`).
- `rowsmith-codegen`: the typed `Db` generator. `rowsmith-gradle-plugin`: `rowsmith { }` and `rowsmithGenerate`.
- `rowsmith-junit`: the `@RowsmithTest` extension.
- `examples/lending-demo`: a Spring Boot app tested with Rowsmith. `benchmark/`: the coding-agent benchmark.
- `skills/rowsmith`: the Claude Code skill; keep `examples/lending-demo/.claude/skills/rowsmith` identical.

## Supporting a new schema feature

1. Add a SQL file to `rowsmith-core/src/test/resources/tricky/`; tables that must fail say so in a header line:
   `-- expect <table>: <ERROR_CODE>`. `TrickySchemaTest` checks every table seeds or fails as declared.
2. For a real-world CHECK, add a case to `rowsmith-core/src/test/resources/checks/` with `-- case:`, `-- source:`
   and `-- license:` headers, plus `-- expect: CLEAR_ERROR` when Rowsmith should ask for values and
   `-- requires: PostgreSQL <major>` when older servers behave differently.
3. Plan output changed on purpose: delete `rowsmith-core/src/test/resources/snapshots/lending-plan.sql` and rerun
   `DeterminismTest` to rewrite it.
4. Generated code changed on purpose: `./gradlew :rowsmith-codegen:test -PupdateGolden`.

## Errors and pull requests

New failure paths throw `RowsmithException` with an `ErrorCode`, name the table, column and constraint, say what
is allowed, have a test, and are documented in `docs/errors.md`. Keep pull requests focused, with tests and a
`CHANGELOG.md` entry for user-visible changes.

By contributing you agree to license your contributions under the [MIT License](LICENSE).
