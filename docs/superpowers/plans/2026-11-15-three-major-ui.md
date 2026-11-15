# Klyro: three major UI corrections — design and execution (2026-11-15)

**Base:** validated `fix/klyro-three-core-hardening-20261115` at `81c2527`.  
**Target:** `fix/klyro-three-major-ui-20261115`; no merge or device installation.  
**Constraints:** native Android Compose/Material 3, existing Hugeicons assets, 48 dp touch targets, TalkBack semantics, no PTY restart, no data loss, no new dependency.

## 1. Settings: reachable controls and non-blocking launcher selection

### Problem
A rows/font-scale guess decides whether the *entire* settings sheet scrolls. The title/back/close controls scroll off with content. On Appearance, changing launcher icons synchronously calls PackageManager from a click callback.

### Design
Keep Settings header (back and close) in a stable top row. Put the varying section body in an independently scrollable, available-height-bounded column; scroll only when its measured content exceeds the available size. Retain navigation-bar and keyboard insets. Switch launcher aliases on Dispatchers.IO, guard reentry while a selection is pending, and announce error/progress without mutating terminal sessions.

**Checks:** structural source regression + existing settings bottom-inset, modal lifecycle, ANR and launcher tests + Android compilation; Pixel QA with large fonts, rotation, gesture nav, long diagnostics.

## 2. Command Palette: keyboard-first discovery and sheet geometry

### Problem
Search must be tapped manually; the keyboard's Search action does not finish search entry; the fixed-height results pane can collide with the IME. List size / active filters have insufficient immediate context.

### Design
Focus the search field when the palette opens, use `ImeAction.Search` only to hide the keyboard (never execute a command), and bound the sheet to the available height with an elastic results list. Show the result count and preserve existing pinned/available filters, empty-state recovery, explicit insert, and caution confirmation. Never auto-submit shell commands.

**Checks:** regression source test, existing command palette tests + CI; Pixel QA with Gboard, TalkBack, narrow landscape, changing filters while typing.

## 3. Terminal: persistent session creation affordance

### Problem
The + session control is inside a horizontally scrollable row. On multiple tabs, it can disappear beyond the right edge, even though creating a new session is a primary action.

### Design
Make the tab list alone horizontally scrollable and keep the 48 dp new-session button fixed at the end of the row. Preserve session labels, selected state, unread signals, tap-and-hold session actions, maximum-eight disabled state and compact toolbar.

**Checks:** structural regression and existing session-tab accessibility tests + CI; Pixel QA with 1 and 8 sessions, renaming, app rotation, small width and large system font.

## Approach selection
Prefer minimal corrections within existing Compose components and scroll/Coroutine primitives. Rejected a replacement navigation architecture, third-party tab library, new UI dependency or reworked terminal rendering because they increase risk without solving these defects.

## TDD and release checklist
1. Add regression tests for every observable objective and demonstrate fail against old UI.
2. Apply each correction within existing files only.
3. Run complete Python + Kotlin/JVM tests; run lint, debug/release/test APK builds, signed packaging, 16-KiB audit.
4. Inspect CI SHA and failures. Device behavior stays *unverified* unless Pixel evidence exists.
5. Keep branch unmerged and preserve all data/runtime.
