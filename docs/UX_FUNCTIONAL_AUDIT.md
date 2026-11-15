# UX functional audit — freeze campaign

Statuts : CONFIRMED (preuve code/comportement), PROBABLE, UNVERIFIED (appareil),
FIXED (code et test adapté disponibles), DEFERRED (changement non justifié).
FIXED ne signifie jamais validation physique. Les 93 cas P0 et leurs limites
restent conservés dans P0_RELIABILITY_CLOSURE.md.

## Cycle 3 — accueil/navigation/Settings

| Action examinée | Résultat / preuve | Décision |
|---|---|---|
| Home avec sessions existantes | HomeActionPolicy : reprend une session même si la vérification runtime échoue | Conserver ; Android UNVERIFIED |
| Home sans runtime vérifié | HomeActionPolicy : pas de nouveau Bash automatique | Conserver |
| Messages runtime et erreurs session | HomeStatusPolicy sépare les deux messages | Conserver |
| Home interne | Navigation n'appelle pas closeTab ; fermeture réservée à confirmation | Audité, physique UNVERIFIED |
| Session active/inactive | Pill Role.Tab/selected/stateDescription et cible ID, minimum 48dp | Conserver ; TalkBack UNVERIFIED |
| Session waiting/running/unavailable | JobProbe distingue foreground group et sortie récente | Conserver ; observation physique requise |
| Création à la limite 8 | Garde dans Activity avant réserve d'ID | Conserver |
| Titre/session | IDs stables ; changement de nom sans changement PTY | Conserver |
| Confirmation End | ID sauvegardé, politique refuse une autre session | P0 conservé |
| Confirmation feuille session | ID indépendant du nom ; pas de fermeture par renommage | P0 conservé |
| Back Settings | Sous-page revient Home ; Freeze reports revient Diagnostics | Implémenté, navigation UI à tester |
| Diagnostics ouvert depuis notification avec Settings existant | Deux sheets pouvaient se superposer dans le nouveau recorder | FIXED : consommer la requête dans Settings existant |
| Rapport très volumineux | Composer un texte 512 KiB risquait de figer les détails | FIXED : preview 8192 UTF-16, frontière surrogate et mention export complet ; 3 tests |
| Export/suppression | SAF volontaire ; annulation non destructive, cible supprimée figée | Deux tests Compose de confirmation fournis (compilation, pas exécution) |
| Thèmes du message recovery | Avertissement était hors du provider de thème | FIXED : host sous MaterialTheme Klyro ; physique UNVERIFIED |

Ces 15 actions sont un audit démontré, pas 15 défauts inventés ni 15 captures
Android. Les comportements déjà corrects ne sont pas réimplémentés. Tests de
policies/preview exécutables JVM ; deux tests Compose utilisent un store jetable
séparé pour ne jamais faire tourner l'historique réel de l'utilisateur.

## Références et décisions

