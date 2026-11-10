#!/usr/bin/env bash
# Run current Klyro-Shell UI robot without requiring a Git checkout.
# Run in Termux: bash <(curl -fsSL https://raw.githubusercontent.com/ja3kies/Klyro-Shell/p1/ux-cross-screen-final-pass-20261110/scripts/klyro-qa-bootstrap.sh)
set -Eeuo pipefail
umask 077

command -v curl >/dev/null || { echo "FAIL: curl is required" >&2; exit 2; }
command -v python3 >/dev/null || { echo "FAIL: python3 is required" >&2; exit 2; }
command -v adb >/dev/null || { echo "FAIL: adb is required" >&2; exit 2; }

SHARED="$HOME/storage/shared"
if [[ ! -d "$SHARED" || ! -w "$SHARED" ]]; then
  echo "FAIL: Shared storage unavailable. Run termux-setup-storage once and allow storage access." >&2
  exit 2
fi

TMP_ROOT="${TMPDIR:-$HOME}"
ROBOT_TMP="$(mktemp -d "$TMP_ROOT/klyro-qa-XXXXXXXX")"
trap 'rm -rf "$ROBOT_TMP"' EXIT

SOURCE="https://raw.githubusercontent.com/ja3kies/Klyro-Shell/p1/ux-cross-screen-final-pass-20261110/tools"
for NAME in klyro_ui_robot.py klyro_robot_report_audit.py klyro_ux_report.py; do
  curl --fail --location --silent --show-error --retry 3 \
    "$SOURCE/$NAME" --output "$ROBOT_TMP/$NAME"
  test -s "$ROBOT_TMP/$NAME"
done

# Exact Kotlin code is included read-only in the detailed UI/UX report.
# Never generate fake source snippets from UIAutomator text.
KOTLIN_ROOT="app/src/main/java/com/oryn/terminal"
for REL in \
  "$KOTLIN_ROOT/MainActivity.kt" \
  "$KOTLIN_ROOT/ui/KlyroPluginsPage.kt" \
  "$KOTLIN_ROOT/ui/KlyroSettingsSheet.kt" \
  "$KOTLIN_ROOT/ui/KlyroUiComponents.kt"; do
  mkdir -p "$ROBOT_TMP/$(dirname "$REL")"
  curl --fail --location --silent --show-error --retry 3 \
    "https://raw.githubusercontent.com/ja3kies/Klyro-Shell/p1/ux-cross-screen-final-pass-20261110/$REL" \
    --output "$ROBOT_TMP/$REL"
  test -s "$ROBOT_TMP/$REL"
done

python3 -m py_compile "$ROBOT_TMP/klyro_ui_robot.py" "$ROBOT_TMP/klyro_robot_report_audit.py" "$ROBOT_TMP/klyro_ux_report.py"

STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="${KLYRO_QA_OUT:-$SHARED/Klyro-Shell/qa-$STAMP}"
mkdir -p "$OUT"

# Only safe navigation actions by default. Never force-stop, install plugins,
# execute commands, clear app data, or terminate a running shell.
set +e
python3 "$ROBOT_TMP/klyro_ui_robot.py" --mode smoke --output "$OUT" --source-root "$ROBOT_TMP" "$@"
CODE=$?
set -e
echo "Result: $OUT/report.html"
echo "Component code and UX review: $OUT/ux-review.html"
echo "Evidence: $OUT.zip"
echo "Accessibility: $OUT/accessibility-audit.md"
exit "$CODE"
