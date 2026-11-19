# Klyro Quality Overhaul

Initial inventory and prioritized engineering plan: [`2026-11-19-product-overhaul.md`](superpowers/plans/2026-11-19-product-overhaul.md).

This report tracks observed defects only. “Not reviewed” is not a pass.

The first local Python run exposed two stale source-contract tests requiring the removed nullable context cast. They were updated to require the Compose lifecycle owner and its observer registration/removal; the complete suite then passed. No assertion was deleted.

| Screen | Components | Observed issue | Priority | References | Corrections | Verification | UI evidence | Status | Next |
|---|---|---|---|---|---|---|---|---|---|
| Home / Welcome | `MainActivity.kt`, `KlyroHomePixelCover.kt` | Lifecycle owner came from a `LocalContext` cast; wrappers could omit stop/resume events and leave GIF running or unobserved. | P1 | Compose lifecycle pattern; Android lifecycle owner local | Cover follows `LocalLifecycleOwner`; instrumented test checks stop/resume on supplied owner. | 654 Python tests passed on `f79dec1`; run `44447468867` built ARM64 debug/instrumented APKs and lint passed; instrumented test not executed | Existing captures are from 2026-11-16 and do not prove current render | Partiel | Run instrumented lifecycle test on Android |
| Terminal | `TerminalPane.kt`, `TranscriptSearch.java`, session client/emulator | Job polling and clipboard lifecycle observation shared a nullable context-cast weakness. FIND query clipping and its limit indicator counted UTF-16 units. | P1 | Compose lifecycle pattern; Unicode text limits; terminal buffer cell semantics | Terminal observes `LocalLifecycleOwner`; FIND query and hint use code points. Added a JVM regression for a complete ZWJ family emoji and corrected its fixture to write zero-width joiners into the preceding terminal cell. | CI `44455689124` passed for `325fcc7`, including JVM/native tests, lint/accessibility and APK assembly. Instrumented UI tests not executed. | NOT INSPECTED | Partiel | Continue input/output behavior review; device-validate FIND navigation |
| Sessions / Workspaces | `KlyroTerminalSessionTab.kt`, `KlyroSessionSheets.kt`, `KlyroWorkspaceSheet.kt`, workspace/session managers | Rename boundary and workspace name caps counted UTF-16 units, not Unicode code points. | P1 | Compose field behavior; shared Unicode text limits | Editors and catalog use code-point limits; JVM and Compose interaction tests added. | 655 Python tests and native PTY suite passed locally; CI `44449476924` passed JVM/native tests and built instrumented APK; Android interaction test not executed | NOT INSPECTED | Partiel | Run Unicode Compose tests on Android when SDK/device is available |
| Plugins / Explore | `KlyroPluginsPage.kt`, `KlyroPluginManager.kt` | Plugin execution is declarative by design. Import and registry flows still need a fresh review against the latest PR state. | P1 | Existing plugin contracts | None | Not yet run on this branch | NOT INSPECTED | Partiel | Review trust and error states |
| Settings / Appearance / App Updates | `KlyroSettingsSheet.kt`, update repository/installer/policy, `.github/workflows/android-build.yml` | Preview publication was gated to the older UI branch; this branch built but did not publish the channel consumed by the updater. | P1 | Existing Preview/update contract and GitHub Actions | `2ea17ac` includes this branch in the publish gate; updater logic did not need changing. | CI `44455689124` passed `publish-preview`; prerelease has `klyro-shell.apk` and `klyro-update.json`. Manifest branch/package/version/digest verified: `feat/product-overhaul-latest`, `com.klyro.shell`, code `127632`, name `0.4.24-dev.1276.1`, SHA-256 `d6e4c914b870a825bddb3d7cb7ee0d6e022661d7f8dfb27b8885e00179c97b0c`. `debug-apk` signing and metadata steps passed. Existing Settings touch-target Compose test is present, but tests were assembled, not run. | No current device render/interaction inspection | Partiel | Run Settings update UI and installation-consent paths on Android |
| Diagnostics / Freeze Recorder | `FreezeReportsPane.kt`, `FreezeRecorder.kt`, report store | Privacy and export/error paths not yet re-audited on the current branch. | P1 | Existing P0 diagnostics reports | None | Not yet run on this branch | NOT INSPECTED | Partiel | Audit exports and deletion failures |
| Command Palette / Find | `KlyroCommandPalette.kt`, `TranscriptSearch.kt`, `TerminalPane.kt` | Focus and safe-insertion behavior not yet revalidated against latest changes. | P1 | Compose semantics/testing docs | None | Not yet run on this branch | NOT INSPECTED | Partiel | Verify insertion never submits |
| Shared modals and menus | `KlyroUiComponents.kt`, dialogs/sheets in screen components | Modal touch freeze was fixed in earlier branches; do not retest as fixed from source alone. Cross-screen race review not yet complete. | P0/P1 | Klyro modal closure docs | None | Earlier CI evidence exists, not re-run here | Device validation unavailable | Partiel | Audit dismissal, focus and taps on device |

