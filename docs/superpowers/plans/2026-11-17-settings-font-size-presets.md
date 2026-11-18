# Appearance font-size presets

## Finding

Three quick font-size choices are shown as separate large tiles beneath a slider. They are mutually exclusive presets, so the repeated tile treatment takes vertical space and weakens their relationship to each other.

## Component decision

Use a Material 3 single-choice segmented control for the three presets on regular-width screens. Keep the slider for intermediate values. At narrow widths or font scales of 1.3 and above, retain full-width radio rows so labels and touch targets remain usable.

The selected size stays visible beside the reset action. Selecting a preset continues through the existing callback and persistence path.

## References

- Android recommends a single-choice segmented button for a short set of mutually exclusive options: https://developer.android.com/develop/ui/compose/components/segmented-button
- Dribbble reference for a focused, customizable reading appearance screen: https://dribbble.com/shots/25591971-Mobile-Book-Customizable-Reading-Experience

## Simulated review

- Rams: removes three repeated containers and keeps only the controls needed.
- Ive: preserves a quiet, compact control with existing Klyro colors.
- Kare: keeps the accent palette and active state recognizable.
- Wroblewski: leaves a full-width fallback for large text and narrow layouts.
- Norman: keeps quick presets distinct from the continuous slider and announces selection.

## Acceptance

- Presets remain mutually exclusive and use the existing font-size update callback.
- Intermediate slider values remain available.
- Large-text settings remain stacked with accessible touch targets.
- Android CI verifies compilation and Compose UI regression coverage. A current Pixel capture is still needed for visual scoring.
