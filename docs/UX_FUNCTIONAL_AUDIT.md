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
- [Termius documentation](https://docs.termius.com/) : organisation explicite des sessions, sans copie de son interface.
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
