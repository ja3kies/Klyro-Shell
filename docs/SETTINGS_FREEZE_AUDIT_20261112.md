# Klyro — freezes on Settings after pkg install (2026-11-12)

## Evidence and limits

The Pixel 9 user reported an unresponsive Klyro interface after installing
software and opening Settings. Earlier, a separate Home/back/return test kept
the Bash PID, and a two-minute background script wrote all 60 heartbeats.
These observations establish that the PTY **can** survive a background trip;
they do not establish whether the new failure was an Android ANR, a Compose
rendering backlog, an application process restart, or a stalled PTY.

No ANR traces, main-thread stack dump, bugreport, or logcat for this particular
freeze were provided. **The exact cause is not yet proven.**

## Static code findings and corrections

| Priority | Before (verified in source) | Failure mechanism | Fix |
|---|---|---|---|
| P0 | `TerminalPane.kt` built `screen.transcriptText` for the smart SELECT state on every `outputRevision` | The full scrollback can contain thousands of lines after `pkg install`. Repeated large string allocations and scans on UI thread can monopolize a frame or cause GC pauses when opening a Compose sheet | Query `activeTranscriptRows` first, scan at most the **visible screen** as a fallback; full transcript remains available only when SELECT is explicitly pressed |
| P0 | `OrynTerminalApp` called `context.filesDir.usableSpace`, `PackageManager.getPackageInfo` and formatted the entire diagnostics report on each recomposition | Filesystem stat and PM Binder query ran again for every PTY output tick, whether Settings were visible or not | Load diagnostics metadata on `Dispatchers.IO`; `remember` the formatted diagnostic snapshot across unrelated output updates |
| P0 | `TerminalSessionViewModel.listenerFor().onOutputActivity()` incremented Compose state for every output callback | Fast-growing apt/dpkg output could schedule many full recompositions, competing with input, menus and sheets | Batch notifications every 120 ms with a guaranteed trailing update. Do **not** throttle the underlying PTY bytes |
| P1 | `SideEffect` repeated system-bar and `WindowInsetsControllerCompat` work after *every* recomposition | Redundant main-thread window work during continuous output | Apply window style only when the actual palette changes |
| P1 | Files & storage section queried `contentResolver.persistedUriPermissions` synchronously in the Compose body | Binder call repeated during recomposition of the subsection | Use `LaunchedEffect(section)` and `withContext(Dispatchers.IO)`; render a remembered count |
| P1 | Opening Settings queried launcher alias state through PackageManager on the UI dispatcher | Slow Binder response could block sheet opening | Resolve alias state on `Dispatchers.IO` |
| P1 | `completeStorageRequest()` wrote filesystem state from an ActivityResult callback on the main thread | Local filesystem operations can contend with active dpkg/pkg IO | Complete request on `Dispatchers.IO` |

Existing safeguards retained: terminal UI callbacks are suspended while the
Activity is paused, PTYs remain service-owned, released AndroidViews do not
detach a replacement view, and storage bridge *polling* already ran on IO.

## Regression coverage

`tools/test_klyro_settings_anr_contract.py` checks the hot paths above,
including no full transcript materialization for the SELECT button and no
Binder grant lookup in the files-section composition.
`tools/test_klyro_p0_lifecycle_shortcuts.py` now checks the coalesced
output publication contract and interactive SELECT/PASTE behavior.

Source checks alone cannot prove lock freedom or rule out a native deadlock.
Release gate remains **NOT READY** for primary Termux replacement until the
following physical tests pass.

## Required Android acceptance

1. **Without uninstalling Termux or wiping Klyro**, start one Klyro Bash tab.
2. In Klyro run `pkg update` and `pkg install git python` (if packages are
   already installed, use `pkg list-installed` to generate output instead).
3. While output is arriving, open Settings from the terminal. Navigate
   Appearance, Files & storage, Diagnostics, Back, then Close.
4. The keyboard should respond; `echo $$` must report the previous Bash PID.
5. Repeat three times with Home/Recents. Verify no lost session, no stopped
   background tasks, and no 5-second main-thread stall.
6. Independently test SELECT on a large output, hold-to-PASTE, CLEAR, job
   activity indicator, SAF and rotation. Then run multi-hour soak tests.

If it freezes again, do **not force-stop first**. From Termux with paired ADB:

```bash
bash <(curl -fsSL https://raw.githubusercontent.com/ja3kies/Klyro-Shell/feat/production-ready-terminal/scripts/klyro-freeze-diagnostics.sh)
```

The script captures app PID, logcat ANR keywords, service/activity state,
process state and memory without killing the app. Review sensitive log lines
before sharing. A recorded main-thread stack trace is needed to attribute
an individual ANR to a specific lock/stack site.

## Review status

- Static hot paths identified and patched in `feat/production-ready-terminal`.
- CI and APK signature: track the workflow run after the **final** patch.
- Reproducing the exact freeze on Pixel after the patch: **NOT TESTED**.
- Independent Klyro process UID: previously observed as `u0_a693`; confirm
  installed package UID separately if necessary.
- Developer packages available: user's initial Klyro inspect had Git/Python
  absent; user subsequently reported installation success, but tool execution
  and full package functionality have not been retested in this pass.
