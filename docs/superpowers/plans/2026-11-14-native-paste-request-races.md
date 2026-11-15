# Native paste request identity and tab-switch race hardening

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans task-by-task, verifying RED→GREEN.

**Goal:** Ensure native clipboard pastes never disappear, get attributed to the wrong tab, clear a newer request, or silently fail on session exit.

**Architecture:** Reuse `TerminalSessionViewModel.NativePasteRequest` as the single pending-state value and add a monotonically unique request identity. Propagate the exact object from ViewModel through MainActivity and TerminalPane. Consume by object identity, not by current active tab. Revalidate the original native TerminalView at callback delivery and use a boolean paste result on single-line native paths.

**Tech stack:** Kotlin, Jetpack Compose, Java 17, JUnit4, Python test contracts, GitHub Actions.

**Spec:** The previous 0.4.18 UX hardening binds *confirmation* to a tab, but loses the original request identity across MainActivity and can consume newer requests using only tab ID. The native single-line path currently ignores the result of `TerminalEmulator.paste`, unlike the Compose path.

## Global constraints
- Continue from `p0/ux-session-safe-paste-review-20261114`; write only to `p0/ux-native-paste-request-races-20261114`. No main merge.
- No new dependencies, no PTY/emulator rewrite, no change to character validation, 65,536-byte limit or bracketed paste.
- Treat requests with identical text as distinct operations; stale acknowledgements must not clear the newest.
- Do not leak native requests between tabs, across view replacement, after app pause, after session exit, or after close.
- Preserve terminal scrollback, sessions and existing confirm/reject notifications.
- Version bump to `0.4.19-dev` (versionCode 26).

## Review focus
- Two same-tab pastes in one Compose frame; latest must still be observable.
- Acknowledgement for A after new B; B must remain pending.
- Native callback queued before tab switch, old view detached, return to old tab; queued action must not revive.
- Session ends between clipboard read and emulator paste; no silent false-success.
- Full input queue: exactly one existing failure feedback, never duplicate toasts.
- Native multiline: exact source tab ID preserved into the confirmation dialog.
- Session switch/close: previously queued request stays cancelled.

### Task 1: identity-safe native request handoff
Files: `app/src/main/java/com/oryn/terminal/TerminalSessionViewModel.kt`, `MainActivity.kt`, `ui/TerminalPane.kt`, source and JVM tests.
- [ ] Add failing tests for distinct repeated payloads, exact consumed request, and origin-tab binding.
- [ ] Observe failing CI.
- [ ] Introduce requestId and clear-by-reference, propagate object unchanged through the entire UI.
- [ ] Green full suite, commit.

### Task 2: safe native terminal-view clipboard delivery
Files: `app/src/main/java/com/oryn/terminal/session/OrynTerminalSessionClient.java`, tests.
- [ ] Add failing contracts for view revalidation, stopped sessions, one-line paste result.
- [ ] Observe red before production.
- [ ] Revalidate view and PTY both at read and delayed callback; use pasteWithResult and rely on existing onInputRejected when full.
- [ ] Green full suite, commit.

### Task 3: release and evidence
- [ ] Bump 0.4.19-dev code 26, update version source contract.
- [ ] Verify JVM/Python/native, lint, ARM64 build, 16KiB audit, signature, signed APK.
- [ ] Audit cross-file edge cases and source tests; no merge.
- [ ] Explicitly separate device QA from CI coverage.
