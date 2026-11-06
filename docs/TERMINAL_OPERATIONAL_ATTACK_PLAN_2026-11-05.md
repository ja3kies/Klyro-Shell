# Klyro terminal — plan d'attaque opérationnalité v0.4.0-dev

**Objectif final :** terminal Android utilisable sans ordinateur :
session PTY indépendante, Bash/proot, commandes, package manager, entrées
interactives, fichiers, persistence, sécurité, erreurs récupérables.

## Lot A — Sûreté et instrumentation (en cours)
1. Compilations sur SHA exact : tests JVM, lint, debug+release, NDK arm64,
   manifest, signature, audit ELF 16 Kio.
2. Sécurité clipboard : toute entrée du collage natif passe par limites et filtre ;
   les multilignes déclenchent une confirmation explicite ; jamais exécution
   implicite de plusieurs commandes.
3. Correction branding : 108dp adaptatif, 78% glyph foreground/mono, pas de
   masque dessiné ; test XML automatisé, puis Pixel Launcher.
4. Versions distinctes à chaque jalon substantiel (`0.4.0-dev`, code 7).
5. Audit de chaque écart des six rapports, classer compilé / dispositif / ouvert.

## Lot B — P0 sur appareil (AUCUNE validation actuellement)
Installer APK signé **sans désinstaller** ; préserver les données. Puis
exécuter dans Klyro même le smoke test non destructif :
- `bash --version`, `id`, `pwd`, `tty`, `stty size`;
- `echo`, pipelines, redirections temporaires, chmod/exécution,
  SIGINT Ctrl+C, Ctrl+D ;
- ANSI/Unicode/couleurs, historique, pressions clavier et Gboard/IME ;
- `apt --version`, `pkg --help` sans mettre à jour les index ;
- deux sessions : cwd indépendants, copier/coller autorisé, fermé l'une ;
- orientation, thème, sortie d'arrière-plan, retour depuis Home ;
- vérifier qu'aucun chemin hôte écrit dans la sandbox réelle Termux.
**STOP immédiat** sur erreur native, disparition de session, violation
de chemins, corruption, freeze ou crash. Capturer seulement statut/stack
expurgé et processus concernés. Pas de `pkg upgrade` non autorisé.

## Lot C — Correction opérationnelle
Reproduire chaque échec du lot B ; isoler couche : Java/Kotlin UI, Termux
TerminalView, emulator, PTY natif, proot ELF loader, bootstrap, APT.
Introduire test minimal couvrant la cause ; corriger sur branche dédiée ;
relancer CI exacte ; vérifier sur appareil et comparer au témoin antérieur.
Ne pas échanger le moteur Termux par un fake shell.
Ordre: PRoot Bash > saisie PTY > fichiers/exec > package tools >
multi-session > ANSI/IME > persistance.

## Lot D — Robustesse / continuité
- Installer: déplacer le Thread brut vers un contrôleur lifecycle-aware,
  garantir rollback et reprise si Activity détruite/processus tué.
- Sessions: foreground service conforme Android 17 et notification, seulement
  après audit restrictions et approbation (nouvelle permission/service).
- Tests rotation/absence de réseau/espace saturé, installation interrompue,
  terminal avec programmes plein écran (`vim`, `top` si installés).
- Tests accessibilité TalkBack / grosse police / clavier externe.
- Tests stockage: import/export SAF, permissions revocables.

## Lot E — Promotion (accord humain requis)
Rapport final par SHA : commandes, sorties PASS/FAIL, screenshots pertinents,
signatures, limites connues. Proposer PR depuis la branche de test vers la
branche d'intégration, mais ne fusionner ni publier sans autorisation.
Rollbacks : revenir à commit+artefact antérieurs ; pas d'uninstall effaçant
le profil et pas de downgrade Android forcé.

### Mesure de progrès
- **Lot A** peut être prouvé par GitHub Actions.
- **Lot B** dépend du téléphone. La CI ne peut pas exécuter un vrai Pixel 9.
- **Lot C-D** débloqués par les résultats B et décisions sensibles.
