# Klyro Shell launcher identity — reference adaptation (2026-11-05)

## Source fidelity
- Reference: uploaded board `69237.png` (brand mockup, not an original SVG).
- Native VectorDrawables are an intentionally hand-redrawn adaptation of the
  reference's upright lime stroke, overlaid diagonal ribbon, deep violet
  terminal arm and small white `>_` prompt. No false claim of pixel-identical
  reconstruction of the 3D lighting/texture.
- Editable, version-controlled geometry: `design/klyro-brand-mark.svg`.
- If an original artist-owned standalone SVG or layered file is supplied later,
  replace the adaptation after side-by-side approval.

## Adaptive launcher constraints
- 108×108dp coordinate system, with the recognizable mark bounded within
  x≈24..85, y≈21..85. Its curved terminal tip and small arrow remain inside
  the centered 66dp safe bounds (21..87).
- Background is full-bleed solid dark violet-black (`#0C0B13`), with **no
  embedded squircle**. Android masks the icon (round, squircle, OEM).
- Foreground contains scalable path geometry, vector gradients and terminal
  glyph paths. No raster images or font-dependant text.
- Monochrome uses solid white K silhouette with transparent background; Android
  applies dynamic theme tint. No fixed gradients in themed layer.
- Both `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round` point to the
  adaptive asset. The app minSdk is 26; older PNG fallbacks are unnecessary.
- Welcome page shares the monochrome mark, not the desktop marketing board.
- QA required: real Pixel Launcher at ~24px and 48px, light/dark wallpapers,
  themed icon toggled, circle/squircle mask and Settings app info icon.
- Tests in `tools/test_klyro_launcher.py` assert manifest references and
  scalable resource structure. Manual SVG preview at 24, 48, 108, 256 pixels
  is informative, **not equivalent** to a physical launcher test.
- Rollback: revert specific commits on isolated draft PR branch only. Do not
  force-update main or wipe user data.
