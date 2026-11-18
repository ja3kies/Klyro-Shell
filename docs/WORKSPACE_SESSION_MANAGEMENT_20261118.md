# Workspaces — session management, 18 November 2026

Status: implementation and static review only; Android acceptance pending.
Base: local c7a7e24, identical tree to remote 230f9e82409a5489f4093f8b2b28caae237927b7.
Branch: design/workspace-session-management-20261118. No main merge.

## Scope and decisions

The workspace sheet could switch sessions but could not rename or close them. Session taps used a rectangular indication, workspace names were silently truncated by UTF-16 slicing, and an inline form could sit below twelve workspace cards. The existing session rename form already handled drafts more carefully. Reuse that form through `KlyroNameDialog`, with a Boolean save result and contextual validation, instead of adding a second validator.

Ruling: tests/build/emulator execution remain assigned to the user's other agent. Existing instrumentation call sites and discard expectations are adapted, not executed. No red/green or device claim is made.
Ruling: the requested edit animation is a finite 620 ms tilt cue, not perpetual motion. Controls remain still for actual editing, system animation duration applies, and the cue stops when backgrounded or covered by a child modal. Device confirmation remains required.
Ruling: Back/swipe dismisses Workspaces in edit mode because it contains no draft; Done exits edit mode. A name form owns dismissal and asks before discarding changes. No shared sheet lifecycle changes.
Ruling: the remote in-app-updates branch is separate work and is not merged into this focused change.

## 45 implementation objectives and acceptance cases

These are behavior/component objectives, including edge cases; they are not 45 independent defects or a count of lines changed. All acceptance cases below still require execution.

1. Expose session editing from the Sessions heading with a labeled icon.
2. Offer direct rename from each session's pen button without requiring edit mode.
3. Enter the same edit mode with long press.
4. Present a Done action to leave edit mode.
5. Expose an independent rename icon for each session during editing.
6. Expose an independent close/remove icon during editing.
7. Make the session body rename in edit mode, rather than unexpectedly switching.
8. Keep ordinary session taps as navigation outside editing.
9. Let the current session row return to its terminal instead of being disabled.
10. Keep Workspaces open after a rename.
11. Keep Workspaces open after closing a non-last session.
12. Reuse existing closeTab/service cleanup rather than closing PTYs in UI code.
13. Target actions by stable session ID, including duplicate titles.
14. Name the actual session in close confirmation.
15. Explain running Bash/child-process termination before confirmation.
16. Use Remove output and retained-output consequences for finished sessions.
17. Keep cancellation non-destructive with Keep session.
18. Guard shared close confirmation against repeated submissions.
19. Remove a stale session rename modal if its target disappears.
20. Remove a stale close modal if its target disappears.
21. Disable underlying sheet controls while a child action owns interaction.
22. Show Current and Finished as separate meaningful states, including their combination.
23. Allow two lines for session titles so action controls retain space.
24. Provide custom accessibility rename/close actions outside edit mode.
25. Give icon actions explicit target-specific accessible names.
26. Clip workspace session touch feedback to rounded row geometry.
27. Clip terminal-tab feedback to its rounded touch target.
28. Add a brief edit-state motion cue to session cards.
29. Suspend/cancel that cue when lifecycle is not resumed or a modal covers it.
30. Return the cue to zero rotation after completion/cancellation; no infinite loop with animations disabled.
31. Use stable composition keys for session rows and target dialogs.
32. Preserve the Workspaces owner, edit mode and pending action IDs through saveable state, including when closing the active session selects its neighbor.
33. Clear edit mode when no sessions remain in the workspace.
34. Preserve the displayed workspace scroll on recreation; start at the top on workspace switching.
35. Move workspace naming to a focused modal instead of a buried inline form.
36. Select the existing workspace name and request IME focus through the shared form.
37. Reject duplicate names case-insensitively before invoking storage.
38. Reject control characters and visibly explain the canonical name mismatch.
39. Reject overlong names visibly instead of silently truncating at 32 UTF-16 units.
40. Bound large pasted input at code-point boundaries without splitting a surrogate pair.
41. Require discard confirmation for changed workspace drafts, restoring focus on Keep editing.
42. Keep a rejected save open and explain the failure; never claim success on false.
43. Recheck workspace presence and creation limit while a form remains open.
44. Permit moving finished sessions, revalidate session/workspace identities, and report a rejected move rather than claiming it succeeded.
45. Share one session-close confirmation implementation, keeping the existing rename API compatible and adapting workspace instrumentation fixtures to the new contracts.

