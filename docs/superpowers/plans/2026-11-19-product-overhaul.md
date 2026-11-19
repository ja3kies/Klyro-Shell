# Klyro Product & Engineering Overhaul Implementation Plan

> **For agentic workers:** implement task-by-task on `feat/product-overhaul-latest`; keep each screen change testable and commit by functional area.

**Goal:** Improve the real user flows, reliability, accessibility and maintainability of Klyro without replacing validated terminal/runtime behavior.

**Architecture:** Keep the existing single-Activity Compose structure and deepen only the screen/component boundaries that are causing measurable defects. Start with lifecycle and interaction correctness, then move screen by screen through existing UI; preserve PTY/session APIs and user data.

**Tech Stack:** Kotlin, Java, Jetpack Compose, Material 3, AndroidX Lifecycle, Gradle, JUnit, Compose UI tests, Python/native CI checks.

**Spec:** User request “KLYRO-SHELL — COMPLETE PRODUCT & ENGINEERING OVERHAUL” (2026-11-19).

## Global Constraints

- Never merge to `main` without authorization.
- Preserve PTY/JNI, terminal input, sessions, workspaces, Freeze Recorder, files and user data.
- No Android permission changes without a demonstrated requirement.
- Do not claim visual or physical-device verification without rendering/testing evidence.
- Maintain `docs/KLYRO_WORK_STATE.md` and `docs/KLYRO_QUALITY_OVERHAUL.md` at milestones.

## Review Focus

- Activity recreation/backgrounding while a sheet, GIF or PTY is active: lifecycle effects must stop/resume without duplicate owners.
- Large font/landscape/IME layouts: primary actions and terminal input remain reachable.
- Invalid or revoked SAF access/update grants: show failure without overwriting or deleting data.
- Rapid repeat taps and navigation: one operation, stable session identity and no stale callbacks.
- TalkBack and keyboard-only use: labels, state, focus and touch feedback remain available.

## Screen and system sequence

1. Home / Welcome: status, runtime actions, navigation, cover lifecycle and constrained layouts.
2. Terminal: input, scrollback, toolbar, search, palette, output and lifecycle. Avoid broad renderer rewrites.
3. Sessions / Workspaces: identity, rename, selection, closure, empty/finished/running states.
4. Plugins / Explore: registry loading, preview/import/install states, errors and accessibility.
5. Settings: navigation owner, per-section state, saved preferences, appearance and storage.
6. Diagnostics / Freeze Recorder: privacy, export/delete, lifecycle, timestamps and operational messaging.
7. Command Palette / Find: focus, query, result states, safe insertion and navigation.
8. Shared modals/menus/design components and cross-cutting security/architecture/CI review.

## Tasks and verification gates

### Task 1 — Establish baseline and preserve the latest work

- Record current branch ancestry, open PRs, CI runs, screen entry points, test inventory and manifest trust boundaries.
- Use `origin/fix/transparent-touch-feedback-20261118` as the latest relevant UI base; keep its two open PRs unmerged.
- Verify the last upstream UI workflow before attributing any failure to this branch.

### Task 2 — Home lifecycle and runtime feedback

- Test a real Compose lifecycle owner against animated Home cover and terminal job polling.
- Replace context-to-owner casts with Compose lifecycle ownership where proven.
- Add regression coverage and run relevant instrumented test compilation.

### Task 3 — Terminal screen

- Audit PTY view callbacks, toolbar actions, search, shortcuts, selection/paste, output batching and lifecycle.
- Change only demonstrated gaps; add behavior-level Compose/native tests for every fix.

### Task 4 — Sessions and Workspaces

- Audit stable IDs, active/finished state, workspace mutations and destructive confirmation targeting.
- Keep process/session operations intact and test stale/repeated actions.

### Task 5 — Plugins and package-facing UI

- Audit registry I/O ownership, imports, preview, state transitions, errors and command insertion.
- Validate declared-only plugin semantics; never infer installed software from a UI state.

### Task 6 — Settings and Appearance

- Audit sections, navigation, persistence, IME/large text, accessibility and color/theme feedback.
- Retain user preferences and current sheet-dismissal invariants.

### Task 7 — Diagnostics and overlays

- Audit report privacy, bounded output, export/delete errors, notification handoff and modal ownership.
- Do not read the full transcript or expose secrets in diagnostics.

### Task 8 — Security and architecture review

- Inspect update APK permission/grants/signature checks, exported components, SAF boundaries, plugin/import parsers, native/runtime inputs and dependency/workflow provenance.
- Refactor only where a measured issue benefits from a small seam or deletion.

### Task 9 — Cross-screen validation and delivery

- Run targeted JVM/Python/native tests, Android lint/build/instrumentation when available, diff checks, and review the complete diff.
- Publish only the dedicated branch; do not merge or release.

## Research references

- Android Compose accessibility semantics and test tree: https://developer.android.com/develop/ui/compose/accessibility/semantics
- Compose UI testing: https://developer.android.com/develop/ui/compose/testing
- Material 3: https://m3.material.io/
- Apple design principles and motion (principles only; Android implementations remain native): https://developer.apple.com/design/human-interface-guidelines/design-principles/ and https://developer.apple.com/design/human-interface-guidelines/motion
- Klyro current open UI work: PR #50 and #51, inspected 2026-11-19; both draft/open, not treated as merged.

## Initial known gaps

- Physical Android interaction is not available in this environment; device-only acceptance remains pending.
- The prior round-10 CI on its own branch passed, but this overhaul branch still needs its own build after changes.
- The app contains large screen files; extraction is deferred until a focused behavior boundary demonstrates a real maintenance or testability gain.
