# Klyro Shell — adversarial UI/UX review, November 4, 2026

**Scope:** integration branch `feat/klyro-0110`. Captured before consolidating design tokens. Pixel 9 portrait screenshots supplied in chat; DSH-Go screenshots and `KLYRO_SHELL_VISION_INTERACTIVE.html` are visual/functional references, not code dependencies.

## Reframed product requirement

> Audit the Android terminal's visual hierarchy, component consistency, accessibility and edge-to-edge system bars; unify every screen around a reusable Klyro-specific design system; shrink session pills while preserving touch targets, align and extend the rounded terminal viewport, harmonize FIND/rename/exit dialogs, and add persistent contrast-aware Mint, Teal, Cyan and Violet presets without losing terminal sessions. Verify source-level invariants and CI before device acceptance.

## Findings before corrections

| Priority | Adversarial finding | Source evidence | Fix / gate |
| --- | --- | --- | --- |
| P0 | Light status and gesture navigation regions did not match white session header/shortcut surface | User screenshot: distinct mint strips above/below white Klyro chrome | Use the same `chrome` color behind transparent bars and header/key rows; enforce correct system-icon polarity. Verify on Pixel 9/Gboard |
| P1 | Oversized active/inactive pills and add control used more vertical space than DSH-Go reference | User side-by-side screenshot: session labels/pills substantially larger in Klyro | Use 36dp visual pills inside 48dp semantic hit targets, and a 38dp visible add button |
| P1 | Terminal rectangle background and AndroidView had different horizontal extents/corners | Klyro screenshot: apparent dark corner wedges and strips | Clip the *actual view container* once at 20dp; eliminate AndroidView's additional 12dp horizontal inset |
| P1 | FIND kept a default Material dialog while Rename was a customized white/mint dialog | Both Compose source files, independently styled `AlertDialog` | Shared `KlyroModalTheme` for FIND/Rename/Copy/Paste/Exit; same 28dp modal corners and contrast-aware colors |
| P1 | Settings/session sheets ignored dark/system theme and user-selected accents | Hardcoded `MintTheme` and `SheetScheme` in sheet files | Shared color tokens, persisted theme and accent, contrast-checked states |
| P2 | UI mixed unicode glyph symbols and Hugeicons without a coherent icon family | Settings Bento symbols and menu text compared to keyboard Hugeicons | Use FOSS MIT Hugeicons vectors for navigation/actions; retain “Aa” for typography sample |
| P2 | Overflow popup was cramped despite reduced item count | 3-item `DropdownMenu` with no minimum width or leading icons | Width 246dp, 56dp rows, consistent iconography |

## Inspiration without code cloning

- [Termux TerminalView](https://github.com/termux/termux-app/blob/master/terminal-view/src/main/java/com/termux/view/TerminalView.java) and [Termux extra keys](https://github.com/termux/termux-app/blob/master/app/src/main/java/com/termux/app/terminal/io/TermuxTerminalExtraKeys.java) inform native keyboard expectations; Klyro retains its independent Compose UI and shell runtime.
- [Android system bars documentation](https://developer.android.com/develop/ui/compose/system/system-bars) explains transparent gesture regions, icon contrast and 3-button navigation scrims.
- [Hugeicons free icon library](https://github.com/hugeicons/hugeicons) is MIT licensed; source attribution accompanies each Android vector. Never copy paid icon assets.

## Automated acceptance

- JVM contrast checks: for each of four presets × light/dark, check 4.5:1 for foreground vs chrome/key and selected-on-accent label.
- CI: Gradle JVM unit tests, Android debug APK, compiled instrumentation APK, APK signature, and strict 16KiB ELF PT_LOAD alignment for all packaged native libraries.
- Source inspection: 50dp extra keys, 48dp session hit targets, one rounded/clipped terminal container, centralized tokens, theme persistence, dialog style consistency, no forced shell restart on color change.

## Device acceptance (not implied by green CI)

Pixel 9 Android 17: light/dark/system, four accents, status/nav/gesture background continuity, keyboard open/closed/landscape, TalkBack traversal of tabs and shortcut keys, FIND/rename focus + IME, no hidden second-row keys, correct shell command output, session switch and termination, 16KiB warning absent, no PTY freeze or scrollback regression. Record screenshots and logcat if any step fails. System IME/navigation colors may be owned by Gboard while the keyboard is visible.

## Deliberately deferred UX choice requiring user approval

Replace modal FIND with an *inline find bar* pinned to the terminal (like a browser's Find in Page) to preserve output visibility and facilitate next/previous navigation. Potential benefit: less modal obstruction; trade-off: reserved terminal height, focus/IME complexity. **Do not implement this alternative until user approves it.**

## Rollback

One feature PR into `feat/klyro-0110`; rollback by reverting its squash commit on integration. `main` and `fix/proot-5.1.107.102` remain unchanged.
