#!/usr/bin/env bash
# Klyro daily-trial acceptance, executed INSIDE the Klyro Bash session.
# Non-destructive: no package installation, force-stop, or private Termux access.
set -u

VERSION="KLYRO-DAILY-TRIAL/v1"
ROOT="${HOME:?}/klyro-qa/daily-trial"
STATE="$ROOT/state"
LOG="$ROOT/background.log"
TASK="$ROOT/task.sh"
mkdir -p "$ROOT" || exit 2

pass=0; fail=0; skipped=0
pass(){ printf 'PASS  %s\n' "$1"; pass=$((pass+1)); }
fail(){ printf 'FAIL  %s\n' "$1"; fail=$((fail+1)); }
skip(){ printf 'NOT TESTED  %s\n' "$1"; skipped=$((skipped+1)); }
report(){
  printf '\nRESULT pass=%s fail=%s not_tested=%s\n' "$pass" "$fail" "$skipped"
  (( fail == 0 ))
}
usage(){
  cat <<'HELP'
Usage: bash klyro-daily-trial.sh inspect|prepare|verify|interactive
  inspect      shell/TTY/tools and small file/network checks (no installations)
  prepare      create a 2-minute file writer in background and record shell/job PIDs
  verify       verify same shell and counter progress after returning to Klyro
  interactive  print interactive read/Ctrl+C/nano/vim validation instructions

After prepare, leave Klyro using Home, switch to another app, return,
and run verify in THE SAME shell. Then test removal from Recents separately.
Run inspect before installing optional developer tools, and repeat afterwards.
This script NEVER installs packages or stops another process.
HELP
}
inspect(){
  printf '%s\n' "$VERSION"
  printf 'uid=%s user=%s home=%s prefix=%s shell_pid=%s\n' \
    "$(id -u)" "$(id -un)" "$HOME" "${PREFIX:-unset}" "$$"
  if [[ -n "${BASH_VERSION:-}" ]]; then pass "Bash interactive environment"; else fail "Bash missing"; fi
  if [[ -t 0 ]] && [[ -t 1 ]] && [[ -t 2 ]]; then pass "stdin/stdout/stderr PTY"; else fail "PTY not attached"; fi
  if stty size </dev/tty >/dev/null 2>&1; then pass "PTY size"; else fail "PTY size"; fi
  if (printf 'ready\n' | grep -qx ready); then pass "pipe"; else fail "pipe"; fi
  local temp
  temp="$(mktemp -d "$ROOT/inspect.XXXXXXXX")" || { fail "temporary directory"; report; return; }
  if (printf 'hello 😀\n' > "$temp/émoji space.txt" && \
      grep -qx 'hello 😀' "$temp/émoji space.txt"); then
    pass "Unicode, spaces and local filesystem"
  else fail "Unicode, spaces and local filesystem"; fi
  if (printf '#!/bin/sh\nprintf done' > "$temp/run.sh" && chmod 700 "$temp/run.sh" && \
      [[ "$("$temp/run.sh")" == "done" ]]); then pass "executable script"; else fail "executable script"; fi
  rm -rf -- "$temp"
  local name
  for name in apt pkg git python node npm clang make nano vim curl ssh; do
    if command -v "$name" >/dev/null 2>&1; then
      pass "executable: $name"
    else
      skip "executable not installed: $name"
    fi
  done
  if command -v curl >/dev/null 2>&1; then
    # Bounded network probe, public and non-mutating. Unreachable != app defect.
    if curl -fsSI --connect-timeout 5 --max-time 12 https://github.com/ >/dev/null 2>&1; then
      pass "HTTPS github.com HEAD"
    else
      skip "HTTPS probe unavailable: check network/TLS separately"
    fi
  fi
  report
}
prepare(){
  if [[ -f "$STATE" ]]; then
    local recorded
    recorded="$(sed -n 's/^job_pid=//p' "$STATE" | head -n 1)"
    if [[ "$recorded" =~ ^[0-9]+$ ]] && kill -0 "$recorded" 2>/dev/null; then
      printf 'Existing QA job %s is still running; do not start a duplicate.\n' "$recorded"
      return 1
    fi
  fi
  : > "$LOG"
  local token
  token="$(date +%s)-$$-${RANDOM}"
  printf '%s\n' '#!/usr/bin/env bash' \
    'for ((i=1; i<=60; i++)); do' \
    '  printf "%s %s\n" "$i" "$(date +%s)" >> "$1"' \
    '  sleep 2' \
    'done' > "$TASK"
  # Write no user-provided input into executable script.
  chmod 700 "$TASK"
  bash "$TASK" "$LOG" >/dev/null 2>&1 &
  local child=$!
  {
    printf 'token=%s\n' "$token"
    printf 'shell_pid=%s\n' "$PPID"
    printf 'job_pid=%s\n' "$child"
    printf 'started_at=%s\n' "$(date +%s)"
  } > "$STATE"
  printf 'READY token=%s parent_shell_pid=%s job_pid=%s\n' "$token" "$PPID" "$child"
  printf 'Press Android Home, use another app, then return to SAME Klyro shell.\n'
  printf 'Run: bash klyro-daily-trial.sh verify\n'
  printf 'Do not force-stop Klyro or uninstall it.\n'
}
verify(){
  if [[ ! -f "$STATE" ]]; then fail "prior prepare state missing"; report; return; fi
  local recorded_shell recorded_job started lines last old
  recorded_shell="$(sed -n 's/^shell_pid=//p' "$STATE" | head -n 1)"
  recorded_job="$(sed -n 's/^job_pid=//p' "$STATE" | head -n 1)"
  started="$(sed -n 's/^started_at=//p' "$STATE" | head -n 1)"
  # Each 'bash script.sh verify' invocation has its own PID. Compare
  # their interactive parent shell, not the short-lived script processes.
  if [[ "$recorded_shell" == "$PPID" ]]; then
    pass "Interactive Bash parent PID unchanged"
  else
    fail "Interactive Bash parent PID changed"
  fi
  if [[ ! "$recorded_job" =~ ^[0-9]+$ ]]; then fail "job PID malformed"; report; return; fi
  if [[ ! -s "$LOG" ]]; then fail "no background log written"; report; return; fi
  lines="$(wc -l < "$LOG")"
  last="$(tail -n 1 "$LOG")"
  if [[ "$lines" =~ ^[[:space:]]*[0-9]+$ ]] && (( lines >= 2 )); then
    pass "background job produced $lines heartbeat lines"
  else fail "background heartbeat absent"; fi
  printf 'last_heartbeat=%s\n' "$last"
  if [[ "$started" =~ ^[0-9]+$ ]] && [[ "$lines" =~ ^[[:space:]]*[0-9]+$ ]]; then
    old=$(( $(date +%s) - started ))
    if (( old >= 8 && lines >= 3 )); then
      pass "heartbeats progressed over an Android absence (confirm return manually)"
    else
      skip "background interval too short to demonstrate persistence"
    fi
  fi
  if kill -0 "$recorded_job" 2>/dev/null; then
    pass "background job PID still exists"
  elif (( lines >= 60 )); then
    pass "background job completed expected 60 iterations"
  else
    skip "job ended early, check system process state and log"
  fi
  # Do not delete evidence or kill the job.
  report
}
interactive(){
  cat <<'HELP'
MANUAL DEVICE CHECKS (only mark PASS after observing them):
1. Run: read -r -p 'Input: ' value; printf 'RECEIVED=%s\n' "$value"
   Type: bonjour Klyro, press Enter; compare exact text.
2. Run: cat; type two lines, then tap CTRL+C.
   Verify prompt returns and input is responsive.
3. Run: nano ~/klyro-qa/nano-test.txt (if installed).
   Type, save, exit; reopen file and verify text.
4. Run: vim ~/klyro-qa/vim-test.txt (if installed); enter/exit safely.
5. Open two distinct tabs; run echo $$ in both; confirm distinct PIDs.
6. While an active heartbeat runs, press Home / return; verify timestamps.
7. Separately swipe app from Recents, relaunch; note PID and job continuity.
8. Open Settings > Files and storage, test SAF folder picker and import/export.
   Revoke grant only in an expendable folder and verify a clear error.
NOT TESTED until a real Pixel run proves each item.
HELP
}
case "${1:-}" in
  inspect) inspect ;;
  prepare) prepare ;;
  verify) verify ;;
  interactive) interactive ;;
  *) usage; exit 2 ;;
esac
