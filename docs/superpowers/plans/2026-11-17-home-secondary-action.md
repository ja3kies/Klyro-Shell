# Home runtime action refinement

## Goal
Keep Launch Bash as the single primary action while making the secondary runtime recheck visibly actionable and consistent with Klyro's component tokens.

## Findings
- The Home capture makes the primary Bash action clear.
- The ready-state Recheck uses a text-style button beside status text, so its affordance is weaker than the action's utility.
- The existing implementation already has a 48dp minimum target, accessible name, busy label, and narrow-width fallback.

## Change
- Use a compact Material 3 OutlinedButton for Recheck, with a Klyro control radius, restrained border, and unchanged label/state semantics.
- Keep the existing button width/row adaptation; do not add a second filled CTA or a decorative card.

## Verification
- 624 Python regressions pass locally.
- Android lint, APK compilation, and device screenshot/TalkBack review must be checked on the resulting branch. No composition rating before reviewing the built screen.

## References
- Material Components overview: https://developer.android.com/design/ui/mobile/guides/components/material-overview
- Dribbble dark mobile dashboard reference: https://dribbble.com/shots/26802318-Modern-Dark-App-UI-Dashboard-Discover-Profile-Screens
