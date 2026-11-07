# Klyro Android acceptance — Hugeicons / UI / storage

This is a manual acceptance checklist for the **APK built from the commit under test**.
Passing Gradle, lint, and static checks is necessary but **not** sufficient.
Do not label the delivery PASS until all required device checks are recorded.

## Preparation

- Install only the APK from the latest successful GitHub Actions run on the exact branch/commit.
- Prefer updating with `adb install -r <apk>` so application data remains intact.
- If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, STOP. Do **not** uninstall;
  that may destroy the current Klyro runtime and user files.
- Record APK SHA-256, Android version, Klyro version, and commit SHA.
- Use a throwaway workspace/session for destructive-action tests.

## Mandatory device checklist (Pixel 9 / Android 17 where available)

| ID | Steps | Expected result |
| --- | --- | --- |
| ICON-01 | Open Settings. Inspect Appearance, Keyboard, Storage, Privacy tiles. | Real consistent 24dp Hugeicons vectors (colors, keyboard, folder, shield), not Unicode glyphs, tinted with active Klyro accent. |
| ICON-02 | Open Settings > Files & storage. Inspect Import a file / Export a file. | Distinct Hugeicons download/upload vectors; chevrons use vector assets. |
| ICON-03 | Open Workspaces > menu of a test workspace. | Real vector three-dot overflow, pen for Rename, trash for Delete. Cancel delete; workspace remains intact. |
| ICON-04 | Open a terminal > overflow menu. | All four actions (Command Palette, Workspaces, Settings, Home) have meaningful aligned Hugeicons leading icons. |
| ICON-05 | Open session actions for a disposable session. | Rename, New terminal, Switch (when applicable), Close terminal and chevron use vectors. Cancelling Close preserves running shell. |
| ICON-06 | Change light/dark and each monochrome theme. | Icons remain readable and follow accent/foreground/danger tokens; none are invisible or rendered as emoji. |
| LIC-01 | Open Settings > About > Hugeicons licence. | The full MIT notice appears, scrolls and closes. |
| SLIDER-01 | Open Appearance > Font size, drag slider to 9, 13.5 and 24 sp. | Thin rounded track, white circular handle, accurate half-sp steps, immediate text preview and terminal resize. |
| SLIDER-02 | Close and reopen Settings after changing font size. | Chosen value persists and handle is correctly positioned; no oversized Material Expressive track. |
| THEME-01 | Change Contrast / OLED / Paper modes; run `printf '\\033[31mANSI test\\033[0m\\n'` in a disposable Klyro shell. | Terminal text/background retain contrast; ANSI sample is grayscale in monochrome themes. |
| SAF-01 | Authorize two distinct, permissible folders via Klyro setup-storage; restart the app; inspect Files & storage. | Both persisted SAF grants remain, and files can be imported to Klyro private storage. |
| SAF-02 | Attempt choosing root Download with Android picker. | Android may block the root. App must explain its SAF limitation and never claim it mounted `~/storage/downloads`. |
| ACCESS-01 | With TalkBack enabled, focus the tiles, menus, and slider. | Actions are announced by label, decorative icons are not read twice, interactive targets remain operable. |

## Cross-page visual and content consistency checks

| ID | Steps | Expected result |
| --- | --- | --- |
| VISUAL-01 | Launch home with a fresh or already verified environment. | No duplicate private-environment explanation. Runtime Status uses theme semantic error/warning colors and common spacing. |
| VISUAL-02 | Open the terminal overflow and a workspace action overflow. | Both menus use identical sheet tokens, item heights, Hugeicons alignment, semantic colors and corners. |
| VISUAL-03 | Open Settings, Command Palette, Workspaces, Sessions and Choose shell. | Bottom-sheet corners, drag handles, backgrounds and gutters match; back/dismiss controls work. |
| VISUAL-04 | Open Settings > About > both licences. | Each licence uses the same modal appearance and scrollable content. Both close normally. |
| VISUAL-05 | Open Workspaces with empty and populated projects. | The session count appears once. Current sessions have a status label; inactive sessions show a navigation chevron. |
| VISUAL-06 | Open Command Palette and inspect an unavailable command. | A short purpose appears in its row; detailed requirements are shown only when tapped. Inserting any available command never auto-executes it. |
| VISUAL-07 | In Appearance, open custom accent > Opacity & saved colors, save a throwaway color. | Exactly one Edit action and one Remove action per color; Edit returns to the picker and Remove requires confirmation. |
| VISUAL-08 | Open Files & storage and Privacy; review visible copy. | No duplicate storage grant explanations. Download-root SAF limitation remains explicit; privacy warnings remain actionable. |
| VISUAL-09 | Set Paper, Contrast, OLED and standard themes, then repeat menu and dialog checks. | Foreground, secondary, danger, backgrounds and icons use consistent accessible semantic palette roles. |
| REG-01 | Rename a test session and workspace, switch tabs, open and dismiss all dialogs. | Existing session state and names persist; no unexpected process termination. |
| REG-02 | Import a file from Android picker and export a harmless test file. | Both workflows remain functional after UI refactor. |

## Results log

Record each item as `PASS`, `FAIL`, or `NOT TESTED` with screenshot/log evidence.
`NOT TESTED` blocks the overall release PASS. Preserve the last stable APK for rollback.

### Known limitations

- SAF does not provide Termux-equivalent POSIX access to `~/storage/downloads`.
- CI validates vector resource references, code wiring, unit tests, lint, and APK
  packaging; it cannot prove appearance, TalkBack behavior, or Android picker
  behavior on a real device.
