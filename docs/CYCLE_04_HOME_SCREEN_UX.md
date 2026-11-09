# Home screen — Complete UX cycle (2026-11-09)

Branch: `p1/home-screen-complete-ux-20261109`.
Base: validated SELECT/PASTE audit branch, without altering the shortcut behavior.
Stable `main` is protected; no merges into `main`.

## Home screen implementation ledger

| ID | Area | Functional improvement |
|---|---|---|
| H01 | Information architecture | Runtime and the main task are shown before secondary Plugins discovery |
| H02 | Information architecture | Identity header precedes hero, primary runtime card and plugin shortcut |
| H03 | Branding | Logo artwork fits inside its visible square without oversized 80 dp crop |
| H04 | Branding | Branding square respects the current theme rather than forced near-black |
| H05 | Branding | Branding square has standardized card rounding |
| H06 | Branding | Decorative icon no longer redundantly reads the name through TalkBack |
| H07 | Branding | Logo selection still matches launcher alias selection |
| H08 | Responsiveness | Weighted brand header preserves the ARM64 identity badge on narrow phones |
| H09 | Responsiveness | Brand text truncates rather than crashing into device badge at large font sizes |
| H10 | Visual consistency | ARM64 chip uses shared corner tokens |
| H11 | Terminal context | Open session count is surfaced directly on Home |
| H12 | Terminal context | Session count uses singular/plural names and reports workspace |
| H13 | Terminal context | Workspace line gracefully ellipsizes |
| H14 | Status integrity | Session failures take priority over stale runtime-ready status |
| H15 | Status integrity | Open sessions remain explicitly available when environment check is not ready |
| H16 | Status integrity | Status accent differentiates errors, ready and available-session states |
| H17 | Status integrity | Installer and verifier status overrides stale terminal output messages |
| H18 | Status integrity | Empty session list shows actual current runtime message |
| H19 | Accessibility | Status-state transitions announce via a polite live region |
| H20 | Visual consistency | Runtime card uses a differentiated bento tile rather than background chrome |
| H21 | Warnings | Unusual `update-alternatives: warning:` diagnostics are not omitted |
| H22 | Warnings | All distinct runtime warnings can be expanded instead of only first line |
| H23 | Warnings | Show less restores a concise first-warning preview |
| H24 | Warnings | Warning card uses shared corner radius |
| H25 | Diagnostics | Technical detail actions stack vertically for larger text settings |
| H26 | Diagnostics | Actions use standard 48dp minimum touch targets |
| H27 | Diagnostics | Technical preview caps rendering at 16,000 characters without losing full copied report |
| H28 | Diagnostics | Very long diagnostics explain preview truncation and whole-report copy |
| H29 | Diagnostics | Copy handles absent/denied Android clipboard without crashing |
| H30 | Diagnostics | Diagnostics viewer adopts shared rounded control corners |
| H31 | Install feedback | Unknown install progress is indeterminate, rather than a misleading 0% determinate bar |
| H32 | Install feedback | Known progress renders as 0–100% bounded and clearly labeled |
| H33 | Accessibility | Installation progress includes semantic description |
| H34 | Session recovery | Return to terminal is offered even if environment verification is currently failing |
| H35 | CTA hierarchy | Existing session resume is the primary filled CTA; setup becomes secondary |
| H36 | CTA hierarchy | Launch Bash appears only when verified and there are no open sessions |
| H37 | Error prevention | Retry install does not falsely imply cleaning or deleting existing data |
| H38 | Controls | Recheck disables during installation/verification and reports checking state |
| H39 | Plugins | Plugin entry uses an explicit button role and on-click label |
| H40 | Plugins | Plugin route survives configuration changes without closing sessions |

The rows describe UX behaviors, not isolated commits. Shared implementation changes may satisfy several behaviors.

## Verification gates
- Static source contracts: `tools/test_klyro_home_screen_ux.py`.
- Existing Android, plugin, PTY, SELECT/PASTE and brand regression tests must pass unchanged.
- Full Android GitHub Actions: tests, lint, ARM64 build, signed APK and 16 KiB audit.
- Pixel 9 acceptance: default/light/dark/monochrome; 200% font; portrait and landscape; zero/open/8 sessions; runtime verification failure; install progress and warnings; copy technical report (including no clipboard service); Plugins -> Back; session resume without PTY loss.
- A green CI is not a physical UI pass. Do not consider Home fully accepted until user confirms the Android test results.
