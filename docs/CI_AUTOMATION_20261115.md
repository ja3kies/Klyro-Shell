# Klyro CI automation — 15 November 2026

This branch adds five narrowly scoped workflows without changing `main`.

1. **Progressive CI**: `fast-gate` validates syntax and Python regression contracts before expensive Android SDK, PRoot, Kotlin, lint, native and APK tasks. `debug-apk` depends on a successful gate and still executes the complete Python, JVM and native suites.
2. **Targeted tests**: `tools/ci_targeted_tests.py` runs only explicitly changed `tools/test_*.py` files when **every** changed file is a Python test. All other changes, missing diff base, invalid base, empty diff or more than 12 tests fall back to the full suite. The full suite still runs in the APK job.
3. **Build reports**: the Android job always writes `klyro-build-summary.md`, publishes it in the GitHub Actions summary and uploads `klyro-build-summary` artifact, even on a failure after the job starts. A failed fast gate prevents the Android job, so the report is not created in that case; fast-gate logs remain the evidence.
4. **Gradle cache**: existing `gradle/actions/setup-gradle@v4` caching is retained, with cache writes restricted to `main` and feature branches read-only. Signed runtime/PRoot assets are **not** cached without checksum validation.
5. **Physical Android robot**: `.github/workflows/android-device-robot.yml` is opt-in via `workflow_dispatch` and requires a trusted self-hosted Linux runner labeled `klyro-android-device`, with `adb`, `gh`, Python, a **single authorized ARM64 device**, and permission to install the APK. Supply the successful Android build `build_run_id`. The job downloads its signed APK, installs it, runs `tools/klyro_ui_robot.py --mode smoke`, and uploads screenshots, XML, UX and accessibility reports. The robot is read-only by default and does not run destructive session operations.

## Limits and acceptance

- **No self-hosted runner is provisioned by this commit.** The robot workflow cannot be called successfully until the device runner is connected; a green hosted CI does **not** mean Android UI acceptance.
- The APK is ARM64-only. An x86_64 emulator is not a substitute for a supported ARM64 device.
- Robot workflow dispatch on GitHub may require the workflow file to exist on the repository default branch; do not merge `main` without approval. Until then, run the robot directly on an authorized Android device with `python3 tools/klyro_ui_robot.py --mode smoke --output klyro-qa-report --source-root app/src/main/java`.
- GitHub Actions caches are best-effort; no speedup is claimed without comparing measured durations.
- The `versionCode` and `versionName` still derive from the same monotonic GitHub run/attempt contract.
- CI success is not permission to merge, release or declare production readiness.
