#!/usr/bin/env python3
"""Builds a fresh, standalone copy of the lending demo for one task and condition.

usage: prepare.py <task-id> <baseline|rowsmith> <dest-dir>
"""
import json
import pathlib
import re
import shutil
import sys

from common import BENCH, DEMO, MIGRATIONS, OVERLAY, REPO, ROUND_DIR, task


def build_file(condition):
    text = (DEMO / "build.gradle.kts").read_text()
    if condition == "baseline":
        lines = []
        for line in text.splitlines():
            if re.search(r"rowsmith|Rowsmith|inRowsmithRepo", line):
                continue
            lines.append(line)
        text = "\n".join(lines) + "\n"
        text = re.sub(r"\n// Inside the .*?\n// published artifacts.*?\n", "\n", text)
    else:
        text = text.replace('val inRowsmithRepo = rootProject.name == "rowsmith"', "val inRowsmithRepo = false")
    return text


def prepare(task_id, condition, dest):
    t = task(task_id)
    dest = pathlib.Path(dest)
    if dest.exists():
        raise SystemExit(f"{dest} exists; use a fresh directory")
    dest.mkdir(parents=True)
    shutil.copytree(DEMO / "src" / "main", dest / "src" / "main")
    if (OVERLAY / "src" / "main").exists():  # round-specific schema and services, same in both conditions
        shutil.copytree(OVERLAY / "src" / "main", dest / "src" / "main", dirs_exist_ok=True)
    test_root = dest / "src" / "test" / "java" / "com" / "example" / "lending"
    test_root.mkdir(parents=True)
    shutil.copy(DEMO / "src/test/java/com/example/lending/TestcontainersConfiguration.java", test_root)
    if condition == "baseline":
        shutil.copytree(BENCH / "conditions/baseline/src/test", dest / "src" / "test", dirs_exist_ok=True)
    else:
        (test_root / "service").mkdir()
        for name in ["DelinquencyServiceTest.java", "CustomerSummaryServiceTest.java"]:
            shutil.copy(DEMO / "src/test/java/com/example/lending/service" / name, test_root / "service")
        shutil.copy(DEMO / "src/test/java/com/example/lending/Presets.java", test_root)
        shutil.copytree(REPO / "skills" / "rowsmith", dest / ".claude" / "skills" / "rowsmith")
    (dest / "build.gradle.kts").write_text(build_file(condition))
    (dest / "settings.gradle.kts").write_text('rootProject.name = "demo-app"\n')
    shutil.copy(REPO / "gradlew", dest / "gradlew")
    shutil.copytree(REPO / "gradle" / "wrapper", dest / "gradle" / "wrapper")
    (dest / ".gitignore").write_text(".gradle/\nbuild/\n")
    claude_md = (BENCH / "conditions/common-CLAUDE.md").read_text() + (BENCH / "conditions" / condition / "CLAUDE-testdata.md").read_text()
    (dest / "CLAUDE.md").write_text(claude_md)
    for rel in t.get("existing_tests", {}).get(condition, []):  # tests that already exist when the task starts
        shutil.copy(ROUND_DIR / rel, test_root / "service" / pathlib.Path(rel).name)
    if t["kind"] == "schema-change":
        shutil.copy(MIGRATIONS / t["migration"], dest / "src/main/resources/db/migration" / t["migration"])
    return dest


if __name__ == "__main__":
    print(prepare(sys.argv[1], sys.argv[2], sys.argv[3]))
