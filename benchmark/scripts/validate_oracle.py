#!/usr/bin/env python3
"""Sanity check for the oracle: the untouched suite must be rejected, the reference fixes must pass.

usage: validate_oracle.py <work-dir> [untouched|fixed]
"""
import json
import pathlib
import shutil
import sys
from concurrent.futures import ThreadPoolExecutor

from common import ROUND_DIR, TASKS
from oracle import verdict
from prepare import prepare


def check_schema_change(work, task_id, condition, fixed):
    """fixed=False: the untouched repo must be rejected; fixed=True: refs/<task>/<condition> files make it pass."""
    base = pathlib.Path(work) / f"{task_id}-{condition}-{'fixed' if fixed else 'untouched'}"
    shutil.rmtree(base, ignore_errors=True)
    repo = prepare(task_id, condition, base / "agent")
    if fixed:
        fix = ROUND_DIR / "refs" / task_id / condition
        for f in (fix.rglob("*") if fix.exists() else []):
            if f.is_file():
                target = repo / f.relative_to(fix)
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy(f, target)
    v = verdict(task_id, condition, repo, base / "oracle")
    return f"{condition}-{'fixed' if fixed else 'untouched'}", task_id, v.get("success"), v.get("checks"), v.get("reason"), v.get("missing_or_failing")


def main_schema_changes(work, fixed_modes=(False, True)):
    """Untouched repo per task and condition (records how many tests the migration breaks), then the reference fix."""
    jobs = [lambda t=t["id"], c=c, f=f: check_schema_change(work, t, c, f)
            for t in TASKS["tasks"] for c in ["baseline", "rowsmith"] for f in fixed_modes]
    with ThreadPoolExecutor(max_workers=3) as ex:
        for r in ex.map(lambda j: j(), jobs):
            print(json.dumps(r), flush=True)


if __name__ == "__main__":
    modes = {"untouched": (False,), "fixed": (True,)}.get(sys.argv[2] if len(sys.argv) > 2 else "", (False, True))
    main_schema_changes(sys.argv[1], modes)
