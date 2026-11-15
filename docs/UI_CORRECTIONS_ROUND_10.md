# UI corrections round 10

Branch: `fix/ui-reliability-round-10`, based on `feat/termux-parity-reliability`.

## Corrections

1. Workspace entry fields cap Unicode code points instead of UTF-16 units.
2. Workspace remaining-character feedback uses the same code-point count.
3. Workspace catalog persistence keeps valid boundary emoji intact.
4. Session rename input caps code points and clamps the selection after clipping.
5. Session rename persistence applies the same Unicode-safe limit.
6. Command Palette query limits no longer split a surrogate pair.
7. Terminal FIND query limits no longer split a surrogate pair.
8. Favorite color name entry and its remaining-character feedback share the Unicode limit.
9. Persisted favorite color labels use the same Unicode-safe cap.
10. Plugin apply errors are truncated without cutting surrogate pairs.
11. Plugin manifest/import errors use the same safe truncation.
12. The installed-plugin registry is read on IO rather than during composition; loading state gates controls.
13. Command Palette plugin commands are loaded on IO rather than synchronously during composition.
14. Terminal job polling observes Compose's `LocalLifecycleOwner`, rather than assuming `LocalContext` is an Activity.
15. Freeze report dates use the device locale and date/time format.

These are discrete user-visible fixes across input, persistence, rendering and loading paths. They do not claim 15 independent root causes: the shared Unicode fixes address one underlying UTF-16/code-point mismatch at multiple entry points.

## Regression checks

- JVM unit coverage added for Unicode code-point truncation, workspace names and favorite labels.
- Compose instrumentation coverage added for pasted workspace names and session renaming at the Unicode boundary. The test was added but could not be run on a device in this environment.
- Existing Python source-contract suite: **526 tests passed**. These checks are not Android runtime tests.
- `git diff --check`: passed.
- Android/JVM Gradle build and instrumentation execution: **NOT RUN**; this environment has no configured Android SDK (`local.properties` / `ANDROID_HOME`). GitHub Actions is required to compile and execute the configured jobs.
- First GitHub Actions attempt failed before creating jobs because the new branch trigger line was misindented. The workflow indentation is corrected in a follow-up commit; that retry is the build verdict to use.
- Physical Pixel validation: **NOT RUN**.

## Scope and limits

No permissions, runtime bootstrap, PRoot, package installation, PTY session lifecycle or user data were changed. Existing theme, icon, font-size and color-picker keyboard fixes from the preceding work are not counted here.

The main remaining risk is Android compilation/instrumentation of the new lifecycle-compose dependency and UI tests. The branch must pass GitHub Actions before it is considered build-verified; physical interaction remains unverified until tested on an Android device.
