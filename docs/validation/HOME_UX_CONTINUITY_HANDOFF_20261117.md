# Home UX continuity — validation handoff

User split on 2026-11-17: this agent implements technical/UX corrections; another agent owns Android tests, captures and acceptance. Do not block implementation on unavailable device access. Do not label implementation as validated UX. No main merge authorized.

Base: 287d86fd61c026798d43f217eec4d680c96530bc (latest fetched appearance-refined branch).
Implementation: 335752a52ee02baf4efc4738dd1d5cb282b795d9.

## Changes and acceptance

| Change | Root cause | Device acceptance |
| --- | --- | --- |
| Retain Home scroll state outside route conditional | Removing Home from composition discarded its local scroll state | Scroll Home, open Plugins, Android Back: preserve offset. Repeat via Terminal and with rotation; tolerate clamping when viewport/content size changes. |
| Wrap technical-detail actions with native FlowRow | Fixed Row shared finite width between expanding detail label and Copy | At 320dp and 200% fonts, expand/collapse and Copy are visible and clickable without overlap; targets at least 48dp high. |
| Explicit ellipsis on warning preview | Three-line cap silently clipped text | Long warning shows ellipsis; Show technical details exposes full original report; copying preserves complete content. |
| Describe Copy action for accessibility | Generic label lacks context when traversed independently | TalkBack announces Copy technical details and invokes the existing callback once. |

## Validation status

Only git diff --check executed locally, without errors. No local compilation, unit/instrumented test execution or new Android captures claimed. Existing CI triggers on integration/** pushes; inspect its conclusion at the tested SHA. Android execution belongs to the validation agent.

## Review and remaining work

Source review: preserves existing callbacks, session state, keyboard, theme tokens and existing font scaling; uses Compose-native scrolling/FlowRow without new dependencies. Four substantive corrections; no artificial quota changes.

Still pending: runtime panel Recheck layout at large fonts; animation while Settings overlays Home; full Home matrix from HOME_INTENSIVE_20261117.md on ux/home-intensive-20261117. Run baseline comparison before claiming fixes for those risks. Validate light/dark, landscape, returning from background, and the completed session path. No final UX score or completion claim.

Next implementation screen: Plugins after remaining Home implementation findings are resolved. Acceptance is tracked separately by the testing agent.
