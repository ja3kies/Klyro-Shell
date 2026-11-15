#!/usr/bin/env bash
# Capture non-destructive evidence of Klyro freezing on return from background.
# Run in Termux (with paired, connected adb) while or shortly after the freeze.
set -Eeuo pipefail
umask 077

if ! command -v adb >/dev/null 2>&1; then
  echo "ADB is required (run in Termux after connecting wireless debugging)." >&2
  exit 2
fi
if ! adb devices | grep -q $'\tdevice$'; then
  echo "No authorized ADB device. Pair/connect ADB first." >&2
  exit 2
fi

OUT="${KLYRO_FREEZE_OUT:-$HOME/klyro-freeze-reports/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$OUT"
printf 'Klyro freeze evidence (no shell restart, no force-stop)\n' > "$OUT/README.txt"
printf 'Captured: %s\n' "$(date -Is)" >> "$OUT/README.txt"
printf 'Report may contain personal process names, logs or file paths. Review before sharing.\n' >> "$OUT/README.txt"

run() {
  local name="$1"; shift
  timeout 25 adb "$@" > "$OUT/$name" 2>&1 || true
}

run package-uids.txt shell cmd package list packages -U
grep -E 'package:(com\.klyro\.shell|com\.termux)( |$)' "$OUT/package-uids.txt"   > "$OUT/target-app-uids.txt" || true
run app-pid.txt shell pidof com.klyro.shell
run app-process-state.txt shell dumpsys activity processes
run activity-state.txt shell dumpsys activity activities
run app-service-state.txt shell dumpsys activity services com.klyro.shell
run app-memory.txt shell dumpsys meminfo com.klyro.shell
run app-exit-history.txt shell dumpsys activity exit-info com.klyro.shell

# Only incident-related log tags and known ANR keywords, not full unfiltered
# device logcat. Even filtered logs can be sensitive.
timeout 30 adb logcat -d -v threadtime -t 12000 2>/dev/null |
  grep -Ei 'ANR in com\.klyro\.shell|Application Not Responding|Input dispatching timed out|FATAL EXCEPTION|com\.klyro\.shell|KlyroLifecycle|KlyroVerify|Choreographer|InputDispatcher|WindowManager'   | tail -n 1800 > "$OUT/incident-logcat.txt" || true

# Retain the small, relevant subset of system state by default.
grep -Ei 'com\.klyro\.shell|mResumedActivity|topResumedActivity|ANR' "$OUT/activity-state.txt"   > "$OUT/activity-klyro.txt" || true
grep -Ei -C 3 'com\.klyro\.shell|ANR|cached|foreground' "$OUT/app-process-state.txt"   > "$OUT/process-klyro.txt" || true
# Remove broad system-state dumps once the focused extraction is complete.
rm -f "$OUT/activity-state.txt" "$OUT/app-process-state.txt" "$OUT/package-uids.txt"

printf '\nEvidence saved: %s\n' "$OUT"
printf 'View app UID: %s\n' "$OUT/target-app-uids.txt"
printf 'View incident logs: %s\n' "$OUT/incident-logcat.txt"
printf 'Review logs before sharing; no app/process was stopped or modified.\n'
