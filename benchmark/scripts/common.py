"""Shared helpers for the Rowsmith benchmark harness."""
import hashlib
import json
import os
import pathlib
import subprocess

BENCH = pathlib.Path(__file__).resolve().parent.parent
REPO = BENCH.parent
DEMO = REPO / "examples" / "demo-app"
ROUND_DIR = BENCH / "schema-changes"
TASKS = json.loads((ROUND_DIR / "tasks.json").read_text())
RESULTS = BENCH / "results" / "schema-changes"
OVERLAY = ROUND_DIR / "overlay"
MIGRATIONS = ROUND_DIR / "migrations"

def task(task_id):
    for t in TASKS["tasks"]:
        if t["id"] == task_id:
            return t
    raise SystemExit(f"unknown task {task_id}")


def prompt_for(t):
    return t["prompt"]


def tree_hash(root, rel="src/main"):
    """sha256 of every file under root/rel, keyed by relative path."""
    out = {}
    base = pathlib.Path(root) / rel
    for f in sorted(base.rglob("*")):
        if f.is_file():
            out[str(f.relative_to(root))] = hashlib.sha256(f.read_bytes()).hexdigest()
    return out


def run(cmd, cwd, timeout, env=None):
    e = dict(os.environ)
    if env:
        e.update(env)
    p = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, timeout=timeout, env=e)
    return p.returncode, p.stdout + p.stderr


def junit_results(repo, class_filter=None):
    """[(classname, testname, status)] from build/test-results/test/*.xml"""
    import xml.etree.ElementTree as ET
    res = []
    d = pathlib.Path(repo) / "build" / "test-results" / "test"
    if not d.exists():
        return res
    for x in d.glob("TEST-*.xml"):
        root = ET.parse(x).getroot()
        for tc in root.iter("testcase"):
            cls = tc.get("classname")
            if class_filter and cls != class_filter:
                continue
            status = "passed"
            if tc.find("failure") is not None or tc.find("error") is not None:
                status = "failed"
            elif tc.find("skipped") is not None:
                status = "skipped"
            res.append((cls, tc.get("name"), status))
    return res
