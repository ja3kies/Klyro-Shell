#!/usr/bin/env bash
# Klyro Shell non-destructive functional smoke test. Run inside Klyro Bash.
set -u
pass=0; fail=0
check() {
  local name="$1"; shift
  if "$@" >/dev/null 2>&1; then
    printf 'PASS %s\n' "$name"; pass=$((pass+1))
  else
    printf 'FAIL %s\n' "$name"; fail=$((fail+1))
  fi
}
check 'Bash available' bash -c 'exit 0'
check 'UID readable' id -u
check 'Current directory readable' pwd
check 'HOME accessible' test -d "$HOME"
check 'PATH configured' test -n "${PATH:-}"
check 'APT executable' command -v apt
check 'pkg executable' command -v pkg
check 'Pipelines' bash -c 'printf "ok\\n" | grep -qx ok'
check 'Subshell' bash -c '(exit 0)'
check 'Environment variables' bash -c 'KLYRO_TEST=ok; export KLYRO_TEST; bash -c '\''test "$KLYRO_TEST" = ok'\'''
tmp="$(mktemp -d "${TMPDIR:-$HOME}/klyro-smoke.XXXXXXXX" 2>/dev/null)" || {
  echo 'FAIL Temporary directory creation'; exit 1;
}
trap 'rm -rf -- "$tmp"' EXIT
check 'File write' bash -c 'printf "klyro\\n" > "$1"' _ "$tmp/sample"
check 'File read' bash -c 'test "$(cat "$1")" = klyro' _ "$tmp/sample"
check 'Directory navigation' bash -c 'cd "$1" && test "$PWD" = "$1"' _ "$tmp"
check 'Executable script' bash -c 'printf "#!/bin/sh\\nexit 0\\n" > "$1"; chmod +x "$1"; "$1"' _ "$tmp/run.sh"
printf '\nRESULT %s passed, %s failed\n' "$pass" "$fail"
[ "$fail" -eq 0 ]
