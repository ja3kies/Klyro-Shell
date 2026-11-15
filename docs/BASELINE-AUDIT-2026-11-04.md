# Klyro Shell — baseline audit reconciliation (2026-11-04)

This document compares the **read-only main audit** at `d62bfaa` with the integration branch `feat/klyro-0110`. It does not certify device behavior.

| Finding from main audit | Integration source evidence | Status |
| --- | --- | --- |
| P0: API 35 | `app/build.gradle.kts` declares `compileSdk = 36` and `targetSdk = 36` | Source-level resolved; Play compliance not independently certified |
| P0: manifest allegedly empty | `app/src/main/AndroidManifest.xml` contains INTERNET permission, MainActivity launcher declaration and application attributes | False for integration branch; merged manifest must still be inspected in build |
| P0: edge-to-edge overlaps system bars | `MainActivity.kt` enables edge-to-edge and the root Surface applies `windowInsetsPadding(WindowInsets.safeDrawing)` and `imePadding()` | Source-level mitigation; device landscape/IME remains unverified |
| P1: failed setup leaves staging | `TermuxBootstrapInstaller.installInternal` calls `paths.deletePrivateTree(staging)` before creating staging | Retry cleanup present; process-death robustness remains unverified |
| P1: installer lifecycle | `TermuxBootstrapInstaller.install` still starts a raw Thread | Open; needs lifecycle design and device validation before service migration |
| Missing terminal search/copy | `TerminalPane.kt` includes transcript FIND and copy action in tools menu | Implemented; interaction/device validation pending |
| Tests and CI absent | `android-build.yml` runs `:app:testDebugUnitTest` then `:app:assembleDebug` and uploads APK artifacts | Implemented; revalidate latest integration commit |

## Release gate

- Build and JVM tests must pass on the exact integration SHA.
- Verify debug APK signature and native ARM64 runtime manifest.
- Install on Pixel 9: status/navigation insets, portrait/landscape IME, tab lifecycle, FIND, clipboard, setup retry, PRoot shell.
- Keep `main` and `fix/proot-5.1.107.102` unchanged until explicit approval.
- Do not treat source inspection or historical PR CI as a passing test of the latest integration commit.
