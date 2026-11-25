# Rowsmith agent benchmark

**Claim under test:** with Rowsmith, coding agents absorb schema changes at lower cost than with hand-written
test data setup.

This directory holds everything needed to reproduce the numbers in the top-level README: tasks, hidden
oracles, the harness, raw results and the summary script.

## Setup

- **App:** `examples/lending-demo` (Spring Boot 4.1, Java 21, PostgreSQL via Testcontainers, Flyway) plus
  `schema-changes/overlay`, copied over it in both conditions: migration `V2__underwriting_payments_collections.sql`
  brings it to 21 tables (credit_application → credit_decision → offer → loan, payment_method, payment_attempt,
  collection_agent, collection_case, promise_to_pay) with FK chains 4-5 levels deep, cross-column and regex CHECKs,
  composite uniques and partial unique indexes, and two services, `OfferAcceptanceService` and `PaymentRetryService`.
- **Two conditions**, each run gets a fresh, standalone copy built by `scripts/prepare.py`:
  - `baseline`: no Rowsmith. Existing tests use a realistic hand-written `TestData` helper
    (`conditions/baseline/.../TestData.java`, JdbcTemplate inserts for merchant, store, customer, address,
    KYC, loan, installment, dispute). JPA repositories are available too.
  - `rowsmith`: the same app with `rowsmith-junit` on the test classpath, `rowsmith.yaml` traits,
    and the Claude Code skill in `.claude/skills/rowsmith`. Existing tests use Rowsmith.
  - Everything else is identical: production code, migrations, `CLAUDE.md` (only its last "Test data"
    paragraph differs, see `conditions/*/CLAUDE-testdata.md`), the same two existing test classes
    (5 tests) written in each condition's style.
- **Agent:** Claude Code CLI, headless (`claude -p ... --output-format stream-json`), model `sonnet`
  (resolved to `claude-sonnet-5-5`) at effort `high`. Effort is passed with `--effort` and verified before
  each batch: `claude -p /model --effort high` must report `(effort: high)`. The harness refuses Sonnet
  below high and Opus above medium.
- **Isolation:** `--setting-sources project` (no user settings, hooks, plugins or user skills),
  `--strict-mcp-config` (no MCP servers), `--tools Bash,Read,Edit,Write,Skill`, `--permission-mode acceptEdits`
  with an allow-list of read-only shell commands plus `./gradlew`, `--permission-prompts none`,
  `--max-budget-usd 5`, 40 minute timeout. Transcripts are scanned for reads of Rowsmith/benchmark internals.

## Oracle (`scripts/oracle.py`)

Success is never self-reported: the full suite must pass, the migration must be byte-identical, and every
original test method must still exist and pass (none deleted, disabled or skipped). Production changes are
allowed.

Metrics come from the stream-json transcript: Gradle test/build invocations ("iterations"), failed ones,
turns, input tokens (incl. cache reads/writes), output tokens, cost reported by the CLI, and agent time.

## Schema changes (`schema-changes/`)

How much does a migration cost a coding agent when test data is created by Rowsmith versus by the hand-written
`TestData` helper and inline inserts?

- **Start:** the 21-table app with 13 existing integration tests per condition, each in its own style:
  the 5 base tests plus `OfferAcceptanceServiceTest` and `PaymentRetryServiceTest`
  (`schema-changes/existing-tests/<condition>/`), which touch
  credit_application → credit_decision → offer → loan and payment_method/payment_attempt/collection_case.
