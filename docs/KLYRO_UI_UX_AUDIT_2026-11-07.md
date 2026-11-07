# Klyro Shell — Visual + static UX audit (07 Nov 2026)

## Evidence and limitations
- Supplied reference screenshots: 864 × 1939 px, light mode, appearance sheet
  and workspace sheet, Klyro 0.4.6-dev.
- Code review: feature/klyro-storage-ux-20261107.
- External checklists: rshzrh/ui-design-skills/ui-android (verification,
  component-anatomy, icons-imagery, ban-patterns), and
  AjnasNB/mobile-app-ux-auditor-skill.
- A GitHub Actions success is NOT proof of physical device contrast,
  focus behavior, Android 17 folder picker behavior, TalkBack, font scaling,
  keyboard coverage, or landscape screenshots.

## Problems visible in supplied screenshots

| ID | Problem / reason | Correction | Validation |
|---|---|---|---|
| V01 | Two large tinted blocks occupy most of Appearance for short controls | Grouped settings without large cards; typography, accent and font controls individually | Source changed; device pending |
| V02 | A-/A+ buttons are poor for continuous font size selection | Discrete slider 9–24sp at 0.5sp increments, current value and Reset | Source changed; device pending |
| V03 | Equal high-emphasis buttons for System/Light/Dark | Compact segmented selection with selected state | Source changed; device pending |
| V04 | Accent preset buttons cover too much area | 48dp hit targets holding 36dp swatches, explicit selected state | Source changed; device pending |
| V05 | Workspace project tiles overly tall for a name + count | 64dp compact rows; project name stays centered + bold | Source changed; device pending |
| V06 | Rename/Delete exposed for every workspace consume vertical space | Trailing per-project menu; confirmation on delete | Source changed; device pending |
| V07 | Huge full-width Create project button dominates workspace browsing | Compact New text action in sheet header | Source changed; device pending |
| V08 | Session list visually competes with workspace list | 52dp session rows with terminal glyph and selected marker | Source changed; device pending |
| V09 | No visual distinction for a SAF folder already authorized | Connected/not-connected status with scoped meaning | Source changed; device pending |
| V10 | Debug Settings had a different experimental Bento hierarchy | One Settings IA in debug + release; redundant Bento retired | Source changed; device pending |

## Cross-screen inventory (all Compose screens and modal states)

| Surface | Review / ownership | Current finding / follow-up |
|---|---|---|
| MainActivity readiness/setup | Runtime state, verified/not verified, installer progress and errors | Existing explicit states, 48dp primary buttons; verify long text / landscape / interrupted install on device |
| MainActivity terminal host | transparent system bars, safeDrawing, IME, session persistence | Existing insets retained; check for top spacing/double inset on Pixel 9, do not guess from screenshots alone |
| TerminalPane header | brand/system architecture, project name, overflow | Centered bold project title retained; verify title truncation and left-right visual balance |
| Terminal tabs | selected PTY, unread indicators, new session | Existing ≥48dp tab hit row; verify focus/TalkBack + large text |
| Terminal extra keys | CTRL/ALT/FN/SHIFT, arrows, FIND, ALL, CLEAR, PASTE | Preserve validated gestures and protections; no duplicate menu actions |
| FIND dialog | case-aware matching, navigation, transcript row previews | Preview is functional to locate hits, retain; verify landscape/IME height |
| Copy/output selection | export current transcript with safe selection | Preserve; verify long output cannot block selection or hide Copy |
| Multiline paste dialog | avoid accidental shell execution | Preserve truncated warning and confirmation; no auto-Enter from palette |
| Session actions sheet | rename, new Bash, switch and close | Reduced padding and sizes, reused Hugeicons; verify close confirmation |
| Session rename dialog | field focused with current name selected | Retain distinct short-field dialog; confirm TalkBack and IME |
| Choose-shell sheet | only verified Bash runtime | Avoid claiming unavailable shells; terminal glyph replaces unrelated symbol |
| Workspaces sheet | create/edit/delete/switch/assign grouping | Compact 64dp rows with contextual actions and 48dp selection targets |
| Command Palette | shell discovery, read-only help, insertion | Compact command rows, mono glyph, native availability checks; no duplicate app action menu |
| Command welcome guide | one-time interactive Bash greeting | Reopen inside palette without new fullscreen preview |
| Settings home | 6 information architecture sections | One ordered glyph-led list in debug and release |
| Settings Appearance | segmented theme, swatches, font choice/slider | No fake terminal preview; observe actual terminal behind sheet |
| Settings Keyboard | release active modifiers | Contextual action row, no redundant giant card |
| Settings Files | SAF authorized state, change folder, import/export | Native picker, least-privilege read permission; test cancellation |
| Settings Privacy | paste/copy privacy guidance | No duplicated copy button; informative text |
| Settings Diagnostics | environment report, recheck/copy/share | Single home in Settings, not secondary modal |
| Settings About | runtime description, font licence | No debug-only fake status telemetry |
| Color picker basic | HSV hue / saturation / brightness controls, preset colors | Real color selection visualization necessary, preserve |
| Color picker advanced | opacity + saved/favorite colors | Preserve dialog separation; verify small-screen accessibility |
| Color delete favorite | destructive confirmation | Keep AlertDialog, check cancel path |
| KlyroTerminalFonts / Palette | native font, terminal colors | Existing token ownership; verify light/dark and custom accent |
| CLI-to-SAF folder picker | launcher, permission, cancellation, result | New bridge with private request/response file; device check required |

