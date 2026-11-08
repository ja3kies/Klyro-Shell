# Klyro Shell 0.4.8-dev — audit transversal UX, comportement et sécurité

Date : 8 novembre 2026  
Branche de développement isolée : `feat/klyro-ux-systematic-audit-20261108`  
Version stable de départ (CI validée) : `4024ddda4730f427895195094a15edf5325afedd`  
Périmètre : toutes les surfaces Compose de Klyro, les interactions native TerminalView, l'identité launcher et les entrées plugins.

## Critères de verdict

- **PASS CI** : test de régression exécuté avec succès sur le SHA final et compilation Android correspondante validée.
- **PASS implémentation** : correctif présent dans le commit et vérifié par tests automatisés. Ne garantit pas le rendu tactile sur appareil.
- **QA appareil requise** : résultats qui ne sont pas déductibles d'un build x86 ou d'une lecture du code : rendu réel, TalkBack, saisie IME, rotation, Android launcher, shell/PTy après mise à jour.
- Les trois audits adversariaux de ce lot sont des **tests de contrats / JVM**, et non trois essais humains sur Pixel 9.

## 1. Audit adversarial : sécurité et intégrité

| Surface | Défaut ou attaque | Correctif | Vérification |
|---|---|---|---|
| Plugins / import SAF | Installation silencieuse depuis JSON externe | Prévisualisation des commandes, confirmation explicite avant persistance | `test_klyro_cross_surface_ux.py` |
| Plugins / CLI | Un argument `git diff --ext-diff` peut déclencher un programme externe | Liste blanche de gabarits littéraux relus, seulement recherche pkg avec motif borné | `KlyroPluginPolicyTest.kt` |
| Palette / listes | Deux packs avec la même commande et le même label peuvent provoquer une collision de clé Compose | Déduplication des suggestions actives | Tests de contrat + compilation |
| Select all | Transcript affiché dans un EditText encore modifiable | Sélection native conservée mais `keyListener = null` pour interdire l'édition | `test_klyro_cross_surface_ux.py` |
| Collage natif | Collage simple précédemment soumis à une alerte « Paste multiple lines » sans justification | Même validation `ClipboardPastePolicy` pour toutes les origines ; confirmation seulement en cas de retour ligne | JVM `ClipboardPastePolicyTest` et contrats UI |
| Rapport diagnostic | Notification « Report copied » même si le service presse-papiers échoue | Résultat réel contrôlé ; échec de partage annoncé | Contrat transversal |

## 2. Audit adversarial : interaction et état fonctionnel

| Surface | Défaut / objectif | Correctif | Vérification |
|---|---|---|---|
| Home | Navigation Plugins perdue en rotation | `rememberSaveable` + Retour système depuis page Plugins | Contrats Home / Plugins |
| Plugins | Icône grille générique pour toutes les extensions | Pictogramme puzzle original et icônes distinctes par fonction | Validation vector XML + compilateur AAPT |
| Plugins | Retour système absent | `BackHandler` explicite | Contrat navigation |
| Plugins | Onglets non exposés comme sélectionnés à TalkBack | Sémantique `selected` sur onglets Installed / Explore | Contrat accessibilité |
| Terminal | Identité de marque absente du header | Même source d'icône vectorielle que Home et launcher | Contrat d'identité |
| Recherche | Case à cocher seulement petite cible tactile | Ligne entière sélectionnable, une seule action pour Match case | Contrat interaction |
| Keyboard Settings | Reset sans indication des touches latched | Statuts réels Ctrl/Alt/Shift/Fn, bouton inactif quand tout est relâché | Contrat clavier |
| Privacy Settings | Page descriptive sans action utile | Résumé exact des données + ouverture des permissions Android système | Contrat confidentialité |
| Workspaces | Défilement déclenché avec seulement 1 projet + 3 sessions | Nombre combiné de lignes (6 maximum sans défilement) | JVM `KlyroSheetLayoutPolicyTest` |
| Session | Risque d'inaccessibilité des actions sur téléphone compact ou grosses polices | Fallback scroll contextualisé, non activé sur hauteur standard | JVM `KlyroSheetLayoutPolicyTest` |

## 3. Audit adversarial : conception visuelle / design system

