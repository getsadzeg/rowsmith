# Benchmark summary (sonnet-high, 16 runs)

Medians with [interquartile range]. Success is decided by the hidden oracle, not by the agent.

## Overall (all tasks pooled)

| Metric | baseline | rowsmith | rowsmith / baseline (median ratio, 90% bootstrap CI) |
|---|---|---|---|
| success | 8/8 | 8/8 | |
| gradle runs | 2 [1–2] | 1 [1–1] | 0.67 (0.40–1.33) |
| failed gradle runs | 0 [0–1] | 0 [0–0] | n/a |
| turns | 14 [12–16] | 13 [10–16] | 0.96 (0.64–1.25) |
| edits | 4 [4–6] | 0 [0–1] | 0.00 (0.00–0.38) |
| input tokens (k, incl. cache) | 155 [137–192] | 156 [108–191] | 1.00 (0.60–1.33) |
| output tokens (k) | 4.1 [3.9–4.4] | 2.1 [1.6–2.8] | 0.53 (0.38–0.76) |
| cost (USD) | 0.14 [0.11–0.14] | 0.11 [0.08–0.13] | 0.75 (0.58–1.02) |
| agent time (s) | 34 [31–41] | 24 [21–34] | 0.71 (0.56–1.05) |
| wall time (s) | 34 [31–41] | 25 [22–35] | 0.72 (0.56–1.05) |

Total agent spend for these runs: $1.90

## Per task

| Task | Condition | n | Tests broken | Success | Gradle runs | Failed gradle runs | Turns | Edits | Input tok (k) | Output tok (k) | Cost $ | Agent s | Wall s |
|---|---|---:|---:|---:|---|---|---|---|---|---|---|---|---|
| sc1-not-null-columns | baseline | 2 | 13 | 2/2 | 2 [1–2] | 0 [0–0] | 16 [15–18] | 6 [5–7] | 167 [159–175] | 4.3 [4.0–4.5] | 0.14 [0.14–0.14] | 32 [31–33] | 33 [32–34] |
| sc1-not-null-columns | rowsmith | 2 | 0 | 2/2 | 1 [1–1] | 0 [0–0] | 12 [10–14] | 0 [0–0] | 131 [100–161] | 2.0 [1.7–2.3] | 0.10 [0.09–0.11] | 27 [24–30] | 28 [25–31] |
| sc2-required-fk | baseline | 2 | 13 | 2/2 | 1 [1–1] | 0 [0–0] | 13 [12–14] | 4 [4–4] | 147 [141–154] | 3.5 [3.3–3.7] | 0.12 [0.12–0.13] | 28 [27–30] | 29 [28–30] |
| sc2-required-fk | rowsmith | 2 | 0 | 2/2 | 2 [1–2] | 0 [0–0] | 14 [13–15] | 2 [1–3] | 149 [133–165] | 2.7 [2.2–3.2] | 0.11 [0.09–0.12] | 29 [25–33] | 29 [25–34] |
| sc3-chain-insert | baseline | 2 | 12 | 2/2 | 2 [1–2] | 0 [0–1] | 11 [10–12] | 4 [3–4] | 124 [118–131] | 4.1 [4.0–4.1] | 0.11 [0.11–0.11] | 36 [35–37] | 36 [35–38] |
| sc3-chain-insert | rowsmith | 2 | 0 | 2/2 | 2 [2–2] | 0 [0–0] | 15 [14–16] | 2 [1–2] | 193 [192–194] | 3.2 [2.8–3.7] | 0.13 [0.13–0.14] | 34 [29–39] | 35 [30–40] |
| sc4-constraints | baseline | 2 | 9 | 2/2 | 4 [3–4] | 2 [2–3] | 16 [16–16] | 6 [6–6] | 240 [229–251] | 4.4 [4.4–4.5] | 0.15 [0.15–0.16] | 50 [49–51] | 51 [50–52] |
| sc4-constraints | rowsmith | 2 | 0 | 2/2 | 1 [1–1] | 0 [0–0] | 9 [8–10] | 0 [0–0] | 107 [95–119] | 1.7 [1.5–1.8] | 0.08 [0.07–0.09] | 21 [20–23] | 22 [20–23] |

## Failures (oracle reasons)


## Integrity

- Runs that touched paths outside their copy related to Rowsmith/benchmark internals: none
- Runs that changed production code: ['sc3-chain-insert__rowsmith__sonnet-high__r1', 'sc1-not-null-columns__baseline__sonnet-high__r1', 'sc3-chain-insert__baseline__sonnet-high__r1', 'sc2-required-fk__baseline__sonnet-high__r1', 'sc2-required-fk__rowsmith__sonnet-high__r2', 'sc3-chain-insert__baseline__sonnet-high__r2', 'sc2-required-fk__baseline__sonnet-high__r2']
- Timed out: none
- Rowsmith runs that loaded the skill: 8/8
- Rowsmith error messages seen by agents (runs with >=1): 0
