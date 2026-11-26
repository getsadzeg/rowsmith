#!/usr/bin/env python3
"""Prints a Markdown summary of results/runs.jsonl.

usage: summarize.py [--model sonnet-high]
Reports medians with interquartile ranges (IQR); small n, so read effects per task, not just pooled.
"""
import argparse
import json
import random
import statistics
from collections import defaultdict

from common import RESULTS as R


def q(values, p):
    v = sorted(values)
    if not v:
        return None
    k = (len(v) - 1) * p
    f, c = int(k), min(int(k) + 1, len(v) - 1)
    return v[f] + (v[c] - v[f]) * (k - f)


def med_iqr(values, fmt="{:.0f}"):
    values = [x for x in values if x is not None]
    if not values:
        return "n/a"
    return f"{fmt.format(statistics.median(values))} [{fmt.format(q(values, .25))}–{fmt.format(q(values, .75))}]"


def tokens_in(r):
    return (r.get("input_tokens") or 0) + (r.get("cache_creation_input_tokens") or 0) + (r.get("cache_read_input_tokens") or 0)


METRICS = [
    ("gradle runs", lambda r: r["gradle_runs"], "{:.0f}"),
    ("failed gradle runs", lambda r: r["gradle_failed_runs"], "{:.0f}"),
    ("turns", lambda r: r["num_turns"], "{:.0f}"),
    ("edits", lambda r: r.get("edits"), "{:.0f}"),
    ("input tokens (k, incl. cache)", lambda r: tokens_in(r) / 1000, "{:.0f}"),
    ("output tokens (k)", lambda r: (r.get("output_tokens") or 0) / 1000, "{:.1f}"),
    ("cost (USD)", lambda r: r["cost_usd"], "{:.2f}"),
    ("agent time (s)", lambda r: (r.get("duration_ms") or 0) / 1000, "{:.0f}"),
    ("wall time (s)", lambda r: r.get("wall_s"), "{:.0f}"),
]


def bootstrap_ratio(a, b, f, n=2000, seed=1):
    """Ratio of medians rowsmith/baseline with a bootstrap 90% interval."""
    a = [f(x) for x in a if f(x) is not None]
    b = [f(x) for x in b if f(x) is not None]
    if not a or not b or statistics.median(a) == 0:
        return None
    rnd = random.Random(seed)
    ratios = []
    for _ in range(n):
        sa = [rnd.choice(a) for _ in a]
        sb = [rnd.choice(b) for _ in b]
        ma = statistics.median(sa)
        if ma:
            ratios.append(statistics.median(sb) / ma)
    return statistics.median(b) / statistics.median(a), q(ratios, .05), q(ratios, .95)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default="sonnet-high")
    args = ap.parse_args()
    runs = [json.loads(l) for l in open(R / "runs.jsonl") if l.strip()]
    runs = [r for r in runs if f"{r['model']}-{r['effort']}" == args.model]
    by = defaultdict(list)
    for r in runs:
        by[(r["task"], r["condition"])].append(r)
    tasks = sorted({r["task"] for r in runs})
    conds = ["baseline", "rowsmith"]
    out = [f"# Benchmark summary ({args.model}, {len(runs)} runs)\n",
           "Medians with [interquartile range]. Success is decided by the hidden oracle, not by the agent.\n"]

    out.append("## Overall (all tasks pooled)\n")
    out.append("| Metric | baseline | rowsmith | rowsmith / baseline (median ratio, 90% bootstrap CI) |")
    out.append("|---|---|---|---|")
    pooled = {c: [r for r in runs if r["condition"] == c] for c in conds}
    sr = {c: (sum(r["success"] for r in pooled[c]), len(pooled[c])) for c in conds}
    out.append(f"| success | {sr['baseline'][0]}/{sr['baseline'][1]} | {sr['rowsmith'][0]}/{sr['rowsmith'][1]} | |")
    for name, f, fmt in METRICS:
        ratio = bootstrap_ratio(pooled["baseline"], pooled["rowsmith"], f)
        rtxt = "n/a" if ratio is None else f"{ratio[0]:.2f} ({ratio[1]:.2f}–{ratio[2]:.2f})"
        out.append(f"| {name} | {med_iqr([f(r) for r in pooled['baseline']], fmt)} | {med_iqr([f(r) for r in pooled['rowsmith']], fmt)} | {rtxt} |")
    total = sum(r["cost_usd"] or 0 for r in runs)
    out.append(f"\nTotal agent spend for these runs: ${total:.2f}\n")

    # schema-change rounds: tests the migration breaks in the untouched suite (validate_oracle.py output)
    broken = {}
    if (R / "oracle-validation.jsonl").exists():
        for l in open(R / "oracle-validation.jsonl"):
            v = json.loads(l)
            if v[0].endswith("-untouched"):
                broken[(v[1], v[0].split("-")[0])] = len(v[5] or [])
    out.append("## Per task\n")
    out.append("| Task | Condition | n | Tests broken | Success | Gradle runs | Failed gradle runs | Turns | Edits | Input tok (k) | Output tok (k) | Cost $ | Agent s | Wall s |")
    out.append("|---|---|---:|---:|---:|---|---|---|---|---|---|---|---|---|")
    for t in tasks:
        for c in conds:
            rs = by[(t, c)]
            if not rs:
                continue
            cells = [med_iqr([f(r) for r in rs], fmt) for _, f, fmt in METRICS]
            ok = sum(r["success"] for r in rs)
            out.append(f"| {t} | {c} | {len(rs)} | {broken.get((t, c), 'n/a')} | {ok}/{len(rs)} | " + " | ".join(cells) + " |")

    out.append("\n## Failures (oracle reasons)\n")
    for r in sorted(runs, key=lambda r: r["run_id"]):
        if not r["success"]:
            o = r.get("oracle") or {}
            out.append(f"- {r['run_id']}: {o.get('reason')}" + (f"; production code changed: {o.get('main_changed')}" if o.get("main_changed") else ""))
    flagged = [r["run_id"] for r in runs if r.get("contamination")]
    out.append("\n## Integrity\n")
    out.append(f"- Runs that touched paths outside their copy related to Rowsmith/benchmark internals: {flagged or 'none'}")
    out.append(f"- Runs that changed production code: {[r['run_id'] for r in runs if (r.get('oracle') or {}).get('main_changed')] or 'none'}")
    out.append(f"- Timed out: {[r['run_id'] for r in runs if r.get('timed_out')] or 'none'}")
    skill_used = [r["run_id"] for r in runs if r["condition"] == "rowsmith" and (r.get("tools") or {}).get("Skill")]
    out.append(f"- Rowsmith runs that loaded the skill: {len(skill_used)}/{len(pooled['rowsmith'])}")
    out.append(f"- Rowsmith error messages seen by agents (runs with >=1): {sum(1 for r in pooled['rowsmith'] if r.get('rowsmith_errors_seen'))}")

    print("\n".join(out))


if __name__ == "__main__":
    main()
