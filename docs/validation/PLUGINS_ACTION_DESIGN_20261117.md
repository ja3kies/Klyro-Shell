# Plugins — action component choices

Base: e58d3b700486d8ad7ca1c1397c9239f0acd968ce, including the other agent's
accessibility corrections. Branch: integration/plugins-action-design-20261117.
No merge into main. Android validation remains assigned to the user's testing
agent. This is an implementation report, not UX acceptance.

## Five implemented decisions

| Intent | Component and visual design | Acceptance |
| --- | --- | --- |
| Discover packs when nothing is installed | Full-width accent button in existing quiet empty-state card; clear heading and navigation chevron | Opens Explore; 48dp minimum; text wraps at 200%; one button in TalkBack |
| Report an already installed pack | Neutral noninteractive status pill with checkmark and readable text | No click action or disabled-button semantics; no duplicate installation |
| Recover failed registry loading | Outlined secondary Retry button | Existing retry callback, loading guard and error message retained |
| Select an import file | Outlined file-picker button labelled Choose JSON file, spaced icon and explicit review step | Picker cancellation changes no registry; JSON still bounded to 16KiB; confirmation still required |
| Communicate a confirmed pending operation | Adding / Installing / Removing labels inside respective confirmation buttons; polite live region | Label follows saved operation ID; no duplicate operation; success closes dialog; failure restores original action |

Design goal: the strongest accent belongs to the next useful action, while
completed states are readable without pretending to be buttons. Existing Klyro
shapes, colors, layout adaptation and native Compose controls are reused. No new
dependency, terminal behavior, shortcut or session change.

## Reference research after implementation

- shadcn/ui Empty: https://ui.shadcn.com/docs/components/base/empty
  Useful structure: title, short explanation, explicit next-action button.
  Adapt the hierarchy in Compose, not React code or web-specific styling.
- Carbon Tag: https://www.carbondesignsystem.com/building-blocks/core/components/tag/guidelines
  Useful distinction: read-only status has no interaction; interactive variants
  have explicit affordances. Adopt this distinction for Installed.

These are design references, not proof that a particular appearance is universally
best or that users prefer it. Actual Android captures are still needed.

## Verification and handoff

Independent static reviewer found a duplicate import, now removed; final review
reported no blocking defect and preserved callbacks. git diff --check is clean.
No tests, APK compilation, Android interaction or screenshots executed here.
Three existing test files updated to match intentional changes: Installed asserts
absence of click action; picker copy names the actual next step; source contract
recognizes the status branch. Validation agent must execute them, retain the
unavailable-registry disabled-button assertion, and cover all table criteria.

For future lots: choose component semantics before styling; compare usefulness,
visual hierarchy, light/dark appearance, large text and touch access; conclude
with a specific researched component reference and its concrete Klyro use.
