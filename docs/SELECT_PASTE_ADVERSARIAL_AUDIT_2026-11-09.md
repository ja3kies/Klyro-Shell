# SELECT/PASTE adversarial audit — 2026-11-09

## Scope and source of truth
Target: `TerminalPane.kt`, `TerminalActionPolicy.java`, `ClipboardPastePolicy.java`, `ClipboardTextReader.java`.
Branch: `p0/select-paste-adversarial-audit-20261109` based on `integration/cycle-03`.
The contextual behavior (copyable transcript → SELECT; otherwise → PASTE if clipboard valid) remains **required**, not replaced with separate commands.

## Adversarial scenarios and changes
- **Displayed action / tap race**: the tap handler now receives the action represented by the key. When SELECT was displayed and the transcript disappears, report a selection failure instead of silently pasting clipboard content (which might execute shell commands).
- **Stopped session**: no direct clipboard, native clipboard, or reviewed multiline paste can write to a terminated PTY; copying old transcript remains allowed.
- **Null emulator during startup or teardown**: reject clipboard write with visible feedback rather than silently doing nothing, including confirmed paste.
- **OS clipboard focus restrictions**: denied read disables paste gracefully. Refresh available clipboard on the actual view-tree lifecycle ON_RESUME; dispose the listener and lifecycle observer.
- **Large terminal output**: bounded 2 Mi UTF-16 code-unit selection snapshot prevents retaining an unbounded extra Compose-state transcript; a truncation warning remains.
- **Unicode boundary**: truncation cannot start at the low half of an emoji/supplementary pair, with JVM regression cases.
- **Selection copying**: account for selection start/end order, null clipboard service and a collapsed selection; UI feedback distinguishes selection from copying all visible output.
- **Clipboard input safeguards preserved**: allow a single text clip only, reject URI/intent, blank text, malformed UTF-16, invisible formatting controls, noncharacters, oversized UTF-8 and unexpected shell controls; multiline and CR-only inputs retain a review confirmation.
- **Visual behavior preserved**: contextual SELECT/PASTE captions and separate ink/background; no extra PASTE button and no changes to shortcut navigation.

## Tests
- `TerminalActionPolicyTest.java`: boundary, length and surrogate-pair logic.
- `ClipboardPastePolicyTest.java`: UTF-8 limits, Unicode and multiline confirmation.
- `tools/test_klyro_select_paste_adversarial.py`: source-level contracts.
- Full Android CI verifies tests, lint, ARM64 build, ELF 16 KiB and APK signature.
- **Device validation is not implied by these tests**.

## Remaining physical Android acceptance
1. Empty terminal + a short text clipboard: PASTE visible and pastes once.
2. Copyable output: SELECT visible and opens read-only selection; Android Copy copies selected text only.
3. Empty clipboard + empty transcript: shortcut disabled, no command sent.
4. Terminal transcript changes during tap: action must never silently switch into a shell paste.
5. Valid multiline clipboard: review whole payload, Cancel leaves PTY unchanged; Paste inserts only after consent.
6. Background app → copy text elsewhere → return: PASTE state refreshes.
7. Shell exits while confirmation is open: pressing Paste does not write and shows an error.
8. Terminated PTY output still allows SELECT and Copy.
9. Output >2 Mi characters: only the bounded latest section is shown with truncation warning, no crash.
10. Portrait/landscape, dark/light/monochrome, TalkBack, long-press label, and Gboard keyboard: controls remain accessible with distinct mode indication.
11. Stress cycles: repeatedly switch tabs, change clipboard, dismiss dialogs and rotate while session output changes; no duplicate paste or lost session.

## Release discipline and next phase
Keep `main` unchanged. This is a targeted adversarial audit gate, **not** the claim that a new 25-action integration cycle has been completed. Continue with one complete screen at a time once the user specifies the screen (Home, Appearance, Terminal, Workspaces, Plugins, etc.). Every per-screen audit must inspect the entire screen, all states and related dialogs, implementation + regression tests, full CI, and Pixel acceptance before closing.
