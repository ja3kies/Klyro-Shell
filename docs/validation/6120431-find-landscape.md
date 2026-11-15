# Correction FIND / paysage — base 6120431

Branche : `fix/find-landscape-6120431`, créée depuis
`6120431c0d396264cb562e89a2935e5808fcb953`.
État initial propre ; `main` et `fix/proot-5.1.107.102` inchangées.

## Diagnostic et périmètre

- FIND : Previous/Next changeaient uniquement le compteur. Le saut final précédait
  la fermeture du dialogue/clavier ; TerminalView 0.118.0 remet `mTopRow` à zéro
  lorsque `updateSize()` redimensionne le PTY. Les résultats mémorisés par identité
  du buffer pouvaient devenir périmés. La limite de 10 000 lignes correspondantes
  excluait les résultats récents d'un grand historique.
- Extraction : les coordonnées attendues sont les lignes physiques négatives dans
  l'historique, puis 0..mRows-1. L'ancienne extraction par ligne utilisait déjà ces
  coordonnées, mais ne trouvait pas un texte coupé par un retour automatique.
  Le helper utilise explicitement la surcharge à six arguments de getSelectedText
  (celle à cinq arguments ignore son booléen dans ce dépôt), recolle les lignes
  repliées et traduit chaque occurrence vers sa ligne physique de départ.
- Paysage : MainActivity applique safeDrawing puis imePadding ; Compose consomme
  les insets déjà appliqués. Le manifeste utilise adjustResize. Pas de preuve de
  double soustraction IME, donc aucun changement de MainActivity ou du manifeste.
  L'en-tête et deux rangées de touches consommaient la hauteur restante. Sous
  300 dp disponibles, le menu rejoint les onglets et les touches occupent une
  seule rangée horizontale ; cibles tactiles de 48 dp conservées.
- Le saut final relit le buffer après disparition IME et stabilisation de la hauteur
  sur trois frames. Previous/Next défilent sans voler le focus au champ FIND.
  La police initiale n'est plus réappliquée à chaque recomposition, pour conserver
  le zoom natif. L'attachement et le focus lors du changement d'onglet restent en place.

Fichiers : TerminalPane.kt, TranscriptSearch.java, TranscriptSearchTest.java,
`scripts/test-terminal-search.sh`, ce rapport. Aucun changement de permissions,
bootstrap, PRoot, dépendances, sessions ou cycle de vie.

## Validation exécutée

- `sh scripts/test-terminal-search.sh` avec JUNIT_JAR (4.13.2) et HAMCREST_JAR
  (1.3) : **5 tests JVM réussis**, sur les vrais TerminalBuffer/TerminalRow/WcWidth.
  Couverture : vide/absent, répétitions, casse, Unicode, historique/écran, lignes
  repliées, buffer circulaire de 15 000 sorties, plus de 10 000 correspondances,
  navigation dans les deux sens et bornes du défilement.
- Gradle 8.9 local : tentative des tâches `:app:testDebugUnitTest`,
  `:terminal-emulator:testDebugUnitTest`, `:app:assembleDebug`. Le mode hors ligne
  échoue faute de plugin en cache ; la tentative en ligne résout le plugin puis
  échoue car AGP 8.10.1 exige Gradle >= 8.11.1. Aucun SDK Android configuré
  (ANDROID_HOME/ANDROID_SDK_ROOT absents). Ni compilation Kotlin/Android ni suite
  JVM complète Gradle validées. La CI d'origine ne valide pas ce nouveau diff.
- `git diff --check` : réussi.

Reproduction JVM sans SDK :

```sh
JUNIT_JAR=/chemin/junit-4.13.2.jar \
HAMCREST_JAR=/chemin/hamcrest-core-1.3.jar \
sh scripts/test-terminal-search.sh
```

## Vérifications Android à exécuter (non exécutées ici)

1. Avec Gradle 8.11.1, SDK 36, NDK 27.0.12077973 et CMake 3.22.1 :
   `gradle :app:testDebugUnitTest :terminal-emulator:testDebugUnitTest :app:assembleDebug`.
2. Produire un historique déterministe dans Klyro :
   `for ((i=1;i<=15000;i++)); do printf 'ligne %05d repere\n' "$i"; done`.
   FIND `absent-xyz` : aucun résultat, saut désactivé. FIND `repere` : compteur,
   précédent/suivant circulaires, puis Go to line sur début/milieu/fin de
   l'historique encore retenu. Après fermeture et animation clavier, vérifier
   que la ligne choisie reste visible. Répéter avec plusieurs occurrences sur
   une ligne et une chaîne assez longue pour se replier.
3. Portrait/paysage, clavier ouvert/fermé, navigation gestuelle et trois boutons :
   les onglets et toutes les touches restent accessibles ; en faible hauteur,
   une seule rangée de raccourcis défile horizontalement. Vérifier le prompt,
   saisir une commande, puis `stty size` avant/après rotation et clavier : les
   dimensions doivent suivre la zone terminal. Répéter avec avertissement runtime.
4. Non-régression explicite : pincer puis ouvrir/fermer FIND (taille conservée),
   défiler à un doigt dans l'historique, créer deux sessions avec sorties distinctes
   et basculer, mettre en arrière-plan puis revenir et saisir dans chaque session.
   Le code des quatre gestionnaires précédemment validés est inchangé ; leur
   comportement physique doit néanmoins être revalidé avec ce diff.

## Limites et rollback

Les tests JVM ne valident ni les insets, ni le focus Android, ni le redimensionnement
PTY réel. La synchronisation après fermeture du clavier reste à vérifier sur appareil.
Une sortie continue conserve le comportement auto-follow normal de TerminalView ;
la recherche n'immobilise pas le processus ni son historique. Une fenêtre extrêmement
basse ou un bandeau d'erreur/avertissement peut encore laisser peu de lignes visibles.

Rollback non destructif : sauvegarder le diff (ou `git stash push -u` si approprié),
puis `git switch --detach 6120431c0d396264cb562e89a2935e5808fcb953` pour reconstruire
l'APK initial. Ne pas utiliser reset --hard. Le checkout ne rétrograde pas une APK
installée : réinstaller explicitement l'APK de référence en conservant les données.
Aucun merge, push, tag, release ou publication effectué.
