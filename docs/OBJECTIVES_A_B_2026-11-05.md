# CAVEMAN — paire d'objectifs A + B — v0.4.2-dev

Ce document réconcilie les anciens audits avec les modifications du lot.
**SOURCE / CI n'est pas DEVICE** : code et tests décrits, validation du
Pixel 9 ouverte. Chaque correction doit être identifiable dans le dépôt.

## A — Identité / launcher : 15 corrections

| ID | Correction, sans changer le concept du logo | Preuve |
|---|---|---|
| A01 | Nouvelle source de référence SVG maître à couches nommées | `design/klyro-logo-master.svg` |
| A02 | Ruban K structuré en partie arrière et partie avant | groupes `k-stem-layer` / `k-ribbon-layer` |
| A03 | Panneau >_ indépendant du K et gap optique stable | `terminal-layer`, panel x49.8 vs ruban x46 |
| A04 | Glyph > et underscore en chemins vectoriels, pas police | groupe `terminal-prompt-layer` |
| A05 | Glint fin autonome, pas de contour périmétrique | `terminal-glint` |
| A06 | Superposition highlight bornée et volontaire | `k-highlight`, alpha=.36 |
| A07 | Gradients source et Android coordonnés exactement | `test_klyro_brand_geometry.py` |
| A08 | Ressource foreground Android vectorielle sans bitmap | `ic_klyro_launcher_foreground.xml` |
| A09 | Ressource monochrome distincte à trous >_ | `ic_klyro_mark_monochrome.xml` |
| A10 | Parité des silhouettes monochrome/color | test `test_monochrome_uses_same_stem_panel_and_ribbon` |
| A11 | Visibilité et taille conformes min48/max66dp d'Android | source uniforme 0.80, vérification bounds |
| A12 | Background plein 108dp, masque laissé au launcher | `ic_klyro_launcher_background.xml` |
| A13 | Variante SVG claire construite sur même source | `design/klyro-logo-light.svg` |
| A14 | Variantes SVG mono clair/sombre | `design/klyro-logo-monochrome-*.svg` |
| A15 | Plan de construction, grille, cercle et zone sûre | `design/klyro-logo-construction.svg` |

Remarque : le concept fourni est un mockup raster sans source éditable ;
le SVG est une reconstruction manuelle des formes, pas une vectorisation
pixel-à-pixel. Rendu final sur Pixel Launcher et thèmes dynamiques non prouvé.

## B — UI/UX globale : 16 corrections

| ID | Correction | Preuve |
|---|---|---|
| B01 | Tokens de rayons, paddings et tailles tactiles communs | `KlyroUiMetrics` |
| B02 | Sheets Sessions + Settings mêmes coins | `KlyroSessionSheets` / `KlyroSettingsSheet` |
| B03 | Modales Rename, close, picker, FIND, END cohérentes | `modalCorner` |
| B04 | Cartes de réglages géométrie commune | `cardCorner` |
| B05 | Settings hub réduit sans réduire les touch targets | min height 116dp, icône 40dp |
| B06 | Titres de cartes sur deux lignes possibles | `maxLines=2` |
| B07 | Sous-titres de cartes à 12sp/16sp | `SettingsTile` |
| B08 | Cards clickables explicitement identifiées comme boutons | `Role.Button` |
| B09 | Bento expérimental plus dense, release toujours isolée | `KlyroExperimentalBento` |
| B10 | Presets couleur mis en attente jusqu'à Apply | `pendingPreset` |
| B11 | Favoris Preview n'appliquent rien sans confirmation | `KlyroColorPickerDialog` |
| B12 | HEX invalide expliqué + clavier ASCII/Done | `KeyboardOptions`, texte d'aide |
| B13 | Picker petit écran : scroll seulement si nécessaire | `LocalConfiguration.screenHeightDp` |
| B14 | Hue rendu en dégradé continu | `Brush.horizontalGradient` |
| B15 | Curseur HSV toujours discernable | anneaux noir/blanc |
| B16 | Dialogues avancés non empilés pour les suppressions | branchements conditionnels |
| B17 | Alertes runtime colorées selon thème/contrast | `warningInk`/`dangerContainer` |
| B18 | Erreur terminal : Home ne termine pas Bash | `onReturnHome` |
| B19 | Barre de progression standard Material 3 | `LinearProgressIndicator` |
| B20 | Statuts d'environnement libellés précisément | `BOOTSTRAP INSTALLED`, `BASH STARTING` |
| B21 | Bouton END destructif correctement distingué | `ui.danger` |
| B22 | Contraste d'états vérifié sur tous les accents | `KlyroDesignTest` |

**Statut** : corrections en source ; CI attendue ; device encore ouvert.

## Sous-problèmes connexes vérifiés
- Même couleur d'erreur corrigée au setup **et** sur le terminal.
- Même géométrie de modales dans picker, sessions, sélection, FIND et END.
- Identité monochrome et light générées sur les mêmes tracés que le launcher.
- Navigation Home distinguée de toute fermeture de PTY.
- Choix de preset, favorite, HEX et HSV ne permettent plus Apply/Cancellation
  contradictoires. Saved favorites sont persistées après `Save` explicite.

## Contraintes et rollback
Branche PR #18 seulement ; `main` intacte, pas d'effacement/upgrade runtime,
pas de merge. `versionName=0.4.2-dev`, `versionCode=9`. Rollback Git par
revert des commits A/B ; rollback appareil **sans uninstall** et sans forcer
un version downgrade. Tests appareil avant fusion.
