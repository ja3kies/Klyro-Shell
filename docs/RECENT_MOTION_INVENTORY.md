# Klyro — Recent motion inventory

Audit of the current integration branch against [PR #52](https://github.com/ja3kies/Klyro-Shell/pull/52), `feat/premium-motion-system`. Status reflects source inspection, not physical-device verification. Do not merge the older branch wholesale: it diverges substantially from the current integration branch.

| Surface | Motion | Current source / status |
|---|---|---|
| Home cover | Accent luminance pulse, diagonal translation, independent square rotation | `KlyroHomePixelCover.kt`: 20s color cycle, four 5s spins/translations; respects reduced motion and lifecycle |
| Home | Animated content size | `MainActivity.kt`: present |
| Home | Staggered branding, runtime card, Plugins and Settings entry | PR #52: missing from current integration |
| Home | Spring press scale on launch, Plugins and Settings | PR #52: missing from those current Home actions |
| Shared action rows | Non-bouncy spring press scale to 97.5% | `ui/KlyroUiComponents.kt`, `ui/motion/KlyroMotion.kt`: present |
| Appearance | Animated accent and Compose surface colors | `ui/KlyroAnimatedPalette.kt`: restored, 320ms; native terminal palette stays atomic |
| Appearance | Theme preview press scale | `ui/KlyroThemePreviewCard.kt`: present |
| Appearance | Animated individual preview label selection | PR #52: explicit label-color transition missing; global theme transition is separate |
| Appearance | Slider thumb spring enlargement during drag (113%) | PR #52 `ui/KlyroUiComponents.kt`: missing |
| Plugins | Tab press scale | `ui/KlyroPluginsPage.kt`: present on label |
| Plugins | Animated tab background/text color | PR #52: missing |
| Plugins | Staggered Installed/Explore card entrance | PR #52: missing |
| Workspaces | Animated workspace selection colors and press feedback | PR #52: missing |
| Workspaces | Session-edit oscillation | `ui/KlyroWorkspaceSheet.kt`: present |

## Restoration constraints

Port only the missing motion behavior to the newest branch. Preserve existing action semantics, touch targets, modal lifecycle and terminal input latency. Never make clickable items invisible during entrance animations; keep durations short, respect reduced-motion settings, and verify rendered motion on a device. A successful Android build does not prove visual motion correctness. No restoration of the missing PR #52 behaviors is implied by this inventory.

## Evidence

- [Current integration source](https://github.com/ja3kies/Klyro-Shell/tree/integration/senior-release-20261121)
- [Prior motion source](https://github.com/ja3kies/Klyro-Shell/tree/feat/premium-motion-system)
- [Rotation implementation](https://github.com/ja3kies/Klyro-Shell/commit/12f332893832d2f9ce7436ded15dadf6501421ea)
- [Rotation regression tests](https://github.com/ja3kies/Klyro-Shell/commit/f640da5fba3435d4b4a2d28be071431b456e4e533)
