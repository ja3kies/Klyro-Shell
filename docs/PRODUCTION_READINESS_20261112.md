# Production readiness — 12 novembre 2026

**Qualification : NOT READY.** Le runtime et les workflows ci-dessous ont été
exécutés dans un rootfs neuf et isolé sur le Pixel 9 hôte (Android API 37, ARM64, pages de 4096 octets), sous l'UID Termux de build/test. Le démarrage dans
l'UID de l'APK Klyro sans Termux et la persistance Android n'ont pas été validés.
Ces deux preuves restent indispensables avant READY FOR DAILY TRIAL.

Branche : `feat/production-ready-terminal`, issue de
`p1/ux-cross-screen-final-pass-20261110` au commit `7bfdae9`.
Arbre initial propre ; ni main ni la branche PRoot locale modifiées. Aucun merge,
tag, release, installation forcée d'APK ou effacement de données utilisateur.
Les installations et suppressions de paquets concernent uniquement le rootfs de
QA sous `.local/production/guest-complete`.

## Architecture vérifiée et décision runtime

Le chemin actif est MainActivity → KlyroSessionRuntime (singleton de processus) →
TerminalSessionViewModel → TerminalSessionManager → PRoot embarqué dans l'APK →
Bash privé, avec le JNI de `terminal-emulator/src/main/jni/termux.c`.
Le second adaptateur `PtyProcess`/`pty_bridge.cpp` est ancien et n'est pas utilisé
par ce chemin ; il n'a pas été refondu.

**Décision : conserver un userland Termux privé sous PRoot**, avec les chemins
compilés d'origine visibles dans le guest. Les sources des binds usr/home sont
les répertoires de Klyro, sous `com.klyro.shell/files/termux`, pas les données de
l'application Termux. Le cache APT absolu est lui aussi créé dans ce rootfs.
HOME, PREFIX, PATH, TMPDIR et LD_PRELOAD du guest sont construits explicitement ;
l'environnement hôte du processus est vidé et les dépendances PRoot proviennent
de l'APK et du stockage privé Klyro. Les binds système fournissent Android,
/dev, /proc et /sys. PRoot n'est pas une frontière de sécurité supplémentaire :
la frontière vis-à-vis des autres applications est l'UID/sandbox Android.

Les exécutables, shebangs, RUNPATH, scripts dpkg et configurations des paquets
Termux contiennent `/data/data/com.termux/...`. Un remplacement textuel de ce
préfixe ou un changement d'applicationId ne les relocalise pas. La correspondance
PRoot conserve ces chemins sans lire le répertoire privé d'une autre application.
Les tests ont utilisé le vrai extracteur et constructeur de commande Java, le
bootstrap épinglé et les PRoot/libtalloc/libandroid-shmem téléchargés pour ce test.
Aucun exécutable ou bibliothèque du préfixe Termux installé n'a été bindé au guest.
Le harnais de build/test tourne toutefois dans Termux : cela **ne démontre pas**
que les politiques SELinux/exécution de l'APK cible SDK 36 seront satisfaites. Le téléphone est bien un Pixel 9 ; aucune connexion ADB n'autorise ici le pilotage de l'APK Klyro.

Le bootstrap `bootstrap-2026.10.25-r1+apt.android-7` a été téléchargé : SHA-256
`964e0473a9fa881600927c0151b99896089fb595b81afdcd74b1a6743b8b5889` vérifié.
Le démarrage à froid a exécuté les vrais scripts de seconde étape puis imprimé
les marqueurs Bash/chemins/UID/APT/pkg. `tools/prepare_proot_runtime.py` vérifie
InRelease par gpgv, SHA-256 de l'index, versions et SHA-256 des paquets ; il
prépare les mêmes assets localement et en CI, sans modifier l'APT de l'hôte.
Un InRelease altéré a été rejeté avec **BAD signature**.

Pour les CLI effectivement testés, les dépôts Termux ARM64 existants suffisent
dans ce guest ; aucun dépôt Klyro ni paquet recompilé n'a été requis dans ce test.
Klyro reste dépendant de la disponibilité et des signatures de ces dépôts. Un
paquet épinglé indisponible fait échouer le build plutôt que changer de version.
Les paquets qui dépendent de l'application, des services, signatures, add-ons ou
API Android de Termux ne sont pas déclarés compatibles. Une reconstruction pour
un préfixe réellement différent reste un chantier distinct si l'on abandonne
PRoot. Il faut alors reconstruire aussi les dépendances et fournir un dépôt signé.

