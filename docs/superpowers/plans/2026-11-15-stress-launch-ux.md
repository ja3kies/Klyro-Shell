# Stress diagnostic launch UX: asynchronous preparation and observable result

> **Superpowers TDD:** red source contracts, observe failure in CI, implement minimal fix, green full Android CI. Ponytail: no dependencies, reuse existing Compose coroutine and Dispatchers.IO.

**Goal:** A tap on "Start isolated 1,500-line test" must immediately acknowledge the tap, never block the main thread doing filesystem setup, prevent duplicate launches, display a persistent failure when the start cannot complete, and preserve other tabs.

**Root cause (source review):** `FreezeStabilityControls` calls synchronous `MainActivity.startDiagnosticStress()` in a TextButton handler; `TerminalSessionManager` constructor executes filesystem `paths.prepareLayout()` on the UI thread. Starting Bash creates a TerminalSession and calls `updateSize` on the UI path; users can tap repeatedly without feedback or guard. The app previously had a real user report that the 1,500-line button does not visibly work. A successful enqueue does not prove the Bash command ran to completion; the UI must not claim that.

**Design:**
- `suspend fun startDiagnosticStress()` on MainActivity; reserve tab identity and obtain tab-scoped listener on main, prepare TerminalSessionManager and construct unstarted session via `withContext(Dispatchers.IO)`, then return to main and revalidate max-eight capacity before attaching/start.
- Keep Termux terminal-emulator initialization/updateSize/PTY writes on the main thread, preserving its single-thread terminal contract.
- `FreezeStabilityControls` uses a remembered coroutine scope plus synchronous `launching` gate on click, feedback "Preparing isolated test…" immediately, disabled button with "Preparing…" and a terminal-start result; dismiss diagnostic sheet only when command is queued.
- When the sheet is dismissed or Activity is destroyed during IO preparation, coroutine cancellation prevents attaching/start of an obsolete tab. No partial tab on construction failure; existing tabs are preserved. On exceptions after attaching, explicitly clean up the isolated tab and stop service if empty.
- The 1,500-line command and its START/END markers are unchanged, as is its three-second delay and PRoot/PTY isolation.
- Add source tests for user feedback/cancellation/capacity and retain JVM Bash scenario test.
- Version bump 0.4.21-dev versionCode 28; no main merge.

**Adversarial checks:** two rapid taps; rotate/close sheet during slow filesystem setup; 8th tab created during preparation; PRoot environment unavailable; session bootstrap exception; immediate PTY input rejection; successful enqueue but runtime output not yet received; existing user shell untouched; test button disabled while preparation; no storage setup on main; confirm CI test/lint/signature/ARM64 and still require Pixel 9 manual end-to-end validation.

**Files:**
- `app/src/main/java/com/oryn/terminal/MainActivity.kt`: coroutine API and prepare on IO.
- `app/src/main/java/com/oryn/terminal/ui/FreezeReportsPane.kt`: button state, failure feedback.
- `tools/test_klyro_stress_launch_ux.py`: regressive checks.
- `app/build.gradle.kts` and version contract.
