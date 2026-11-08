# Klyro Shell — P0 Cycle 01: implementation ledger

Branch: `integration/cycle-01`. Reference: `main` remains unchanged.

**Definition:** each item below is a distinct code behavior or regression fix, not a commit count or a unit-test assertion. The merged CI run is the build gate; Android-device acceptance is a separate, mandatory gate.

## Stability and service (5)
1. Stop foreground-service teardown from terminating live process-owned PTYs (`KlyroSessionService.onDestroy`).
2. Use `stopSelf(startId)` for a start request with no sessions, avoiding a stale request stopping later starts.
3. Make the ongoing session notification reopen `MainActivity` with an immutable `PendingIntent`.
4. Display the actual active session count, with correct singular/plural.
5. Classify the notification as a service and suppress repeated alerting on updates.

## Terminal keyboard and paste (16)
6. Restore a dedicated, discoverable `END` shortcut.
7. Route `END` through the existing modifier-aware and alternate-screen-safe navigation policy.
8. Scroll the second portrait shortcut row rather than clipping the restored eighth action.
9. Show the select-output icon when the terminal contains copyable output.
10. Show the paste icon when the terminal has no copyable output.
11. Expose the action actually performed by `ALL` in accessibility descriptions.
12. Make paste availability depend on readable, allowed clipboard text—not MIME type alone.
13. Reject whitespace-only paste, including non-ASCII space characters.
14. Cap pasted input by UTF-8 bytes in addition to UTF-16 character count.
15. Reject invisible Unicode noncharacters in the BMP.
16. Reject supplementary-plane Unicode noncharacters.
17. Treat multiple empty Klyro prompts as non-copyable, without hiding real output.
18. Update smart shortcut state when an active PTY emits new output.
19. Track output change revisions per session and discard stale revisions when a tab closes.
20. Preserve UTF-16 surrogate-pair boundaries when truncating a transcript selection.
21. Present the *entire* bounded multiline paste for review rather than hiding all commands after 500 characters.

## Android brand switching and UX (12)
22. Interpret default-enabled launcher aliases according to their manifest defaults, not as disabled.
23. Reject invalid alias combinations where both icons are enabled or both are disabled.
24. On older Android releases, enable a replacement launcher alias before disabling its predecessor.
25. Verify the real OS alias configuration before persisting or reporting a successful switch.
26. Restore prior component states on failure, including half-completed switches.
27. Serialize concurrent icon-selection operations.
28. Allow repairing a broken both-disabled launcher state even when the displayed default is B.
29. Avoid redundant launcher component switching when the requested choice is already OS-verified.
30. Refresh the settings selection from PackageManager after each attempted switch.
31. Explain failure and launcher-cache refresh behavior instead of implying immediate UI updates.
32. Fit the selected About-page icon inside its container.
33. Prevent clipping in the terminal header, cache repeated PackageManager reads and refresh the icon after closing settings.

**Implemented total: 33 code actions.** Some actions share commits by design. Tests are **not** counted toward this total.

## Release gates
- All specialist changes must be merged into `integration/cycle-01`.
- Python adversarial checks, app and terminal-emulator JVM tests, Android lint, ARM64 debug/release builds, signed APK verification, native 16 KiB alignment and artifact availability must pass together on the final integration commit.
- Physical-device acceptance remains mandatory for PTY survival across recents, icon launcher refresh and switching A/B, live shortcut transitions, multiline paste and background service behavior. A successful CI run **does not prove** these.
- Never merge into `main` without explicit user approval.
