# Modal touch freeze closure — 0.4.11-dev (18)

Statut : **TEST BUILD — PENDING DEVICE VALIDATION**. La violation du contrat de
fermeture est corrigée dans le code. Le blocage tactile rapporté sur Pixel 9 /
Android 17 n'a pas été reproduit ni validé physiquement par l'agent. Aucun ANR
Android n'est déduit de cet incident.

## Cause et preuves

Settings utilisait `onDismissRequest = ::navigateBack`. Depuis Appearance, le
callback ne retirait pas le propriétaire Home/Terminal de la composition : il
changeait seulement la section. Les sources exactes Material3 1.3.1 montrent que
swipe/scrim masquent avant d'appeler ce callback ; la DialogWindow est libérée au
retrait du composable. Le contrat applicatif erroné est **CONFIRMED**. La fenêtre
invisible qui expliquerait le Pixel reste **PROBABLE**, faute de trace système.

Le différentiel physique fourni par l'utilisateur (swipe depuis Appearance
échoue, retour interne puis Close fonctionne) est cohérent avec ce défaut.
Il n'établit pas à lui seul l'état exact de WindowManager/InputDispatcher.

Une seconde course est comprise et testée : une protection apparue pendant
l'animation ne doit pas faire ignorer la fermeture d'une feuille déjà Hidden.
Le veto protège avant Hidden ; une fermeture engagée retire toujours le owner.

## Correctifs et cycles

| Cycle | Livraison substantielle | Preuve / limite |
|---|---|---|
| 1 | Dismiss externe séparé ; propriétaire unique ; gate idempotent/disposable ; protection Workspace, Palette, Session ; drafts Rename/Color ; navigation interne conservée | 8 cas JVM initiaux ; contrat et sources Material examinés ; aucun test Pixel |
| 2 | 18 interactions Compose réelles : Settings/sous-pages, scrim, Back/Close, Workspace, Session, Palette, rapports ; tap sous-jacent via OS InputDispatcher | Compilées ; NOT TESTED sur appareil |
| 3 | Course Hidden couverte ; 14 cas Android adversariaux ; robot corrigé avec tests de résultat ; événements modaux/focus/taps bornés ; test de flood du ring ; validation transversale | 9 cas gate + ring + robot ; 32 cas UI au total ; aucun PID Android déclaré validé |

Le registre détaillé et l'inventaire exhaustif des surfaces sont dans
[MODAL_LIFECYCLE_AUDIT.md](MODAL_LIFECYCLE_AUDIT.md). Les actions y sont distinctes :
aucun simple commentaire/commit n'est compté comme correction ou test physique.
Les objectifs d'implémentation des trois cycles sont livrés ; les actions qui
nécessitent un appareil restent bloquées et ne sont pas converties en PASS.

Modales examinées : Settings (toutes sous-pages), Color/advanced/favorites,
licences, Workspaces/editor/menu/delete, Session/actions/rename/chooser,
Palette/guide/caution/recents, SELECT/FIND/paste/end, outils/long press,
Freeze notice/reports/details/delete/stall/export, dialogues Main/export/repair,
Plugins/import/install/delete et picker SAF. Leurs propriétaires, états enfants,
sorties et limites de restauration figurent dans l'inventaire.

Politique : Retour interne navigue ; Back système/Close/swipe/scrim ferment la
feuille complète. Une édition ou confirmation protégée garde le parent visible
et exige Save/Cancel. Aucun callback ne sert uniquement à naviguer après Hidden.
Aucun restart Activity, suppression de données, PTY reset, délai arbitraire,
polling produit ou injection tactile produit n'est utilisé.

## Validation réellement exécutée

| Vérification | Résultat |
|---|---|
| JVM application | PASS : 336 tests, 0 échec/erreur/skipped |
| JVM terminal-emulator | PASS : 34 tests, 0 échec/erreur/skipped |
| Python outils/contrats | PASS : 507 tests ; les assertions de source ne prouvent pas le tactile |
| Compilation Kotlin/Java AndroidTest | PASS local ; exécution NOT TESTED |
| Harnais natif PTY/JNI | PASS : 20 PTY, 20 000 lignes, signaux, EINTR, UTF-8, cwd espacé/emoji, rejet NUL |
| Descripteurs du harnais | 4 avant / 4 après ; ne démontre pas l'absence de toute fuite dans l'app |
| Device / UI robot / instrumentations | BLOCKED : `adb devices -l` vide, `adb shell am instrument` sans device ; robot INCOMPLETE, 0 assertion |
| Revue diff, permissions, chemins sensibles | PASS : aucun changement Manifest/PRoot/bootstrap/PTY/émulateur ; Freeze Recorder conservé |
| CI, APK ARM64/signatures/ELF | Voir les preuves finales ci-dessous |
| Android lint | PASS : 45 warnings et 11 hints existants, aucun nouveau problème par fichier/message |

