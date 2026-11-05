# Klyro Launcher Identity — 2026-11-05

This is a purpose-built scalable Android adaptation of the user-supplied
Klyro branding sheet. The glossy 3D artwork was **not** available as original
editable paths. These assets therefore reproduce the characteristic three-part
K silhouette and lime-to-olive gradients rather than claiming an exact trace.

- `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` are adaptive
  launcher aliases (API minSdk 26); OEM masks own circle/squircle shape.
- Foreground/background authored as independent 108×108dp VectorDrawable layers.
  Glyph designed inside the safe x=21..87/y=21..87 region: actual bounds
  x=27..84 and y=23..85; no glyph clipping, no background-baked rounded square.
- Native separate solid monochrome K, used by Android themed icons and the
  app's welcome header. No bitmap scaling artifacts, text, icon shadows or
  tiny unresolvable details.
- Foreground is intentionally two-tone gradient; monochrome is a simple solid
  silhouette. Verify recognizability at 24/48dp, on circular and squircle masks,
  in light/dark and Android themed-icon mode.
- All source paths remain in the repo for further designer iteration.
- `main` and integration branch untouched. Rollback is a commit revert.
- Reference: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
