#!/usr/bin/env python3
"""Always-run CI build summary; inspect the final signed APK when available."""
import os
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
debug_dir = root / "app/build/outputs/apk/debug"
apk = next((p for p in (
    debug_dir / "klyro-shell-signed.apk",
    debug_dir / "app-debug.apk",
) if p.is_file()), None)
metadata = "unavailable"
if apk is not None:
    sdk = Path(os.environ.get("ANDROID_HOME", "")) / "build-tools/36.0.0/aapt"
    if sdk.is_file():
        result = subprocess.run([str(sdk), "dump", "badging", str(apk)],
                                capture_output=True, text=True, check=False)
        if result.returncode == 0:
            match = re.search(r"versionCode='([^']+)' versionName='([^']+)'", result.stdout)
            if match:
                metadata = f"{match.group(2)} (versionCode {match.group(1)})"

report = [
    "## Klyro CI build summary",
    "",
    f"- Commit: `{os.environ.get('GITHUB_SHA', 'local')}`",
    f"- Run: {os.environ.get('GITHUB_RUN_NUMBER', 'local')} / attempt {os.environ.get('GITHUB_RUN_ATTEMPT', '1')}",
    f"- Fast gate: {os.environ.get('FAST_GATE_RESULT', 'unknown')}",
    f"- Android build job: {os.environ.get('ANDROID_JOB_RESULT', 'unknown')}",
    f"- APK versionName / versionCode: {metadata}",
    f"- APK file: {apk.name if apk else 'unavailable'}",
    f"- APK size: {apk.stat().st_size if apk else 'unavailable'} bytes",
    "",
    "Physical-device acceptance is separate from CI; no device result is inferred.",
]
text = "\n".join(report) + "\n"
Path("klyro-build-summary.md").write_text(text, encoding="utf-8")
if os.environ.get("GITHUB_STEP_SUMMARY"):
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as stream:
        stream.write(text)
print(text)
