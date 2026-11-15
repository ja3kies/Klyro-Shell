#!/usr/bin/env python3
"""Fast, conservative regression selector: any unknown change runs everything."""
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def changed_files():
    base = os.environ.get("CI_BASE_SHA", "").strip()
    if not base or set(base) == {"0"}:
        return None
    result = subprocess.run(
        ["git", "diff", "--name-only", "--diff-filter=ACMRT", base, "HEAD"],
        cwd=ROOT, capture_output=True, text=True, check=False,
    )
    return result.stdout.splitlines() if result.returncode == 0 else None


def command_for(files):
    full = [sys.executable, "-m", "unittest", "discover", "-s", "tools", "-p", "test_*.py"]
    if files is None or not files:
        return full
    # Only isolated Python QA edits qualify; any Kotlin, Gradle, workflow or
    # runtime change must execute the entire suite.
    if any(not f.startswith("tools/") or not f.endswith(".py") for f in files):
        return full
    if any(not Path(f).name.startswith("test_") for f in files):
        return full
    tests = [f for f in files if Path(f).name.startswith("test_")]
    if not tests:
        return full
    return [sys.executable, "-m", "unittest", "discover", "-s", "tools", "-p", "test_*.py"] if len(tests) > 12 else [
        sys.executable, "-m", "unittest", *["tools." + Path(f).stem for f in tests]
    ]


if __name__ == "__main__":
    cmd = command_for(changed_files())
    print("Selected regression command:", " ".join(cmd), flush=True)
    raise SystemExit(subprocess.run(cmd, cwd=ROOT, check=False).returncode)
