#!/usr/bin/env python3
"""Small, always-run CI build summary without external dependencies."""
import os
from pathlib import Path

root = Path(__file__).resolve().parents[1]
apk = root / "app/build/outputs/apk/debug/app-debug.apk"
report = [
    "## Klyro CI build summary",
    "",
    f"- Commit: `{os.environ.get('GITHUB_SHA', 'local')}`",
    f"- Run: {os.environ.get('GITHUB_RUN_NUMBER', 'local')} / attempt {os.environ.get('GITHUB_RUN_ATTEMPT', '1')}",
    f"- Fast gate: {os.environ.get('FAST_GATE_RESULT', 'unknown')}",
    f"- Android build job: {os.environ.get('ANDROID_JOB_RESULT', 'unknown')}",
    f"- Debug APK present: {'yes' if apk.is_file() else 'no'}",
    f"- Debug APK size: {apk.stat().st_size if apk.is_file() else 'unavailable'} bytes",
    "",
    "Physical-device acceptance is separate from CI; no device result is inferred.",
]
text = "\n".join(report) + "\n"
Path("klyro-build-summary.md").write_text(text, encoding="utf-8")
if os.environ.get("GITHUB_STEP_SUMMARY"):
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as stream:
        stream.write(text)
print(text)
