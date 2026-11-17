# Settings controls — implementation handoff

Base combines published Plugins actions `3eb72ee` and Home Recheck `ba4ff14`.
The merge conflict in the Home source contract retained all assertions from both
sides. No main merge. Existing plugin toggle accessibility fixes preserved.

## Implemented choices

1. Keyboard modifier statuses use a wrapping FlowRow instead of four equal-width
   cells that squeeze text. Read-only cards keep native text scaling.
2. Each modifier displays Active/Inactive, so state no longer depends on accent
   color. One semantic label/state per card; no false button or toggle role.
3. Launcher icon retry uses an outlined secondary button, preserving its callback.
4. Folder access retry uses the same secondary-control style and existing callback.
5. Diagnostics actions say Copy report and Share report; native selection supports
   copying an individual part of the report without copying everything.
6. Storage help names the actual Import a file action and explains Android's
   Downloads limitation without internal SAF implementation vocabulary.

The release-modifiers action already used the correct outlined control and was
retained. Existing terminal keyboard, session persistence and storage callbacks
remain unchanged. Large-font Diagnostics actions still use the adaptive grid.

## Validation for the designated testing agent

- Keyboard at 320dp / 200% text: all four statuses readable, wrap without overlap.
- Active/inactive modifiers distinguishable in grayscale; TalkBack reads each once.
- Release active modifiers resets actual state; no-op stays disabled.
- Launcher/folder read errors: outlined Retry updates state, no duplicate mutation.
- Diagnostic report long-press selects text, scroll remains usable, copy-all and
  share still receive the original report. Verify selection handles in the sheet.
- Confirm Copy report / Share report at large text in both themes.
- Recheck Home remains the other agent's outlined button; prior Plugin changes
  remain present after branch integration.

Executed here: git diff --check and static review only. No local build, test
execution, device capture, contrast measurement or full UX validation claimed.
Existing copy-label source assertions updated intentionally; execution delegated.

## Reference researched after implementation

https://www.carbondesignsystem.com/building-blocks/core/patterns/status-indicators

Useful principle: communicate state with descriptive labels and restrained visual
emphasis. Adapted to modifier status, retaining Klyro typography/shapes/colors.
No dependency added and no claim that this is universally the best design.
