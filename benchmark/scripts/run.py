#!/usr/bin/env python3
"""Runs coding agents headlessly (Claude Code CLI) on every task in both conditions and judges them.

usage: run.py --work DIR [--tasks t1,t2|all] [--conditions baseline,rowsmith] [--reps 3]
              [--model sonnet] [--effort high] [--parallel 3] [--budget 5]

Each run gets a fresh copy of the demo (prepare.py), runs `claude -p` inside it, then the hidden
oracle (oracle.py). One JSON line per run is appended to results/runs.jsonl (resumable).
"""
import argparse
import gzip
import json
import pathlib
import random
import re
import shutil
import subprocess
import threading
import time
from concurrent.futures import ThreadPoolExecutor

from common import BENCH, RESULTS, TASKS, prompt_for, task
from oracle import verdict
from prepare import prepare

LOCK = threading.Lock()
SPEND = {"spent": 0.0, "inflight": 0, "total": None}  # hard cap across the whole batch (--total-budget)

# Hard rule for this benchmark: Opus at most medium effort, Sonnet at least high.
ALLOWED = {"sonnet": {"high", "xhigh", "max"}, "opus": {"low", "medium"}}

ALLOWED_TOOLS = [
    "Bash(./gradlew:*)", "Bash(ls:*)", "Bash(cat:*)", "Bash(find:*)", "Bash(grep:*)", "Bash(head:*)",
    "Bash(tail:*)", "Bash(wc:*)", "Bash(sed -n:*)", "Bash(mkdir:*)", "Bash(git diff:*)", "Bash(git status:*)",
    "Bash(docker ps:*)",
]
CONTAMINATION = re.compile(r"/Users/[^/]+/rowsmith(?!-)|rowsmith/benchmark|oracle\.py|tasks\.json|\.m2/repository/io/github/getsadzeg/rowsmith")
# a Gradle invocation that compiles or tests (not just a path like src/test in some other command)
GRADLE = re.compile(r"(?:^|[;&|(]|\s)(?:\./)?gradlew\b[^;&|\n]*\b(?:test|build|check|compileTestJava|compileJava)\b")
BLOCKED = "Permission for this tool use was denied"


def verify_effort(model, effort):
    if effort not in ALLOWED.get(model, set()):
        raise SystemExit(f"refusing to run {model} at effort {effort}: rule is Opus <= medium, Sonnet >= high")
    out = subprocess.run(["claude", "-p", "/model", "--model", model, "--effort", effort, "--output-format", "json",
                          "--setting-sources", "project", "--strict-mcp-config", "--no-session-persistence"],
                         capture_output=True, text=True, stdin=subprocess.DEVNULL, timeout=120)
    line = json.loads(out.stdout)["result"].splitlines()[0]
    if f"(effort: {effort})" not in line:
        raise SystemExit(f"effort not applied: {line}")
    return line


def analyze(transcript_path):
    tool_names = {}
    gradle_runs = 0
    gradle_failures = 0
    blocked_gradle = 0
    bash_calls = 0
    edits = 0
    contamination = []
    pending = {}
    init = {}
    result = {}
    rowsmith_errors = 0
    for line in open(transcript_path):
        try:
            ev = json.loads(line)
        except json.JSONDecodeError:
            continue
        if ev.get("type") == "system" and ev.get("subtype") == "init":
            init = {"model": ev.get("model"), "skills": ev.get("skills"), "version": ev.get("claude_code_version")}
        if ev.get("type") == "result":
            result = ev
        msg = ev.get("message") if isinstance(ev.get("message"), dict) else {}
        for block in msg.get("content") or []:
            if not isinstance(block, dict):
                continue
            if block.get("type") == "tool_use":
                name = block.get("name")
                tool_names[name] = tool_names.get(name, 0) + 1
                text = json.dumps(block.get("input"))
                if CONTAMINATION.search(text):
                    contamination.append(text[:200])
                if name in ("Edit", "Write"):
                    edits += 1
                if name == "Bash":
                    bash_calls += 1
                    cmd = block["input"].get("command", "")
                    if GRADLE.search(cmd):
                        pending[block["id"]] = True
            if block.get("type") == "tool_result" and block.get("tool_use_id") in pending:
                content = block.get("content")
                text = json.dumps(content) if not isinstance(content, str) else content
                if block.get("is_error") and BLOCKED in text:  # refused by the allow-list: Gradle never ran
                    blocked_gradle += 1
                    continue
                gradle_runs += 1
                if block.get("is_error") or "BUILD FAILED" in text or "FAILED" in text:
                    gradle_failures += 1
                if "Rowsmith:" in text:
                    rowsmith_errors += 1
    usage = result.get("usage") or {}
    return {
        "init": init,
        "gradle_runs": gradle_runs,
        "gradle_failed_runs": gradle_failures,
        "gradle_blocked_calls": blocked_gradle,
        "bash_calls": bash_calls,
        "edits": edits,
        "tools": tool_names,
        "rowsmith_errors_seen": rowsmith_errors,
        "contamination": contamination[:5],
        "num_turns": result.get("num_turns"),
        "cost_usd": result.get("total_cost_usd"),
        "duration_ms": result.get("duration_ms"),
        "result_subtype": result.get("subtype"),
        "is_error": result.get("is_error"),
        "input_tokens": usage.get("input_tokens"),
        "cache_creation_input_tokens": usage.get("cache_creation_input_tokens"),
        "cache_read_input_tokens": usage.get("cache_read_input_tokens"),
        "output_tokens": usage.get("output_tokens"),
        "model_usage": result.get("modelUsage"),
    }


