# Klyro Quality Overhaul

Initial inventory and prioritized engineering plan: [`2026-11-19-product-overhaul.md`](superpowers/plans/2026-11-19-product-overhaul.md).

This report tracks observed defects only. “Not reviewed” is not a pass.

The first local Python run exposed two stale source-contract tests requiring the removed nullable context cast. They were updated to require the Compose lifecycle owner and its observer registration/removal; the complete suite then passed. No assertion was deleted.

| Screen | Components | Observed issue | Priority | References | Corrections | Verification | UI evidence | Status | Next |
|---|---|---|---|---|---|---|---|---|---|
| Home / Welcome | `MainActivity.kt`, `KlyroHomePixelCover.kt` | Lifecycle owner came from a `LocalContext` cast; wrappers could omit stop/resume events and leave GIF running or unobserved. | P1 | Compose lifecycle pattern; Android lifecycle owner local | Cover follows `LocalLifecycleOwner`; instrumented test checks stop/resume on supplied owner. | 654 Python tests passed on `f79dec1`; run `44447468867` built ARM64 debug/instrumented APKs and lint passed; instrumented test not executed | Existing captures are from 2026-11-16 and do not prove current render | Partiel | Run instrumented lifecycle test on Android |
| Terminal | `TerminalPane.kt`, `TerminalSessionViewModel.kt`, session client/emulator | Job polling and clipboard lifecycle observation shared a nullable context-cast weakness. FIND query clipping and its limit indicator counted UTF-16 units. | P1 | Compose lifecycle pattern; Unicode text limits | Terminal observes `LocalLifecycleOwner`; FIND query and indicator use code points. PTY/session paths unchanged. | 655 Python tests and native PTY passed; CI `44449476924` passed JVM/native tests, lint/accessibility, ARM64 APK and instrumented APK assembly, metadata, ELF 16 KiB and signatures; instrumented tests not executed | NOT INSPECTED | Partiel | Continue audit of input/output and run instrumented tests when device is available |
| Sessions / Workspaces | `KlyroTerminalSessionTab.kt`, `KlyroSessionSheets.kt`, `KlyroWorkspaceSheet.kt`, workspace/session managers | Rename boundary and workspace name caps counted UTF-16 units, not Unicode code points. | P1 | Compose field behavior; shared Unicode text limits | Editors and catalog use code-point limits; JVM and Compose interaction tests added. | 655 Python tests and native PTY suite passed locally; CI `44449476924` passed JVM/native tests and built instrumented APK; Android interaction test not executed | NOT INSPECTED | Partiel | Run Unicode Compose tests on Android when SDK/device is available |
| Plugins / Explore | `KlyroPluginsPage.kt`, `KlyroPluginManager.kt` | Plugin execution is declarative by design. Import and registry flows still need a fresh review against the latest PR state. | P1 | Existing plugin contracts | None | Not yet run on this branch | NOT INSPECTED | Partiel | Review trust and error states |
| Settings / Appearance | `KlyroSettingsSheet.kt`, `KlyroAppearanceGroup.kt`, shared design components | Latest PRs contain ongoing refinements. No duplicate redesign planned; functional state/persistence review remains. | P1 | Material 3; Compose accessibility | None | Latest upstream CI success only | Latest device capture not available | NOT INSPECTED | Review semantics, lifecycle, persistence |
| Diagnostics / Freeze Recorder | `FreezeReportsPane.kt`, `FreezeRecorder.kt`, report store | Privacy and export/error paths not yet re-audited on the current branch. | P1 | Existing P0 diagnostics reports | None | Not yet run on this branch | NOT INSPECTED | Partiel | Audit exports and deletion failures |
| Command Palette / Find | `KlyroCommandPalette.kt`, `TranscriptSearch.kt`, `TerminalPane.kt` | Focus and safe-insertion behavior not yet revalidated against latest changes. | P1 | Compose semantics/testing docs | None | Not yet run on this branch | NOT INSPECTED | Partiel | Verify insertion never submits |
| Shared modals and menus | `KlyroUiComponents.kt`, dialogs/sheets in screen components | Modal touch freeze was fixed in earlier branches; do not retest as fixed from source alone. Cross-screen race review not yet complete. | P0/P1 | Klyro modal closure docs | None | Earlier CI evidence exists, not re-run here | Device validation unavailable | NOT INSPECTED | Audit distinct remaining owners |

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
| Validation UI sur Android réel | Impossible d’attester les gestes, TalkBack, IME et tailles réelles | Aucun appareil autorisé observé dans cette session | 0 | Les anciens rapports contiennent un essai ADB absent/échoué sur des branches antérieures; aucun test physique courant n’est attesté | Installer l’APK de cette branche sur Pixel/émulateur, exécuter la matrice par écran et enregistrer les captures/résultats | Appareil Android autorisé; ADB facultatif |
| Exécution Android locale | Les nouveaux tests Compose ne peuvent pas être exécutés depuis cet environnement | `ANDROID_HOME` et `ANDROID_SDK_ROOT` sont vides; SDK absent des emplacements usuels | 1) Variables d’environnement vérifiées. 2) `~/Android/Sdk`, `/opt/android-sdk` et `/usr/lib/android-sdk` vérifiés, absents | Sortie shell du 2026-11-19; CI `44447468867` a compilé les APKs mais n’a pas exécuté l’instrumentation | Configurer Android SDK/émulateur, construire le commit Unicode puis lancer les tests instrumentés | SDK et émulateur/appareil Android |

