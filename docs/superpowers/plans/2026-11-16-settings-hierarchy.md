# Settings hierarchy and state refinement

Continue from `e01be6130fe815b95593725322947de9f9b2c824` (PR #35),
on an isolated branch with no main merge. Evidence for this step is source and
test inspection; a current Settings capture and device execution are pending.

## Design decisions

- Keep six root sections and the established bento identity. Use one native
  responsive choice layout for root tiles, themes, typefaces, launcher icons,
  font presets and diagnostic actions; narrow windows and large text stack.
- Place theme, accent and terminal text controls before the less frequent icon
  choice. Let labels wrap instead of clipping. Keep Back and Close reachable.
- Add 12 / 13.5 / 16 sp shortcuts beneath the existing font slider; preserve the
  same callback, range, default and immediate persistence.
- Reserve strong accent treatment for the main action and selection. Copy and
  Share use neutral outlines with consistent icon spacing.
- Expose sheet/section headings to accessibility navigation; omit decorative
  preview glyphs from speech. Keep meaningful labels and selection semantics.
- Keep Settings category tiles as large bento navigation buttons, but let their
  merged button semantics announce both the destination and its short context.
  Avoid replacing those visible labels with a title-only content description.
- Show folder checking, failed lookup, empty and connected states separately.
  Failed permission reads offer a retry instead of being reported as zero folders.
- A launcher display fallback is not a confirmed selection. Confirm the OS state
  only when Appearance is opened; unknown states show no selected icon and allow
  a check retry or explicit repair choice. Distinguish failed changes with a known
  current selection from failures to determine the current selection.
- Put Diagnostics first in its page and Freeze reports after the report/actions.

## Principles and primary references

- Rams: understandable controls and fewer competing elements. See
  [Vitsœ — good design](https://www.vitsoe.com/us/about/good-design).
- State feedback, recovery and exits: see
  [Nielsen's usability heuristics, NN/g](https://www.nngroup.com/articles/ten-usability-heuristics/).
- Native responsive layout: see
  [Android Compose flow layouts](https://developer.android.com/develop/ui/compose/layouts/flow).
- Heading, state and decorative-content semantics: see
  [Android Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).

## Simulated review lenses

These are review criteria, not quotes, endorsements or a visual acceptance result.

| Lens | Required result |
| --- | --- |
| Rams | Six understandable sections, relevant controls first, no duplicate progress announcement |
| Ive | Shared spacing, shapes, neutral secondary actions and natural text wrapping |
| Kare | Legible labels and useful glyphs without decorative speech noise |
| Wroblewski | Reachable controls on narrow layouts and at 200% text; quick font choices |
| Norman | Honest selected/loading/error states, clear retries and reliable exits |

The 8.5/10 composition target requires inspection of the modified Android render.
No score is assigned here.

## Verification and delivery gate

- Source guards were run red before implementation, then updated only where the
  intended layout/state contract changed.
- Android UI tests cover stacked theme choices at 200%, decorative semantics,
  distinct folder states, font preset callback/selection, Back and Close.
- Existing modal dismissal, IME, bottom inset, launcher rollback, palette and
  terminal regressions remain part of the full suite.
- Compile/lint/package/sign the exact commit in CI. Download the signed candidate
  and verify its published archive digest before delivery.
- Device tests and screenshots remain required: six-section root, Appearance,
  folder loading/error, Diagnostics, light/dark, landscape and 200% text. Test
  compilation and source checks do not constitute device execution.
- Keep Klyro's terminal keyboard and PTYs intact. Terminal visual work follows
  the Settings review; no replacement shortcut bar is introduced.
