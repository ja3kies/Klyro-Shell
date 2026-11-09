# Cycle 03 — Keyboard feedback, appearance and modal consistency

Integration target: `integration/cycle-03`. Specialist branch: `p1/ux-modal-keyboard-cycle-03`. Base: successful T4 confirmation fix `63713b9`. Do not merge into `main`.

## Verified code changes to qualify (30 distinct UX behaviors)

These are implemented behaviors, not a count of tests, commits, formatting edits or CI assertions. Evidence: changed files plus `tools/test_klyro_cycle03_modal_keyboard.py`, full CI build and a separate Android acceptance pass.

| ID | UX surface | Implemented behavior |
|---|---|---|
| 01 | Keyboard | A visual action label appears while a shortcut is held |
| 02 | Keyboard | Arrow long-hold labels describe line navigation rather than a generic arrow |
| 03 | Keyboard | Press hint is non-focusable and does not intercept the held key |
| 04 | Keyboard | SELECT displays a visible action caption below its glyph |
| 05 | Keyboard | PASTE displays a different visible action caption below its glyph |
| 06 | Keyboard | Paste state has distinct warning-color treatment rather than select/default ink |
| 07 | Keyboard | Paste state has a different colored background while preserving the contextual action |
| 08 | Plugins | Imported plugin confirmation uses the same modal radius as bundled installation |
| 09 | Plugins | Plugin removal confirmation uses that modal radius |
| 10 | Workspace | The rare unavailable-workspace recovery dialog uses the shared radius |
| 11 | Color picker | Hex input has a minimum 48dp high interactive surface |
| 12 | Color picker | Live color preview matches the shared 48dp touch-size rhythm |
| 13 | Color picker | Incomplete hexadecimal input announces an accessible error |
| 14 | Color picker | Advanced-color entry control reaches the shared minimum touch size |
| 15 | Saved colors | Saving a favorite exposes a 48dp minimum action |
| 16 | Saved colors | Remaining color-name length is visible during editing |
| 17 | Saved colors | Each favorite row retains a 48dp minimum height |
| 18 | Saved colors | Edit and Remove each have a 48dp minimum target |
| 19 | Saved colors | Destructive Remove uses semantic danger color |
| 20 | Saved colors | Done control uses a 48dp minimum target |
| 21 | Shell selection | Back button is reachable with a 48dp target and nonduplicated icon semantics |
| 22 | Shell selection | Dialog spacing derives from shared design metrics |
| 23 | Palette | Android Back from welcome guide returns to commands |
| 24 | Workspaces | Android Back cancels in-sheet editing instead of closing the whole workspace sheet |
| 25 | Appearance | Long subpages scroll even when Settings home fits without scrolling |
| 26 | Appearance | Back and font reset actions use the common 48dp touch minimum |
| 27 | Appearance | Current launcher icon gains an explicit visual checkmark |
| 28 | Appearance | A previously failed icon-switch warning clears after reselecting a verified icon |
| 29 | Terminal | Runtime error notices announce changes to accessibility services |
| 30 | Terminal | Environment warning notices announce changes to accessibility services |
| 31 | Terminal | Error/warning notice corners use common tokens |
| 32 | Terminal search | Search hit previews use common rounded shapes |
| 33 | Terminal search | Previous/Next/Go-to-line actions use the 48dp minimum |
| 34 | Terminal recovery | Home action in error state uses a 48dp minimum target |
| 35 | Plugins | Installed/Explore tabs use 48dp minimum targets |
| 36 | Plugins | Home navigation has a 48dp minimum target |
| 37 | Plugins | Add, Remove and Import JSON have 48dp minimum targets |
| 38 | Plugins | Cancel cleans up stale error state after unsuccessful install/remove |
| 39 | Plugins | Import and Remove cancellation use 48dp targets |

Some individual entries share a component or implementation; the count is a functional-behavior ledger, not separate source files or commits.

## Nonnegotiable invariants

- Keep ALL/PASTE contextual behavior unchanged; only visual affordances are improved.
- Keep PTY, session persistence, clipboard confirmation, permissions and plugin installation consent intact.
- Android launcher icon visual cache may require launcher refresh; do not equate OS alias calls with on-device UI proof.
- Do not modify the stable `main` branch.

## Final validation

CI: Python regression suites, app/emulator JVM tests, Android lint, debug/release builds, ELF 16KiB alignment and signed APK. Android Pixel 9: hold every shortcut and confirm label visibility without extra key events; compare SELECT/PASTE in light/dark/monochrome; scroll Appearance with large text and keyboard up; cancel/retry plugin install; compare rounded modals; TalkBack announcements and focus order; navigate Back from palette guide and workspace edit. **Do not claim device PASS without user-provided results.**
