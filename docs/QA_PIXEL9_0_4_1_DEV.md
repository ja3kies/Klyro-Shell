# Klyro-Shell — QA Pixel 9 — v0.4.1-dev / code 8

**CAVEMAN**: fichier = protocole; `[ ]` = PAS ENCORE TESTÉ.
Ne cochez jamais PASS sur la seule base de GitHub Actions. Exécuter dans
l'APK exact et enregistrer `PASS / FAIL / BLOCKED` + capture et comportement.

## Conditions et seuil d'arrêt
- Pixel 9, Android 17, Termux/ADB disponible sans root; Klyro APP `com.klyro.shell`.
- Mêmes clés de signature, `adb install -r` uniquement ; **aucun uninstall**.
- Conserver fichiers privés. Ne jamais exécuter `pkg upgrade`, mise à jour de
  dépôts, réinitialisation, `am force-stop` ou destruction de processus
  sans accord séparé.
- Interrompre les essais sur corruption, perte de session, sortie du stockage
  Klyro, plantage répété ou blocage d'un processus du shell.
- Capturer screenshot et courte sortie sans adresse privée, token, secret ni
  chemin de données personnelles.
- Build installé : SHA `________` ; RUN `________` ; versionCode `8`.

## Q1 — Modales et confirmation de fermeture
- [ ] 01. Session unique, toucher son onglet puis `Close terminal` :
      **dialogue distinct** « Close Session 1? ».
- [ ] 02. Le dialogue annonce que les processus enfants seront interrompus.
- [ ] 03. `Keep session` conserve le PTY et le contenu.
- [ ] 04. Toucher hors du dialogue conserve le PTY.
- [ ] 05. `Close session` termine seulement la session visée.
- [ ] 06. Plusieurs sessions, fermer une session inactive : la session active
      continue sans changer de répertoire courant.
- [ ] 07. Le bouton `END` demande confirmation avant de stopper Bash.
- [ ] 08. Le menu `Home` ne déclenche jamais la fermeture.
- [ ] 09. Les noms automatiques se renumérotent après fermeture.
- [ ] 10. Les noms personnalisés restent associés à leur PTY.

## Q2 — Navigation / lifecycle
- [ ] 11. Home → Return to terminal conserve PID, output et cwd.
- [ ] 12. Rotation sur Home reste sur Home, sans nouveau Bash.
- [ ] 13. Rotation sur terminal conserve le même PID et son tampon.
- [ ] 14. Si le picker de couleur est ouvert, rotation conserve la session.
- [ ] 15. Changer d'onglet ferme les actions transitoires de l'ancien onglet.
- [ ] 16. Un collage multiligne en attente ne s'exécute pas dans un autre onglet.
- [ ] 17. Après rotation, SHIFT et FN affichent leur état réel.
- [ ] 18. Retour arrière sur popup/modale ne ferme pas Bash.
- [ ] 19. Écran verrouillé/déverrouillé, commandes en cours non perdues.
- [ ] 20. Arrière-plan puis retour, même session sans duplication.
- [ ] 21. Process-kill : **NON TESTER sans autorisation**, P1 ouvert, aucune
      garantie de survie tant qu'un service Android durable n'existe pas.
- [ ] 22. Installation/bootstrap interrompu : **NON TESTER** avec données
      réelles sans sauvegarde et accord.

## Q3 — Terminal fonctionnel P0
- [ ] 23. La vérification d'environnement montre Bash/UID/APT/pkg réels.
- [ ] 24. `bash --version`, `id -u`, `pwd`, `tty` répondent.
- [ ] 25. `stty size` reflète le PTY après ouverture du clavier.
- [ ] 26. Shell accepte espaces, unicode, accents, emoji.
- [ ] 27. Shell accepte pipe, redirection temporaire, sous-shell.
- [ ] 28. `Ctrl+C` interrompt un processus lancé volontairement.
- [ ] 29. `Ctrl+D` fonctionne et la fermeture automatique est gérée.
- [ ] 30. HIST/↑/↓/TAB fonctionnent avec Gboard.
- [ ] 31. Deux PTY conservent cwd et historique indépendants.
- [ ] 32. ANSI et sortie volumineuse affichées sans gel.
- [ ] 33. FIND trouve dans l'historique puis revient au terminal.
- [ ] 34. Long press → Copy / More → Select all fonctionne en entier.
- [ ] 35. `apt --version`, `pkg --help` répondent, sans mutation.
- [ ] 36. Pas d'écriture dans le vrai stockage de Termux.
- [ ] 37. Le script `scripts/klyro-operational-gate.sh` n'est exécuté
      **qu'après** confirmation de l'installation et du Bash interactif.

## Q4 — Presse-papiers / import/export
- [ ] 38. Collage simple fonctionnel (barre dédiée + Gboard + action native).
- [ ] 39. Collage avec retours à la ligne → avertissement avant toute exécution.
- [ ] 40. Annuler avertissement ne transmet **aucun** octet au PTY.
- [ ] 41. Collage texte de contrôle/ESC interdit proprement, avec feedback.
- [ ] 42. Collage de 65 537 caractères interdit sans ANR.
- [ ] 43. Import SAF via fichier unique, nom hostile normalisé.
- [ ] 44. Import dossier : maximum 30 fichiers et 32 MiB/fichier.
- [ ] 45. Fichiers déjà présents ne sont pas écrasés.
- [ ] 46. Tentative de symlink dans `imports` rejetée (test contrôlé).
- [ ] 47. Export `~/imports/klyro-export.txt` uniquement et 32 MiB max.
- [ ] 48. Après annulation SAF, aucune donnée modifiée.

## Q5 — UI globale et identité
- [ ] 49. Icône launcher normale et thématique centrées, pas zoomées.
- [ ] 50. `K` et `>_` séparés/lisibles à 48dp ; rond et squircle.
- [ ] 51. Thèmes clair/sombre, quatre accents, contraste des états.
- [ ] 52. Session pill active/inactive/+ translucide lisibles.
- [ ] 53. Color Picker par défaut sans scroll à densité standard.
- [ ] 54. Presets sous Hue, HEX et preview de faible hauteur.
- [ ] 55. Menu ordonné Settings → Files & storage → Home, sans Paste.
- [ ] 56. Apparence, sessions, FIND, rename, picker, alertes utilisent
      même grille/rayons/couleurs et CTA.
- [ ] 57. À facteur police 1.3–1.5, CTA pas tronqués ni inaccessibles.
- [ ] 58. TalkBack identifie les cibles et les états actifs.
- [ ] 59. Barres système contrastées sur fond clair et sombre.
- [ ] 60. Paysage + IME : terminal conservé, clavier et FIND utilisables.

## Résultat
Statut global : `NOT RUN`. P0 : ___/___ PASS, P1 : ___/___ PASS.
Blocage : ___ ; SHA témoin : ___ ; captures : ___.
**Règle:** 1 erreur P0 = aucune fusion. Les problèmes physiques doivent
produire un test de reproduction puis une correction spécifique sur branche.
