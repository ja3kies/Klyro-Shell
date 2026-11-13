# Campagne P0 — progression

Source distante : `022d758`, branche `fix/p0-reliability-closure`.
Le dépôt initial était propre. Les validations de novembre 12 sont conservées
comme antérieures, pas comptées comme actions de cette campagne.

Flux actif : Bash → PRoot → slave/master PTY (JNI termux.c) → lecteur ByteQueue
→ Handler/TerminalEmulator (thread principal) → OrynTerminalSessionClient
→ rafraîchissement TerminalView + notifications Compose regroupées à 120 ms.
Le regroupement Compose ne ralentit ni le lecteur PTY ni ses octets.

## Cycle 1 — implémentation et tests hôte terminés

Défauts CONFIRMED par code et cas ciblés : la suppression du dernier identifiant
pending permettait de programmer un second timer pendant que le premier restait
actif ; SELECT allouait toujours le transcript entier avant de le borner ; une
cellule acceptait une quantité illimitée de caractères combinants.

Correctifs : ownership explicite du timer, suffixe de transcript borné avant
allocation, plafond 64 unités UTF-16 par cellule pour le texte combinant.
Preuves comportementales : dix scénarios OutputUpdateBatchTest (horloge virtuelle
120 ms, rafale 10 000 chunks, dernière notification, fermeture, réentrance,
exception), cinq OutputBoundsTest (scrollback 50 000 lignes, UTF-16 et flood
100 000 caractères combinants). 15 nouveaux scénarios comportementaux : PASS dans les deux tâches JVM Gradle. La CI finale doit encore confirmer le lot.
Ces tests ne constituent pas une mesure de latence Settings ou un test Android.

P0-06 à P0-10 : UNVERIFIED, réservés aux cycles 4–6. ANR utilisateur original :
UNVERIFIED sans trace. Appareil ADB : aucun connecté au début de campagne.

## Cycle 2 — implémentation et tests hôte terminés

CONFIRMED : CLEAR injectait du texte de contrôle dans le parseur partagé avec
le PTY, et tentait de reconstruire Readline à partir d'un prompt visuel imitable.
Correction : purge directe du transcript, viewport/cursor/parser/stdin préservés ;
refus des sessions terminées et de l'alternate screen. Changement conservateur
intentionnel : CLEAR n'efface plus le viewport d'un programme dont l'état est inconnu.

15 tests comportementaux TerminalDisplayActionsTest : terminal absent, émulateur
absent, prompt vide, saisie partielle, multiligne, curseur déplacé, ligne repliée,
prompt usurpé, alternate screen, CSI incomplète, OSC incomplète, UTF-8 fractionné,
bracketed paste, historique saturé, isolation et resize. PASS JVM ; 506 contrats
Python PASS. Tests physiques nano/vim/rotation encore UNVERIFIED.

## Cycle 3 — tests hôte terminés, lifecycle Android bloqué

15 nouveaux scénarios BootstrapAdversarialTest : ZIP absolu, traversal, lecteur
Windows, traversal backslash, NUL, parent symlink, archive interrompue, exécutable
Unicode, données non exécutables, staging fichier, staging symlink, prefix symlink,
backup dangling, sauvegarde existante et staging absent. PASS dans Gradle JVM.
CONFIRMED : activatePrefix acceptait un staging fichier et la récupération
acceptait un lien de sauvegarde. Refus avant toute mutation, sauvegardes intactes.

Le singleton et le service existants conservent l'ownership des PTY ; aucune
navigation, expiration ni destruction d'Activity ne doit appeler closeTab.
Les observations physiques antérieures du rapport daily-trial sont conservées
comme antérieures. Home/récents/pression mémoire/force-stop : PENDING DEVICE
VALIDATION, pas RESOLVED. Aucun appareil ADB connecté ; ne fermer aucune session
utilisateur pour fabriquer une preuve. P0-06 à P0-10 restent UNVERIFIED à ce stade.

## Cycle 4 — transferts et intégrité des fichiers

15 scénarios FileTransferAdversarialTest : annulation avant lecture/après lecture,
provider sans progrès, interruption, document vide, lectures courtes, lecture
révoquée, ENOSPC simulé au stream, huit commits concurrents, fichier utilisateur
existant, temporaire extérieur, temporaire symlink, temporaire absent, nom caché
Unicode et export absent. PASS JVM (ce ne sont pas des providers Android).

CONFIRMED : la copie ne consultait pas l'annulation du propriétaire avant de
committer ; la sélection du nom précédait la copie et pouvait devenir obsolète
entre deux imports ; un stream retournant constamment zéro provoquait une boucle.
Correctifs : checkpoints du contexte coroutine, commit local sérialisé avec
nouvelle sélection du nom, refus d'un stream sans progrès, ouverture export
NOFOLLOW sur le fichier final. Aucun document utilisateur supprimé en cas d'erreur.
Les courses avec un processus hostile remplaçant les répertoires parents dans le
même UID ne sont pas prétendues éliminées par ce verrou applicatif.
P0-06/07/09 : encore UNVERIFIED ; P0-08 : protections hôte testées, SAF physique
PENDING DEVICE VALIDATION.

