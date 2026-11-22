# Approved feature implementation ledger

Review this ledger before every cumulative Preview build. An approved design is an implementation commitment, not a loose concept. No declined or undecided feature should be added without a new decision. Keep evidence and status separate: code present does not mean verified on a physical Android device.

| Approved decision | Implementation | Verification |
|---|---|---|
| One recent session on Home, direct resume, hidden when absent | KlyroHomeRecentSession + MainActivity | JVM selection tests; device verification pending |
| Last workspace shortcut, no Home command history or Home command pins | MainActivity + existing KlyroWorkspaceSheet | Device verification pending |
| Keep original terminal keys; second page for four executable command slots, with animated 2-dot page indicator (no text tabs) | TerminalPane HorizontalPager + KlyroPinnedCommandBar + KlyroKeyboardPagerIndicator | JVM scope/capacity/reorder tests; Compose page-indicator UI tests added; physical verification pending |
| Custom label, global/workspace scope, edit/delete/reorder; no duplicate action | KlyroPinnedCommandStore + policy | JVM tests; device verification pending |
| Compound/multiline commands, no execution from command discovery | Explicit pinned-command submit path; command palette stays insert-only | JVM validation + atomic terminal input tests; device verification pending |
| Advanced settings switch, inline animation speed, persisted selection | Home illustration settings + Appearance | JVM settings tests; device verification pending |
| Four native theme-aware illustrations with separate motion | KlyroHomePixelCover + illustrations | Render tests compiled; device verification pending |
| User-facing cumulative Preview notes, preserve signed lineage | PREVIEW_NOTES.md + android-build.yml + updater | CI contracts |
| Saved accent favorites can be applied later without entering Edit, with ARGB preserved | KlyroColorPickerDialog + KlyroFavoriteColors | JVM saved-color roundtrip and Compose tap-to-apply regression tests added; device verification pending |\n| No customization/hiding Home components for now; no history or command pins on Home | Excluded intentionally | Review before publication |

Deferred: alternate command creation from terminal history (user wants to discuss later). No new creation-from-history workflow. Physical tests and screenshot comparison remain necessary before claiming the approved UI is visually validated.
