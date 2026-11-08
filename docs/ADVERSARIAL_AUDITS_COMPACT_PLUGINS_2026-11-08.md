# Klyro Shell — trois audits adversariaux (8 novembre 2026)

**Branche isolée :** `feat/klyro-compact-sheets-plugins-20261108`.
**Base :** `c94579a2d59e3eca2bd4e50591f2868cb26271d3`.
**Objet :** audit source + tests déterministes. **Ne vaut pas recette tactile sur Pixel 9.**
Aucune fusion sur `main`, suppression de données, ni installation sur appareil.

## Audit 1 — frontières de sécurité des plugins

**Attaques considérées :** injection de commandes via manifests non fiables, caractères de contrôle,
manifest volumineux, identité réservée, remplacement silencieux, exécution de JS ou Bash depuis un
fichier importé, persistance des données.

**Constats / corrections :**
- Les plugins ne sont pas des exécutables : ils contiennent des *suggestions de commandes en JSON*,
  rendues dans Command Palette. Aucun moteur JavaScript, WebView ou exécuteur de plugin n'a été ajouté.
- Import SAF en lecture seule, limité à **16 Kio** ; au plus **12 packs** et **8 commandes par pack**.
- Politique d'insertion limitée à des gabarits littéraux de lecture seule, sans métacaractères shell,
  retours ligne ou séquences de substitution ; une deuxième vérification a lieu juste avant l'insertion.
- Une suggestion ne produit jamais de touche Entrée. L'insertion est interdite en tampon alternatif.
- Un pack local ne peut pas prendre un identifiant réservé ni écraser silencieusement un pack existant.
  Activation/désactivation et suppression sont explicites ; la suppression demande confirmation.
- Limite de sécurité assumée : les champs de description sont des données fournies par un tiers.
  L'utilisateur doit examiner chaque commande avant de valider Entrée.
- **Vérifications automatiques :** `KlyroPluginPolicyTest.kt`,
  `test_klyro_adversarial_three_passes.py`. Pas de test instrumentation SAF exécuté.

**Risque résiduel :** absence de signatures pour les manifests importés et absence de sandbox pour
des extensions de code arbitraires. Il serait trompeur de présenter cette première version comme une
marketplace ou un hôte JS extensible équivalent à Acode.

## Audit 2 — interaction terminal et séparation des actions

**Attaques considérées :** glissement de rôle entre Select all et Paste, commande shell involontaire,
état pression non représenté, contrôle désactivé mais cliquable, doublons dans More, régression END.

**Constats / corrections :**
- La touche **ALL** ouvre la sélection du transcript en lecture seule ; la touche **PASTE** ne lit
  que le presse-papiers et garde la confirmation de collage multilignes.
- Suppression du doublon **Select output** dans le menu More.
- Les CTA à glyphes n'affichent plus de libellé sous l'icône ; leur nom reste exposé à TalkBack.
- Les quatre états `DEFAULT`, `ACTIVE`, `HOLD`, `INACTIVE` partagent les couleurs sémantiques
  de Klyro. `HOLD` est déclenché par la pression réelle, pas par un timer artificiel.
- L'action END auparavant dédiée est accessible par appui long sur la flèche droite. CLEAR et FIND
  conservent leurs chemins sans envoi de commande Bash.
- **Vérifications automatiques :** tests de contrats terminal préexistants révisés, plus
  `test_klyro_adversarial_three_passes.py`.

**Risque résiduel :** action mode natif Termux, focus, presse-papiers Android, maintien de touche et
accessibilité doivent être essayés sur appareil ; la lecture statique ne prouve pas ces gestes.

## Audit 3 — densité, design system, identité adaptative

**Attaques considérées :** défilement superflu sur sheets courtes, dernier élément inaccessible,
lisibilité à grand facteur de police, preview factice, duplication des valeurs de slider, disparition
de l'identité de marque dans Home.

**Constats / corrections :**
- Réduction centralisée des espacements et bento tiles. Les sheets Settings courtes (y compris
  Files et Diagnostics) gardent une hauteur fixe en configuration nominale. Le rapport de diagnostic
  défile **dans sa carte uniquement** quand il dépasse 260 dp ; les petits écrans et les polices
  agrandies conservent un repli scrollable **pour éviter les contrôles inaccessibles**.
- Workspaces désactive le scroll quand la liste est courte et garde un repli pour de nombreux
  onglets, la saisie inline ou les configurations contraintes.
- 6 thèmes présentés en cartes d'aperçu cohérentes (trois système/couleur et trois monochromes),
  avec couleurs réelles et contrôle de sélection. Slider : valeur unique + curseur, sans
  bornes répétées ni paragraphe de prévisualisation.
- Home réaffiche **ARM64** et la marque Klyro couleur ; même source foreground que le launcher.
  Les ressources adaptatives sont en **108×108 dp**, avec art centré et marge de masque.
- Le CTA Plugins ouvre une page autonome à cartes, onglets, switchs et import local.
- **Vérifications automatiques :** `test_klyro_launcher.py`,
  `test_klyro_brand_geometry.py`, `test_klyro_mobile_design.py`,
  `test_klyro_adversarial_three_passes.py`.

**Risque résiduel :** vérifier sur Pixel 9 en portrait/paysage, à taille de police 1x/1,5x,
avec clavier ouvert/fermé, en thèmes OLED/Contrast/Paper et wallpaper dynamique.
Contrôler 24 px / 48 px en launcher OEM et les surfaces de toucher à 48 dp minimum.

## Gates et retour arrière

La CI Android doit confirmer, sur le **SHA exact de la branche** : tests Python/JVM,
`lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease`, l'audit
ELF 16 Kio, le package ID et les assets native ARM64. La présence de tests n'est pas une
preuve de réussite : lire le statut final du run et les logs des jobs.

Réversibilité : commits sur branche dédiée, aucun changement sur `main`. Garder l'APK stable
avant toute installation. Un retour en arrière se fait par réversion des commits sur une branche
de rollback et une compilation de `versionCode` supérieur ; ne jamais désinstaller l'application
ou effacer son stockage sans autorisation.