| Surface | Défaut / objectif | Correctif | Vérification |
|---|---|---|---|
| Thème Contrast | UI claire mais terminal noir : ancien prompt ANSI cyan/noir invisible | Couleurs natives calculées à partir du **terminal** et non du thème de chrome ; curseur, prompt et ANSI visibles | JVM `KlyroDesignTest` |
| Theme System | Aperçu dépendant du thème courant, parfois monochrome | Aperçu fixe double clair/sombre du thème système | Contrat thèmes |
| Settings / Sheets | Seuils de défilement recopiés avec dérive | `KlyroSheetLayoutPolicy`, 6 lignes, seuil hauteur et fontScale, tests de limites | JVM + source |
| Color picker | Canvas saturation/luminosité inutilisable à TalkBack | Quatre actions d'accessibilité bidimensionnelles + état annoncé | Contrats UI |
| Color picker / clavier | Ouverture IME pouvant cacher les contrôles | Défilement interne si clavier visible, petite hauteur ou forte échelle texte | Contrats de responsive |
| Command Palette, Workspaces | Titres susceptibles d'empiéter sur les CTA sur petits écrans | Titre partagé à une ligne et ellipsis, flex avec poids borné | Contrats typographie |
| About | Logo, moteur et architecture dispersés | Carte de marque regroupant logo couleur, version et ARM64 | Contrat identité |
| Diagnostics | Deux simples CTA texte identiques | Boutons secondaires à pictogrammes Copier/Partager ; action principale Recheck mise en évidence | Contrat actions |
| Launcher | Différence d'échelle avec le visuel Home | Ressource adaptative du launcher réutilisée ; masque Android 108×108dp inchangé | `test_klyro_launcher.py`, `test_klyro_brand_geometry.py` |

## Matrice des écrans et tests

| Écran / composant | Test de contrat ou métier | Gate compilation |
|---|---|---|
| Home, logo, statut, navigation | `test_klyro_cross_surface_ux.py` | `:app:assembleDebug` |
| Terminal, sessions, raccourcis, Find, sélection et collage | `ClipboardPastePolicyTest`, `TerminalNavigationPolicyTest`, contrats UX | `:app:assembleDebug` |
| Settings Home, Appearance, Keyboard, Storage, Privacy, Diagnostics, About | `KlyroSheetLayoutPolicyTest`, `KlyroDesignTest`, contrats UX | `:app:lintDebug` |
| Color Picker, presets, favoris, licences | `KlyroFavoriteColorsTest`, `KlyroHexInputTest`, contrats UX | `:app:lintDebug` |
| Workspaces, session sheet, shell chooser | `KlyroWorkspaceCatalogTest`, contrats UX | `:app:assembleDebug` |
| Command Palette, Plugins, import | `KlyroPluginPolicyTest`, `KlyroCommandCatalogTest`, contrats UX | `:app:assembleDebug` |
| Launcher, ressources SVG adaptatives, native runtime | tests Python geometry, ELF 16 KiB, signature v2/v3 | `:app:assembleRelease` et audit APK |

### Gate CI requis

1. Préparer les assets Termux vérifiés ARM64 et les licences.
2. `python3 -m unittest discover -s tools -p "test_*.py"`.
3. `gradle :app:testDebugUnitTest :terminal-emulator:testDebugUnitTest`.
4. `gradle :app:lintDebug`.
5. `gradle :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease`.
6. Vérifier package `com.klyro.shell`, target SDK 36, assets natifs, ELF 16 KiB et signatures.
7. Archiver les APK et rapports ; **ne pas fusionner main**.

Les verdicts de CI doivent être lus **sur le SHA final**, pas réutilisés depuis un run antérieur. Aucune compilation ne peut prouver qu'une interaction tactile Pixel 9 fonctionne sans l'exécuter.

### Rollback

Préserver l'APK et le SHA de base `4024ddda...`. Ne pas effacer ni désinstaller les données Android. Le nouveau `versionCode=15` (`0.4.8-dev`) peut être installé au-dessus de la version précédente seulement si la signature correspond. En cas d'échec, corriger sur cette branche ; ne forcer aucune fusion ni installation. Pour revenir à l'ancien code tout en gardant les données, compiler ce code avec une nouvelle versionCode supérieure et la même clé.