Sources de référence : [préfixes Termux](https://github.com/termux/termux-packages/wiki/Termux-file-system-layout),
[restrictions Android d'exécution](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission),
[outil pkg amont](https://github.com/termux/termux-tools/blob/master/scripts/pkg.in),
[gestion des tracees PRoot](https://github.com/termux/proot/blob/master/src/tracee/event.c).
Le contexte SELinux de Klyro doit être testé, pas déduit du succès dans Termux.

## Audit initial et priorités traitées

| Priorité | État constaté | Correction / état final |
|---|---|---|
| P0 | APT update répond, installation/upgrade échouent : cache absolu absent | Création de `data/data/com.termux/cache/apt/archives/partial` dans le rootfs privé ; transactions réelles relancées |
| P0 | Six heures sans toucher l'écran peuvent tuer tous les jobs | Suppression du délai destructeur ; fermeture explicite et contraintes Android seules déterminent la durée |
| P0 | stdin borné à 4096 octets écrit de façon bloquante depuis le thread UI | File non bloquante de 128 KiB ; refus atomique signalé à l'utilisateur en cas de saturation |
| P0 | Descripteur PTY partagé puis fermé par plusieurs propriétaires | Duplicata distinct par stream via ParcelFileDescriptor ; fermeture unique du master ; retrait de la réflexion sur FileDescriptor |
| P0 | SIGKILL du groupe PRoot laisse potentiellement des jobs dans d'autres groupes | SIGQUIT pour laisser PRoot terminer ses tracees, secours SIGKILL borné ; `--kill-on-exit` sur sortie du shell |
| P0 | Une réparation efface l'ancien préfixe, qui peut contenir des projets | Conservation de `/usr.previous`, restauration sur échec, refus d'écraser une sauvegarde existante |
| P1 | waitpid peut être interrompu ; erreur native mal interprétée | Réessai EINTR, erreurs explicites, fermeture des descripteurs sur échec de création |
| P1 | Sonde de vérification tuée brutalement sur timeout | PID conservé par exec dans un wrapper système ; nonce vérifié avant SIGQUIT et secours borné |
| P1 | JNI modified UTF-8 casse les arguments/env/chemins avec emoji | Encodage UTF-8 réel avant exec ; rejet des NUL ; test JNI réel |
| P1 | Fin de process peut devancer sa dernière sortie ; fermeture différée peut viser un PID déjà récolté | Drain borné de sortie avant notification ; état de récolte publié par le waiter avant le drainage |
| P1 | Collage bracketed envoyé en trois morceaux | Envoi atomique préfixe/texte/suffixe ; aucune demi-séquence si l'entrée est refusée |
| P1 | Export limité au nom fixe klyro-export.txt ; noms importés dégradés | Choix d'un chemin HOME validé, export binaire, restauration du choix après rotation ; espaces, Unicode courant et noms cachés conservés, contrôles normalisés |
| P1 | setup-storage interrompu peut abandonner une demande | Trap de nettoyage sur sortie et signaux, limité à son token ; test avec vrai Bash interrompu |
| P0 validation | UID Klyro, bootstrap sans Termux, exécution sous SELinux cible 36 | NOT TESTED sur appareil |
| P1 validation | FGS, arrière-plan, retrait des récents, rotation, IME, mémoire, SAF/révocation | NOT TESTED sur appareil |
| P2 | Ancien adaptateur PTY compilé, outils/API Termux non couverts | Conservés hors du chemin actif ; pas de refonte ni de promesse de compatibilité globale |

Une erreur de mise à niveau a aussi été reproduite : le harnais avait remplacé
sources.list puis ne pouvait répondre au prompt de conffile dpkg. Cet état
partiellement configuré a été réparé réellement avec
`dpkg --force-confold --configure -a`, `apt-get -f install`, puis upgrade en
conservant ce fichier. Le harnais final conserve la configuration initiale.
Klyro ne masque pas les prompts et n'impose pas cette politique aux utilisateurs.
Le test a tenu le vrai verrou POSIX `lock-frontend` : APT échoue tant qu'il est
occupé, puis réussit après libération, sans supprimer le verrou.

## Preuves exécutées

Résultats structurés : [results.json](validation/production-20261112/results.json).
Les journaux bruts, y compris les tentatives échouées et leur relance, restent
sous `.local/production`. Le rapport ne remplace pas ces journaux.

- 69 contrôles CLI/paquets/workflows : PASS lors de la passe finale.
- 12 contrôles PTY interactifs : PASS ; ils couvrent read Unicode, EOF/cat,
  Ctrl+C, Ctrl+Z/fg, resize réel, nano, vim, less, top, séquences ANSI truecolor,
  arrêt d'un job détaché et huit entrées de sessions simultanées distinctes.
- Test natif C : vingt créations PTY, isatty sur stdin/stdout/stderr, statut de
  sortie, waitpid interrompu et signal. Test JNI Java/C réel : argv/env/cwd avec
  emoji/espaces, rejet NUL. PASS.
- Tests JVM Gradle : application et émulateur exécutés ; compilation Kotlin/Java
  et compilation des quatre tests instrumentés Android réalisées localement.
  Voir les comptes finaux et la CI dans le relevé de build ci-dessous.
- 192 tests JVM application et 14 tests émulateur : PASS.
- 479 tests Python : PASS ; mélange de tests de logique/harnachement et contrats
  de source existants. Ces derniers ne prouvent pas un comportement physique.
- Métadonnées du dépôt signées vérifiées ; métadonnées altérées rejetées.
- Wrapper de sonde réel exécuté : PID conservé par exec, nonce retrouvé et arrêt SIGQUIT borné : PASS.
- OpenSSH loopback : vraies clés éphémères, contrôle strict de la clé hôte, authentification et commande distante : PASS.
- `git diff --check`, syntaxe du script natif et compilation Python : PASS.

Le build JVM local utilise Gradle 8.13, SDK 36 et une surcharge locale NDK 28.2
pour configurer le SDK disponible ; aucune version NDK du projet n'a été changée.
La compilation APK de référence utilise la CI standard avec Gradle 8.11.1,
SDK 36, NDK 27.0.12077973, CMake 3.22.1. L'audit ELF 16 KiB et la vérification de
signature font partie de cette CI. Les APK applicatif et instrumenté sont signés
avec le même certificat lorsqu'il est disponible.

### Outils : installation et exécution séparées

| Outils | Installation | Exécution vérifiée |
|---|---|---|
| Bash, coreutils, findutils, grep, sed, gawk | pkg install : PASS | versions, commandes, pipeline/redirections : PASS |
| tar, gzip, unzip, zip, rsync | PASS | archive, extraction, copie et comparaison de noms Unicode : PASS |
| curl, wget | PASS | HTTPS réel et erreurs réseau : PASS |
| ping | fourni par termux-tools ; installation du paquet vérifiée | wrapper vers `/system/bin/ping`, version et localhost : PASS ; pas de binaire privé d'une autre app |
| openssh | PASS | ssh -V, authentification par clé et commande SSH en loopback : PASS ; serveur distant externe : NOT TESTED |
| Git | install, uninstall puis réinstallation : PASS | clone HTTPS public, édition, commit local et branche : PASS ; push distant : NOT TESTED |
| Python, python-pip | PASS | dépendance PyPI packaging 24.2 et petit programme : PASS |
| Node.js, npm | PASS | dépendance npm is-number 7.0.0 et petit programme : PASS ; scripts npm tiers désactivés pour ce test |
| clang, make, pkg-config | PASS | compilation/exécution C et détection zlib : PASS |
| nano, vim, less, procps/top | PASS | versions et commandes interactives sous PTY : PASS |

Les installations ont réellement téléchargé/décompressé/configuré des paquets et
dépendances. Les répétitions sur paquets déjà installés sont identifiées comme
telles dans les logs APT. Aucun script n'annonce une installation simulée.
Les extensions natives Python/npm complexes, cross-compilation et paquets hors
Termux main ne sont pas qualifiés par ces petits projets.

### Scénarios d'acceptation

| # | Scénario | Rootfs/PTY hôte | APK / Pixel 9 |
|---:|---|---|---|
| 1 | Installer sans Termux | rootfs indépendant construit, bootstrap vérifié | NOT TESTED |
| 2 | Obtenir Bash au lancement | PASS | NOT TESTED |
| 3 | pwd, ls, cd, mkdir, touch, cat, echo | PASS | NOT TESTED |
| 4 | Installer Git, cloner, modifier | PASS | NOT TESTED |
| 5 | Python et script | PASS | NOT TESTED |
| 6 | Node.js et npm | PASS | NOT TESTED |
| 7 | nano/vim interactifs | PASS | NOT TESTED |
| 8 | read, cat, pipelines, redirections | PASS | NOT TESTED |
| 9 | Ctrl+C | PASS | NOT TESTED |
| 10 | Sessions indépendantes | PASS, jusqu'à huit entrées distinctes | NOT TESTED |
| 11 | Quitter/rouvrir l'interface sans perdre les processus | non applicable au harnais CLI | NOT TESTED |
| 12 | Stockage Android autorisé | règles privées/copie testées en JVM | NOT TESTED pour SAF |
| 13 | Téléchargement Internet | PASS | NOT TESTED |
| 14 | Erreurs réseau/installation | PASS : erreurs, signatures, verrou et configuration dpkg | NOT TESTED |
| 15 | Usage durable sans crash/perte de données | tests courts uniquement | NOT TESTED |

## Limites et vérifications restantes

Un service foreground ne garantit pas la survie sous pression mémoire, arrêt par
le gestionnaire Android, force-stop ou redémarrage. Les sessions en mémoire ne
sont pas restaurables après destruction du processus. Aucun nouveau wake lock :
la suspension CPU/Doze et l'autonomie restent à mesurer. Références :
[cycle de vie des processus](https://developer.android.com/guide/components/activities/process-lifecycle),
[arrêt utilisateur d'un service foreground](https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping).

Restent NOT TESTED : vrais démarrages propres de l'APK sans Termux, durée de
plusieurs heures, arrière-plan/récents, orientation et clavier, notification FGS
refusée, pression mémoire, SAF multi-provider et révocation, TLS invalide sur
l'APK, coupure réseau au milieu d'un téléchargement, builds natifs de dépendances
Python/npm et accès SSH distant. Les permissions Android n'ont pas été élargies.
SAF reste un import/export, pas un montage POSIX ; limite 32 MiB/fichier, import
au plus 30 fichiers de premier niveau, export d'un fichier privé à la fois.
Une erreur de provider pendant l'export peut laisser un document partiel : l'erreur
est signalée, sans annoncer le succès ni supprimer un document de l'utilisateur.

La file stdin a un plafond 128 KiB par session (clipboard limité à 64 KiB avant
encapsulation). Une saisie qui ne tient pas est refusée intégralement avec un
message ; les anciens octets en attente ne sont pas effacés. Sous saturation,
les caractères de contrôle dans stdin peuvent attendre le consommateur ; la
fermeture explicite d'une session reste disponible.

Les gestionnaires de pinch, scroll à un doigt, raccourcis/navigation, sélection,
palette et FIND dans TerminalPane/OrynTerminalViewClient n'ont pas été modifiés.
Le collage ne change que son atomicité ; les tests conservent les filtres de
contrôle et le protocole bracketed paste. Revalider physiquement ces interactions
après la modification des streams PTY et du cycle de fermeture. Le fonctionnement sur un système à pages 16 KiB reste NOT TESTED ; seul l'alignement ELF de l'APK est audité pour ce format.

### Procédure physique sans effacer les données

1. Avant une mise à jour APK, fermer volontairement les sessions de travail : Android peut arrêter le processus pendant l'installation. Sur un appareil/profil de test propre ARM64 sans Termux, installer l'APK et
   réaliser 1–10 avant d'autoriser le niveau DAILY TRIAL. Ne pas désinstaller le
   Termux personnel du Pixel pour ce test.
2. Installer le test APK signé avec le même certificat. Si l'installation refuse
   le certificat, conserver les données et utiliser la bonne paire ; ne pas
   contourner le problème par une désinstallation.
3. Exécuter `adb shell am instrument -w com.klyro.shell.test/androidx.test.runner.AndroidJUnitRunner`.
   Les quatre tests créent uniquement leurs propres PTY `/system/bin/sh` et
   répertoires temporaires dans l'UID Klyro ; ils ne testent pas le bootstrap.
4. Dans deux shells Klyro, lancer des compteurs et noter les PID. Quitter vers
   Home, fermer la tâche des récents, rouvrir, puis tourner l'écran et ouvrir/
   fermer le clavier : vérifier PID, saisie, dimensions `stty size` et fichiers.
   Observer plusieurs heures et tester séparément une pression mémoire contrôlée.
5. Autoriser un dossier SAF, importer noms Unicode/espaces et .gitignore,
   exporter un fichier binaire choisi, comparer les octets ; révoquer le grant
   et vérifier une erreur explicite, puis reconnecter. Répéter avec rotation
   pendant le picker et interruption de setup-storage.
6. Vérifier ensuite 11–15, stabilité, récupération et quatre gestes historiques.
   Ne pas faire de force-stop sur des sessions de travail. Sur une installation
   de test, un force-stop/redémarrage doit être présenté comme une perte des
   processus, avec conservation des fichiers, sans faux rétablissement de shell.

### Reproduction et rollback

- `CC=clang sh scripts/test-native-pty.sh` avec JAVA_HOME (JDK).
- `gradle :app:testDebugUnitTest :terminal-emulator:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` avec le SDK/NDK épinglé.
- `python3 tools/prepare_proot_runtime.py .local/runtime-payload-neuf` pour les
  assets authentifiés ; copier jniLibs/assets générés comme le fait la CI.
- Compiler `tests/runtime/BootstrapFixture.java` contre les classes Java de
  l'application, de l'émulateur et android.jar, puis lancer son main avec un
  répertoire neuf, le ZIP bootstrap vérifié et les jniLibs préparées.
- `python3 tools/verify_runtime_fixture.py .local/chemin-fixture` puis
  `python3 tools/verify_runtime_pty.py .local/chemin-fixture`. Les scripts refusent
  un rootfs hors `.local` ; ne pas leur donner le HOME réel d'une application.
- Pour rollback source : sauvegarder le diff local, puis checkout détaché de
  `7bfdae9`, ou révoquer les commits ciblés sur une branche séparée. Aucun reset
  --hard ni changement de main requis. Réinstaller une APK précédente seulement
  avec le certificat compatible et sans désinstallation/effacement de données.
  Conserver et archiver `/usr.previous` avant toute réparation avec un ancien APK,
  dont le code pourrait encore supprimer cette sauvegarde.

## Comparaison limitée aux preuves

| Capacité | Klyro dans cette validation | Termux installé |
|---|---|---|
| Userland Bash/Git/Python/Node/outils | PASS dans le rootfs privé de QA | sert d'hôte de build ; pas de benchmark comparatif exécuté |
| Paquets signés et upgrade | PASS dans ce rootfs | mêmes dépôts amont utilisés ; aucun paquet personnel de l'hôte modifié |
| PTY, job control, éditeurs | PASS dans le harnais et JNI hôte | pas de comparaison physique des deux UI |
| Stockage Android et services/add-ons | code partiel ; limites SAF documentées ; NOT TESTED sur APK | API/intégration Termux ne sont pas reproduites globalement |
| Persistance Android et usage quotidien | NOT TESTED, bloquant pour DAILY TRIAL | aucune nouvelle mesure comparative |

L'objectif suivant est la validation dans l'UID Klyro et sur appareil. Les preuves
actuelles permettent d'identifier et corriger les blocages reproduits ; elles ne
permettent pas d'annoncer un remplacement de Termux.

## Relevé final de build

- Code APK : `40733ce2ec749690413fef0abeffa2116bcd1a64` ; branche `feat/production-ready-terminal`.
- [CI finale réussie](https://github.com/ja3kies/Klyro-Shell/actions/runs/43197678741) : JVM, tests natifs, lint Android,
  assembleDebug, assembleDebugAndroidTest et assembleRelease, vérification des
  assets et de l'ABI ARM64, audit ELF 16 KiB, signatures et upload des deux APK.
- APK applicative : `klyro-shell-signed.apk` (10524442 octets), SHA-256
  `d838c88378361136cce0d8d682e4383b0dfde72d8890f16fa7bfea486bf6dc62`.
- APK instrumentée : `klyro-shell-tests-signed.apk` (365259 octets), SHA-256
  `51d9c0240c8c2310012b7b64a89325d3a75e63888e166b18b502c5fa89742ffe`.
- Signature v2/v3 vérifiée de nouveau après téléchargement. Même certificat :
  `01a7e05326237fdfa4cda54e75fb8164cbe12bbc964d36e2d9c2d1a8ef7e5c14`.
- Sept bibliothèques .so : alignement PT_LOAD >= 16384, aucune inconnue ou
  incompatible dans l'audit local de l'APK finale. Le Pixel hôte utilise des
  pages 4096 : l'exécution de l'APK sur pages 16 KiB reste NOT TESTED.
- Les quatre tests instrumentés sont livrés et compilés, **non exécutés**.
  Aucun scénario d'acceptation de l'APK n'est transformé en PASS par la CI.

Les APK téléchargées se trouvent sous `.local/production/apk-final` et
`.local/production/apk-tests-final`. L'archive de preuves locale est
`.local/production/klyro-production-evidence-20261112.zip` ; son contenu exclut
les projets et données personnelles. Les artefacts GitHub sont conservés 14 jours.
