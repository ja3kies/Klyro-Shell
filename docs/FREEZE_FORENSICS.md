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
