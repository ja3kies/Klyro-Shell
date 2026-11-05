# Klyro — consolidation de TOUS les audits (05 novembre 2026)

## Base et règles de preuve
Source: `BASELINE-AUDIT-2026-11-04.md`, `TECHNICAL_AUDIT.md`,
`UI_ADVERSARIAL_AUDIT_2026-11-04.md`,
`UI_DEVICE_SCREENSHOT_AUDIT_2026-11-04.md`,
`UI_TERMINAL_TONAL_AUDIT_2026-11-05.md`,
`multi-session-plan.md`, plus captures fournies.
La colonne « source » ne prouve **pas** une exécution sur Pixel 9.
État: SOURCE/C I = implémenté/compilé ; DEVICE = verdict appareil requis.

| Domaine | Exigence persistante | État | Preuve et travail restant |
|---|---|---|---|
| P0 Installation | bootstrap ARM64 épinglé, haché, staging/recovery | SOURCE/CI | Tests archives/paths; interrompre pendant extraction sur appareil |
| P0 Isolation | chemins privés et aucune écriture dans Termux | SOURCE/CI | `OrynPaths`, proot bind tests; vérifier `pwd`, `readlink`, `pkg` sur l'appareil |
| P0 Core | proot Bash/UID/APT/pkg réellement fonctionnels | **DEVICE NON ACQUIS** | `verifyBash()` et smoke test existent; aucune preuve sur téléphone |
| P0 PTY | exécution shell interactive, Unicode, ANSI, Ctrl+C, TTY | SOURCE/CI / DEVICE OUVERT | Termux TerminalSession; tester les vrais programmes interactifs |
| P0 Collage | filtrage contrôles, confirmation multiligne y compris collage natif | SOURCE/CI | `OrynTerminalSessionClient` aligne callback et `ClipboardPastePolicy`; tester le menu Android |
| P1 Lifecycle | sessions après rotation / arrière-plan / pression mémoire | PARTIEL | ViewModel retient rotation. Aucun service durable en cas de mort de processus |
| P1 Setup | progression, erreurs actionnables, aucun stacktrace brut | SOURCE/CI | UI message + détails explicites; installation thread non lifecycle-aware |
| P1 Écran | statut/navigation/IME, portrait et paysage, zones sûres | PARTIEL | safeDrawing/imePadding; écart vide en haut reste à mesurer sur appareil |
| P1 Terminal | coin/padding, zone homogène, polices | SOURCE/CI | clip réel, 8dp/6dp, JetBrains/System Mono et rendu natif |
| P1 Sessions | tabs, renommage stable, fermeture et renumérotation | SOURCE/CI | `KlyroSessionNamesTest`; tests appareil multi-PTY restants |
| P1 Trouver | FIND + IME, navigation, extrait, scrollback | SOURCE/CI | tests TranscriptSearch; paysage/IME réel à valider |
| P1 Sélection | menu More / Select all, copie/historique | PARTIEL | action existante, limite 2MiB; tester UX Android |
| P1 Affichage | contraste des quatre accents, modes clair/sombre, prompt | SOURCE/CI | tests de contraste et palette ANSI; truecolor OSC à éprouver |
| P1 Color Picker | presets directs, HEX compact en hauteur, un slider, aucun scroll normal | SOURCE/CI | main dialog compact, sous-panneau facultatif; agrandissement police réel à tester |
| P1 Logo | ressource vectorielle Android, mono, safe area et zoom | SOURCE/CI / DEVICE OUVERT | groupes à 78% identiques dans les calques; comparaison marque vs référence et launcher requis |
| P1 Identité | bento Klyro sans clone de DSH Go | DEBUG SEULEMENT | release conserve UI stable; promouvoir seulement sur accord |
| P2 A11y | 48dp cibles, états sélectionnés, TalkBack | SOURCE/CI / DEVICE OUVERT | thème, polices, tabs; exploration TalkBack incomplète |
| P2 Sécurité | OSC52 bloqué, clipboard ordinaire préservé | SOURCE/CI | test de régression OSC52; essais Android requis |
| P2 Perf | pas de redémarrage shell sur changement de thème, éviter recompositions | SOURCE/CI / DEVICE OUVERT | ViewModel + AndroidView stable; traces profilage Android manquantes |

## Régressions suivies
- VersionCode 6 restait identique pendant plusieurs builds: passage à **7** et versionName **0.4.0-dev** car édition identité graphique, correction de sécurité clipboard, redéfinition UI et entrée en phase opérationnelle.
- Le logo initial était surdimensionné. Échelle centrée à 78% sans réduire le viewport 108dp; Android applique son masque.
- Un contrôle documentaire et des tests automatisés ne remplacent pas les observations du launcher réel.
- Pas de décision implicite sur l'**inline FIND**, ni sur le bento release (demande d'approbation antérieure).
- Aucune modification de `main` ni du stockage Termux existant.

## Critères de promotion
Aucune intégration en branche stable tant que tous les P0 DEVICE ne sont pas réussis
et que les P1 affectant saisie/selection/retour shell n'ont pas un rapport
reproductible de l'APK du SHA exact.