## Cycle 5 — identité des actions, callbacks et accessibilité

15 scénarios SessionDispatchTest : cible active/inactive/terminée, absence de
cible/onglet, zéro/négatif, ID Long, snapshot confirmé, fermeture obsolète,
callback destructif terminé, ANSI, UTF-16 invalide, séparateur invisible, borne
en octets. PASS JVM ; tests instrumentés existants compilés, non exécutés.

CONFIRMED : la confirmation End utilisait onClose() visant l'onglet actif au
moment du clic, et l'affectation workspace relisait également cet onglet courant.
Les callbacks UI provenant d'un ancien pane pouvaient encore envoyer des entrées
à son PTY inactif. Correctifs : cible Long sauvegardée, identité courante relue
au dispatch, IDs explicites pour close/workspace et même garde pour touches,
presse-papiers natif, confirmation et palette. Le titre de confirmation nomme la
cible ; aucune refonte visuelle.

CONFIRMED : les OSC title avaient une file UI non regroupée. Réutilisation du
batch testé à 120 ms, dernière valeur conservée et publication différée après
pause. Les protections de viewport/accessibilité n'ajoutent aucun lecteur du
transcript entier. TalkBack réel, grandes polices, focus après rotation et
contrastes affichés : PENDING DEVICE VALIDATION. Hauteur fixe des touches sous
police fortement agrandie : PROBABLE risque, non présenté comme résolu.
Les anciens tests de chaînes restent des contrats structurels, jamais des
preuves de comportement ; ils sont complétés par les tests de dispatch réels.


## Cycle 6 — saturation, intégration et recherche de régressions

18 scénarios comportementaux : six PasteDeliveryTest (enveloppe atomique,
refus sans demi-enveloppe, mode normal, CRLF, filtrage ESC/C1, UTF-8 exact), neuf
QueueAdversarialTest (producteurs concurrents, fermeture producteur/consommateur,
refus surdimensionné, ring wrap, préservation des octets en attente, drain final,
4 MiB avec backpressure, bornes invalides), plus refus explicite du sink, renommage et titres identiques dans
SessionDispatchTest. La palette propage désormais l'accusé de réception de la
file stdin ; elle ne mémorise plus une insertion refusée comme réussie.
L'API publique void paste(String) reste compatible avec TerminalView.
Suppression de la branche CLEAR devenue inaccessible.

Test natif réel sur l'hôte Android ARM64 : 20 créations PTY, trois descripteurs
TTY, code de sortie, attente interrompue EINTR, SIGTERM, SIGHUP hérité ignoré,
20 000 lignes sans perte, même nombre de descripteurs avant/après ; JNI UTF-8,
arguments, environnement et cwd Unicode/espaces. SIGHUP est réinitialisé dans
l'enfant. Reproduction comparative : le même harness compilé avec termux.c
du commit da3b430 échoue (enfant toujours vivant après SIGHUP, puis nettoyage
de son seul groupe de test) ; le code corrigé passe. Défaut CONFIRMED dans
cette condition d'héritage, sans prétendre avoir observé ce cas dans l'APK Pixel.
Mesure finale seq : 128 894 octets, 0,044688 s, maxrss du harness 3540 KiB,
FD 4 → 4. Ce n'est pas un benchmark du rendu de l'application. Les anciens
essais avec printf externe par ligne sont exclus des mesures comparables.
Le premier test HUP exigeait uniquement WIFSIGNALED ; le shell peut aussi
rapporter 128+SIGHUP. Les deux résultats sont acceptés et vérifiés.

Robot Android et instrumentation tentés : BLOCKED, aucun appareil ADB autorisé
(`adb: no devices/emulators found`). Aucun screenshot, aucune assertion UI
exécutée. Les cas réels SAF, rotation, TalkBack, clavier et lifecycle restent
PENDING DEVICE VALIDATION ; aucune session utilisateur fermée.

Les six cycles ont leurs corrections et scénarios ciblés ; cela ne clôture pas
les dix objectifs P0 sur appareil. La CI, les artefacts et la matrice finale sont
référencés dans P0_RELIABILITY_CLOSURE.md après leur exécution.

Revue transversale : la confirmation de KlyroSessionSheet était mémorisée par
nom de session, réutilisable entre deux IDs portant le même nom. Elle stocke
maintenant l'ID cible et utilise la même garde de dispatch ; le renommage ne
réattribue plus la confirmation. Les deux cas ont une vérification de politique
comportementale ; la recomposition réelle reste à tester sur appareil.

Contrôle additionnel du guest PRoot jetable existant : dpkg --audit vide,
apt-get check réussi, exécutables Bash/Git/Python/Node/npm/curl/nano/vim présents,
versions Git/Python/Node/npm exécutées (exit 0, 7,17 s). Ce contrôle ne réexécute
pas leurs installations réseau et ne valide pas l'isolation APK sans Termux.