- **Tasks** (`schema-changes/tasks.json`), four independent `V3` migrations (`schema-changes/migrations/`), each a
  separate task with the same prompt in both conditions ("migration V3 was added; make the suite pass, do not
  modify the migration or delete, disable or weaken tests"):
  - sc1 `V3__compliance_identifiers.sql`: NOT NULL columns without defaults on merchant (unique, regex),
    store (IN list), customer (regex) and payment_method.
  - sc2 `V3__brands.sql`: new `brand` table; required `brand_id` FK on customer and merchant.
  - sc3 `V3__merchant_contracts.sql`: new `merchant_contract` table between store and merchant
    (`store.contract_id` NOT NULL, composite FK `(contract_id, merchant_id)`, CHECKs, partial unique index).
  - sc4 `V3__data_quality_constraints.sql`: new CHECKs (KYC provider list, SHA-256 fingerprint, semver model
    version, corporate agent e-mail) and a partial unique index (one ACTIVE loan per customer and store).
- **Oracle:** before any agent run, `validate_oracle.py` recorded the tests each migration breaks in
  the untouched suites and checked that the reference fixes (`schema-changes/refs/<task>/<condition>/`) pass
  (`results/schema-changes/oracle-validation.jsonl`).
- **Runs:** 4 tasks × 2 conditions × 2 reps, Sonnet at effort high, $0.80 per-run cap, $6 total cap, run on
  2026-11-25 against the current library. Total spend $1.90. Every run succeeded.

Medians of 2 runs per cell (`results/schema-changes/summary-sonnet-high.md` has IQRs and the raw `runs.jsonl`):

| Task | Condition | Tests broken (of 13) | Success | Gradle runs | Turns | Edits | Input tok (k) | Output tok (k) | Cost $ | Wall s |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| sc1 NOT NULL columns | baseline | 13 | 2/2 | 2 | 16 | 6 | 167 | 4.3 | 0.14 | 33 |
| | rowsmith | 0 | 2/2 | 1 | 12 | 0 | 131 | 2.0 | 0.10 | 28 |
| sc2 required FK | baseline | 13 | 2/2 | 1 | 13 | 4 | 147 | 3.5 | 0.12 | 29 |
| | rowsmith | 0 | 2/2 | 2 | 14 | 2 | 149 | 2.7 | 0.11 | 29 |
| sc3 table in FK chain | baseline | 12 | 2/2 | 2 | 11 | 4 | 124 | 4.1 | 0.11 | 36 |
| | rowsmith | 0 | 2/2 | 2 | 15 | 2 | 193 | 3.2 | 0.13 | 35 |
| sc4 CHECK / unique | baseline | 9 | 2/2 | 4 | 16 | 6 | 240 | 4.4 | 0.15 | 51 |
| | rowsmith | 0 | 2/2 | 1 | 9 | 0 | 107 | 1.7 | 0.08 | 22 |
| **all (pooled)** | baseline | 47/52 | 8/8 | 2 | 14 | 4 | 155 | 4.1 | 0.14 | 34 |
| | rowsmith | 0/52 | 8/8 | 1 | 13 | 0 | 156 | 2.1 | 0.11 | 25 |

Notes:

- No Rowsmith test broke under any migration, so in every Rowsmith run the untouched suite already passed. The
  only Rowsmith edits (one sc2 run, one sc3 run) were optional production changes mapping the new columns on JPA
  entities; agents in both conditions made such edits in 7 of 16 runs, and production edits are allowed for
  these tasks.
- With 2 runs per cell the per-task differences are small samples; the pooled row and the bootstrap CIs in the
  summary are the better guide.
- Gradle runs count only invocations that executed; calls refused by the allow-list are recorded separately
  (`gradle_blocked_calls`).

## Reproduce

```bash
./gradlew publishToMavenLocal                     # the rowsmith condition resolves the library from ~/.m2
cd benchmark/scripts
python3 validate_oracle.py /tmp/rowsmith-oracle-sc > ../results/schema-changes/oracle-validation.jsonl
python3 run.py --work /tmp/rowsmith-bench-sc --reps 2 --parallel 3 --budget 0.8 --total-budget 6
python3 summarize.py --model sonnet-high         # writes results/schema-changes/summary-sonnet-high.{md,csv}
```

Requires Java 21, Docker, Python 3.11+ and an authenticated `claude` CLI. Per-run artifacts (gzipped
transcript, the agent's test sources) go to `results/schema-changes/artifacts/<run-id>/`.
