# Freeze forensics — incident Pixel 9, Android 17

Incident signalé par l'utilisateur sur l'APK CI 43409919352 : sortie 1 500
lignes, Home, autre app, retour ; interface figée près du marqueur P0-STRESS.
C'est un freeze signalé, pas un ANR officiellement confirmé. La CI P0 verte
n'en prouvait pas la résolution. Cause racine de cet incident : UNVERIFIED.

Base propre ecdf89c ; branche fix/freeze-forensics-ux-completion ; aucun reset,
merge, nettoyage de données/PRoot ou arrêt de session utilisateur.

## Cycle 1 — recorder

Machine FreezeWatchdog pure Java : ping unique en attente, génération lifecycle,
horloge elapsedRealtime, grâce initiale 1 s, STALL à 2 s, snapshot heuristique à
5 s, seconde capture à 10 s, RECOVERED acquitté avec durée monotone. PAUSED et
STOPPED invalident le ping ; un ancien acquittement ne récupère pas un nouveau
blocage. Un singleton de processus évite un watchdog par rotation.

FreezeRecorder : thread dédié, Handler main uniquement pour les pings, stacks
capturées hors UI, 24 threads/64 frames au maximum sérialisées. La capture Java
ne permet pas nécessairement d'expliquer JNI/Binder. Thread names arbitraires,
transcript, commandes/arguments, env, fichiers, clipboard et URI ne sont pas
collectés. Les breadcrumbs sont des enums et nombres uniquement.

FreezeReportStore : noBackupFilesDir/freeze-reports, écriture fsync + rename
atomique, 10 incidents, 512 KiB chacun, troncation UTF-8 explicite. Les temporaires
et fichiers invalides ne sont pas présentés. Export ZIP volontaire via SAF ;
suppression d'un seul rapport confirmée. Un provider lent n'immobilise pas le
verrou du store pendant son écriture. Erreurs exposées dans l'UI, jamais dans la
logique PTY. Aucun upload réseau.

ApplicationExitInfo API >=30 : lecture hors UI, import des seuls événements du
processus, curseur dédupliquant, état SYSTEM_ANR_CONFIRMED pour REASON_ANR,
PROCESS_EXIT_OTHER autrement. Trace absente explicitée ; seules les lignes de
frames Java bornées sont importées, pas le dump brut potentiellement sensible.
Une absence de trace ne réfute pas un freeze. Les rapports non recovered après
redémarrage ne prétendent pas connaître la raison de fin du processus.

Notification avec permission existante seulement, canal distinct, PendingIntent
immutable et explicite ; aucune ouverture automatique d'Activity. Notification
best effort, refus sans crash attendu. Après recovery/redémarrage, avertissement
unique acquittable et ouverture volontaire des rapports. Settings → Diagnostics
→ Freeze reports donne détails, export, suppression et test volontaire debug.

Tests comportementaux ajoutés : 15 machine watchdog + 15 store ; 8 ring/counters
concurrents. Deux tests instrumentés bloquent volontairement le main thread
6,5 s / 11 s dans le processus de test et exigent une capture avant recovery,
puis RECOVERED ; deux stacks pour le blocage persistant. Non exécutés sans ADB.

## Limites à conserver

Seuils heuristiques, pas définitions Android ANR. Une pause de toute la VM ou une
surcharge système peut être indiscernable d'un stall UI ; les stacks/événements
sont des observations, pas une preuve de cause. Pas de garantie de snapshot si
le processus est tué, la VM entièrement suspendue ou le stockage bloqué. Les
appels Binder de notification/historique sont isolés du heartbeat ; queue bornée.
L'export SAF interrompu peut laisser un document partiel, jamais supprimé sans
consentement. Le ZIP contient des métadonnées/stacks techniques ; le relire avant
partage. Les traces système natives brutes sont volontairement exclues.

## Protocole sans ADB

Dans une build diagnostic : Settings → Diagnostics → Freeze reports → Start
isolated 1,500-line test. Une nouvelle session reçoit uniquement le script fixe,
après consentement par le bouton ; aucun travail existant ne reçoit de commande.
Délai de préparation 3 s, START, 1 500 lignes, END. Home → autre application →
retour. Les transitions et compteurs monotones sont enregistrés. Le rapport de
lancement indique Completion=NOT_VERIFIED : chercher le END marqueur, ne pas
interpréter l'absence de freeze détecté comme PASS. Fermer ensuite cette seule
session de test explicitement si souhaité. Export ZIP via SAF, sans ADB.

Les boutons de stall artificiel sont debug-only et indisponibles avec des
sessions ouvertes ; Android peut terminer le processus. Jamais de stress au
lancement. Toute validation physique reste NOT TESTED ici tant qu'ADB/appareil
n'est pas accessible et qu'aucun rapport de l'APK n'a été exporté.

## Références consultées

