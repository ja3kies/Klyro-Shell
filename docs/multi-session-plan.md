# Klyro Shell — multi-session architecture gate

## Baseline verified in feature branch
- `TerminalSessionViewModel` currently owns exactly one `activeSession`, `sessionClient`, and `viewClient`.
- `MainActivity.launchBash()` refuses a second launch while `activeSession != null`.
- `TerminalPane` receives a single session and attaches it to `TerminalView`.
- `onSessionFinished()` ignores callbacks from sessions other than the active one.
- `onCleared()` finishes the owned session. Activity recreation retains it via ViewModel.
- The existing terminal key rows and CLEAR/Ctrl+L behavior must remain unchanged.

## Reference strategy
- Cortex Terminal: horizontal tabs and multi-session UX are *design references*, not evidence that its code is compatible.
- Termux terminal-emulator / terminal-view: keep as the existing terminal core; do not write a second emulator.
- Chimera Terminal: compare Android lifecycle/session architecture after locating and verifying its actual source and license.
- Verify source, license, and API compatibility before importing code.

## Required implementation sequence
1. Introduce a session registry keyed by stable generated IDs; each entry owns its TerminalSession, OrynTerminalSessionClient and OrynTerminalViewClient. Expose active ID as observable state.
2. Route callbacks (title, PID, process exit) by session identity. Never allow an inactive session's exit to dismiss the currently visible terminal.
3. Add horizontally scrollable session tabs with accessible labels, active indicator, + action and per-tab close confirmation. Keep the current keyboard rows.
4. On tab switch, detach the old TerminalView and attach the new session, preserving scrollback, cwd, shell process and modifier state.
5. Handle close-current, close-background, close-last, process exit, Activity recreation and ViewModel teardown without leaking PTYs or retaining Activity references.
6. Add deterministic unit tests for registry transitions, callback isolation, tab switching, and cleanup. Run Android CI and on-device tests before merging.

## Acceptance tests
- Two tabs can execute different `pwd` and `cd` commands without affecting each other.
- Switching tabs preserves each shell's command input, cwd and output.
- Closing a background tab does not interrupt the foreground tab.
- An inactive process exiting does not navigate away from an active session.
- Rotation preserves both live sessions and selected tab.
- Closing the last tab returns to setup; all PTYs are eventually terminated.
- Existing Ctrl+L clear, navigation keys, copy, and signed APK workflow still work.

## Explicit non-goals
No Ubuntu migration, no PRoot replacement, no automatic package installation, no changes to main, no new dependencies without review.
