#!/usr/bin/env bash
# One-command, non-destructive Klyro device QA harness.
# Run from a clone of the same branch: bash scripts/klyro-qa.sh
set -Eeuo pipefail
umask 077
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
command -v python3 >/dev/null || { echo "FAIL: python3 missing" >&2; exit 1; }
command -v adb >/dev/null || { echo "FAIL: adb missing" >&2; exit 1; }
STAMP="$(date +%Y%m%d-%H%M%S)"
DEFAULT_OUT="$HOME/storage/shared/Klyro-Shell/qa-$STAMP"
OUT="${KLYRO_QA_OUT:-$DEFAULT_OUT}"
mkdir -p "$OUT"
# Always test the currently installed APK; do not force-stop, clear data,
# change runtime settings or install untrusted package artifacts.
set +e
python3 "$ROOT/tools/klyro_ui_robot.py" --mode smoke --output "$OUT" "$@"
RESULT=$?
set -e
echo "Klyro QA artifacts: $OUT"
echo "HTML: $OUT/report.html"
echo "JSON: $OUT/results.json"
echo "Screenshots and XML: $OUT"
echo "ZIP: $OUT.zip"
if [[ -f "$OUT/accessibility-audit.md" ]]; then
  echo "Accessibility: $OUT/accessibility-audit.md"
fi
exit "$RESULT"
