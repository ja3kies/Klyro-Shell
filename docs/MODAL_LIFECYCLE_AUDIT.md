# Modal lifecycle audit — Pixel 9 / Android 17

Base propre 4e765b5 ; branche fix/modal-lifecycle-touch-freeze. Main/PRoot et
les données/sessions restent intacts. Skill Ponytail : correction locale des
propriétaires et de l'abstraction existante, pas de gestionnaire global.

## Incident et niveau de preuve

CONFIRMED par le test physique **rapporté par l'utilisateur** : Settings →
Appearance → swipe ferme visuellement puis bloque les taps ; Back interne →
Home Settings → Close fonctionne. Le terminal peut continuer de se dessiner.
Aucune reproduction instrumentée par l'agent, aucun appareil ADB autorisé.
L'hypothèse de fenêtre/scrim invisible est **PROBABLE**, pas confirmée sur Pixel.
Ce n'est pas une preuve d'ANR ni une preuve que Compose/PTY s'est bloqué.

CONFIRMED dans le code : onDismissRequest appelait navigateBack, qui changeait
section sans passer showHomeSettings/showTerminalSettings à false. Les propriétaires
conservaient alors KlyroSettingsSheet dans la composition. La documentation exige
le retrait de ModalBottomSheet après son masquage.

Sources exactes Material 3 1.3.1 consultées (sources.jar Maven) :
ModalBottomSheet.kt animateToDismiss/settleToDismiss hide/settle puis appellent
onDismissRequest lorsque isVisible est faux ; ModalBottomSheet.android.kt garde
la DialogWindow jusqu'au DisposableEffect de retrait. Cela démontre la violation
du contrat par Klyro ; l'attribution du défaut tactile Pixel reste à confirmer.

## Contrat de fermeture corrigé

- Swipe/drag handle/scrim et Back système : fermeture du propriétaire complet,
  indépendamment de la sous-page. Close : même résultat.
- Bouton interne Settings/Diagnostics : navigation interne, aucune fermeture.
- Parent protégé par dialogue enfant/édition : veto Hidden **avant** masquage,
  drag handle retiré, Back désactivé ; conserver contenu visible et boutons
  Save/Cancel. Aucun callback qui ignore une fermeture après Hidden.
- ModalDismissal est une porte locale, non saveable, à une seule consommation ;
  elle invalide les callbacks après disposal et avant callback réentrant.
  La visibilité appartient toujours au parent, pas à un nouveau gestionnaire.
- Pas de hide() applicatif, délai arbitraire, restart Activity/PTY ni injection
  tactile dans le code produit.

## Inventaire des surfaces

