# PTY output backpressure and main-loop scheduling

> **For agentic workers:** Use Superpowers executing-plans; verify red→green and final full CI.

**Goal:** Prevent high-volume PTY output from scheduling unbounded Android main-looper work while preserving all output, prompt order, input responsiveness and session exit semantics.

**Architecture:** Keep Termux's existing ByteQueue and TerminalSession. Enqueue at most one pending PTY notification; process a bounded byte budget per main-loop dispatch, reschedule if bytes remain, and coalesce view refreshes upstream (already throttled at 32ms). Never touch text/selection data on a background thread. Retain lossless blocking backpressure and atomic shell input writes.

**Tech:** Java 17, Android Handler, JUnit4, Python contract tests, native PTY harness, GitHub Actions.

**Spec:** Existing Pixel 9 Android 17 reports of repeated/disordered echoed text during lengthy script paste and extensive `seq` output; expected terminal behavior must preserve order and remain responsive. Device repro remains unconfirmed, so do not claim UI symptom fixed from CI alone.

## Global constraints
- No dependency additions, no change to copy/paste confirmation, Bash, PRoot or user files.
- Work in `p0/pty-main-loop-backpressure-20261114`, no merge.
- Never drop output bytes, including when subprocess exits with queued output.
- Maintain bracketed paste atomicity and 16 KiB native alignment.
- Advance Android `versionCode` beyond 23.

## Review focus
- Producer wakes between queue-drain and reset of pending notification: output must not be stranded.
- Pending read notification after process exit: must not redraw an empty/dead emulator repeatedly.
- Producer backpressure and a full ring buffer: no dropped or reordered bytes.
- Foreground high-throughput text: bounded handler dispatch with fair main-loop access.
- Background/resume or terminal close: preserve existing output and lifecycle callbacks.

### Task 1: ByteQueue visibility
- Modify `terminal-emulator/src/main/java/com/termux/terminal/ByteQueue.java`.
- Test `terminal-emulator/src/test/java/com/termux/terminal/ByteQueueTest.java`: unread-byte count is correct after writes, wrap, reads and close; fail before implementation, then pass.
- Expose synchronized `available()` without extra locks, maintaining existing semantics.

### Task 2: Bounded PTY main-loop work
- Modify `terminal-emulator/src/main/java/com/termux/terminal/TerminalSession.java`.
- Add regression `tools/test_klyro_pty_main_loop_contract.py` before implementation, observe failing source contracts.
- Use atomic pending flag for reader wakeups, bounded drain (max 16 KiB per notification), schedule another notification whenever bytes remain, and suppress empty notifications. Drain final queue on process-exit and discard queued obsolete input messages.

### Task 3: Release and verification
- Update `app/build.gradle.kts` and existing version contract to `0.4.17-dev` and versionCode `24`.
- Run full GitHub Actions (unit Python/JVM, PTY/JNI, lint, ARM64 signed APK, alignment). Address every failure.
- Review implementation against concurrency races, and report Android device verification separately.