Lint : la convention du nom de fixture introduite par cette campagne a été corrigée,
sans suppression. Les avertissements antérieurs (SAM, chemins stockage,
compatibilité SDK/ABI, état, ressources et recommandations KTX) restent classés
dans [l’audit UX précédent](UX_FUNCTIONAL_AUDIT.md) ;
[comparaison détaillée](validation/modal-touch/lint-comparison.json).

Les tests sauvegarde utilisent StateRestorationTester ; le test lifecycle utilise
CREATED → RESUMED. Ils ne remplacent ni rotation réelle ni Home → autre app.
Les fixtures n'ouvrent aucun shell et ne terminent aucun job utilisateur.
Scrim/IME sans précondition disponible sont SKIPPED, pas faussement PASS.

Non-régression : stdin/stdout/Ctrl/signaux sont exercés par les suites JVM et le
harnais natif. Les implémentations FIND, SELECT/PASTE, pinch/scroll, resize IME,
sessions multiples, SAF/packages et reprise n'ont pas été modifiées ; leurs
interactions Android restent NOT TESTED. Aucune continuité de PID sous charge,
aucun accès SAF physique ni aucune sélection tactile n'est affirmé.

## Performance et diagnostic

Dismissal, retour du focus, première interaction, recompositions, latence des
gestes et fenêtres persistantes : **NOT MEASURED** sur Android. Aucun avant/après
n'est inventé. Le débit natif du harnais est enregistré séparément ; ce n'est pas
une mesure de fluidité Compose.

Freeze Recorder conservé. Les nouveaux événements MODAL_OPEN/MODAL_DISPOSE,
WINDOW_FOCUS et TOUCH_DISPATCH contiennent seulement temps monotone, identifiant
technique local ou booléen. Aucun texte terminal, commande, coordonnées, clipboard,
chemin ou URI ajouté. Un flood de taps ne chasse pas les transitions modales du
ring critique ; stockage borné existant conservé. dispatchTouchEvent appelle le
comportement original sans interception.

Un main looper vivant peut accompagner une fenêtre bloquant le toucher : le
watchdog ne garantit donc pas de signaler cet incident. Les événements aident
l'analyse après récupération mais ne remplacent pas une inspection des fenêtres.

## Pixel : scénarios restant à exécuter

Toutes les cases de [dismissal-matrix.json](validation/modal-touch/dismissal-matrix.json)
sont NOT TESTED : six familles × quinze chemins, attendu et préconditions inclus.

A. Sans commande : Settings → Appearance → swipe ; activer les contrôles du
terminal ; rouvrir Settings, retour interne puis Close ; vérifier encore les taps.

B. Avec consentement, Diagnostics → Stability test dans sa session isolée :
1 500 lignes ; Settings → Appearance → swipe ; vérifier taps pendant sortie ;
Home → autre app → retour ; vérifier END, état du processus et diagnostics.
Ne pas utiliser une session contenant du travail utilisateur pour ce stress.

C. Répéter Appearance → Settings → Diagnostics → Freeze reports → Diagnostics →
Settings → fermeture ; recommencer avec swipe direct depuis chaque sous-page.

D. Feuille/dialogue ouvert : rotation, Home/retour, IME ouvert ; fermer ; contrôler
focus, fenêtres, taps, navigation, noms/IDs et continuité des sessions.

E. Feuille → sous-dialogue → Cancel/Close → parent → nouvelle feuille ; répéter
avec double tap, geste interrompu, animation, sombre, grande police et TalkBack.
Aucune confirmation destructive ne doit s'exécuter implicitement.

Pour A–E, consigner préconditions, étapes, attendu, observé, PASS/FAIL, capture ou
export et SHA APK. Sans exécution réelle : NOT TESTED. Le test permanent
`ModalLifecycleUiTest.appearanceSwipeRemovesWindowAndUnderlyingTapExecutes`
vérifie le retrait puis une action sous-jacente passant par InputDispatcher.

Sans ADB : après récupération, ouvrir Settings → Diagnostics → Freeze reports,
consulter les rapports puis Export ZIP via SAF, choisir explicitement un document.
Si aucun freeze heartbeat n'est enregistré, ne pas conclure que le tactile était
fonctionnel. Noter l'heure, la sous-page et le chemin de fermeture ; ne pas effacer
les données pour récupérer. Les rapports se suppriment uniquement volontairement.

## Limites et sécurité de livraison

