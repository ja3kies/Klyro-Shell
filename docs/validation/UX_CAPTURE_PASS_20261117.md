# UX capture pass — implementation handoff, 17 November 2026

## Authority and scope

User supplied Home, Plugins and Settings Android captures plus a minimal slider
reference. APK SHA of captures is unknown. User explicitly assigns tests to a
separate agent; this pass implements changes and performs code review only.
Ruling: device/TDD gates belong to that agent, so no Android test claims or
blocked implementation. No main merge. No new dependencies or terminal changes.

Base combines `f58ecde` (Home continuity) and `1b8485f` (Settings actions).
The newer Settings controls are retained instead of repeating another agent's
work based on older screenshots.

## Implementation ledger

| Area | Change | Acceptance for validation agent |
| --- | --- | --- |
| Home | Recheck moves below status at large font / narrow width | 320dp, 200% text: status readable and action reachable |
| Home | Pause GIF behind Settings and repair dialog, retaining drawable | No decoding while covered; resume without replacing cover |
| Plugins | Single header row combines back navigation and title | Home action and title readable in portrait/landscape/200% text |
| Plugins | Shorter explanatory copy | User understands commands never execute automatically |
| Plugins | Neutral thin selected outline replaces dominant accent outline | Selected tab remains distinguishable visually and semantically |
| Plugins | Preserve independent Explore/Installed scroll positions | Toggle tabs and return to same list offset; rotation supported |
| Plugins | Reset manifest preview pending flag after cancellation | Cancel reading, reopen import: no permanently disabled button |
| Plugins | Entire enabled row toggles pack; switch is passive visual child | One TalkBack switch and one registry update per tap |
| Plugins | Compact Explore card padding | More useful list content without reducing 48dp actions |
| Plugins | Installed checkmark supplements disabled action text | Recognizable installed state without relying on color |
| Slider | Floating value during drag/press/keyboard focus | Bubble follows actual thumb; no focus theft or stuck overlay |
| Slider | Unit-aware labels for font, hue and opacity | Half-sp sizes, degrees and percentages represented correctly |
| Slider | Matching stateDescription | TalkBack announces current value with units; retains range actions |
| Slider | Explicit minimum 48dp interaction height | Slim visual track retains usable touch target |
| Slider | Circular inset thumb and restrained shadow | Clear handle in light/dark, resting and pressed states |

Native Compose Slider still owns gestures, stepping, range semantics and keyboard
input. The indicator is nonfocusable and cleared from accessibility traversal;
the slider announces its value. Existing color track and presets remain.

## Root causes and dependent flows

Home's single Row shared status width with Recheck; conditional placement avoids
squeezing feedback. The cover's Activity lifecycle alone did not know a modal was
covering Home, so visibility now additionally gates frame playback.

Plugins reset one shared scroll position whenever tabs changed. Two saveable
scroll states preserve each browsing context. Cancelling a manifest preview
changed its URI to null, but that early return did not reset previewBusy; it now
does. Installed toggle previously accepted taps only on the switch, leaving its
label inert. The row owns exactly one toggle callback now.

## Verification split

Executed here: `git diff --check` (clean), source inspection and independent code
review. No APK compiled locally, no device tests executed, no after captures or
final UX score. CI conclusion must be read at the published commit before any
build claim. Validation agent should add behavioral coverage for cancellation,
tab restoration, large-text runtime status and slider focus/units.

Independent review found no blocking code defect; popup edge positioning and
large-font geometry still require device verification. Reviewed against the
Compose API generation used by BOM 2024.10.01. No compilation claimed.

Updated four obsolete expectations in three source-contract files (header
size/copy, cover visibility parameter, preserved tab offsets). No test was run.
Behavioral tests remain the validation agent's responsibility.

References consulted:
- https://developer.android.com/reference/kotlin/androidx/compose/material3/Slider.composable
- https://developer.android.com/reference/kotlin/androidx/compose/material3/SliderDefaults

Remaining: device validation, actual after captures, contrast measurements,
TalkBack, full lifecycle/navigation matrix. Workspaces, Command Palette,
Terminal and Diagnostics have not been claimed as audited or complete.
