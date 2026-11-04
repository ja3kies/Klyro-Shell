# Klyro Shell 0.3.2 — Tonal theme and typeface audit

## Root causes resolved
- `KlyroColors.create()` changed accent only; hardcoded mint backgrounds, key, sheets, terminal and chrome were left unchanged. All surface roles are now lightly tinted by the selected accent, in both light and dark modes.
- The native Termux `TerminalEmulator` was still using the original Neon Obsidian default palette. Merely painting the Compose background cannot change its ANSI foreground, background and cursor. `KlyroTerminalPalette.apply()` synchronizes renderer defaults and updates existing sessions' default color slots while attempting to preserve OSC-customized slots.
- The terminal canvas was always near black even when the app was in light mode. The terminal now uses a light theme-toned background in light mode, plus dark ANSI values; dark mode retains a dark low-glare terminal. `TerminalView.setBackgroundColor()` uses the same Compose token to remove mismatched corners/edges.
- The session plus circle is visually only 38dp and translucent (14% accent alpha plus fine outline), but the accessible clickable surface remains 48dp.
- DSH Go source indicates **JetBrains Mono** (SIL OFL 1.1) with Android monospace fallback; it does not expose a list of preset terminal typefaces. Klyro now offers a choice of System Mono and JetBrains Mono without copying DSH code. Both persist independently of font size. The signed CI build downloads a pinned immutable font file and verifies Git blob SHA and size. OFL licence is available from About.
- Pinch zoom receives the actual configured size baseline instead of a guessed starting pixel size. No shell restart required for theme or font changes.

## Sources and constraints
- DSH Go bundled typeface: https://github.com/ccch1mneyyy/dsh-Go/blob/20447ce63b9ce38ab1757d14137467f7d268224a/app/lib/ui/theme/typography.dart
- Font and SIL OFL: https://github.com/ccch1mneyyy/dsh-Go/tree/20447ce63b9ce38ab1757d14137467f7d268224a/app/assets/fonts
- Termux TerminalView `setTypeface()`, `setTextSize()` and emulator indexed color table are used, not reimplemented.
- Device test still required for third-party programs emitting explicit RGB ANSI colors, existing sessions' custom OSC colors, and auto-follow after IME, plus session roundtrip and typeface persistence.
- `main`, isolated Termux storage, bootstrap, shell process lifecycle and permission model are unchanged. Rollback: revert this commit on draft PR #18.

## Release gates
Tests: every accent must change every surface role; WCAG readability of foreground/muted on key/chrome/tile and terminal; font selection normalization. CI compiles app and terminal-emulator JVM tests, lint, signed ARM64 APK and strict 16KiB check; no claim of on-device validation until Pixel 9 testing.

## Follow-up root cause: truecolor prompt
The existing generated `~/.bashrc` used `ESC[38;2;0;240;255m` (always neon cyan), even in light mode. Merely recoloring the terminal's default indexed slots could not change this truecolor output. New and Klyro-generated legacy profiles now use ANSI 36, whose palette index 6 follows the selected accent. The migration touches **only** the exact legacy Klyro/Oryn PS1 line, backs up the original private `~/.bashrc` to `.bashrc.klyro-before-color.bak` before the first change, then atomically replaces the file. User-customized prompt lines are left intact. Already-running Bash prompts will take effect on the next shell start; they are never silently injected into live processes.
