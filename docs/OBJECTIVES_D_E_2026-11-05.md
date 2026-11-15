# CAVEMAN — paire d'objectifs D + E — 2026-11-05

**Règle** : 10 corrections MINIMUM par objectif, y compris hors demandes
immédiates. Les lignes ci-dessous sont des modifications de code identifiables,
pas des garanties de validation physique. L'APK doit être compilé et testé
avant promotion.

## Objectif D — robustesse/lifecycle (18 corrections source)

| # | Correction | Preuve |
|---|---|---|
| D01 | Dialogue explicite pour fermer une session | `KlyroSessionSheets.kt` AlertDialog |
| D02 | Conséquence « Bash et enfants » annoncée | Texte d'avertissement |
| D03 | Bouton annulation non destructif | `Keep session` |
| D04 | Désactivation de fermeture du sheet pendant confirmation | `onDismissRequest` conditionnel |
| D05 | Fermeture appelée uniquement après confirmation | `confirmButton` |
| D06 | Navigation Home restaurée après rotation | `onSaveInstanceState` / `onCreate` |
| D07 | Collage natif en attente invalidé au changement d'onglet | `TerminalSessionViewModel.selectTab` |
| D08 | Callback de collage n'accepte que la session active | `isSessionActive` |
| D09 | Sheet de session isolé par objet PTY | `remember(session)` |
| D10 | Rename isolé par objet PTY | `remember(session)` |
| D11 | Choix de shell isolé par PTY | `remember(session)` |
| D12 | Sélection native et buffer d'output isolés par PTY | `remember(session)` |
| D13 | Confirmation de collage multiligne isolée par PTY | `remember(session)` |
| D14 | TerminalView mémoire isolé par PTY | `remember(session)` |
| D15 | FIND et sa requête isolés par PTY | `remember(session)` |
| D16 | Confirmation END isolée par PTY | `remember(session)` |
| D17 | État SHIFT restauré depuis client vivant | `isShiftDown()` |
| D18 | État FN restauré depuis client vivant | `isFnDown()` |

**Et contrôles transverses :** overflow Settings, section de réglages,
warnings et session selection sont également réinitialisés par PTY.
**Restant/risque :** process death réel et installer lifecycle-aware/service
foreground demandent architecture et tests appareil ; ne pas déclarer résolus.

## Objectif E — optimisation, sécurité et hygiène code (18 corrections source)

| # | Correction | Preuve |
|---|---|---|
| E01 | Sanitisation unique des noms SAF | `KlyroFileSafety.safeName` |
| E02 | Suppression traversée chemins slash/backslash | `substringAfterLast` |
| E03 | Nom limité à 80 caractères et fallback | `safeName` |
| E04 | Tentatives de conflit limitées à 1000 | `MAX_COLLISIONS` |
| E05 | Pas d'écrasement implicite en import | `Files.move` sans REPLACE |
| E06 | Répertoire cible symlink refusé | `verifyDestination` |
| E07 | Copie unique bornée 32 MiB | `copyLimited` |
| E08 | Import fichier unique utilise la même limite | `MainActivity` |
| E09 | Import dossier utilise la même limite | `MainActivity` |
| E10 | Export borné 32 MiB avant ouverture | `verifyExportSource` |
| E11 | Export symlink et hors HOME rejeté | `verifyExportSource` |
| E12 | UTF-16 invalide refusé avant envoi au shell | `ClipboardPastePolicy` |
| E13 | Contrôles C1 Unicode refusés | `Character.isISOControl` |
| E14 | Recherche FIND bornée à 512 caractères | `MAX_QUERY_CHARS` |
| E15 | Dimensions nulles et query null protégées | `findRows` |
| E16 | Index négatifs/visibles bornés | `nextIndex/topRow` |
| E17 | Regex de favoris/renommage précompilées | `KlyroFavoriteColors`, ViewModel |
| E18 | Pas de recomposition pour titre/renommage identique | `updateTitle/renameTab` |

**Tests :** `KlyroFileSafetyTest`, `ClipboardPastePolicyTest`,
`TranscriptSearchTest`, `KlyroSessionNamesTest` étendus.
**Restant :** mesurer ANR/CPU mémoire sur appareil et confirmer la résilience
aux interruptions pendant SAF/bootstrap. Ne pas lancer benchmark destructif.

## Git
Branche test `fix/klyro-v4-upgrade-ci-20261104`; `main` intacte.
Version `0.4.1-dev`, code 8, pour paire D/E.
Rollback : revert commits de la PR, pas d'uninstall ni de downgrade forcé.
