# Async Bash launch across every entry point — implementation plan

> **For agentic workers:** Use Superpowers test-driven-development and executing-plans: RED → GREEN → whole CI. Ponytail default: reuse the existing `startDiagnosticStress` coroutine pattern, no new dependencies.

**Goal:** Avoid freezing the UI when users create Bash terminals; deliver one correctly scoped new session from Home, + tab or shell chooser, even under I/O delays, repeated taps, rotation and an eight-tab capacity race.

**Architecture:** Retain `launchBash(): Unit` as the UI callback. Gate immediately with Activity-local Compose state `bashLaunchInProgress`; create the unstarted Termux session within `withContext(Dispatchers.IO)` inside `lifecycleScope.launch`; attach on main thread only after rechecking capacity. Keep existing foreground-service ownership. Expose a single loading flag through existing Compose screens. Unattached clients must be disposed; after attach, exceptional failure must close only the attempted tab, not existing ones.

**Tech Stack:** Kotlin + Jetpack Compose, Termux terminal-engine Java, Python source contracts, GitHub Actions Android CI.

**Spec:** Root-cause audit: `TerminalSessionManager(Context)` calls `OrynPaths.prepareLayout()` (filesystem) in its constructor, and `startBash` checks private runtime and builds its command. `MainActivity.launchBash` runs both synchronously inside tap handler for all three entry points. In contrast, the previously validated diagnostic launcher already prepares the session on `Dispatchers.IO`. There is no in-flight guard, so fast taps can create unexpected parallel sessions.

## Boundaries and review focus
- [ ] No disk-heavy `TerminalSessionManager(...).startBash` on UI thread; Termux emulator view initialization remains Android main/UI thread.
- [ ] Bind Termux `TerminalSession.MainThreadHandler` explicitly to `Looper.getMainLooper()`: sessions created on IO must never dispatch terminal parsing onto IO or require an IO-thread Looper (also protects the existing diagnostic launcher).
- [ ] Prevent two rapid same-frame taps, re-check eight-tab limit after suspension, no bogus "Maximum 8" explanation merely due to loading.
- [ ] Preserve the selected workspace captured at tap time, even if user switches workspaces during preparation.
- [ ] Existing shell sessions remain interactive while preparing; keep ability to return Home or inspect other tabs.
- [ ] Rotation/background cancellation before attach leaves no orphan PTY or tab. Error after attach closes only new tab; absent attach client disposes.
- [ ] New Home CTA shows progress and disables only `LAUNCH_BASH` (never disables `RESUME_SESSION`).
- [ ] + tab and session-sheet/shell-chooser new-terminal actions disabled during launch and expose truthful "Starting Bash" status.
- [ ] Failed environment preparation displays Home technical error and terminal-context feedback, with retry possible.
- [ ] No new Gradle dependencies, no main merge, no re-run of already user-validated 1,500-line diagnostic.
- [ ] Release `0.4.23-dev` `versionCode=30`; existing JVM/Python, lint, native 16 KiB, ARM64 APK signing pass.

## Task 1: Test-first contract
- [ ] Add `tools/test_klyro_async_bash_launch_ux.py` with off-main, cap guard, in-flight lock, cancellation, workspace and UI-entrypoint checks.
- [ ] Run RED in CI; check failures are the expected missing behavior.

## Task 2: Minimal implementation
- [ ] Modify `MainActivity.kt`: process user tap, launch coroutine, prepare on IO, attach on UI, cleanup and status on failure.
- [ ] Modify `TerminalPane.kt` and `KlyroSessionSheets.kt` to disable/label all new terminal entry points while launch is in flight.
- [ ] Bump build and version assertion.
- [ ] Re-run all CI tests and Android build/signing; review diff for abstractions to remove.

## Acceptance evidence
Green CI and signed `klyro-shell-debug` artifact; device-specific latency reduction remains a QA validation, not a CI claim.