| Surface | Visibilité / propriétaire | États internes, sortie et recreation |
|---|---|---|
| Settings toutes pages (Home/Appearance/Keyboard/Files/Privacy/Diagnostics/Freeze/About) | Main.showHomeSettings saveable ou TerminalPane.showTerminalSettings remember(session) | section saveable ; externe ferme owner, bouton interne remonte ; nouvelle instance repart Home/initialSection |
| Settings Color Picker | showColorPicker, Settings | parent protégé ; Apply/Cancel enlèvent enfant ; draft couleur protège Back/outside |
| Licences Hugeicons/font | showHugeiconsLicense/showFontLicense, Settings | dialogs read-only ; Close/outside/Back enlèvent enfant, parent reste visible |
| Permissions/brand errors | cannotOpenPermissions/brandSwitchError | notices inline, pas fenêtres ; erreur permission ne bloque pas la fermeture |
| Workspaces liste/création/rename | showWorkspaceSheet, TerminalPane | editorId/editorText saveables ; parent veto pendant édition, Save/Cancel explicites ; pas de PTY fermé |
| Workspace menu | menuId, WorkspaceSheet | Dropdown ; outside/Back null ; parent protégé tant que menu ouvert |
| Suppression workspace | deletingId, WorkspaceSheet | AlertDialog ciblé ; Cancel/null, confirm métadonnées seulement ; parent veto |
| Palette recherche/guide | showCommandPalette, TerminalPane | query/guide saveables ; externe ferme ensemble, bouton Commands/guide navigue |
| Palette review/clear recents | pendingCautionCommand/Id, confirmClearRecent | dialogs null/false après Cancel/confirm ; parent veto ; insertion ≠ exécution |
| Session actions | sessionSheetId, TerminalPane | ID cible ; externe null ; closeTargetId confirme explicitement, parent veto |
| Rename session | renameDialogId, TerminalPane | text saveable ; Back/outside désactivés si changé ; Save/Cancel explicites |
| Choose shell | shellChooserSourceId, TerminalPane | externe onBack retire chooser et ouvre actions ; geste n'est pas un simple changement de sous-page dans la même sheet |
| SELECT | selectingOutput/selection snapshot, TerminalPane | AlertDialog closeSelection ; retire dialog, aucune écriture stdin |
| FIND | showTranscriptSearch, TerminalPane | fermeture false ; recherche/cible scroll séparées, aucune suppression de commande |
| Collage multiligne | pendingMultilinePaste, TerminalPane | Cancel/null ; confirm snapshot et ID ; aucune exécution implicite ajoutée |
| End session | endSessionTargetId, TerminalPane | dialog cible stable ; uniquement confirmation explicite peut fermer le PTY |
| Outils terminal | showToolsMenu | Dropdown false avant ouverture d'une feuille ; pas de hide applicatif |
| Astuces long press | held et enabled, TerminalExtraKey | Popup non focusable, retiré au release/disposal ; aucune visibilité indépendante |
| Freeze notice/notification | notice/openReports/settingsVisible, FreezeRecorder | notification PendingIntent explicite ; notice acquittée, requête consommée par Settings existant |
| Rapports autonomes | openReports et !settingsVisible | externe/Close false ; détails inline, aucune fenêtre de détail |
| Suppression rapport / test stall | deleteTarget / blockSeconds | AlertDialog null/0 ; aucune commande/destruction sans confirmation |
| Export rapports/fichiers/imports | launchers SAF, Activity | fenêtres système, pas ModalBottomSheet ; résultats IO/cancellation existants conservés |
| Export path / réparation | showExportPathDialog / showRepairConfirmation, Main | AlertDialogs retirés par bool false ; confirmations de réparation conservées |
| Plugins liste/explore/import/bundle/delete | page et pending*/confirmed* IDs, PluginsPage | page normale, AlertDialogs sur opérations ; callbacks refusant un dismiss pendant mutation restent des dialogs visibles, pas sheets déjà Hidden |
| Color advanced/favorite deletion | showAdvanced / pendingFavoriteDeletionHex | AlertDialogs exclusifs, pop propre vers parent avec HSV conservé, pas de Hidden Material sheet |

Toute saisie conservée par rememberSaveable reste subordonnée à la visibilité du
propriétaire ; le gate local n'est jamais restauré fermé dans une nouvelle sheet.
Les fenêtres réelles/focus après rotation restent PENDING DEVICE VALIDATION.

## Premier cycle : actions substantielles

1. Source Material 1.3.1 exacte examinée, ordre hide → callback → disposal établi.
2. Dismiss Settings séparé de navigation interne.
3. Deux propriétaires Home/Terminal vérifiés : onDismiss met leur flag à false.
4. Veto Hidden central avec valeur enabled mise à jour.
5. Drag handle et Back désactivés sur états protégés.
6. Dismiss Palette guide corrigé.
7. Workspaces editor/menu/delete protégés avant masquage.
8. Session confirmation protégée avant masquage.
9. Drafts Rename/Color protégés, Save/Cancel explicites.
10. Huit tests comportementaux du gate : single callback, protection, déprotection,
    disposal, réentrance, exception, nouvelle instance et geste interrompu.
11. Tests Python conservés mais attentes de fermeture corrigées : certains
    exigeaient expressément l'antipattern responsable ; ils ne prouvent pas le tactile.

Ces actions dépassent 15 avec les huit tests distincts ; aucun nombre de simples
lectures/commentaires n'est utilisé comme validation. JVM/compilation locale PASS ;
506 Python PASS après adaptation des contrats. Interactions Android NOT TESTED.

## Dépendances et amont

BOM 2024.10.01 épingle Material3 1.3.1. Pas de mise à jour générale : 1.3.1 corrige
la priorité Back/IME ; 1.3.2 ne liste pas de correction de ce contrat propriétaire.
Les versions ultérieures ont des changements de motion et options de scrim,
mais aucun ne rend correcte une sheet Hidden maintenue par son propriétaire.

- https://developer.android.com/develop/ui/compose/components/bottom-sheets
- https://developer.android.com/jetpack/androidx/releases/compose-material3
- https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.3.1/material3-android-1.3.1-sources.jar

Les cycles 2–3 fournissent les tests d'interaction et la clôture technique ; voir
MODAL_TOUCH_FREEZE_CLOSURE.md. Aucun freeze Pixel n'est annoncé résolu sans test.
