# Failed PTY session recovery — UI and lifecycle

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans and test-driven-development, with RED then GREEN.

**Goal:** Preserve the terminal transcript and error code when a Bash process exits unsuccessfully; allow read-only inspection and explicit close, without retaining the foreground service after all PTYs stop.

**Architecture:** Retain failed ShellTab objects with an `exitStatus: Int?` rather than auto-removing them. Existing TerminalPane already disables writes when PTY stops and supports read-only ALL/FIND. Derive a per-selected-tab failure notice from stored exit status, make Back close a failed tab explicitly, and distinguish open tabs from running PTYs in foreground service checks. Successful exit (0) retains existing auto-close behavior.

**Tech stack:** Kotlin/Compose, Java Termux TerminalSession, Python source contracts, JVM/Android CI, signed ARM64 APK.

**Spec:** A failed Bash shell currently triggers `TerminalSessionViewModel.finishTab -> closeTab` even when nonzero, dropping the only scrollback useful for troubleshooting. User previously needed to examine diagnostic scripts and output on their Pixel 9.

## Constraints
- Start at validated `p0/ux-native-paste-request-races-20261114` (0.4.19-dev).
- No force-closing or mutating other PTYs and no unrequested session autostart/retry.
- No new dependencies or data persistence for sensitive shell text.
- Shells with exit code 0 still close automatically; nonzero remain viewable until explicit close.
- A failed tab is read-only, can be selected/copied/searched, displays its exit code and a close affordance.
- Shell tabs retain 8-tab cap; preserve failed tab until manually closed.
- Foreground service tracks **running PTYs**, not merely tabs.
- Bump version to 0.4.20-dev (versionCode 27); no main merge.

## Review focus
- Failure in non-active tab: show unread output and failure banner upon selection.
- Exit 0 in foreground/background: no retained tab.
- Tab switch away/from failed tab: banner always matches selected tab.
- Hardware Back after failed exit: close-tab confirmation must not reject because PTY no longer runs.
- Native paste to failed tab: existing guards still reject it.
- Last process exits with retained failed tab: foreground service must stop automatically.
- Explicit close of a retained tab: release buffers, observer and session resources.
- PTY existing output remains inspectable, including final exit marker.

### Task 1 — test-first retained failed tab and accurate state
- [ ] Add Python cross-file contracts to verify all flows; observe a failing CI run.
- [ ] Add exitStatus state to ShellTab and retain nonzero in finishTab.
- [ ] Keep error message and copy/find available across tab switches and close.
- [ ] Verify regression tests.

### Task 2 — service ownership and explicit close
- [ ] Add hasRunningPtys() computed from session.isRunning() and point foreground-service checks to it.
- [ ] Allow Back confirmation to close finished tab without trying to send PTY input.
- [ ] Preserve existing session cap, native paste refusal, success exit cleanup.
- [ ] Verify full regression suite.

### Task 3 — release
- [ ] Bump to code 27 / 0.4.20-dev, update version test.
- [ ] Verify JVM/Python, lint, native audit 16KiB, ARM64 APK and signing.
- [ ] Review branch diff, record outcome in Create State, no merge.