Aucune dépendance ni permission ajoutée. BOM 2024.10.01 / Material3 1.3.1 conservés.
Version portée à 0.4.11-dev (18). Ni main ni branche PRoot modifiée ; aucune fusion.
Rollback source : créer une branche depuis 4e765b5 ou appliquer les reverts des
commits correctifs ; ne pas utiliser reset destructif ni supprimer HOME/bootstrap.
Un downgrade APK n'est pas garanti par Android : ne pas désinstaller l'application
pour contourner cette restriction et perdre ses données. Aucune APK n’a été installée
par l’agent ; une mise à jour Android peut arrêter le processus applicatif :
sauvegarder le travail en cours avant de l’installer volontairement.

## Commits et artefacts vérifiés

Sources APK : `7c730536d53991eb70d558babc0aa367cf244123`. Le commit documentaire
qui publie ce bilan ne change pas ces sources. Une compilation AndroidTest n'est
jamais présentée comme une exécution UI.

- [4f7a447](https://github.com/ja3kies/Klyro-Shell/commit/4f7a4474f1bb53cbe2b5759546ed356607b2b24e) — fix(modal-cycle1): separate external dismissal from nested navigation
- [cb73a7c](https://github.com/ja3kies/Klyro-Shell/commit/cb73a7c59bca45e9797fdaad4ce903531ea15450) — test(modal-cycle2): verify dismissal through underlying InputDispatcher actions
- [f125c2a](https://github.com/ja3kies/Klyro-Shell/commit/f125c2a7b0edc70f31eef4ee973f4742cbf7aa6e) — fix(modal-cycle3): close committed hidden owners and cover adversarial lifecycles
- [3e12f1e](https://github.com/ja3kies/Klyro-Shell/commit/3e12f1e5b52cd45b0df48d9003ad26ef599a1aba) — test(modals): reach scrolled report and licence controls by their real labels

- [7c73053](https://github.com/ja3kies/Klyro-Shell/commit/7c730536d53991eb70d558babc0aa367cf244123) — fixture Compose renommée, avertissement lint introduit retiré sans suppression.


CI finale : [Android build #43549053497](https://github.com/ja3kies/Klyro-Shell/actions/runs/43549053497) — **SUCCESS**.
JVM/Python/native, lint, assembleDebug, assembleDebugAndroidTest et assembleRelease
ont réussi ; la release compilée ne constitue pas une publication.

- [APK ARM64 signée](https://github.com/ja3kies/Klyro-Shell/actions/runs/43549053497/artifacts/14094693990)
- [APK tests signée avec le même certificat](https://github.com/ja3kies/Klyro-Shell/actions/runs/43549053497/artifacts/14094743807)
- [XML JVM](https://github.com/ja3kies/Klyro-Shell/actions/runs/43549053497/artifacts/14094897989)
- [Rapport lint](https://github.com/ja3kies/Klyro-Shell/actions/runs/43549053497/artifacts/14094569115)
- [Audit ELF CI](https://github.com/ja3kies/Klyro-Shell/actions/runs/43549053497/artifacts/14095182857)

Artefacts GitHub conservés 14 jours ; copies locales dans `.local/modal/final-ci-artifacts`.

APK ARM64 SHA-256 : `7ec2b0ddda0b931a4cb6e78bb8360254be996d07f38dd20b25f4d1498d365bcf`.

APK tests SHA-256 : `373e61c3904987125500ef2e9cc94335d7d21c80085c4cdb7b50e8b2442ad3f9`.

Certificat SHA-256 commun : `01a7e05326237fdfa4cda54e75fb8164cbe12bbc964d36e2d9c2d1a8ef7e5c14`.

Vérification locale `apksigner` v2/v3 PASS pour les deux APK ; ABI arm64-v8a,
version 18 / 0.4.11-dev, target 36, permissions identiques à la livraison précédente.
Audit strict des sept ELF PASS (PT_LOAD ≥ 16 KiB) ; zipalign -c 4 PASS.
Les avertissements compilateur de dépréciation StatusBar/Thread.id et la macro
_GNU_SOURCE concernent des lignes préexistantes inchangées ; aucune suppression.

Preuves lisibles : [résultats](validation/modal-touch/test-results.json),
[métadonnées/empreintes](validation/modal-touch/artifacts.json),
[signatures](validation/modal-touch/app-signature.txt),
[ELF](validation/modal-touch/native-16kb.md). Archive locale :
`.local/modal/modal-touch-evidence.zip` (logs, XML CI, sources de référence, rapports).

Conclusion : livraison technique **TEST BUILD**. La résolution physique du bug
reste **PENDING DEVICE VALIDATION** ; ni DAILY TRIAL ni PRIMARY USE n’est attribué.
