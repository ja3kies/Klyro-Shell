# Campagne P0 — état de clôture vérifiable

Source : `022d758` de `feat/production-ready-terminal`. Branche de travail :
`fix/p0-reliability-closure`. Source des APK :
[`3d0b447`](https://github.com/ja3kies/Klyro-Shell/commit/3d0b447cc660f02ee22d54009f13aa1ae8fcd2ba).
Aucun merge, tag, release ou changement de permissions. Main et la branche
PRoot locale sont conservées. Aucun processus utilisateur arrêté.

**Clôture physique P0 : BLOCKED.** Aucun appareil ADB autorisé disponible.
Le robot et l'instrumentation ont été tentés ; zéro capture et zéro assertion UI.
Le téléphone héberge les tests natifs/JVM via Termux : cela ne valide pas l'APK
sous son propre UID, SELinux, Activity, service ou clavier.
Statut : **NOT READY — validation P0 incomplète**. Aucun niveau
READY FOR DAILY TRIAL / READY FOR PRIMARY USE n'est attribué.

## A. Dix objectifs P0

| Objectif | État vérifiable | Ce qui reste ouvert |
|---|---|---|
| P0-01 ANR/réactivité | Correctifs ciblés testés hôte ; PENDING DEVICE VALIDATION | ANR original UNVERIFIED sans trace ; Settings sous charge, frames et latence NOT MEASURED |
| P0-02 PTY/entrée/processus | Files et émulateur testés ; PTY/JNI réels hôte PASS | SIGINT/EOF/Ctrl+Z, programmes interactifs et reprise dans l'APK UNVERIFIED |
| P0-03 CLEAR | Purge conservatrice testée sur l'émulateur réel JVM | Readline/nano/vim et rotation physiques PENDING DEVICE VALIDATION |
| P0-04 lifecycle | Ownership existant audité ; aucun arrêt ajouté | Home/récents/service/rotation/pression mémoire BLOCKED sans appareil |
| P0-05 paquets/bootstrap | Transactions de préfixe protégées ; 15 cas adversariaux PASS ; audit guest et paquet réseau PASS | Réinstallation nano et pkg update guest PASS ; verrou frontend et dépôt injoignable guest PASS ; interruptions/signatures invalides/continuité APK NOT TESTED |
| P0-06 sessions | IDs stables pour confirmation, touches, workspace ; politique testée | Recomposition/rotation/doubles clics réels et suppression workspace PENDING DEVICE VALIDATION |
| P0-07 presse-papiers | Snapshot, contrôles, borne UTF-8 et refus atomique testés | Clipboard Android, plugins et palette physiques PENDING DEVICE VALIDATION |
| P0-08 fichiers | Copies annulables, commit sérialisé, export NOFOLLOW ; tests filesystem/streams PASS | Providers SAF réels, révocation, rotation picker, stockage plein physique BLOCKED |
| P0-09 accessibilité | Libellé CLEAR explicite ; aucune lecture automatique du transcript ajoutée | TalkBack, focus, grandes polices, contrastes et targets réels UNVERIFIED |
| P0-10 qualité des tests | 93 scénarios ciblés traçables ; tests faibles conservés comme contrats structurels | Les tests Python ne prouvent ni absence d'ANR ni lifecycle Android |

Aucun objectif entier n'est marqué RESOLVED sur la seule base du code ou des
JVM. Les défauts circonscrits ci-dessous ont leur correction et une preuve hôte.

## B. Causes, reproductions et correctifs

| Défaut / preuve | Cause et reproduction | Correction / fichiers principaux |
|---|---|---|
| CONFIRMED — double timer de rendu | Retirer le seul onglet pending puis recevoir une sortie avant l'ancien timer ; horloge virtuelle | `OutputUpdateBatch.java`, `TerminalSessionViewModel.kt` : ownership du timer indépendant du Set ; dernière notification conservée |
| CONFIRMED — allocation SELECT | SELECT sur 50 000 lignes matérialisait tout avant truncation | `TerminalBuffer.java`, `TerminalPane.kt` : suffixe borné avant extraction, coupe UTF-16 sûre |
| CONFIRMED — cellule non bornée | 100 000 caractères combinants à une seule position | `TerminalRow.java` : plafond 64 unités UTF-16/cellule ; graphèmes usuels testés |
| CONFIRMED — CLEAR désynchronisant | Prompt imitable, saisie partielle ou CSI/OSC/UTF-8 interrompu pendant injection locale | `TerminalDisplayActions.java` : purge directe du seul historique ; aucun texte envoyé au shell/parseur ; refus alternate/terminé |
| CONFIRMED — activation bootstrap invalide | Staging fichier, liens de transaction/dangling backup | `TermuxBootstrapInstaller.java` : refus avant mutation, sauvegardes conservées |
| CONFIRMED — import concurrent | Deux copies choisissaient le même nom avant leur commit | `KlyroFileSafety.kt`, `MainActivity.kt` : choix du nom et move sérialisés au commit ; huit imports sans écrasement testés |
| CONFIRMED — annulation ignorée / boucle copie | Annuler après read ; provider retourne zéro continuellement | Même helper : checkpoints coroutine/interruption avant commit et entre chunks ; zéro-progrès refusé |
| CONFIRMED — cible de confirmation variable | Ouvrir End pour A puis activer B ; ancienne lambda relisait l'actif | `TerminalActionPolicy.java`, `TerminalPane.kt`, `MainActivity.kt` : ID cible sauvegardé et relu au dispatch ; close/workspace explicites |
| CONFIRMED — état de feuille par nom | Deux IDs de session portent le même nom | `KlyroSessionSheets.kt` : confirmation liée à l'ID, pas au titre ; politique testée, recomposition Android non exécutée |
| CONFIRMED — OSC titles non regroupés | Rafale de titres créait un post UI par callback | `OrynTerminalSessionClient.java` : même batch 120 ms, dernière valeur dirty conservée pendant pause |
| CONFIRMED — faux succès d'insertion | Queue stdin refuse mais l'API void retournait un succès logique à la palette | `TerminalOutput/TerminalSession/TerminalEmulator` : tryWrite/pasteWithResult propagent le refus ; API void paste conservée |
| CONFIRMED dans le harness — SIGHUP ignoré hérité | Parent ignore SIGHUP ; enfant exec sleep ; signal de groupe reste ignoré avant correctif | `termux.c` remet SIGHUP à SIG_DFL ; même test avant échoue, après passe ; groupe de test seul nettoyé |

Le refus atomique de stdin ne ralentit pas la lecture stdout. Le rendu est
regroupé, les octets PTY restent intégralement consommés. Le sink accepte une
transaction de collage entière ou n'en accepte aucun octet.

## C. Lots et commits

| Cycle | Commit | Actions comportementales ciblées |
|---|---|---|
| 1 — flux/rendu/allocations | [1f644e6](https://github.com/ja3kies/Klyro-Shell/commit/1f644e6) | 10 batch + 5 limites émulateur |
| 2 — CLEAR/parser/interactions | [ea4d92d](https://github.com/ja3kies/Klyro-Shell/commit/ea4d92d) | 15 cas CLEAR |
| 3 — transactions/bootstrap/lifecycle | [d3fa783](https://github.com/ja3kies/Klyro-Shell/commit/d3fa783) | 15 cas bootstrap ; lifecycle réel bloqué |
| 4 — données/transferts | [1fc1332](https://github.com/ja3kies/Klyro-Shell/commit/1fc1332) | 15 cas filesystem/streams |
| 5 — identité/callbacks/accessibilité | [da3b430](https://github.com/ja3kies/Klyro-Shell/commit/da3b430) | 15 cas dispatch/policy ; TalkBack réel bloqué |
| 6 — adversarial transversal | [3d0b447](https://github.com/ja3kies/Klyro-Shell/commit/3d0b447) | 6 collages + 9 files + 3 dispatch ; stress natif en plus |

Le [registre détaillé](validation/p0-reliability/BEHAVIORAL_LEDGER.md) donne les
93 noms de scénarios et leurs preuves XML. Un cas de test correspond à une
vérification comportementale substantielle, pas à un commentaire ou une lecture.
Ces six cycles d'implémentation/tests hôte n'équivalent pas à six cycles de
validation physique achevés. Aucun travail P1/P2 n'a été commencé.

## D. Tests et mesures réellement exécutés

- Gradle local : `:app:testDebugUnitTest` **263 PASS** ;
  `:terminal-emulator:testDebugUnitTest` **34 PASS** ;
  `:app:compileDebugAndroidTestJavaWithJavac` PASS (compilation seulement).
- Suite Python : **506 PASS** ; contrats structurels et tests outils, sans
  prétendre qu'une assertion de chaîne valide un comportement Android.
- Neuf tests QueueAdversarialTest réexécutés directement avec JUnit après
  renforcement : worker réellement WAITING avant fermeture, 9 PASS.
- Native/JNI compilés avec clang sur Android ARM64 : 20 PTY, isatty stdin/out/err,
  code exit 37, EINTR wait, SIGTERM, SIGHUP, UTF-8/env/cwd et stress stdout PASS.
- Guest PRoot jetable préexistant : `dpkg --audit` vide, `apt-get check` PASS,
  Bash/Git/Python/Node/npm/curl/nano/vim trouvés, versions Git/Python/Node/npm
  exécutées ; exit 0 en 7,17 s. En plus : `pkg update`, téléchargement HTTPS et réinstallation réelle nano 9.3,
  `nano --version`, nouvel audit dpkg/check PASS en 22,74 s ; uniquement guest jetable.
- Guest : verrou dpkg frontend détenu par un autre processus → APT refuse la
  transaction ; dépôt localhost injoignable → apt update échoue explicitement ;
  audit dpkg/check intacts ensuite. PASS, sans modifier les sources réelles.
- Robot smoke et `adb shell am instrument` : **BLOCKED**, aucun appareil ADB.
- `git diff --check` PASS ; aucune modification du manifeste/permissions.

| Mesure | Résultat | Portée |
|---|---|---|
| Sortie native seq | 20 000 lignes, 128 894 octets, 0,044688 s | PTY hôte, pas rendu APK |
| FD natifs | 4 avant / 4 après | Harness court ; pas preuve de stabilité prolongée APK |
| RSS max | 3540 KiB | Processus harness natif uniquement |
| File Java | 4 194 304 octets sans perte, 398 041 505 ns | JUnit hôte ; inclut vérification de chaque octet |
| Settings/latence/frames lentes/UI thread | NOT MEASURED | Appareil requis |
| Allocations/GC/mémoire/CPU APK | NOT MEASURED | Profilage appareil requis |
| PID/reprise après arrière-plan | NOT MEASURED | Pas de reprise physique exécutée |

Aucune comparaison de performance avant/après annoncée sans protocole comparable.
Les premiers essais stdout avec printf externe par ligne sont exclus des mesures.
L'assertion HUP initiale n'acceptait que WIFSIGNALED ; le shell pouvant rapporter
128+signal, le harness accepte ces deux terminaisons. Aucun échec produit n'est
masqué par un faux PASS.

## E. Non-régressions et audit adjacent

- Suffixe SELECT testé avec CJK, emoji, frontière surrogate, scrollback saturé ;
  flood combinant borné sans déplacer la colonne du graphème suivant.
- CLEAR préserve viewport, curseur, saisie et parseur ; tests CSI/OSC/UTF-8
  fractionnés, alternate screen, bracketed paste, resize et deux émulateurs.
- Refus stdin ne détruit pas les entrées antérieures ; ring wrap, producteurs
  concurrents, fermeture pendant attente et drain final testés.
- Imports concurrents ne remplacent pas les fichiers existants ; annulation,
  symlink temporaire, chemin extérieur, Unicode et export absent testés.
- Confirmation ciblée, changement d'ID, session terminée, renommage, titres
  identiques, limites UTF-8 et snapshot du clipboard testés comme logique pure.
- Zoom par pincement et défilement un doigt : code des gestes inchangé ;
  **nouvelle validation physique NOT TESTED**. Plusieurs sessions : tests de
  routage/isolation hôte PASS ; nouvelle validation physique NOT TESTED.
  Retour au premier plan : ownership existant préservé, test physique NOT TESTED.
- FIND et géométrie IME n'ont pas été refondus ; aucun changement de bootstrap
  téléchargé ou de PRoot, uniquement refus de transactions de préfixe dangereuses.
- Aucune réparation supprimant données/bootstrap, aucune permission ajoutée,
  aucun arrêt de PTY à la navigation ou au onCleared de VM.

## F. Risques et limites ouverts

- ANR d'origine non reproduit dans l'APK : pas de trace, pas d'affirmation
  d'absence de freeze. Les callbacks natifs et les Binder réels restent à profiler.
- Ctrl+C peut être refusé lorsque stdin est saturé ; le refus est explicite,
  mais aucune garantie d'interruption immédiate sous cette saturation n'est donnée.
- Les gardes de dispatch testées n'éliminent pas une erreur de composition UI
  non exercée. Doubles clics, navigation simultanée, workspace supprimé et
  rotation doivent être exécutés dans l'APK.
- Un provider peut bloquer dans read/Binder : les checkpoints n'interrompent pas
  magiquement un syscall qui ne revient pas. Un export SAF interrompu peut laisser
  un document partiel ; aucun effacement automatique de document utilisateur.
- Sérialiser les imports de l'app n'empêche pas un processus hostile du même UID
  de remplacer les répertoires parents entre vérification et ouverture/move.
- Plafond de 64 UTF-16/cellule : graphèmes exceptionnellement longs tronqués.
  CLEAR conserve volontairement le viewport ; il purge l'historique seulement.
- PROBABLE : hauteur fixe des raccourcis sous police très agrandie ; UNVERIFIED :
  coût résiduel de callbacks métadonnées/workspace sur UI. Pas de refonte spéculative.
- Aucun processus ne peut être promis survivant après force-stop, mort du
  processus ou reboot. Ne jamais présenter des sessions mortes comme restaurées.
- Packaging de paquets Termux : préfixe guest attendu via PRoot ; l'absence de
  Termux installé et l'isolation réelle de l'APK restent à vérifier sur appareil.

## G. Matrice adversariale et procédure Pixel

Chaque ligne ci-dessous a un résultat attendu. « NOT TESTED » n'est jamais PASS.
Les seules preuves disponibles sont précisées ; exécuter les lignes Android
sur des sessions jetables explicitement créées pour le test.

| Scénario | Résultat attendu | Résultat / preuve |
|---|---|---|
| Sortie rapide milliers de lignes | Octets/lignes complets ; UI utilisable | 20k natif + 4 MiB queue PASS ; UI NOT TESTED |
| Settings pendant sortie/pkg install | Ouverture réactive, commande/PID continuent | NOT TESTED ; latence NOT MEASURED |
| Navigation pendant PTY, feuilles successives | Aucun gel, aucun callback sur mauvaise session | Policy PASS ; UI NOT TESTED |
| Collage pendant read/cat | Une transaction, refus explicite si plein | Émulateur/queue PASS ; read APK NOT TESTED |
| CLEAR saisie partielle/longue/Unicode | Saisie et curseur préservés | Émulateur PASS ; Readline réel NOT TESTED |
| Ctrl+C/Ctrl+D/Ctrl+Z | SIGINT/EOF/suspension cohérents | NOT TESTED dans APK ; SIGTERM/SIGHUP natifs PASS |
| nano/vim/alternate | CLEAR conservateur ; aucune corruption | Alternate émulateur PASS ; éditeurs APK NOT TESTED |
| Home/retour, retrait récents | Même PID tant que processus Android survit | NOT TESTED |
| Rotation/thème/clavier ouvert-fermé | Même PTY, saisie conservée, taille PTY cohérente | Resize émulateur PASS ; Android NOT TESTED |
| Pression mémoire/force-stop/reboot | Pas de perte fichier ; pas de fausse restauration | NOT TESTED ; survie non promise |
| Fermer A, activer B, confirmer | Jamais fermer B par confirmation de A | Policy PASS ; UI/rotation NOT TESTED |
| Deux sessions même nom/renommage | Identité indépendante du titre | Policy PASS ; recomposition NOT TESTED |
| Fichier inaccessible/export absent | Erreur, aucun faux succès | Filesystem/streams PASS ; SAF NOT TESTED |
| Permission SAF révoquée/provider lent | Erreur/annulation, fichiers existants intacts | IOException/annulation modélisées PASS ; provider réel NOT TESTED |
| Stockage insuffisant/document partiel | Erreur visible, aucun écrasement utilisateur | Stream ENOSPC modélisé PASS ; disque physique NOT TESTED |
| Import concurrent/Unicode/hidden/symlink | Noms distincts, limites et refus sûrs | Filesystem JVM PASS ; provider Android NOT TESTED |
| Installation interrompue/archive corrompue | Échec explicite ; sauvegarde et original intacts | Extracteur/transitions JVM PASS ; apt interrompu APK NOT TESTED |
| Verrou dpkg/signature/TLS/réseau interrompu | Pas de faux succès ni réparation destructive | Update/réinstallation/verrou frontend/dépôt injoignable guest PASS ; signature invalide/interruption/APK NOT TESTED |
| Commande risquée/clipboard modifié | Aperçu exact, confirmation snapshot, pas d'exécution implicite | Policy PASS ; Android NOT TESTED |
| Plugin invalide/session fermée | Refus sans réattribution ni exécution inattendue | Dispatch session PASS ; plugin complet NOT TESTED |
| TalkBack/focus/clavier matériel | Commandes accessibles, pas de lecture du flux entier | NOT TESTED |
| Grandes polices/paysage | Contrôles lisibles et terminal utilisable | NOT TESTED |
| Plusieurs sessions et sortie prolongée | Isolation, ressources stables, reprise cohérente | Stress court/file/FD PASS ; endurance APK NOT TESTED |

Préparation : appareil Pixel autorisé ADB, APK app/test signés identiquement,
sessions de test et fichiers sentinelles distincts. Enregistrer version, build,
SHA APK, PID/session, écran, logcat/ANR et métriques monotoniques. Exécuter le
robot smoke puis les tests instrumentés et les scénarios manuels. Ne pas lancer
force-stop, fermer une session ou supprimer un fichier personnel pour tester.
Les scénarios destructifs éventuels exigent un environnement de test dédié.

## H–I. CI et artefacts

[Workflow Android build — succès](https://github.com/ja3kies/Klyro-Shell/actions/runs/43409919352), commit source `3d0b447`.
Tests JVM 263 + 34, Python 506 et native JNI PASS ; lint PASS avec **45 warnings
et 11 hints** (pas une validation TalkBack). Debug ARM64, APK instrumenté et
release compilés. Les rapports documentaires suivants ne changent pas ces APK.

- [APK ARM64 signé Klyro](https://github.com/ja3kies/Klyro-Shell/actions/runs/43409919352/artifacts/14028892679)
- [APK instrumenté signé avec le même certificat](https://github.com/ja3kies/Klyro-Shell/actions/runs/43409919352/artifacts/14029112245)
- [XML des tests JVM](https://github.com/ja3kies/Klyro-Shell/actions/runs/43409919352/artifacts/14029087360)
- [Rapport lint](https://github.com/ja3kies/Klyro-Shell/actions/runs/43409919352/artifacts/14028634465)
- [Audit ELF 16 KiB](https://github.com/ja3kies/Klyro-Shell/actions/runs/43409919352/artifacts/14028643464)

Ces artefacts CI ont une rétention de 14 jours. Copies locales conservées dans
`.local/p0/ci-artifacts/`. App `com.klyro.shell`, version `0.4.9-dev` / code 16,
minSdk 26, targetSdk 36, ABI unique arm64-v8a. Signature v2/v3 vérifiée de nouveau
localement pour les deux APK ; certificat SHA-256 commun :
`01a7e05326237fdfa4cda54e75fb8164cbe12bbc964d36e2d9c2d1a8ef7e5c14`.

APK app SHA-256 : `80e2d088e81b9219ef992d7a2fddd342d8935accbefc895978ff299e14129145` (10544922 octets).

APK tests SHA-256 : `07b070bb81eea970d60faff2fe2c6fc497abdeff34e8b2ebbe7405e40a530836` (369413 octets).

Les sept bibliothèques .so de l'APK signé ont PT_LOAD p_align ≥ 16384,
audit strict local PASS. L'hôte a des pages de 4096 octets : exécution sur
appareil 16 KiB **NOT TESTED**. Aucune permission ajoutée au manifeste ; la
permission privée de receiver générée par AndroidX reste présente dans l'APK.

Revue lint P0 : avertissement ImplicitSamInstance sur stopService(Intent)
UNVERIFIED/faux positif probable (l'API prend Intent, pas un listener SAM).
MutableCollectionMutableState concerne palette/workspaces ; pas de refactor
sans reproduction. Dépendances, ressources inutilisées et KTX restent hors
périmètre. Le rapport complet est conservé, aucun warning supprimé pour masquer
un résultat. Les quatre permissions Android déclarées restent inchangées.

[Empreintes et liens machine](validation/p0-reliability/artifacts.json).
[Résultats machine](validation/p0-reliability/results.json).


## J. Préparation P1 (aucune implémentation)

- Mesurer les budgets de rendu et Settings après la validation physique P0.
- Étendre le confort du scrollback et des grandes polices selon les résultats.
- Étendre la matrice d'outils/workflows après fermeture des blocages runtime.

## Retour arrière

Le point de départ exact est conservé : `022d758` (source distante choisie),
les commits de chaque cycle restent indépendants. Pour examiner un rollback,
créer un worktree/une branche neuve à ce commit ; ne jamais reset le checkout
courant, effacer HOME/bootstrap ou rétrograder une installation avec données
sans procédure adaptée. Le commit UX historique `6120431` reste une référence
antérieure, pas la source de cette campagne. Aucun rollback automatique exécuté.