## Independent review and evidence

Whole-lot independent static review completed; no build or runtime checks. Reviewer identified an overstatement about per-workspace scroll retention: the implementation retains the visible workspace on recreation, but intentionally resets to the top on workspace switches. Objective 34 corrected; no A→B→A position retention is claimed. Important finding fixed: ordinal session labels change when another session exits; using the live name as a rememberSaveable key discarded an in-progress rename. KlyroNameDialog now captures its initial name once inside the caller's ID-keyed form, and keeps field/discard/error state independent of later display-label changes. This addresses both workspace and terminal rename entry points. Execution of the regression remains delegated.

Ruling: no A→B→A scroll retention added; resetting on workspace change remains intentional. No other concrete blocking issue was identified by the independent static reviewer, which is not a completeness guarantee.

No compilation, JVM tests, instrumentation, lint, emulator, Pixel capture or CI result is claimed. No new APK produced. Before images are the user's earlier screenshots, which do not show this new workspace flow; no fabricated after images.

## Handoff for the test agent

- Compile app plus androidTest sources, run relevant JVM suites and lint. Old source-shape assertions may reference the removed inline workspace form; replace with behavior checks without weakening intent.
- Execute ModalLifecycleUiTest, especially changed-workspace drafts, Back/IME, swipe, discard/Keep editing and underlying terminal interaction.
- Explicit regression: edit default Session 2, let Session 1 exit and disappear, confirm typed draft/selection/discard state remains on the original ID, then save and check only that session changes. Repeat after rotation.
- Add/run workspace session flows with two identical names in different workspaces, active/inactive/finished sessions, last session closure, target removal while dialog open, rename then close, cancel and repeated confirm.
- Test 12 workspaces, empty workspace, 32-unit boundary, emoji at boundary, 128-code-point paste bound, controls, duplicate casing, backend false, unchanged names.
- Verify moving a finished current session and stale callback rejection; no PTY restart or unrelated session closure.
- Pixel/emulator screenshots: normal rows, edit mode, rename with keyboard, validation error, discard confirmation, close live/finished, empty state. Check font scale 2x, narrow landscape, TalkBack custom actions, keyboard activation, animation scale 0 and app backgrounding.
- Confirm edit cue feels deliberate and rounded touch feedback remains visible. No continuous animation or keyboard shortcut changes are introduced.

## Design reference

[Material / Jetpack Compose icon buttons](https://developer.android.com/develop/ui/compose/components/icon-button), consulted 18 November 2026. Use a recognizable pen for rename/edit and a destructive close glyph only in edit mode, with names and disabled states. This library reference supports action clarity; it does not establish that this untested layout is aesthetically superior. A pen opens rename directly; three dots are reserved for actual workspace menus.

[Compose animation guidance](https://developer.android.com/develop/ui/compose/animation/customize): use the existing Compose animation clock, without a custom timer or a new motion dependency.

## Simulated expert critique (not user validation)

- Ergonomics: explicit pen actions and contextual confirmation reduce navigation; actual narrow-screen proportions need screenshots.
- Material: native IconButton/AlertDialog and shape-clipped indications retain platform interaction feedback; newer Expressive APIs are not assumed available in this BOM.
- Motion: finite edit cue avoids continually moving targets; animation scale 0 and lifecycle interruption remain device acceptance gates.
- Accessibility: named icon actions, state descriptions and custom actions are implemented; TalkBack order/contrast/font scaling are not measured here.
- Android: stable target identity and initial-name capture address ordinal-renumbering draft loss; instrumented recreation and modal disposal are still unverified.
- Terminal user: rename/move stays metadata-only; close explicitly warns about process loss and uses existing cleanup. Multi-session/foreground-job regression checks remain assigned.
