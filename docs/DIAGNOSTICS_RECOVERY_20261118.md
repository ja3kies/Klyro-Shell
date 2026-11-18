# Diagnostics and recovery — implementation handoff

Branch: integration/diagnostics-recovery-20261118.
Base: terminal-interactions cd2d998 plus session-tab-actions 5719802. Merge preserves the extracted tab component, explicit management action, compact menu and adaptive typography. These inherited improvements are not counted below.

## 45 implementation objectives

### Loading and report inspection
1. Loading has its own state rather than overwriting success/error messages.
2. An indeterminate progress indicator identifies real report-loading work.
3. Loading blocks refresh/export/deletion to serialize UI operations.
4. Refresh acquires its guard at click dispatch, before coroutine/recomposition.
5. Failed refresh preserves the previous list instead of replacing it with an empty state.
6. First-load failure and stale-list refresh failure have different recovery messages.
7. Load failures have a named Retry loading action and live-region announcement.
8. An unreadable individual report remains listed with its own failure state; readable siblings survive.
9. Cancellation propagates rather than becoming a false diagnostic failure.
10. Successful reload removes stale expanded/deletion targets absent from the store.
11. Expanded-report identity survives configuration recreation.
12. Keyed report composition keeps scroll/selection state associated with the report during deletion/reordering.
13. Report dates use local date/time formatting rather than Date.toString.
14. Invalid timestamps show date unavailable instead of fabricating a 1970 incident.
15. State parsing distinguishes suspected delay, recovery, Android-confirmed ANR and other process exit.
16. Truncated report summaries take precedence over older state lines in the preview.
17. Stability reports explicitly remain completion-unverified; observation is not presented as a passed test.
18. Expanded details support native selection/copy in a bounded monospace viewport.
19. Details controls expose expanded/collapsed accessibility state and a Hide details action.
20. Shortened previews explain how to obtain the full report; readable IDs identify the inspected artifact.

### Export and deletion
21. Choosing a destination is tracked separately from exporting, preventing duplicate pickers.
22. Pending picker state survives configuration recreation through rememberSaveable and ActivityResult registration.
23. Picker launch failure releases the guard and provides a recovery message.
24. Picker cancellation is an explicit neutral outcome and preserves reports.
25. Export obtains its operation guard before launching IO, preventing repeated competing writes from the UI.
26. Export progress is shown only while writing, not while waiting for the user's destination choice.
27. Timestamped default ZIP names reduce accidental overwrite of earlier exports.
28. Destination errors explain retry and possible incomplete files; reports remain local.
29. Export interruption is distinguished from success and cancellation is rethrown.
30. Feedback has independent severity and can be dismissed without clearing report data.
31. New feedback is brought into view, including repeated identical messages from actions lower in the list.
32. Delete confirmation survives recreation, identifies the selected report and states that deletion is irreversible.
33. Delete confirmation text scrolls with large fonts and limited height.
34. Deletion locks synchronously at dispatch, removes only the target row on success and clears its expanded details.
35. Delete failure retains the row with a visible retry explanation; successful deletion is acknowledged.
36. Export, refresh and per-report action layouts wrap on narrow screens rather than crowding fixed rows.

### Stability controls and recovery
37. The Strict I/O setting is a single accessible whole-row switch with a passive native control.
38. Stability launch is blocked while report IO or another test launch is active; launch guards remain synchronous.
39. Stall tests retain an explicit Runnable reference so cancellation removes the actual pending task.
40. Scheduled stalls can be cancelled explicitly and are cancelled on screen disposal or Activity pause.
41. A valid-duration/no-existing-task dispatch guard prevents rapid confirmation taps from overwriting the cancellable task.
42. Session availability and resumed state are rechecked before the deliberate stall; refusal has feedback.
43. Recovery notices are suppressed while Settings/report navigation already owns the flow; the dialog uses the modal theme, bounded text and a direct View report action.
44. Environment Diagnostics distinguishes absent report, checking state and previous snapshot; progress/status are accessible and empty-report copy/share are disabled.
45. Empty reports, report count, local-only handling and cautious detection wording distinguish absence of evidence from a verified healthy app.

## Evidence and test handoff

`git diff --check`: clean. Independent static review checked Kotlin/Compose risks, report classification, cancellation, export and merged tab call signatures. It identified the scheduled-stall double-tap race; the dispatch guard was added. No runtime validation is claimed.

Compilation, lint, JVM tests, instrumented tests, Pixel checks: NOT RUN here, as requested by the user. CI: UNKNOWN. Before/after Android captures: NOT PRODUCED. No main merge.

Testing agent should cover: initial load/error/retry; failed refresh retaining data; one disappearing/unreadable report; truncated latest state; unknown timestamp; locale/timezone; rotation during expanded details/delete confirmation/document picker; rapid refresh/export/delete taps; denied/no document provider; cancelled picker; slow/failed/cancelled ZIP write; deletion failure; TalkBack feedback from lower cards; long-font/narrow layouts; Strict I/O restoration; isolated stress launch and duplicate taps; stall cancellation on pause/navigation and stale sessions; recovery notification routed to Settings; empty/current/refreshing environment reports. Preserve all safety assertions. Source-string expectations may require intentional updates to the new labels/state structure, not blind rebaselining.

Operations remain composition-scoped. Leaving during an IO operation cancels UI work; a provider write already executing may still complete or leave a partial destination. The UI does not claim resumable background export or process-death recovery. The exported document must be inspected by the testing agent for interruption cases.

## Component reference

https://www.carbondesignsystem.com/building-blocks/core/components/inline-loading/guidelines
https://www.carbondesignsystem.com/building-blocks/core/components/notification/guidelines

Applied: progress tied to the actual operation, recoverable inline errors and persistent readable feedback. Native Compose controls retained; no dependency added.
