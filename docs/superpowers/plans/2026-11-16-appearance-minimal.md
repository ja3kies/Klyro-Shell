# Minimal Appearance page

User direction: make Appearance more minimal and clean, with an iOS-inspired
feeling while keeping Klyro's Android controls, palette and behavior.

## Layout

Three quiet groups replace the field-by-field card treatment: App theme, Text,
Launcher icon. Group headings are subdued; spacing, surfaces and rounded corners
reuse Klyro tokens. Theme thumbnails have transparent outer surfaces. Font and
size selectors use neutral selected pills instead of repeated accent borders.
The current value and accessibility selection state remain visible.

Normal System/Light/Dark choices remain immediately visible. Monochrome is an
inline disclosure, initially expanded when a monochrome theme is active. Its
collapsed summary names the active mode; all three existing monochrome choices
and their callbacks remain available. The custom accent stays hidden in mono
mode, matching existing behavior.

Launcher choices are smaller (48dp artwork, 80dp minimum container) and use only
one selected outline. Error, progress, repair and retry behavior are preserved;
the launcher refresh hint is shorter. Font slider, preset values, persistence,
Back/Close behavior and large-text escape remain unchanged.

## Verification

Run the full Python regressions and exact-commit Android build/lint/package/sign
checks. The new Android UI test expands monochrome options, selects OLED, folds
them again and checks the current summary and accent visibility. Existing tests
cover presets, large text and navigation. A narrow-layout test checks that all
five accent controls retain 48dp targets after grouping; the accent flow wraps
instead of squeezing them. Tests must execute on Android before
claiming device acceptance.

## Visual review gate

Simulated Rams/Ive/Kare/Wroblewski/Norman lenses: less competing decoration,
coherent spacing, legible labels, easy primary choices and truthful selection
feedback. No current Appearance render is available in this session. Inspect
new Pixel captures in light/dark, mono, portrait/landscape and 200% text before
assigning a composition score. No visual acceptance score is claimed here.

Work on an isolated branch above PR #36; do not merge main. Keep the terminal
keyboard and all other Settings sections intact.