## Source audit against GitHub design skills

| Audit | Status | Basis / remaining exception |
|---|---|---|
| Token ownership | PARTIAL | KlyroUiMetrics expanded; older files still include numeric dp/sp literals. Do not claim universal token purity. |
| WCAG contrast | PARTIAL | Selected accent code contains AA adjustment; dynamic screenshots/secondary statuses not contrast-measured on device. |
| Touch targets ≥48dp | PARTIAL | New workspace/appearance/palette rows explicitly ≥48dp; older screen controls need instrumentation/size inspection. |
| Typography | PARTIAL | M3 typography used for new controls; existing TerminalPane has inline terminal font sizes by necessity. |
| Shapes and spacing rhythm | PASS for new sheets | New sheet rows use tokens; brand overrides documented in DESIGN_RULES.md. |
| Dark theme parity | SOURCE REVIEWED | KlyroColors light/dark and KlyroModalTheme, but no new dark screenshots supplied. |
| Icons | BRAND EXCEPTION | Existing Hugeicons retained, documented instead of silently mixing with Material Symbols. |
| Responsive width/height | SOURCE REVIEWED | Sheets capped at 540dp content width / 620dp height; lists scroll; IME padding added. |
| Empty/loading/error | PARTIAL | Workspaces empty state; picker cancel/error; asynchronous package progress existing. Other historic states to test. |
| Navigation and back | SOURCE REVIEWED | M3 sheet and AlertDialog dismiss; check Android predictive back on Pixel 9. |
| Redundancy | PASS for reviewed flows | No copy/clear in palette, no sessions in Settings, no duplicate Files main menu. |
| Copy, filler, banned gradients | SOURCE REVIEWED | No added filler or decorative gradients; older code merits broader static scan. |
| Detekt | NOT CONFIGURED | Existing project validates Python/JVM + lint. Do not report a passing detekt task. |
| Device screenshots | PENDING | Physical light/dark, 200% font, IME, landscape, portrait, compact widths. |
| Native Android 17 SAF | PENDING | Needs user test with actual folder provider and cancellation. |

## Reproduction checklist (Pixel 9)

1. Update over stable APK, no uninstall.
2. Appearance Light/Dark/System: selected state, font slider 9 to 24sp,
   Reset 13.5sp; verify text behind Settings changes without restarts.
3. Workspace sheet: tap target, bold centered project name, project menu
   rename/delete, cancellation preserves data, tabs remain alive.
4. New project with keyboard open, rotate to landscape mid-edit; no clipped Save.
5. Settings: confirm same six sections in debug/release; single Diagnostics entry.
6. Terminal: verify CLEAR, FIND, extra key long presses, pastes, tab sessions.
7. Palette: search termux-setup-storage, insert only; press Enter, Android
   folder picker opens, Cancel returns failed status; grant returns success.
8. Settings Files: connected state after grant; import only from authorized URI.
9. New shell: termux-info runs and only identifies Klyro paths.
10. Font scaling 1.5x and 2.0x, TalkBack, IME focus, landscape and screen rotation.
11. Capture light and dark screenshots on real device and compare to token
    intent before claiming full pass.

## Rollback

Parent validated baseline: 4b847fb487d29d2638be0a9de5e7aeebdee05b76
(Klyro 0.4.6-dev). Work is isolated to feature/klyro-storage-ux-20261107.
Do not merge or replace main without explicit approval. To rollback on Android,
build the stable source with a higher versionCode rather than uninstall and
lose private files or persistent permissions.
