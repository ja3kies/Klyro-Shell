# Klyro — Pixel 9 daily-trial acceptance

Status: **PENDING PHYSICAL VALIDATION**. Code-level tests and a compiled APK
are not equivalent to observing Klyro across Android background/recents.

## Scope and safeguards

The script `scripts/klyro-daily-trial.sh` is run **inside Klyro** and creates
files only under `$HOME/klyro-qa/daily-trial`. No packages are installed,
no sessions are closed and no Android permissions are modified.

Termux is not required for these Bash tests, except as an optional ADB host
for independent package UID verification. Do not uninstall your Termux app.
Avoid updating Klyro while any session or background job is active.

## Baseline and installed tools

From Klyro's Bash prompt, copy:

```bash
curl -fsSL https://raw.githubusercontent.com/ja3kies/Klyro-Shell/feat/production-ready-terminal/scripts/klyro-daily-trial.sh -o ~/klyro-daily-trial.sh
bash ~/klyro-daily-trial.sh inspect
```

Expected: Bash, PTY, pipe, Unicode filesystem and script execution PASS. Git,
Python, Node/npm and development tools that aren't installed are **NOT
TESTED**, not FAIL or PASS. An unavailable network probe also remains NOT
TESTED. Installation testing is a separate deliberate operation.

Optional package installation (only when the user explicitly chooses to modify
their Klyro package environment, with sufficient network and storage):

```bash
pkg update
pkg install git python nodejs nano vim clang make
bash ~/klyro-daily-trial.sh inspect
```

Never run these in a Termux terminal if your intent is to test the isolated
Klyro environment; verify the active app first.

## Background job with retained shell PID

```bash
bash ~/klyro-daily-trial.sh prepare
```

It starts a 60-iteration background heartbeat in a new file, two seconds
apart, and records the *interactive parent shell PID* rather than the
short-lived script PID. Now press Android Home, open another app for at least
10 seconds, then return to the **same session** and run:

```bash
bash ~/klyro-daily-trial.sh verify
```

Verify that the shell parent PID is unchanged and that heartbeat lines have
increased while Klyro was backgrounded. A script cannot tell on its own when
Android Home was pressed, so an observed increase plus manual confirmation
is required. The task stops on its own after 60 samples (roughly 2 minutes).

**Run the Recents-removal trial separately**, using a new `prepare` run only
after the earlier job has completed. Swipe Klyro away from Recents without
force-stop, relaunch and `verify`. This may fail legitimately on devices
that terminate the process. Any process death invalidates the same-PID
criterion; the files can still survive.

## Interactive shell and SAF

```bash
bash ~/klyro-daily-trial.sh interactive
```

Follow the printed manual sequence for `read`, `cat` and Ctrl+C,
interactive editors, two independent tabs and Storage Access Framework.
Do not mark these PASS from source tests or an emulator alone.

## Test records

| Gate | Evidence | Status |
|---|---|---|
| Clear / smart Select-Paste | Pixel 9 user-reported success | PASS (observed) |
| Bash initial resume | Same Bash PID and REPRISE_OK reported | PASS (one cycle) |
| 2-minute background heartbeat | `background.log` lines and PID from Klyro | NOT TESTED |
| Remove task from Recents | Return to same session and same PID | NOT TESTED |
| Git/Python/Node install under Klyro UID | Real pkg transaction and execution | NOT TESTED |
| Interactive editors, Ctrl+C and two tabs | Hands-on Pixel 9 | NOT TESTED |
| SAF import/export with Unicode | Hands-on Pixel 9 | NOT TESTED |
| Several hours of use | Real Pixel 9 | NOT TESTED |

No data should be deleted in response to a failed gate. If a crash or freeze
occurs, use `scripts/klyro-freeze-diagnostics.sh` from an authorized ADB
host, without force-stopping Klyro before the diagnostic capture.
