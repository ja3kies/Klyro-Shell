# Klyro — Freeze forensics et UX functional completion

**Statut recommandé : TEST BUILD.** Le freeze Pixel de 1 500 lignes est signalé
sur appareil mais sa cause reste **UNVERIFIED**. Aucun ANR système n'a été fourni
pour cet incident. Une CI verte ne prouve ni sa résolution ni l'UX physique.

Base propre : ecdf89c, branche source fix/p0-reliability-closure. Travail sur
fix/freeze-forensics-ux-completion ; aucun merge/main/PRoot modifié, aucune donnée,
installation ou session utilisateur supprimée. Code des artefacts finaux : d634b4d (convergence + frontière mémoire + timeline exportable).

## A. Six cycles

| Cycle | Travail substantiel / preuves | Commit |
|---|---|---|
| 1 — recorder | Détection indépendante, persistence privée, notification/recovery, ANR historique, page/export ; 30 nouveaux cas engine/store + 8 ring, deux tests instrumentés fournis | [8ccd4d3](https://github.com/ja3kies/Klyro-Shell/commit/8ccd4d3) |
| 2 — foreground | Frontières lifecycle/PTY/UI, StrictMode opt-in ; 7 nouveaux cas adversariaux + 8 ring réexécutés ; audit parsing/exit drain | [ebea2d2](https://github.com/ja3kies/Klyro-Shell/commit/ebea2d2) |
| 3 — Home/Settings | 15 contrôles documentés, host de rapports sans double sheet courante, preview borné ; 3 tests preview, 2 Compose fournis | [379de90](https://github.com/ja3kies/Klyro-Shell/commit/379de90) |
| 4 — terminal | 15 interactions auditées, garde FIND/native paste, hauteur min accessible, 3 Compose fournis ; policies/moteur existants conservés | [84cedf3](https://github.com/ja3kies/Klyro-Shell/commit/84cedf3) |
| 5 — données | 15 contrôles workspaces/SAF/plugins/a11y, feedback provider confidentiel, annulation multi-grants ; 8 nouveaux cas d'erreur | [653fe51](https://github.com/ja3kies/Klyro-Shell/commit/653fe51) |
| 6 — convergence | 15 écrans revus, 7 tests nouveaux, stress PTY exact, garde des preuves critiques et reprise de temporaires, version 17 | [7090ad7](https://github.com/ja3kies/Klyro-Shell/commit/7090ad7), [e562032](https://github.com/ja3kies/Klyro-Shell/commit/e562032), [d634b4d](https://github.com/ja3kies/Klyro-Shell/commit/d634b4d) |

Ce registre distingue audits démontrés, corrections et tests créés/exécutés.
Il ne transforme pas les 15 écrans en 15 défauts, ni les tests Compose compilés
en manipulations Android. Les validations physiques empêchées restent ouvertes.

## B–D. Inventaire, causes prouvées et correctifs

L'[audit fonctionnel](UX_FUNCTIONAL_AUDIT.md) donne les 15 contrôles des cycles
3, 4, 5 et 6, écran/composant/preuve/décision. Les fonctions correctes n'ont pas
été réimplémentées. Le [rapport forensics](FREEZE_FORENSICS.md) détaille le flux
et les limites ; les 93 validations P0 antérieures restent conservées.

| Défaut / statut | Preuve et cause | Correctif / test |
|---|---|---|
| Freeze Pixel — UNVERIFIED | Témoignage utilisateur, pas de stack exportée de cet APK | Recorder livré ; aucun faux RESOLVED |
| Diagnostic par modale seule — CONFIRMED limitation | Un main bloqué ne peut pas composer une modale | Worker + capture + notification best effort ; recovery UI différé |
| Double capture trop rapprochée — FIXED | Tick retardé pouvait franchir 5 et 10 s ensemble | Séparation réelle minimale 5 s ; ConvergenceTest |
| Perte de timeline critique — FIXED | Ring uniforme évincé par rafale PTY/IME/resize | Ring critique séparé 32, compteurs et ring 128 ; flood test |
| Détails volumineux — FIXED | 512 KiB pouvaient entrer dans Text/Compose | Preview 8192 UTF-16, export complet ; PreviewTest |
| Timeline saine absente de l'export — FIXED dans le code | STARTED était persistant mais pas les transitions ultérieures sans freeze | Snapshot manuel IO avant affichage/export ; heartbeat last/max avec garde de génération ; aucun faux END/PASS |
| Statut recovery caché — FIXED | Fin du rapport après la coupure du preview | Summary du dernier state ; ConvergenceTest |
| Erreur mémoire du collector — FIXED dans le code | OOME pouvait annuler la tâche périodique ScheduledExecutor | Frontière OOM explicite, tâche suivante conservée ; test instrumenté d'injection fourni (NOT TESTED) |
| Temporaires après interruption — FIXED | Fichiers .pending non listés mais pouvant s'accumuler | Reprise startup avant publication, noms dédiés uniquement ; ConvergenceTest |
| Export bloquant le store — FIXED | Un provider lent sous verrou retarderait les snapshots | Snapshot borné puis ZIP hors verrou ; RuntimeAdversarialTest |
| Rapports superposés — FIXED dans le code | Requête globale alors que Settings était déjà affiché | Settings absorbe la requête ; cas rotation/navigation physique UNVERIFIED |
| Scroll FIND obsolète — FIXED dans le code | View attachée ne suffit pas après changement d'actif | ID et identité View/session vérifiés ; UI différée à valider |
| Paste natif obsolète — FIXED dans le code | Client vivant ne prouve pas View/session active | UI active/View attachée/currentSession exigées avant lire clipboard |
| Touches à taille imposée — FIXED dans le code | 50dp coupe les grandes polices | Hauteur minimale/intrinsèque, 3 tests Compose compilés, non exécutés |
| Message provider sensible — FIXED | it.message pouvait contenir URI/chemin/credentials | Messages contrôlés et type inattendu ; 8 tests StorageFailure |
| Annulation multi-grants — FIXED | catch Exception pouvait continuer après cancellation | ensureActive avant nouvelle requête et dans catch ; checkpoints P0 conservés |
| MutableCollection lint — clarification | Helpers retournaient déjà copies/read-only mais type platform ambigu | List Kotlin explicite ; pas présenté comme trois bugs |

## E. Freeze Recorder : architecture et confidentialité

- Singleton de processus ; un worker scheduled distinct du main looper, aucune
  possession de PTY. Un seul ping en attente, acquittement par génération.
- Temps monotone ; warm-up premier foreground, heuristiques STALL 2 s, FREEZE
  5 s, deuxième snapshot espacé d'au moins 5 s ; recovery avec durée observée.
- PAUSE_BEGIN instrumente la transition ; PAUSED/STOPPED acquis désactivent les
  alertes normales de background. Les pings d'anciennes générations sont ignorés.
- 24 threads/64 frames sérialisées maximum ; noms arbitraires de threads exclus.
  Les stacks Java ne suffisent pas à prouver un deadlock natif/JNI/Binder.
- Métadonnées PID/version/lifecycle, IDs techniques, nombre de sessions, service,
  compteurs d'octets/callbacks/batching, curseur/scrollback et géométrie. Les délais last/max de pings acquittés excluent les anciennes générations. Backlog
  approximatif : bytes lus moins parsés ; pas une mesure de charge complète.
- noBackupFilesDir, 10 incidents / 512 KiB, fsync et rename atomique ; report
  tronqué explicitement, temporaires invalides non présentés, récupération au boot.
- Jamais transcript, commandes/arguments, env, tokens, mots de passe, clipboard,
  contenu de fichier ou URI détaillée collectés automatiquement. Pas de réseau.
- ApplicationExitInfo API >=30 hors UI ; cursor dédupliquant, frames Java filtrées
  seulement. REASON_ANR → SYSTEM_ANR_CONFIRMED ; autres → PROCESS_EXIT_OTHER.
  Trace absente explicitée ; pas de trace brute système dans les ZIP.
- Notification sous permission/canal existants, PendingIntent explicite immutable,
  jamais d'ouverture d'Activity automatique. Notification non garantie si OS/Binder
  ou processus l'empêche. Refus de permission ne bloque pas la persistence.
- Recovery/redémarrage : avis acquittable, détails et ZIP SAF volontaires, suppression
  d'un rapport confirmée. Aucun effacement de document utilisateur en cas d'échec.

Limites : démarrage avant premier RESUMED, blocage alors que PAUSED/STOPPED déjà
acquis, suspension VM entière, stockage bloqué ou mort immédiate du processus
peuvent empêcher une capture. STALL/FREEZE sont heuristiques, pas ANR officiels.
getAllStackTraces peut allouer une map complète et suspendre la VM avant les bornes de sérialisation ; son coût dans l'APK n'est pas mesuré. OOM est traité explicitement pour ne pas perdre la tâche périodique, sans garantie de capture sous pression mémoire globale.
Un provider peut bloquer dans read/Binder ; l'export annulé peut laisser un ZIP
partiel. Relire le diagnostic technique avant partage.

## F. Investigation du freeze réel

Cause racine **UNVERIFIED**. Aucune stack physique de l'incident CI 43409919352
n'est disponible. Analyse du chemin Home/pause/stop/service/start/resume/Compose :
le parser/reflow et certains callbacks restent sur main, indépendamment du batch
Compose. La boucle de drain à la fin d'un processus reste un risque plausible
non attribué à cet incident ; aucune réécriture spéculative du PTY.

Le script exact StabilityScenario.COMMAND a tourné dans un **vrai PTY du guest
PRoot jetable** : 1 500 lignes, END présent, exit 0 en 4,012 s (inclut le délai
volontaire 3 s). Ce test n'exécute ni Activity Android, ni Home/retour.
Le diagnostic de l'APK doit maintenant être exporté sur Pixel pour attribuer le gel.

## G. Tests et mesures

Validation locale disponible :

- 326 tests JVM app, 34 émulateur, zéro échec ; 63 nouveaux cas app de cette campagne.
- 506 Python PASS, dont contrats structurels clairement distincts des preuves comportementales.
- Core diagnostics réexécuté directement : 55 tests PASS.
- Compilation des tests instrumentés/Compose PASS ; **exécution NOT TESTED**.
- PTY/JNI natifs hôte Android ARM64 PASS : 20 créations, isatty 3 fd, exit,
  EINTR, SIGTERM/SIGHUP, UTF-8/env/cwd ; 20 000 lignes sans perte, FD 4 → 4.
- Stress 1 500 lignes exact via guest/PTY PASS ; aucun processus utilisateur concerné.
- Robot et am instrument tentés : aucun appareil ADB autorisé, zéro screenshot,
  zéro assertion UI ; aucun PASS physique.
- Diff check et état Git contrôlés ; compilation ARM64/signature/lint/ELF : voir CI finale ci-dessous.

| Mesure disponible | Résultat | Portée |
|---|---|---|
| 100k événements | 13 481 160 ns | Noyau Java hôte, hors UI APK |
| 100 snapshots de ring | 62 209 473 ns | Même harness, pas deux stacks système |
| 10 écritures atomiques | 159 997 152 ns | Fichiers privés jetables |
| Export ZIP vers mémoire | 64 753 214 ns, 2432 octets compressés | Pas un provider SAF |
| PTY natif 20k lignes | 0,097377 s / 128 894 octets | Harness Android hôte, pas rendu APK |
| Démarrage/reprise/Settings/latence APK | NOT MEASURED | Appareil requis |
| Frames lentes/recomposition/CPU/GC/mémoire APK | NOT MEASURED | Pas de profilage réel |
| Export SAF réel | NOT MEASURED | Pas de provider appareil exécuté |

Aucune comparaison avant/après de performances sans protocole comparable.
Le recorder ajoute un heartbeat léger et des compteurs, pas un profiler réseau.
StrictMode I/O est debug et opt-in ; politique restaurée à la fermeture du contrôle.

## H–J. Matrice, restant et Pixel

[Matrice adversariale détaillée](validation/freeze-ux/adversarial-matrix.json) :
préconditions, étapes, attendu, observé, statut, preuve et correctif pour chaque
scénario. [Matrice écrans](UX_FUNCTIONAL_AUDIT.md) : Home, Terminal, Sessions,
Workspaces, Settings, Appearance, Theme/Color picker, Files, Packages, Plugins,
Palette, Find, Diagnostics et Freeze reports.

Bloqués sans appareil : freeze artificiel/réel dans l'APK, recovery, notification
refusée, import ANR réel, relance processus, Home/récents, rotation/IME/paysage,
grands caractères, TalkBack/focus/clavier matériel, SAF/révocation/provider lent,
réseau/installation APK, navigation rapide et coût réel du recorder.

Les tests Compose de suppression et raccourcis existent mais ne sont pas marqués
PASS. Prévoir une installation de test dédiée ; ne pas injecter de stall sur une
session contenant un travail utilisateur. Les tests de stall refusent des sessions
vivantes. Ne pas supprimer de données personnelles pour préparer ces tests.

Reste également : course de répertoires parents manipulés par un processus du
même UID (limite SAF P0 conservée), impossibilité de promettre la survie après
force-stop/reboot, et absence de capture garantie sous arrêt VM/stockage bloqué.
Les observations visuelles en clair/sombre/monochrome/paysage n'ont pas été faites.

## K. Guide sans ADB

1. Installer la build de test signée sans effacer les données. Vérifier version
   0.4.10-dev / code 17 et conserver ses tâches usuelles intactes.
2. Settings → Diagnostics → Freeze reports. Les rapports précédents sont locaux.
3. Choisir Start isolated 1,500-line test. Une session supplémentaire est créée ;
   aucune commande n'est envoyée aux sessions existantes. Prévoir une place parmi 8.
4. Pendant le délai de 3 s : Home → autre application → retour Klyro. Observer
   START, les lignes et END dans la session Stability test.
5. Après retour, ouvrir Freeze reports puis Refresh/Export : la timeline récente et les délais de heartbeat du test sont persistés sur IO, même si aucun freeze n'a été détecté. Un état OBSERVED ne vaut pas réussite du test. En cas de freeze, attendre si possible sans force-stop immédiat : le worker
   tente les captures. Notification seulement si Android et sa permission le permettent.
6. Après recovery, View / Export diagnostic ; sinon après relance retourner aux
   rapports. SYSTEM_ANR_CONFIRMED est distinct des heuristiques UI_FREEZE_DETECTED.
7. Export ZIP, choisir un document via SAF, attendre le message de résultat.
   Une annulation n'est pas un succès ; un export interrompu peut être partiel.
8. Relire puis partager volontairement le ZIP. Supprimer un rapport seulement via
   Delete report puis confirmation. Fermer la seule session de test explicitement
   après inspection ; aucune fermeture automatique de tâche n'est faite.
9. Les boutons Test 6s / 11s stall sont debug-only, volontaires et gardés sans
   sessions ouvertes ; Android peut tuer le processus. Ne pas les utiliser pendant
   un transfert important. Un rapport STARTED ne prétend pas un test terminé PASS.

## L–M. CI et APK

[CI Android finale — SUCCESS](https://github.com/ja3kies/Klyro-Shell/actions/runs/43530165958), source `d634b4d`.
326 tests app, 34 émulateur, 506 Python, native PTY/JNI, lint, debug/test/release
ARM64 PASS. AndroidTest et Compose compilés, jamais déclarés exécutés.
Le commit documentaire final ne change pas ce code.

- [APK ARM64 signé Klyro](https://github.com/ja3kies/Klyro-Shell/actions/runs/43530165958/artifacts/14087296767)
- [APK instrumenté signé avec le même certificat](https://github.com/ja3kies/Klyro-Shell/actions/runs/43530165958/artifacts/14087496298)
- [XML tests JVM](https://github.com/ja3kies/Klyro-Shell/actions/runs/43530165958/artifacts/14087765568)
- [Rapport lint](https://github.com/ja3kies/Klyro-Shell/actions/runs/43530165958/artifacts/14087146940) : 45 warnings et 11 hints, classés dans l'audit UX ; pas de suppression artificielle.
- [Audit ELF 16 KiB](https://github.com/ja3kies/Klyro-Shell/actions/runs/43530165958/artifacts/14087785752) : 7 bibliothèques, aucun incompatible/inconnu ; audit strict local répété sur l'APK signé PASS.

App com.klyro.shell, version 0.4.10-dev / code 17, minSdk 26, targetSdk 36,
ABI arm64-v8a. Signatures v2/v3 revalidées localement. Certificat SHA-256 commun :
`01a7e05326237fdfa4cda54e75fb8164cbe12bbc964d36e2d9c2d1a8ef7e5c14`.

APK app SHA-256 : `874614e2a896991efe9f0e2df7ac28cf863e9d94700695026b3a10fd199c6976` (10614712 octets).

APK tests SHA-256 : `183936e8021e28342a9c974b8b4bf58499745b45f529c435432084510c670d95` (1096091 octets).

Les permissions fusionnées de l'APK sont identiques à celles du P0, y compris
la permission privée générée AndroidX. L'hôte utilise des pages de 4096 octets ;
exécution sur appareil 16 KiB NOT TESTED. Artefacts CI retenus 14 jours ; copies
locales dans .local/freeze/ci-delivery-artifacts/. Seuls les APK signés Klyro
ci-dessus sont les livrables, les variantes debug-key ne sont pas proposées
comme mises à jour de l'installation utilisateur.

[Empreintes/liens machine](validation/freeze-ux/artifacts.json).
[Résultats/JUnit et 63 nouveaux cas](validation/freeze-ux/test-results.json).


## N. Permissions et dépendances

Aucune modification des permissions Android déclarées ni du targetSdk 36.
POST_NOTIFICATIONS est réutilisée, aucun accès storage large ou nouvelle permission.
PRoot, bootstrap et packages utilisateur ne sont pas changés.

BuildConfig activé pour version et DEBUG. Ajouts de test uniquement :
androidTestImplementation androidx.compose.ui:ui-test-junit4 et debugImplementation
androidx.compose.ui:ui-test-manifest (hôte des tests Compose). Pas de nouvelle
dépendance runtime release, pas de JankStats ou SDK de télémétrie réseau. Le debug
contient l'Activity vide de test Compose standard ; comparer manifestes des APK
lors de la validation finale. Version augmente de 16/0.4.9-dev à 17/0.4.10-dev.

## O. Recommandation et retour arrière

**TEST BUILD**, pas DAILY TRIAL ni PRIMARY USE. Le recorder est compilé et ses
composants sont testés hôte ; le freeze Pixel et l'UX physique restent UNVERIFIED.

Base ecdf89c conservée ; les six commits sont thématiques. Pour examiner un
retour arrière, créer un nouveau worktree/une branche à ecdf89c, sans reset ni
nettoyage HOME/bootstrap/PRoot. Ne pas rétrograder une installation avec données
sans procédure adaptée. Aucun rollback, merge ou publication exécuté.
