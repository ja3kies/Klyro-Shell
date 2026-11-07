#!/usr/bin/env bash
# P0: non-destructive operational acceptance, run *inside Klyro Bash*.
# No pkg update/install/upgrade and no access to stock Termux sandbox.
set -u
passed=0; failed=0
pass(){ printf 'PASS %-24s\n' "$1"; passed=$((passed+1)); }
fail(){ printf 'FAIL %-24s\n' "$1"; failed=$((failed+1)); }
check(){
  local label="$1"; shift
  if "$@" >/dev/null 2>&1; then pass "$label"; else fail "$label"; fi
}
printf 'KLYRO-OPERATIONAL-GATE/v1\n'
check 'Bash process' bash -c 'test -n "$BASH_VERSION"'
check 'UID readable' id -u
check 'HOME exists' test -d "$HOME"
check 'Shell working dir' pwd
check 'PTY available' tty -s
check 'Terminal dimensions' bash -c 'test -n "$(stty size 2>/dev/null)"'
check 'TERM configured' test -n "${TERM:-}"
check 'UTF-8 locale' bash -c 'test -n "${LANG:-}"'
check 'APT reads version' apt --version
check 'pkg present' bash -c 'command -v pkg'
check 'Bash pipeline' bash -c 'printf "hello\n" | grep -qx hello'
check 'Subshell exit' bash -c '(exit 0)'
check 'Unicode encoding' bash -c 'test "$(printf "é")" = "é"'
check 'ANSI sequence' bash -c 'test "$(printf "\033[32mX\033[0m" | wc -c)" -ge 7'
TMPBASE="${TMPDIR:-$HOME}"
if test -d "$TMPBASE"; then
  work="$(mktemp -d "$TMPBASE/klyro-operational-XXXXXXXX" 2>/dev/null || true)"
else
  work=""
fi
if test -z "$work"; then
  fail 'Temporary directory'
else
  pass 'Temporary directory'
  trap 'rm -rf -- "$work"' EXIT
  check 'Write file' bash -c 'printf "%s\n" klyro > "$1"' _ "$work/check.txt"
  check 'Read file' bash -c 'test "$(cat "$1")" = klyro' _ "$work/check.txt"
  check 'Execute script' bash -c 'printf "#!/bin/sh\nexit 0\n" > "$1"; chmod 700 "$1"; "$1"' _ "$work/test.sh"
  check 'Change directory' bash -c 'cd "$1" && test "$PWD" = "$1"' _ "$work"
fi
printf 'TOTAL pass=%d fail=%d\n' "$passed" "$failed"
if test "$failed" -ne 0; then exit 1; fi
