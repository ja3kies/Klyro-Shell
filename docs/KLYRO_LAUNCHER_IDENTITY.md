# Klyro Launcher Identity — 2026-11-05

This is a purpose-built scalable Android adaptation of the user-supplied
Klyro branding sheet. The glossy 3D artwork was **not** available as original
editable paths. These assets therefore reproduce the characteristic three-part
K silhouette and lime-to-olive gradients rather than claiming an exact trace.

- `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` are adaptive
  launcher aliases (API minSdk 26); OEM masks own circle/squircle shape.
- Foreground/background authored as independent 108×108dp VectorDrawable layers.
  Glyph designed inside the safe x=21..87/y=21..87 region: actual bounds
  approximately x=23..86 and y=22..85; control points may extend
  marginally but the Bézier outline stays within the safe area.
  No background-baked rounded square.
- Native separate solid monochrome K, used by Android themed icons and the
  app's welcome header. No bitmap scaling artifacts, text, icon shadows or
  tiny unresolvable details.
- Foreground is intentionally two-tone gradient; monochrome is a simple solid
  silhouette. Verify recognizability at 24/48dp, on circular and squircle masks,
  in light/dark and Android themed-icon mode.
- All source paths remain in the repo for further designer iteration.
- `main` and integration branch untouched. Rollback is a commit revert.
- Reference: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive

- Supplied brand-sheet comparison prompted a second vector pass with a broader
  diagonal ribbon, tucked lower arm and rectangular terminal-negative-space slit.
- Local SVG rendering was inspected at 24, 48, 108 and 256px before commit.
  The size review is a geometry check, **not** a physical Android launcher test.
- Editable original: `design/klyro-brand-mark.svg`.
