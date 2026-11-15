# Klyro — Three core hardening corrections (15 Nov 2026)

> Execution follows Superpowers: isolate, reproduce, test, correct, verify. Base: `fix/modal-lifecycle-touch-freeze` at `f63f77e`. Work stays on `fix/klyro-three-core-hardening-20261115`. Never merge to main automatically, delete user data, reset a workspace, or force an install.

## Scope and invariant map

### 1. Concurrent PTY callbacks and UI rendering

**Root cause.** `OutputUpdateBatch` was called from terminal callback threads, including title changes, but mutated a `LinkedHashSet` and scheduling flags without synchronization. A real eight-thread reproducer lost 920 of 960 IDs in one run. Multiple concurrent title callbacks also used non-atomic dirty state. Immediate native view refreshes could saturate the main thread during large shell output.

**Fix.** Serialize the batch's mark/remove/close and snapshot/clear, publish outside the lock; atomically claim dirty title updates and read the volatile latest title; coalesce native terminal refreshes at 32 ms. Preserve final output, late title updates, disposal cancellation, inactive background behavior, and reentrant publication.

**Proof.** `OutputUpdateBatchTest.parallelPtyCallbacksRetainEveryDistinctTabId`, prior burst/removal/reentrant tests, Android build/CI, and on-device 1,500-line diagnostic + PTY interaction.

### 2. Remote terminal clipboard writes (OSC 52)

**Root cause.** Terminal-provided text could call `ClipboardManager.setPrimaryClip` without foreground/session/size/content checks, on the terminal callback's thread. That allows background output to replace the clipboard and can trip Android clipboard restrictions.

**Fix.** A named policy requires visible UI, a running PTY, and validated bounded text (same UTF-8/Unicode/safety policy as shell paste). Recheck at main-thread dispatch and handle denied Android clipboard operations without terminating the PTY. Do not change explicit user paste confirmations, bracketed paste, or user-initiated transcript copy.

**Proof.** `ClipboardPastePolicyTest.remoteClipboardWriteRequiresVisibleSessionAndSafeText`, UI source integration, background/foreground Android manual checks.

### 3. Session/workspace name and metadata integrity

**Root cause.** Workspace names allowed malformed UTF-16 surrogates, persisted records decoded malformed UTF-8 with replacement characters, and the UUID shape validator accepted misplaced hyphens. Session renaming sliced UTF-16 at 32 code units, which could create an invalid trailing surrogate.

**Fix.** Strip isolated surrogates while preserving emoji pairs; require strict UTF-8 decoding and the canonical 8-4-4-4-12 UUID pattern; use the same Unicode-safe cleaner for UI tab rename and native PTY display names. Keep the existing 32 UTF-16-unit limit, General workspace, existing valid records, and project limits.

**Proof.** `KlyroWorkspaceCatalogTest.malformedUtf16NamesAreNotPersistedAsReplacementGlyphs`, `corruptUtf8AndInvalidUuidShapesCannotBecomeWorkspaces`, existing round-trip tests, cross-screen rename checks.

## Release gates

- Run every JVM/Java/Kotlin unit test, all Python tests, Android lint, debug/release assembly, test APK assembly, native 16-KiB inspection, and signing checks using the repository's `android-build.yml`.
- Review exact run SHA and failure logs; never treat a queued run as a pass.
- Pixel 9 validation still required: multi-tab switches/rename/closure; background PTY activity; clipboard behavior for safe, malicious, oversized, multiline and inactive writes; 1,500-line diagnostics, Settings and Command Palette sheet dismissal, keyboard responsiveness and screen rotation.
- Treat Android instrumentation or real-device behavior as **not verified** until device evidence is provided. Keep this branch unmerged.
