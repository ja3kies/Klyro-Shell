# Klyro Shell — Objectifs H + I, lot de consolidation

Version 0.4.3-dev (versionCode 10). Branche `fix/klyro-hi-terminal-resilience-20261106`.

## H — 15 interventions vérifiables sur terminal et presse-papiers

1. Centralise la lecture du clipboard dans `ClipboardTextReader`.
2. Refuse les presse-papiers dépourvus de type texte explicite.
3. Refuse les payloads comportant plusieurs items ambigus.
4. Refuse les items portant un URI (pas de conversion de fichier en commande).
5. Refuse les items portant un Intent (pas de conversion implicite).
6. Refuse les items sans texte explicite, même s'ils ont un type texte.
7. Valide taille maximale et contrôles interdits avec `ClipboardPastePolicy`.
8. Refuse les overrides bidi / caractères de format invisibles.
9. Refuse les séparateurs Unicode U+2028 et U+2029.
10. Réutilise la même validation sur le bouton PASTE de Compose.
11. Réutilise la même validation pour le collage natif Termux.
12. Revalide le texte immédiatement avant de confirmer un collage multiligne.
13. Respecte Ctrl/Alt/Shift sur les quatre flèches avec les CSI xterm.
14. Respecte Ctrl/Alt/Shift sur END avec les CSI xterm.
15. Couvre les combinaisons de navigation et les payloads dangereux par tests JVM.

Les protections antérieures (CLEAR local sans Ctrl+L, ALL/PASTE contextuel) sont préservées et non recomptées.

## I — 15 interventions vérifiables sur le cycle de vie

1. Dispose explicitement le client de session à la fermeture de l'onglet.
2. Retire les callbacks UI en attente du client fermé.
3. Révoque l'observateur de sortie au moment de la disposition.
4. Empêche les rafraîchissements PTY différés après fermeture.
5. Empêche les mises à jour de titre différées après fermeture.
6. Empêche les callbacks de fin périmés après fermeture.
7. Empêche une copie presse-papiers d'un client disposé.
8. Refuse les collages natifs sur un processus terminé.
9. Refuse le collage natif si le client est disposé.
10. Revalide session active et client vivant avant confirmation différée.
11. Réserve la vibration terminal au bon écran encore attaché.
12. Refuse d'attribuer les anciens collages sans tabId à l'onglet actif.
13. Refuse d'attribuer les anciens titres ou PID sans tabId à l'onglet actif.
14. Bloque les identifiants d'onglet nuls ou négatifs.
15. Détecte et annonce la perte des sessions après mort du processus Android, sans la confondre avec une rotation.

Protection supplémentaire : remplace `System.exit(1)` dans l'accès au descriptor PTY par une exception et la fermeture du fd.

## Critères de validation

CI sur SHA exact : tests JVM app et terminal-emulator, Python, lint, assemblages debug/release et APK de test, audit ELF 16 Kio, signatures et artefact `klyro-shell-debug`.
Tests appareil non encore effectués pour cette version : clipboard non texte et multiligne, navigation modifiée, rafraîchissements intensifs, rotation et mort du processus.
Le nombre d'interventions ci-dessus décrit des garde-fous fonctionnels individualisés; il ne représente pas 30 corrections de bugs distincts préalablement observés sur le Pixel.
Aucun merge dans main. Retour arrière via dernier artefact valide (0.4.2-dev, run 42197483515), sans effacer les données de l'application.