- [ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo)
- [PendingIntent](https://developer.android.com/reference/android/app/PendingIntent)
- [Accessibilité Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)

Résultats de compilation/CI, commits et matrice consolidés dans
KLYRO_UX_FREEZE_COMPLETION.md en fin de campagne. Les preuves P0 antérieures
restent dans P0_RELIABILITY_CLOSURE.md, sans être converties en preuves physiques.

## Cycle 2 — reprise et frontières critiques

Instrumentation sans texte : lifecycle, première frame Compose, callbacks PTY,
rafraîchissements View, batches, sélection de session, workspace, taille View,
IME et navigation Settings/rapports. Compteurs volatile des octets lus/parsés
sans verrou de buffer ; backlog approximatif = différence positive (inclut un
chunk éventuellement bloqué à l'enqueue). L'écran et le service sont des états
techniques, pas des titres/chemins. Le switch StrictMode debug opt-in détecte
I/O UI sans journaliser les messages/arguments de violations ; politique
restaurée en quittant les contrôles. Pas de JankStats/télémétrie ajoutée.

Audit démontré : parser append et reflow appartiennent toujours au main looper ;
callbacks suspendus en background n'arrêtent pas ce parsing. Le handler de sortie
n'est donc pas exclu par les seuls tests de batching. L'exit-drain possède une
boucle jusqu'à file vide : risque plausible, pas cause confirmée de cet incident.
Aucune réécriture spéculative des PTY. Settings ne relit plus PackageManager pour
la version : BuildConfig fournit la valeur. Les blocs IO/PM déjà déplacés en P0
restent déplacés ; pas de restauration simulée ni d'arrêt de session au retour.

15 contrôles ciblés de ce cycle : huit cas de ring/counters concurrents réexécutés
et sept tests adversariaux store/export/erreurs. En particulier un provider ZIP
bloqué n'empêche pas une nouvelle écriture d'incident ; un commit atomique échoué
ne détruit pas un autre rapport. Les anciens tests P0 sont des non-régressions,
pas de nouvelles corrections du freeze. Correction du freeze Pixel : UNVERIFIED.

## Convergence

La seconde capture est espacée d'au moins 5 s depuis la première, même si le
watchdog lui-même se réveille tard. Un ring critique séparé retient lifecycle et
navigation malgré un flood PTY. FIRST_FRAME distingue le callback Compose initial
(a=0) et le callback de frame suivant Resume (a=1) : ce n'est pas une mesure de
fin de dessin ni de frames lentes. Curseur/scrollback ne sont lus qu'au callback
UI déjà existant et copiés en compteurs ; le watchdog ne lit pas le transcript.
Les temporaires d'écriture interrompue sont récupérés avant de publier le store.

Le démarrage avant le premier RESUMED n'est pas diagnostiqué comme freeze UI.
La build 0.4.10-dev / code 17 conserve les mêmes permissions déclarées ; seules
les dépendances de tests Compose et BuildConfig sont ajoutés. Aucune télémétrie.
Le diagnostic est implémenté et testé hôte ; son fonctionnement dans l'APK est
PENDING DEVICE VALIDATION, et la cause du freeze utilisateur reste UNVERIFIED.

onPause marque d'abord PAUSE_BEGIN ; la détection foreground est désactivée
après les opérations de pause/super, afin de pouvoir observer un blocage dans
ces opérations. Un main thread bloqué alors que l'état PAUSED/STOPPED est déjà
acquis peut rester invisible au watchdog jusqu'à un vrai callback Resume ;
l'historique Android est le recours si le système confirme un ANR. L'absence de
rapport n'est donc jamais une preuve d'absence de gel. Les événements IME/resize,
comme les callbacks PTY, ne chassent pas les transitions du ring critique.

Dernière revue : un OutOfMemoryError de getAllStackTraces pouvait annuler
silencieusement la tâche périodique ScheduledExecutor. La frontière d'erreur
retient maintenant le watchdog et expose une indisponibilité mémoire, sans
nouvelle allocation de stack. Un test instrumenté injecte l'erreur (sans consommer
la RAM du téléphone) puis vérifie qu'une tâche suivante peut s'exécuter ; compilé,
à exécuter sur appareil. Pas de promesse de capture sous OOM global.

Dernier contrôle fonctionnel d'export : les événements d'un round trip sain
restaient seulement dans le ring après le rapport STARTED. L'ouverture/Refresh
et l'export des rapports prennent maintenant un snapshot de la timeline du test
courant sur IO, sans polling disque en arrière-plan. Les délais last/max des
heartbeats acquittés sont inclus (pas la durée d'un ping encore bloqué, déjà
mesurée séparément par blocked_ms). Les générations obsolètes ne contribuent pas
à ces métriques. La fin reste NOT_VERIFIED tant que l'utilisateur n'a pas observé
END ; aucun transcript n'est lu pour déduire artificiellement PASS.
