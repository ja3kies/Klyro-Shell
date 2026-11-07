# CAVEMAN — Objectifs F et G (06 novembre 2026)

**Version de test : 0.4.2-dev / versionCode 9.** Branche isolée
`fix/klyro-fg-operability-20261106`, dérivée des corrections précédentes.
Le code est en attente de CI et de tests physiques. **Aucun résultat de
GitHub Actions n'est un certificat de fonctionnement réel de Bash sur Pixel 9.**

## F — 16 corrections concrètes du démarrage et du PTY

| # | Correction appliquée | Source |
|---|---|---|
| F01 | Nonce unique pour chaque vérification Bash : les anciennes sorties ne peuvent être réutilisées | TerminalSessionManager.freshVerificationMarker |
| F02 | Encodage du nonce en majuscules, conforme au validateur `[A-Z0-9_]` | TerminalSessionManager |
| F03 | `KLYRO_PATHS_OK` nécessaire pour considérer Bash lancé | TerminalSessionManager.verifyBash |
| F04 | Vérifie `HOME` égal au HOME virtuel Klyro | TermuxBootstrapInstaller.buildCommand |
| F05 | Vérifie `PREFIX` égal au préfixe virtuel isolé | idem |
| F06 | Vérifie `TMPDIR` égal au tmp du préfixe | idem |
| F07 | Vérifie que le répertoire HOME existe | idem |
| F08 | Vérifie que le répertoire PREFIX/bin existe | idem |
| F09 | Vérifie que TMPDIR existe réellement | idem |
| F10 | Vérifie que le Bash privé est exécutable | idem |
| F11 | Refuse un UID root ou non numérique comme validité UID | idem |
| F12 | Refuse les éléments nuls / avec NUL dans argv avant JNI | TerminalSessionManager.buildTerminalArgv |
| F13 | Refuse un client terminal nul avant la création du PTY | TerminalSessionManager.startBash |
| F14 | Limite et normalise le nom de session transmis au moteur | TerminalSessionManager.startBash |
| F15 | Définit UTF-8 explicitement dans l'environnement hôte | TerminalSessionManager.buildHostEnvironment |
| F16 | Vérifie le format et l'unicité du nonce, les marqueurs complets et argv | TerminalSessionManagerTest |

**P0 appareil encore requis** : `bash --version`, `tty`, `stty`, UID réel,
APT/pkg, prompt/Unicode, Ctrl+C, multi-PTY, HOME/PREFIX, aucune écriture
dans les données de Termux. Voir `scripts/klyro-operational-gate.sh`
et `docs/QA_PIXEL9_0_4_1_DEV.md`.

## G — 20 corrections concrètes de résilience et diagnostics

| # | Correction appliquée | Source |
|---|---|---|
| G01 | Sérialise les vérifications sur un même manager | TerminalSessionManager.verifyBash |
| G02 | Réduit la capture des logs de sonde à 16 Kio/flux | TerminalSessionManager |
| G03 | Supprime les caractères de contrôle des détails affichés | TerminalSessionManager.safeDiagnostics |
| G04 | Masque les chemins Android privés dans le diagnostic de Bash | KlyroDiagnostics |
| G05 | Supprime le stdout temporaire si création stderr échoue | TerminalSessionManager.verifyBash |
| G06 | Signale si les journaux temporaires ne peuvent pas être nettoyés | idem |
| G07 | Utilise un verrou global process pour bootstrap | TermuxBootstrapInstaller.install |
| G08 | Utilise un verrou global process pour PRoot | ProotRuntimeInstaller.install |
| G09 | Empêche les clics concurrents install / recheck | MainActivity |
| G10 | Réattache un nouvel Activity à une installation en cours après rotation par observation | MainActivity.observePreviousInstall |
| G11 | Ne relance pas un second PRoot si le premier tourne encore | MainActivity.installProotRuntime |
| G12 | Ignore les callbacks d'une Activity détruite | MainActivity install listeners |
| G13 | Borne le téléchargement à 4 minutes (+ timeout socket) | TermuxBootstrapInstaller |
| G14 | Gère proprement l'interruption du téléchargement | idem |
| G15 | Borne l'extraction à 8 minutes | idem |
| G16 | Vérifie interruption pendant extraction | idem |
| G17 | Refuse les lignes de manifeste PRoot démesurées | ProotRuntimeInstaller.readManifest |
| G18 | Refuse les chemins de manifeste dupliqués | idem |
| G19 | Vérifie interruption du hash et des étapes PRoot | ProotRuntimeInstaller.verifyFile |
| G20 | Affiche le type d'erreur utile sans stack trace ni chemin personnel | KlyroDiagnostics + MainActivity |

**Ce qui reste ouvert** : survie à une mort complète du processus Android
(pas de foreground service durable), reprise d'un bootstrap coupé lors
d'un crash, tests d'orientation, UI/TalkBack, état d'installation sans réseau
et validation de fichiers réels sur Pixel. Aucun de ces points n'est déclaré
PASS DEVICE. Un service permanent demande autorisation et audit Android 17.

## Validation reproductible

CI demandée sur SHA exact : tests JVM app+emulator, Python, lint, debug et
release, test APK, signature, alignement ELF 16 Kio. L'environnement de la CI
est Linux x86_64 avec assets ARM64 : il ne remplace pas un appareil Android.
Scripts device non destructifs déjà versionnés. Si CI échoue, corriger la
cause avant de diffuser le nouvel APK.

**Rollback** : revenir au dernier commit/artefact signé validé, sans supprimer
l'application ni écraser la branche stable. Aucun merge automatique.