## Initial security review

| Area | Evidence | Initial classification |
|---|---|---|
| App private backup | Manifest sets `allowBackup=false` and disables full backup | Confirmed protective control |
| Session service | `exported=false`, `stopWithTask=false`, special-use FGS | Confirmed app-only service; lifecycle/platform policy still requires device validation |
| FileProvider | `exported=false`, URI grants enabled | Confirmed private provider surface; path mapping and grant lifetime still need review |
| Launcher entry points | Exported launcher aliases with MAIN/LAUNCHER filters | Expected Android launcher surface |
| Plugin runtime | Manager documents declarative command packs and no WebView/script engine | Confirmed reduced execution surface; import validation still requires adversarial review |
| APK updater | `REQUEST_INSTALL_PACKAGES`, Internet and FileProvider are declared; update installer exists | Review required: installer consent, signing/version verification, URI grants and partial download cleanup |
| SAF / files | Import/export entrypoints exist in Settings and MainActivity | Review required: revoked grants, atomic writes, cancellation, traversal and large files |

No new security vulnerability is claimed from this initial inventory. The outstanding lines are not yet confirmed defects.

## À FAIRE APRÈS — BLOQUANTS ET LIMITATIONS

| Problème | Impact | Cause / hypothèse | Tentatives | Preuves / logs | Prochaine action exacte | Prérequis |
|---|---|---|---|---|---|---|
| Tests Android instrumentés et robot UI | Les tests Compose restent compilés mais non exécutés; gestes, TalkBack, IME, rotation et comportement réel ne sont pas validés | Aucun SDK local/appareil ADB; le workflow robot attend un runner `[self-hosted, linux, klyro-android-device]`, mais GitHub renvoie `total_count: 0` | 1) `adb devices -l` ne liste aucune cible. 2) Déclenchement du robot sur la bonne branche; job `44454522204` est resté queued puis a été annulé après vérification du registre des runners vide. | `ANDROID_HOME`/`ANDROID_SDK_ROOT` vides; aucune commande `emulator`; runners: 0; APK de tests assemblée en CI `44455689124`; robot non exécuté. | Brancher un appareil Android ARM64 autorisé ou provisionner le runner labellisé, puis lancer `connectedDebugAndroidTest` et le workflow robot sur le commit de l’APK. | Appareil/runner Android ARM64 autorisé et SDK Android. |

## Progression du cycle 2

- Vérifié sur la branche `feat/product-overhaul-latest`: le test ZWJ est une régression de FIND au niveau terminal-buffer, et l’échec CI précédent provenait du fixture de test, pas du chemin de recherche. Le fixture a été corrigé et le test comportemental passe dans `44455689124`.
- Vérifié le chemin de publication Preview et les métadonnées de mise à jour dans le workflow courant. Pas de modification supplémentaire du mécanisme déjà fonctionnel.
- Audité la page App Updates: test de cible tactile et garde d’actions dupliquées existants; aucun nouveau défaut démontré dans le code étudié. Interaction Android et consentement d’installation restent non vérifiés.
- Revu les actions Terminal ciblées: le collage est lié à l’ID d’onglet et refuse les sessions terminées/changées; les collages multiligne nécessitent une revue; « Go to line » attend que l’IME et le viewport se stabilisent. Les recherches lisent le buffer terminal mutable et restent sur le thread UI; les tests actuels vérifient la logique de coordonnées, d’Unicode et de scrollback volumineux mais ne mesurent pas la latence physique. Aucun nouveau défaut fonctionnel confirmé.
- Revu Sessions/Workspaces: la confirmation de fermeture garde un ID stable, `closeTab(id)` ignore une session déjà absente et ne ferme que l’ID ciblé; supprimer un workspace réaffecte ses PTY à General sans les terminer. Les tests couvrent l’édition Unicode et les chemins de fermeture de modales; l’interaction réelle sur Android reste non exécutée.
- Les autres écrans du cycle restent à traiter dans l’ordre; aucun statut « Validé » ne leur est attribué sur la seule base de compilation.
