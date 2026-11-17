# Plugins empty-state action

## Finding

The Installed tab's empty state explains the next step, but exposes “Explore plugins” as a low-emphasis text button. This is the sole forward action when no packs are installed, so its visual weight undersells its purpose.

## Component decision

Use a compact filled Material button with the existing Klyro accent, control radius, and minimum touch target. Keep it content-width so it reads as the next step without turning the empty-state card into a second full-width banner. A trailing decorative chevron reinforces navigation; the accessible name remains the action text.

## References

- Android's Compose button guidance: outlined buttons suit secondary actions, while filled buttons carry primary actions: https://developer.android.com/develop/ui/compose/quick-guides/content/create-button
- Dribbble's mobile empty-state references: https://dribbble.com/shots/26810621-Mobile-Empty-State-Screens

## Acceptance

- The Installed (0) empty state presents one obvious, accessible route to Explore.
- The button uses Klyro accent contrast, shape, and touch-size tokens.
- Explore / Installed switching, list scroll positions, installation confirmation, and terminal keyboard behavior stay unchanged.
- Automated source tests and CI pass. Pixel screenshot and TalkBack verification remain separate from CI.
