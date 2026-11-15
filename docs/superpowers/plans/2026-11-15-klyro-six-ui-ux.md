# Klyro: Six UI/UX corrections

> **For agentic workers:** use superpowers:executing-plans, systematic-debugging, test-driven-development and verification-before-completion. Ponytail: reuse native Compose controls, no new dependency.

**Goal:** Correct three UI and three UX defects across Workspaces, Command Palette, session management, and the terminal tab strip.

**Architecture:** Minimal changes in the existing Kotlin/Compose screens. Preserve the session manager, runtime, installed files, command safety, existing settings and modal lifecycle. No changes to main, no merge.

**Base:** `eb6f02f2a2eac402e49f8a3eaa9b3922fe75c468` (validated).

**Verification:** New source regressions (RED on base, GREEN after), existing source/JVM/PTy/JNI suites, lint, debug/release/test APK builds, signatures, native 16-KiB audit. Manual Pixel screenshots, Gboard and TalkBack require separate device validation.

## UI 1 — Workspaces shell reachability
**Cause:** scrollable content wraps the title/New action, which vanishes on long project/session lists. A heuristic guesses whether scrolling is needed.

**Change:** fixed Workspaces heading + New action; measured-overflow scrollable body, capped height, safe IME and gesture-nav insets. The selected workspace, all sessions, current-session move actions and confirmation menus must remain reachable.

## UI 2 — Command Palette filter discoverability
**Cause:** two minimally highlighted TextButtons render filters as ordinary actions rather than selectable states and compete for width on small screens.

**Change:** native Material 3 FilterChip selection state; two equal flexible targets, compact ellipsized labels, active/inactive theme tokens and TalkBack state. Pinned vs Available must filter the same list; plugin dependencies remain truthfully unverified.

## UI 3 — Session sheet action hierarchy
**Cause:** session name and actions scroll together, concealing identity and dismissal in long/small layouts.

**Change:** fixed session title and explicit Done, independent measured scroll area for status/actions; safe insets, 48dp controls. Dangerous Close terminal remains a separate confirmation, not the Done action.

## UX 1 — Workspace keyboard flow
**Cause:** workspace edit requires manual scrolling and touching Save, with no IME Done action or initial focus.

**Change:** request editor focus when opened and keep keyboard-friendly viewport. IME Done and Save use the exact same guarded submit logic; empty, unchanged, deleted target and duplicate names remain rejected without losing the draft. Cancel hides keyboard.

## UX 2 — Session rename keyboard flow
**Cause:** default Enter action isn't linked to explicit save; newline normalization may leave cursor bounds stale.

**Change:** IME Done reuses guarded Save (never autosaves without explicit user action); selection clamped after sanitized input. Unchanged/blank/overlong names don't submit; Cancel remains explicit, rotation keeps TextFieldValue.

## UX 3 — Active tab visibility
**Cause:** `maxValue * selectedIndex / (count - 1)` guesses active-pill position. Different title lengths and selected activity tags make it inaccurate.

**Change:** use native Compose BringIntoViewRequester on the selected pill after layout. Keep the stationary + CTA, state semantics, long-press actions, 8-tab limit and no terminal focus steals.

## Plan
- [ ] Add six regression tests and observe failure on base.
- [ ] UI 1, UI 2, UI 3: apply isolated minimal edits and correct obsolete structural contracts.
- [ ] UX 1, UX 2, UX 3: apply and verify keyboard/scroll state branches.
- [ ] Review whole diff for modal lifecycle, focus, disabled states and accessibility.
- [ ] Pass full CI on exact final SHA. No real-device claim without Pixel proof.

## Review focus
- Long and non-ASCII workspace/session names, large font scale, short landscape height.
- More than six projects and eight tabs, selected pill whose widths differ.
- Duplicate/blank names and deleted rename target, repeated keyboard Done taps.
- IME covering Save and action buttons; nested warning/close confirmations.
- Screen-reader selected state; no shell command autoexecution.
