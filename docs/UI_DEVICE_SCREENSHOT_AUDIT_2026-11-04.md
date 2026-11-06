# Klyro-Shell — Android UI audit from Pixel 9 screenshots (4 November 2026)

**Baseline:** PR #18, commit `cda9f5c5`. Reviewed provided screenshots of portrait terminal, FIND with Gboard, and Appearance sheet, plus five common UX anti-pattern reference cards. No claim of device verification from source inspection.

| Severity | Evidence / issue | Applied correction |
| --- | --- | --- |
| P1 | Terminal prompt is very close to the clipped top-left corner | Add 8dp/6dp *internal* padding to real TerminalView, keeping one outer clipping shape and stable PTY lifecycle |
| P1 | FIND modal title wraps and crowds content above the keyboard | Short single-line heading, full-width field, bounded two-line result preview; retain user-approved modal style |
| P1 | Landscape compact mode omits title but still uses two full rows of keys | One horizontally scrollable 48dp key row below 300dp available height, preserving terminal height; two fully visible rows otherwise |
| P1 | 38dp plus Button inside non-actionable 48dp wrapper | Full 48dp accessible Button-role target with unchanged 38dp visual |
| P2 | Overflow menu lacks a spoken name, tab semantics duplicate "Session" | "More actions" and active/inactive tab state semantics |
| P2 | Clipboard paste fails without feedback for empty/invalid input | Immediate concise and actionable Toast, preserving multiline confirmation |
| P2 | Accent labels repeated in unnecessary copy; limited selection semantics | Remove redundant caption; add selected accessibility state and font action descriptions |
| P2 | Session sheet handles use fixed gray | Use shared Klyro muted color |

## Reference-card assessment
- No raw stack traces in the ordinary runtime status; the existing optional technical-details affordance is intentional and must remain for debugging.
- Setup already reports installation stage and numeric progress. Skeleton placeholders are not appropriate for opaque native runtime installation or an interactive terminal.
- Shell termination requires a clear confirmation, without manipulative retention flows.
- Clipboard now provides immediate response when the requested action cannot execute.
- Keep primary setup action in a stable position.

## Remaining physical-device acceptance
CI must pass JVM app + emulator tests, Android lint, ARM64 build, signing and strict native 16KiB audit. On Pixel 9 verify FIND counts/navigation and keyboard dismissal, portrait/landscape text insets and key scroll, light/dark accent contrast, modifier behavior, session switching, TalkBack labels and runtime startup. The large blank area near the top of the screenshot could involve system insets/cutout: `MainActivity` currently uses `safeDrawing` and `imePadding`; do not remove cutout safety without physical inset measurements.

**Rollback:** revert this commit on the isolated PR branch. `main`, `feat/klyro-0110`, installed APK data and independent Termux state are untouched.