- [Termux](https://termux.dev/en/) et [TerminalView réellement consulté](https://github.com/termux/termux-app/blob/master/terminal-view/src/main/java/com/termux/view/TerminalView.java) : préserver le moteur, les gestes et la sémantique de terminal existants.
- [Ghostty features](https://ghostty.org/docs/features) : distinguer confort UI et protocoles terminal ; aucun remplacement de rendu.
- [iTerm2 documentation](https://iterm2.com/documentation-one-page.html) : sélectionner/rechercher reste distinct d'exécuter une commande.
- [Termius documentation](https://docs.termius.com/) : documentation overview consultée ; aucune interaction Termius exécutée, aucune interface copiée.
- [Bottom sheets Android](https://developer.android.com/develop/ui/compose/components/bottom-sheets) : retirer la feuille fermée de la composition, éviter la superposition.
- [Accessibilité Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) : targets minimum 48dp, états et boutons standards.
- [Apple feedback](https://developer.apple.com/design/human-interface-guidelines/feedback.md) : feedback lié à l'action et compréhensible ; adapté à Android, sans fausse modale système.
- Material 3 foundations consulté mais contenu principal JavaScript inaccessible ; les décisions concrètes reposent sur la documentation Android Compose ci-dessus et les composants Klyro existants.

Aucune refonte Soft Bento, branding, ressources/logos ou palettes validées.
Les appréciations visuelles portrait/paysage/thèmes/grandes polices restent
UNVERIFIED tant qu'aucun appareil n'a été utilisé.

## Cycle 4 — terminal/clavier/recherche

| Contrôle substantiel | Résultat / test |
|---|---|
| SELECT vide vs sortie | TerminalActionPolicyTest : état calculé sans transcript entier par tick ; conservé |
| SELECT explicite du menu natif | expectedSelection=true : ne bascule jamais en paste ; audit du callback |
| Paste simple | ClipboardPastePolicy/SessionDispatchTest : cible vivante et exacte ; conservé |
| Paste vide/contrôles | Politique rejette avant sink ; conservé, choix whitespace-only DEFERRED |
| Paste multiligne | Snapshot confirmé, aucune relecture du clipboard ; conservé |
| Paste Unicode/saturé | PasteDelivery/QueueAdversarial : une transaction acceptée ou zéro octet ; conservé |
| Palette refusée | Feedback précise indisponibilité OU file pleine ; aucun recent enregistré au refus |
| Callback clipboard natif obsolète | FIXED : nécessite UI active et View attachée à cette session avant lecture clipboard |
| Touches Ctrl/Alt/Shift/Fn | TerminalNavigationPolicyTest vérifie les séquences xterm ; conservé |
| Home/End et alternate | Long press Readline vs CSI ; End reste présent ; conservé |
| Long press sans short click | Test Compose d'action réelle ajouté ; compilé, exécution NOT TESTED |
| Raccourcis grandes polices | FIXED : min-height au lieu de 50dp imposés ; test Compose d'expansion + test tap, compilés |
| FIND précédent/suivant/zéro résultats | TranscriptSearchTest/RecentTest : coordonnées physiques et pagination ; conservé |
| FIND Go to après changement de tab pendant IME | FIXED : identité active et View/session revalidées avant scroll ; contrats structurels adaptés, scénario UI UNVERIFIED |
| Gestes/resize/ANSI/UTF-8/CLEAR | Moteur de gestes et octets inchangés ; suites émulateur/CLEAR conservées, physique UNVERIFIED |

Le report de scroll n'écrit aucune donnée stdin. Fermer FIND ne vide pas la
ligne Bash. Les tests de source des deux gardes restent des contrats structurels,
pas une preuve que la rotation a été manipulée. Les tests Compose supplémentaires
ne sont jamais comptés PASS sans appareil. Ni fling/pinch ni l'IME ne sont refondus.

## Cycle 5 — workspaces/fichiers/packages/plugins/accessibilité

| Contrôle | Preuve / décision |
|---|---|
| Créer et renommer workspace | Catalog UUID et noms uniques nettoyés ; tests Catalog conservés |
| Nom identique / General protégé | Catalog refuse collision et suppression General ; pas de nouvelle politique |
| Changer workspace | Métadonnées seulement, aucun signal PTY ; breadcrumb numérique ajouté |
| Supprimer avec sessions actives | Confirmation explique le déplacement vers General, aucun closeTab ; audit |
| Workspace disparu pendant édition | Message existant, Save désactivé si cible absente ; conservé |
| Préférences/persistance | Snapshot read-only explicitement typé ; aucune mutation de collection Compose en place |
| Import/Export réussis | Copie bornée puis commit P0 intact ; succès après résultat réel |
| Permissions révoquées | FIXED : message d'accès refusé et action reconnecter, sans URI provider |
| Fichier absent | FIXED : message choisir à nouveau, sans chemin privé ; test comportemental |
| Espace insuffisant | FIXED : message libérer espace/retry, sans recopier le provider ; test |
| Fichier trop grand | Limite 32 MiB explicitée ; test de message + tests de copie P0 |
| Annulation import multi-grants | FIXED : contexte coroutine vérifié avant nouvelle requête et dans catch ; annulation ne devient pas simple folder-skipped |
| Packages | Palette insère un modèle, pas une installation implicite ; erreurs runtime déjà préservées ; réseau APK UNVERIFIED |
| Plugins Add/Import/Remove | Aperçu et confirmation déjà présents ; transaction confirmée sérialisée, Back préservé ; ne pas réimplémenter |
| Accessibilité critique | Role.Tab/selected, labels, switches/boutons Material, customActions color picker ; aucun lecteur automatique stdout ; TalkBack physique UNVERIFIED |

Huit tests StorageFailureTest couvrent permission, document absent, ENOSPC,
limite, interruption, credentials de provider, erreur inattendue typée et null.
Les erreurs inattendues restent visibles par type ; les messages arbitraires du
provider ne sont jamais montrés. SAF réel/révocation Android et rotation picker
restent NOT TESTED. Les callbacks ne suppriment que leurs fichiers temporaires,
jamais un document utilisateur pour masquer une erreur.

Lint MutableCollectionMutableState : types Kotlin List explicités sur les snapshots
workspace/pins/recents (les helpers retournaient déjà des copies/unmodifiable).
Ce n'est pas présenté comme trois bugs fonctionnels. Reste à vérifier sur Pixel :
TalkBack, focus, ordre de lecture, grands caractères, contraste effectif.

## Cycle 6 — revue transversale et convergence

Les quinze écrans ci-dessous ont été revus dans leur code et tests disponibles ;
aucune capture, contraste affiché ou animation physique n'est déclaré validé.

| Écran | Vérification / résultat |
|---|---|
| Accueil | Actions gardées par runtime/session ; pas de nouveau stress automatique |
| Terminal | Octets stdin/stdout et moteur conservés ; compteurs techniques seulement |
| Sessions | Cible ID et libellés, aucune fermeture ajoutée à lifecycle |
| Workspaces | Liste read-only, IDs UUID, édition obsolète signalée |
| Settings | Retour hiérarchique ; requête rapports absorbée par la feuille existante |
| Appearance | Préférences existantes conservées ; rendu réel UNVERIFIED |
| Theme picker | Palettes/aperçus existants et tests de couleurs conservés ; aucun branding changé |
| Color picker | Champs hex ASCII, preview et Cancel existants ; tests HexInput/FavoriteColors conservés |
| Files | Messages contrôlés et checkpoints ; pas de destruction de document |
| Packages | Suggestions insérées seulement ; APT/runtime non réécrits |
| Plugins | Add mène déjà à aperçu/confirmation ; aucune installation silencieuse ajoutée |
| Palette | Succès conditionné à l'accusé stdin ; collections read-only explicites |
| Find | Cible revalidée après attente IME ; moteur de coordonnées conservé |
| Diagnostics | Export sans verrou pendant écriture SAF ; pas de profilage réseau |
| Freeze reports | Preview bornée, état recovered exact même après gros stacks, suppression ciblée |

Régressions trouvées/corrigées pendant convergence : deux snapshots pouvaient
être produits au même tick tardif → séparation minimale réelle 5 s ; les callbacks
PTY pouvaient chasser les transitions du ring → ring critique 32 en plus du ring
128 ; un preview tronqué pouvait cacher RECOVERED final → summary explicite ;
les temporaires interrompus pouvaient s'accumuler après redémarrages → récupération
de seuls .pending-*.tmp avant publication du store ; le stall différé revalide
absence de sessions et lifecycle RESUMED au moment d'exécuter.

Les tests Convergence couvrent ces limites, plus le script Bash réellement exécuté
avec 1 500 lignes. Le même script exact est aussi exécuté par un harness PTY du
guest jetable. Cela ne valide pas le Home/retour dans l'APK. Notification refusée,
TalkBack, SAF et gestes restent NOT TESTED sur appareil.

Revue lint de fin : 45 warnings et 11 hints, sans suppression artificielle.
Les 3 MutableCollectionMutableState initiaux disparaissent avec les types
read-only ; deux StaticFieldLeak concernent FreezeRecorder.app, alimenté
exclusivement par applicationContext (durée de vie processus voulue, aucun
Activity stocké). Ils sont classés faux positifs probables, pas ignorés.
ImplicitSamInstance/stopService(Intent) est aussi un faux positif probable.
ChromeOsAbiSupport est attendu sur une cible ARM64. GradleDependency, UseKtx,
ModifierParameter, autoboxing, ressources/vector paths et SDK checks restent
hors correction fonctionnelle sans preuve de défaut ; logos validés conservés.
UsableSpace est déjà hors UI ; les chemins SdCardPath existants ne confèrent
aucune permission SAF supplémentaire. Le rapport lint complet est livré.

Validation consolidée : [CI finale 43530165958](https://github.com/ja3kies/Klyro-Shell/actions/runs/43530165958)
SUCCESS sur d634b4d. JVM/outil/native/build/signatures exécutés, tests Android
et Compose compilés seulement. La matrice détaillée et le statut TEST BUILD
sont dans KLYRO_UX_FREEZE_COMPLETION.md. Aucun contraste, geste ou TalkBack
physique n'est converti en PASS par cette CI.
