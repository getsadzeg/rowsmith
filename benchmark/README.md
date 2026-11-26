# Rowsmith agent benchmark

**Claim:** with Rowsmith, coding agents absorb schema changes at lower cost than with hand-written test data.
Tasks, hidden oracles, harness, raw results and the summary script are all here.

## Setup

- **App:** `examples/demo-app` (Spring Boot 4.1, Java 21, Flyway, PostgreSQL via Testcontainers) plus
  `schema-changes/overlay`, whose `V2__underwriting_payments_collections.sql` brings it to 21 tables
  (credit_application → credit_decision → offer → loan, payments, collections) with 4-5 level FK chains,
  cross-column and regex CHECKs, composite and partial unique indexes, and two services,
  `OfferAcceptanceService` and `PaymentRetryService`.
- **Conditions** (fresh standalone copy per run, `scripts/prepare.py`):
  - `baseline`: tests use a realistic hand-written JdbcTemplate `TestData` helper
    (`conditions/baseline/.../TestData.java`); JPA repositories are available too.
  - `rowsmith`: `rowsmith-junit` on the test classpath and the skill in `.claude/skills/rowsmith`; tests use
    Rowsmith.
  - Everything else is identical, except the "Test data" paragraph of `CLAUDE.md`
    (`conditions/*/CLAUDE-testdata.md`).
- **Agent:** headless Claude Code (`claude -p --output-format stream-json`), `sonnet` (`claude-sonnet-5-5`) at
  `--effort high`, verified before each batch (`claude -p /model --effort high` must report `(effort: high)`);
  the harness refuses Sonnet below high and Opus above medium.
- **Isolation:** `--setting-sources project`, `--strict-mcp-config`, `--tools Bash,Read,Edit,Write,Skill`,
  `--permission-mode acceptEdits` with a read-only allow-list plus `./gradlew`, `--permission-prompts none`,
  `--max-budget-usd 5`, 40 min timeout. Transcripts are scanned for reads of Rowsmith/benchmark internals.

## Oracle (`scripts/oracle.py`)

Success is never self-reported: the full suite passes, the migration is byte-identical, and every original test
method still exists and passes. Production changes are allowed. Metrics come from the transcript: Gradle
test/build runs, failed runs, turns, input tokens (incl. cache), output tokens, CLI-reported cost, agent time.

## Schema changes (`schema-changes/`)

- **Start:** 13 existing integration tests per condition, in that condition's style
  (`schema-changes/existing-tests/<condition>/`).
- **Tasks** (`schema-changes/tasks.json`): four independent `V3` migrations, same prompt in both conditions
  ("migration V3 was added; make the suite pass, do not modify the migration or delete, disable or weaken tests"):
  - sc1 `V3__compliance_identifiers.sql`: NOT NULL columns without defaults (unique, regex, IN list).
  - sc2 `V3__brands.sql`: new `brand` table; required `brand_id` FK on customer and merchant.
  - sc3 `V3__merchant_contracts.sql`: new `merchant_contract` between store and merchant (NOT NULL composite FK,
    CHECKs, partial unique index).
  - sc4 `V3__data_quality_constraints.sql`: new CHECKs and a partial unique index (one ACTIVE loan per customer
    and store).
- **Oracle validation:** before any agent run, `validate_oracle.py` recorded the tests each migration breaks and
  checked that the reference fixes (`schema-changes/refs/<task>/<condition>/`) pass
  (`results/schema-changes/oracle-validation.jsonl`).
- **Runs:** 4 tasks × 2 conditions × 2 reps, Sonnet at effort high, $0.80 per-run and $6 total caps, 2026-11-25.
  Total spend $1.90; every run succeeded.

Medians of 2 runs per cell (raw runs: `results/schema-changes/runs.jsonl`):

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

- No Rowsmith test broke under any migration. The only Rowsmith edits (one sc2, one sc3 run) were optional JPA
  entity mappings of the new columns; agents in both conditions made such edits in 7 of 16 runs.
- 2 runs per cell is a small sample; prefer the pooled row. Pooled rowsmith / baseline median ratios (90% bootstrap
  CI): output tokens 0.53 (0.38–0.76), edits 0.00 (0.00–0.38), cost 0.75 (0.58–1.02), wall time 0.72 (0.56–1.05),
  Gradle runs 0.67 (0.40–1.33), turns 0.96 (0.64–1.25), input tokens 1.00 (0.60–1.33).
- Integrity: no run read Rowsmith or benchmark internals, none timed out, all 8 Rowsmith runs loaded the skill,
  and no agent saw a Rowsmith error.
- Gradle runs count only executed invocations; refused calls are recorded as `gradle_blocked_calls`.

## Reproduce

```bash
./gradlew publishToMavenLocal                     # the rowsmith condition resolves the library from ~/.m2
cd benchmark/scripts
python3 validate_oracle.py /tmp/rowsmith-oracle-sc > ../results/schema-changes/oracle-validation.jsonl
python3 run.py --work /tmp/rowsmith-bench-sc --reps 2 --parallel 3 --budget 0.8 --total-budget 6
python3 summarize.py --model sonnet-high         # medians, IQRs and bootstrap CIs
```

Requires Java 21, Docker, Python 3.11+ and an authenticated `claude` CLI. Per-run artifacts (gzipped transcript,
the agent's test sources) go to `results/schema-changes/artifacts/<run-id>/`.
