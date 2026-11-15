# UX cycle 02 — five priority corrections

Branch: `feat/ux-five-priorities-cycle-02`. Baseline: `integration/cycle-01`.
**Exclusion:** the user explicitly retained the contextual ALL/PASTE shortcut; its action and visuals remain unchanged.

## Changes grouped by UX principle

| ID | Area | Implemented behavior | Verification |
|---|---|---|---|
| UX02-01 | Shared actions | Screen readers receive a descriptive action label plus subtitle | `test_klyro_ux_five_priorities.py` |
| UX02-02 | Shared actions | Destructive actions announce their destructive state | Source contract |
| UX02-03 | Shared menus | Every menu item receives a 48 dp minimum touch target | Source contract |
| UX02-04 | Shared menus | Menu actions expose a consistent accessible name | Source contract |
| UX02-05 | Rename session | Empty invalid name has in-place explanatory feedback | Source contract |
| UX02-06 | Rename session | Name-length constraint is explained before Save | Source contract |
| UX02-07 | Choose shell | Available Bash selection is explicitly a button with a minimum touch area | Source contract |
| UX02-08 | Palette | Search text can be cleared without dismissing the palette | Source contract |
| UX02-09 | Palette | Empty search results offer Show all commands | Source contract |
| UX02-10 | Palette | Rejected plugin command insertion gives a visible reason | Source contract |
| UX02-11 | Palette | Inline error and requirements messages use a polite accessibility live region | Source contract |
| UX02-12 | Palette | Both built-in and plugin command rows meet 48 dp minimum target | Source contract |
| UX02-13 | Workspaces | An empty catalog renders recovery guidance instead of calling `entries.first()` | Source contract |
| UX02-14 | Workspaces | Remaining workspace-name characters are shown while editing | Source contract |
| UX02-15 | Workspaces | Save is disabled for blank project names | Source contract |
| UX02-16 | Workspaces | Saving an unchanged name is disabled in rename mode | Source contract |
| UX02-17 | Workspaces | Session navigation rows meet minimum touch height | Source contract |
| UX02-18 | Workspaces | Move-session action meets minimum touch height | Source contract |
| UX02-19 | Settings | Unknown deep-linked section falls back to Settings home | Source contract |
| UX02-20 | Settings | Android Back from a subpage returns to Settings home first | Source contract |
| UX02-21 | Settings | Nested color/licence dialogs retain their own Back dismissal | Source contract |
| UX02-22 | Launcher UX | Icon choices expose radio-button semantics to assistive tech | Source contract |
| UX02-23 | Launcher UX | Icon selection skips switching only if PackageManager confirms alias state | Source contract |
| UX02-24 | Launcher UX | Failed icon changes announce errors to accessibility services | Source contract |
| UX02-25 | Settings | Unavailable system-permission page announces a recoverable error | Source contract |
| UX02-26 | Settings | Done action meets the global minimum touch height | Source contract |
| UX02-27 | Plugins | Back dismisses a pending import/remove confirmation before leaving the page | Source contract |
| UX02-28 | Plugins | A failed plugin state change is not treated as success | Source contract |
| UX02-29 | Plugins | Enable/disable switch names correspond to actual active states | Source contract |
| UX02-30 | Plugins | Errors are announced by accessibility services | Source contract |
| UX02-31 | Plugins | Failed manifest installation retains its review dialog for retry | Source contract |
| UX02-32 | Plugins | Failed uninstall retains its confirmation dialog for retry | Source contract |
| UX02-33 | Plugins | Import/uninstall dialog shows errors in place rather than behind modal | Source contract |
| UX02-34 | Plugins | Bundled-install navigation changes only after a successful installation | Source contract |

**Scope warning:** this is a source-level change ledger, not evidence that all 34 fixes are proven on hardware. Some items are complementary subbehaviors of the same interaction. Do not use the count alone as a release verdict.

## Release gates
1. Run all Python, app JVM, terminal-emulator JVM and lint checks on the final cycle commit.
2. Build debug and release APK, verify metadata, signing and native 16 KiB alignment.
3. Execute physical Android scenarios: TalkBack for icons/errors/labels; 200% font; smallest portrait and landscape; Android Back and modal dismissal; empty workspace catalog; invalid workspace/session name; command search reset; plugin install/remove failure and retry.
4. Record screenshots or device logs for device findings and fix any regression.
5. Preserve `main` and avoid changing the contextual ALL/PASTE design.
