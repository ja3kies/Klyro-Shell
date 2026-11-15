# Klyro Shell — branch and release policy

## Versioning: semantic milestone vs. APK revision

- **Patch** (`0.4.11` → `0.4.12-dev`): fixes, security and reliability work without redefining the product's feature scope.
- **Minor** (`0.4.x` → `0.5.0`): a coherent, meaningful new compatible capability or feature milestone, with tests and Android acceptance evidence. A count of commits or corrections alone never triggers a minor change.
- **Major** (`0.x` → `1.0.0`): product readiness and a stable public contract, with explicit acceptance and compatibility checks. During 0.x development, even minor increments can carry incompatible changes; they must be documented.

### What the 0.4.x line currently represents

The evolving 0.4.x development branches cover the standalone private Bash/PRoot/PTY environment; multiple terminal sessions and workspaces; Command Palette, plugins and search; Android-native terminal controls, accessibility and visual personalization; and reliability/security testing (clipboard, lifecycle, session cleanup, installer and keyboard). **This is a retrospective feature grouping, not an official 0.4.0 release changelog.** Device validation is still required before claiming a release ready.

### Automatically version every CI-distributed APK

- The manually chosen semantic baseline for this branch is `0.4.12-dev`, Android base `versionCode=19`. Only change that baseline when the **scope** changes; do not auto-bump 0.5.0.
- The existing `android-build.yml` workflow automatically builds each APK with `versionName=0.4.12-dev.<GITHUB_RUN_NUMBER>.<GITHUB_RUN_ATTEMPT>` and `versionCode=19 + 100 * GITHUB_RUN_NUMBER + GITHUB_RUN_ATTEMPT`. This applies to the debug, release and instrumented APK built in that run; the separately signed debug APK retains the same manifest.
- The CI rejects missing/invalid counters; supported `GITHUB_RUN_ATTEMPT` is 1..99, `GITHUB_RUN_NUMBER` is 1..20,000,000, keeping Android's code ceiling intact. Build verification compares the **packaged** debug APK metadata with the Gradle-calculated values.
- The run counter is scoped to this single workflow, **not** other repositories or workflow names. Avoid renaming or duplicating the APK-producing workflow without migrating the counter. A rerun of an *older* workflow execution remains an older build relative to more recently triggered executions, even if it finishes later; this prevents old code from upgrading over newer builds.
- Standalone local Gradle builds intentionally stay at reproducible `0.4.12-dev`, `versionCode=19`. **For installation/upgrades on Pixel, use the signed GitHub CI APK**, not the locally built debug APK, which can have a lower versionCode and a different signing key.
- A successful build does not prove an Android runtime/UI check. No version number or green CI automatically authorizes merging into `main`, tagging a release, or declaring the app production-ready.

## Protected reference
- `main` is the reference branch. Do not merge, publish, or rewrite it without explicit user approval.
- `integration/cycle-01` is the shared integration baseline for the first 25-action P0 cycle.
- `p0/stability` owns PTY lifetime, foreground service, process exit, idle policy and recovery.
- `p0/keyboard` owns shortcuts, clipboard, selection, navigation and accessibility.
- `p0/branding` owns launcher aliases, adaptive icons and original visual assets.
- `p1/developer-tools` owns toolchain diagnostics, Git and project workflows.
- `p1/plugins` owns plugin manifests, permissions and lifecycle.

## Definition of a validated cycle
A cycle requires at least 25 distinct substantive implemented actions, not 25 assertions, cosmetic edits or commits. Track each action with affected code, a reproducible before/after test, and evidence. A green CI proves only CI checks; Android launcher, PTY lifecycle and UI behavior require instrumented or physical-device evidence. All required actions must be integrated and tested together before declaring the cycle validated.

## Integration protocol
1. Develop in the corresponding specialist branch, preserving commits.
2. Review diff and test each specialist branch.
3. Merge into the integration branch after conflict review, rerun all tests.
4. Produce and inspect APK and signatures.
5. Verify on Android, including update-in-place and rollback compatibility.
6. Report unverified device behavior as unverified; never claim it passed.
7. Do not update `main` without user approval.

## Existing work
The prior `feat/p0-cycle1-25-actions-20261108` branch contains early P0 work. Its CI run 42507371954 passed at commit f9a0a949e57142988547b4aef5b0e550c127338a. This is **not** a completed 25-action cycle and is **not** evidence of Android-device validation.
