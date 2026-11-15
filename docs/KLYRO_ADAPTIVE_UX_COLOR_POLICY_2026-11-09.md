# Klyro — couleur adaptative et hiérarchie UX

Règle : **adaptatif ne veut pas dire accent partout**.

## Système de couleurs

- Fonds, textes, touches de navigation, menus, métadonnées, pictogrammes
  secondaires : tokens neutres de `LocalKlyroColors` (`key`, `tile`,
  `chrome`, `foreground`, `muted`).
- Actions principales, sélection active et quelques repères ciblés :
  `accent` et `onAccent` issus du thème actif.
- Erreur, avertissement et état prêt : tokens sémantiques
  `danger`, `warningInk`, `readyInk`, sans confusion avec les actions
  ordinaires.
- Select/Paste : les fonds viennent de la palette active. Select est un
  raccourci contextuel à indication subtile, Paste une action neutre avec
  contour. Paste ne doit **jamais** emprunter la couleur d'avertissement.
  Les états DEFAULT, ACTIVE, HOLD et INACTIVE restent distincts.
- Exceptions fixes justifiées : représentation des couleurs dans le
  sélecteur HSV, contenu intrinsèque des fichiers SVG du logo et les couleurs
  ANSI représentées par le moteur de terminal.

## Identité visuelle

Le logo complet n'apparaît que sur l'accueil et dans les choix de launcher.
L'en-tête des sessions porte le projet courant en avant, sans logo répété.
La page À propos garde son identité typographique, sans autre logo.

## Portée de la validation

Contrats statiques et tests JVM évaluent le flux de tokens, les rôles et les
états. Les contrastes effectifs avec accents personnalisés, le rendu à grande
police et les transitions visuelles exigent encore une QA instrumentée / Pixel.