def one_run(work, task_id, condition, model, effort, rep, budget):
    run_id = f"{task_id}__{condition}__{model}-{effort}__r{rep}"
    with LOCK:  # never start a run that could push the batch over the total budget
        if SPEND["total"] is not None and SPEND["spent"] + (SPEND["inflight"] + 1) * budget > SPEND["total"]:
            print(f"{run_id}: skipped, total budget (spent ${SPEND['spent']:.2f}, {SPEND['inflight']} in flight)", flush=True)
            return None
        SPEND["inflight"] += 1
    try:
        return _one_run(work, task_id, condition, model, effort, rep, budget, run_id)
    finally:
        with LOCK:
            SPEND["inflight"] -= 1


def _one_run(work, task_id, condition, model, effort, rep, budget, run_id):
    base = pathlib.Path(work) / run_id
    shutil.rmtree(base, ignore_errors=True)
    repo = prepare(task_id, condition, base / "repo")
    t = task(task_id)
    cmd = ["claude", "-p", prompt_for(t), "--model", model, "--effort", effort,
           "--output-format", "stream-json", "--verbose",
           "--setting-sources", "project", "--strict-mcp-config", "--no-session-persistence",
           "--permission-mode", "acceptEdits", "--permission-prompts", "none",
           "--tools", "Bash,Read,Edit,Write,Skill",
           "--max-budget-usd", str(budget),
           "--allowedTools", *ALLOWED_TOOLS]
    transcript = base / "transcript.jsonl"
    start = time.time()
    timed_out = False
    with open(transcript, "w") as out:
        try:
            subprocess.run(cmd, cwd=repo, stdout=out, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, timeout=40 * 60)
        except subprocess.TimeoutExpired:
            timed_out = True
    wall = time.time() - start
    stats = analyze(transcript)
    v = verdict(task_id, condition, repo, base / "oracle")
    v.pop("log_tail", None)
    record = {"run_id": run_id, "task": task_id, "condition": condition, "model": model, "effort": effort, "rep": rep,
              "wall_s": round(wall, 1), "timed_out": timed_out, "success": bool(v.get("success")),
              "oracle": v, **stats, "finished_at": time.strftime("%Y-%m-%dT%H:%M:%S")}
    art = RESULTS / "artifacts" / run_id
    shutil.rmtree(art, ignore_errors=True)
    (art).mkdir(parents=True)
    with open(transcript, "rb") as src, gzip.open(art / "transcript.jsonl.gz", "wb") as dst:
        shutil.copyfileobj(src, dst)
    for f in (repo / "src/test").rglob("*.java"):
        target = art / "src-test" / f.relative_to(repo / "src/test")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(f, target)
    for f in (repo / "src/main/resources").glob("rowsmith.yaml"):
        shutil.copy(f, art / "rowsmith.yaml")
    with LOCK:
        SPEND["spent"] += stats["cost_usd"] or 0
        with open(RESULTS / "runs.jsonl", "a") as fh:
            fh.write(json.dumps(record) + "\n")
    print(f"{run_id}: success={record['success']} gradle={stats['gradle_runs']} turns={stats['num_turns']} "
          f"cost={stats['cost_usd']} wall={wall:.0f}s", flush=True)
    shutil.rmtree(base / "oracle", ignore_errors=True)
    return record


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--work", required=True)
    ap.add_argument("--tasks", default="all")
    ap.add_argument("--conditions", default="baseline,rowsmith")
    ap.add_argument("--reps", type=int, default=3)
    ap.add_argument("--rep-start", type=int, default=1)
    ap.add_argument("--model", default="sonnet")
    ap.add_argument("--effort", default="high")
    ap.add_argument("--parallel", type=int, default=3)
    ap.add_argument("--budget", type=float, default=5.0, help="per-run cap passed to claude --max-budget-usd")
    ap.add_argument("--total-budget", type=float, default=None, help="hard cap for all runs in this results file")
    args = ap.parse_args()
    print(verify_effort(args.model, args.effort), flush=True)
    ids = [t["id"] for t in TASKS["tasks"]] if args.tasks == "all" else args.tasks.split(",")
    done = set()
    if (RESULTS / "runs.jsonl").exists():
        prior = [json.loads(l) for l in open(RESULTS / "runs.jsonl") if l.strip()]
        done = {r["run_id"] for r in prior}
        SPEND["spent"] = sum(r.get("cost_usd") or 0 for r in prior)
    SPEND["total"] = args.total_budget
    if args.total_budget is not None:
        print(f"total budget ${args.total_budget:.2f}, already spent ${SPEND['spent']:.2f}", flush=True)
    jobs = []
    for rep in range(args.rep_start, args.rep_start + args.reps):
        batch = [(t, c) for t in ids for c in args.conditions.split(",")]
        random.Random(rep).shuffle(batch)  # interleave conditions so time-of-day effects hit both
        for t, c in batch:
            run_id = f"{t}__{c}__{args.model}-{args.effort}__r{rep}"
            if run_id not in done:
                jobs.append((args.work, t, c, args.model, args.effort, rep, args.budget))
    print(f"{len(jobs)} runs to do", flush=True)
    RESULTS.mkdir(parents=True, exist_ok=True)
    with ThreadPoolExecutor(max_workers=args.parallel) as ex:
        list(ex.map(lambda j: one_run(*j), jobs))


if __name__ == "__main__":
    main()
