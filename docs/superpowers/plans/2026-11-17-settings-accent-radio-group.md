# Settings accent radio group

## Finding

Appearance already exposes each accent swatch as a labeled, selected radio option, but the swatches were not grouped as one mutually exclusive choice set. The custom-accent action is a separate command and must stay outside that radio group.

## Component decision

Keep the existing circular swatches and custom-accent action. Group only the preset radios with Compose's `selectableGroup` semantics; this fixes TalkBack grouping without changing the visual system or making the custom picker sound like a radio option.

## References

- Android Compose radio-button guidance: https://developer.android.com/develop/ui/compose/components/radio-button
- Dribbble Appearance settings reference: https://dribbble.com/shots/24366677-Appearance-settings-Untitled-UI

## Simulated design review

- Rams: one coherent choice set, no added decoration.
- Ive: preserves the existing restrained color-swatch design.
- Kare: leaves the distinctive Klyro accent colors intact.
- Wroblewski: makes the customization choice easier to scan with assistive technology.
- Norman: exposes the mutual-selection relationship while keeping “Custom” a separate action.

## Acceptance

- Presets remain individually labeled radio options with selected state.
- Custom accent remains an independent button.
- No visible layout or behavior change; Klyro accent, light/dark modes, and terminal keyboard stay unchanged.
- Regression and Android CI pass. Visual score requires a current device capture.
