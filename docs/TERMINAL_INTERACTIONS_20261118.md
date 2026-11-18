# Terminal interactions — implementation ledger

Branch: integration/terminal-interactions-20261118. Parent integration: palette-actions c4ef4be plus terminal-warning-diagnostics-action fa7318a. Existing warning/Diagnostics action, workspace selector and Settings accessibility changes retained.

Ruling: implementation and static review here; execution of build, lint, JVM, instrumented and Pixel checks belongs to the user's testing agent. No Android success or completed experience is claimed. No main merge.

## Implemented interventions

Each entry names a changed behavior and the acceptance condition for the testing agent. Variants of a shared fix are identified rather than represented as separate root defects.

### Session editing and lifecycle
1. Rename uses the same canonical name validator as the session model; a save cannot silently produce a different accepted name.
2. Unsupported internal controls/format characters produce a field error instead of being silently removed on save.
3. Overlong pasted names remain editable with a visible error rather than silently rejecting the entire edit.
4. Draft input is bounded by Unicode code points; truncation cannot split a surrogate pair.
5. Normal IME composition/selection is retained; only truncated edits clear composition and clamp selection.
6. Invisible-only names cannot enable Save.
7. Surrounding-whitespace-only changes do not create a redundant save/discard prompt.
8. Accepted Save cannot be submitted twice while the dialog closes.
9. Back, outside dismissal and Cancel on changed drafts offer an explicit discard choice instead of ignoring Back or silently losing changes.
10. Keep editing restores focus/IME after discard-dialog teardown via a frame boundary.
11. Rename state is scoped by tab ID, preventing two identically named sessions from sharing a draft.
12. Validation feedback is exposed as a polite accessibility live region.
13. Session-close confirmation state is keyed to session ID, preventing reuse for another sheet target.
14. Finished sessions expose Remove output, distinguishing transcript deletion from stopping a running process.
15. Close confirmation bodies scroll under large text/short viewport conditions.
16. New-terminal actions guard launchingBash independently of the caller's capacity flag.
17. Shell chooser prioritizes the launching status over a stale ready flag.
18. Shell chooser scrolls at constrained height and bounds its text column alongside the icon.
19. Session activity and launch status expose live/state semantics instead of silent status changes.

### Terminal navigation and selection
20. Tab strip exposes a selectable group for accessible navigation.
21. Tab targets/pills grow with large fonts instead of clipping text to fixed 48/36dp heights.
22. Tab titles reserve width for activity indicators, preventing long titles from consuming the JOB/ended indicator.
23. Compact More actions stays outside horizontal tab scrolling and remains reachable at either end.
24. Each tab offers a named Manage session accessibility action, without requiring a long press.
25. Changing system font scale brings the selected tab back into view using its new measured size.
26. Native selection preview height adapts to the available screen and remains bounded.
27. Native selection preview announces its read-only purpose.
28. Copy explicitly says Copy all output when there is no selected range; the label follows native selection changes.
29. Select all is available inside the selection dialog after adjusting/collapsing a range.
30. Native preview padding uses density conversion rather than raw physical pixels.
31. Read-only terminal transcript preview is excluded from autofill.

### Clipboard review
32. Paste review identifies the destination session by name.
33. Rejected paste retains the reviewed payload and offers Retry, without rereading a changed clipboard.
34. Accepted paste is guarded against repeated taps before dismissal; no clipboard content is persisted.
35. The entire review body scrolls in one bounded viewport, keeping actions accessible with long warnings/large fonts.
36. Reviewed clipboard text supports selection/copy for precise inspection.
37. Paste failures are inline live-region feedback; every attempt brings the error into view, including identical repeated failures.
38. Menu Paste is disabled when no safe clipboard payload or writable terminal exists; the callback still revalidates state.

### Transcript search and recovery
39. Search query limiting/counting uses code points rather than splitting emoji at the 128-character boundary.
40. Find obtains field focus, disables autocorrection and gives IME Search a non-executing action; clear restores editing and close hides IME.
41. Previous/Next traverse adjacent result pages and wrap across the whole result set, not only the current 300 results.
42. Match navigation/page controls wrap instead of hiding actions in a horizontal overflow or squeezing opposite buttons.
43. The bounded excerpt is fully displayed so the matched span cannot disappear behind three-line truncation.
44. A case-sensitive empty result offers Ignore capitalization while preserving the query.
45. Streaming output is sampled independently of query debounce; continuous output cannot indefinitely postpone search or repeatedly hide results/actions during refresh.
46. Go to line revalidates session identity before scheduling navigation.
47. Session-failure text scrolls within a bounded area and its recovery actions wrap separately, keeping both readable without consuming the terminal viewport.

## Review / evidence

`git diff --check`: clean. Independent static review identified output-refresh flicker and premature rename-focus restoration; both corrected. Additional author review covered full-page navigation, Compose state capture and bounded dimensions. No runtime evidence is inferred from static review.

Tests, build, lint: NOT RUN here. CI status: UNKNOWN. Android captures: NOT PRODUCED. Known source-string assertions for old rename validation, fixed selection height, query take(128), and old clipboard confirmation labels may need intentional-contract updates by the testing agent; none were deleted, skipped or weakened here.

Required verification: compile; session rename/control/emoji/IME tests; rotation during rename/discard; repeated saves; close running/finished sessions; large fonts/landscape/short viewport; 8 tabs and compact menu; TalkBack tab management and live errors; clipboard retry/full queue/session switch/changed clipboard; native selection collapse/reselect/copy; >600 search matches across pages; sustained streaming while searching; case recovery and query Unicode; existing terminal keyboard shortcuts and persistent PTY sessions.

## Design references

https://www.carbondesignsystem.com/building-blocks/core/components/search/accessibility — explicit clear action and accessible search messaging.
https://www.carbondesignsystem.com/building-blocks/core/patterns/search — scoped search with useful result context.
https://developer.android.com/develop/ui/compose/text/user-input — native input/keyboard behavior. No new dependency.
