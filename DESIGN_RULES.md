# Klyro Shell — Android design contract

Version: 2026-11-07. This repository is in EXTEND mode: preserve the existing
Klyro design system and adapt its patterns instead of introducing a second UI.

## Identity and anchors

- Brand anchor: Klyro Soft Mint / Dark private terminal. The active terminal is
  the product, not a decorative preview.
- Token ownership: ui/KlyroDesign.kt (KlyroColors and KlyroUiMetrics).
  Do not place literal color values inside new composables.
- Type: Material 3 typographic hierarchy for chrome; terminal uses the existing
  JetBrains Mono / System Mono selection. Distinguish headline, group label,
  control label and secondary information.
- Icon family: existing Hugeicons drawable set for navigation/action glyphs.
  Exception to third-party ui-android skill's Material Symbols default: Klyro
  has an already-established cohesive Hugeicons family and should not switch
  families without a reviewed migration. Text-only glyphs (plus, arrows, >_)
  are restricted to clear terminal meanings and accessible labeled controls.
- Accent: user-selected Lime/Cyan/Coral/Orchid/Custom. Accent highlights focus,
  selection and primary actions, not every background. Light and dark palettes
  must preserve contrast via KlyroColors.

## Layout

- Spacing and interaction dimensions come from KlyroUiMetrics: 4/8/12/16/24
  rhythm, 48dp minimum tap targets, 52dp compact rows, 64dp workspace rows.
- Sheet content max width 540dp; main sheet content max height 620dp; palette
  list max 420dp; scroll internally instead of occupying more than the viewport.
- Keyboard-open sheets use imePadding, and edge-to-edge is handled by the
  root app and Material 3 sheets. No extra hardcoded top status bar padding.
- Do not force large full-width CTAs for selection operations. Use semantic
  segmented controls, accent swatches, slider, list rows and contextual menus.
- Dialogs are reserved for destructive confirmations, short rename inputs or
  explicit safety gates. Avoid an editor dialog layered on a bottom sheet.
- Never render a fake terminal inside Settings; the real terminal is visible.
  A color picker may display a real, necessary color preview.

## Component ownership and navigation

- Extra keys: terminal control / FIND / CLEAR / selection / paste.
- Tabs: session selection, rename, close with confirmation.
- Workspaces sheet: organize project membership, compact list and contextual menu.
- Command Palette: discover shell commands; insert text without Enter.
- Settings sheet: appearance, keyboard, Android files, privacy, diagnostics, about.
- All platform-specific termux-* equivalents must be honest about permissions,
  native availability and whether Android imposes capability limits.

## Accessibility and states

- Minimum interactive target 48dp even if a glyph is visually 20/24dp.
- Icon-only controls use clear TalkBack action descriptions; decorative icons
  have null description under a labeled parent.
- State selected/disabled/error must be expressed by semantics and text, never
  color alone. Use sentence-case labels, concise purpose-specific microcopy.
- Async operations need empty, loading, permission-denied, cancelled, success and
  failure states. Never declare a command successful based on a file's existence.
- Back dismisses sheets safely; never execute text or close a PTY when dismissing.

## Verification and limits

Reference:
- https://github.com/rshzrh/ui-design-skills/tree/main/skills/ui-android
- https://github.com/AjnasNB/mobile-app-ux-auditor-skill

Automated checks: Python UI architecture/design tests, JVM tests, lintDebug,
Gradle ARM64 assemble, native 16K alignment and signature audit.

The upstream ui-android skill requires Material Symbols, all M3 theme roles,
detekt, dark/light screenshot verification and zero literal dimensions. Klyro
retains documented brand exceptions and older Compose code: do NOT report
full compliance until each is independently audited and verified on-device.
Instrumentation tests are compiled by CI but not executed there.
