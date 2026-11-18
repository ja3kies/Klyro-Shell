# Klyro in-app updates

## Intent

Let a Klyro user discover and install app updates from Settings without visiting GitHub. Settings checks for updates when the Updates section opens and offers a clear manual recheck action.

## Chosen approach

Use two channels behind one compact channel selector:

- **Stable** (default): GitHub's latest published, non-prerelease release and its signed `klyro-shell.apk` asset.
- **Preview**: latest successful `android-build.yml` artifact for the branch embedded in the installed build. Local builds default to `main`. Only the Klyro-signed `klyro-shell-debug` artifact is eligible; the per-run debug-key artifact is never offered.

Stable release assets are durable; preview artifacts are temporary and the UI will identify them as previews. The release workflow will only create/update a release after a maintainer pushes a semantic `vMAJOR.MINOR.PATCH` tag whose commit is already reachable from `main`. This branch will not create a tag, publish a release, or merge to `main`.

## Update flow

Settings home gains an Updates row. Its page shows the installed version, channel, check state, available version and release notes. Checking begins when the page is opened. An explicit button rechecks or retries. An available build gets one prominent download/install button; download progress, cancellation, stale preview, no release, network/API errors, digest mismatch, invalid APK, and installation permission each have distinct states and recovery actions.

The user initiates download and Android's package installer confirms installation. The app never silently installs an APK. Before launching the installer, Klyro verifies the GitHub SHA-256 digest, APK package ID, strictly increasing versionCode, and signer equality with the installed Klyro app. Downloads use private app cache, bounded sizes and cleanup of partial/invalid files.

## UI and accessibility

Use the existing Klyro settings surfaces, typography and theme colors. Stable/Preview is a single-choice segmented control; the main action is a full-width filled button, with a compact state card for version/status and a readable release-note block. All interactive targets remain at least 48 dp, selected channel and progress are announced, and errors include a direct retry or permission action. The installed update channel persists across app restarts.

Visual reference: [Mobile App Update Screen by Danish Rehman on Dribbble](https://dribbble.com/shots/22884492-Mobile-App-Update-Screen) informed the concise status-plus-primary-action hierarchy. The implementation keeps Klyro's established Settings surfaces and avoids borrowing the reference's brand styling.

## Alternatives considered

1. Stable releases only: simpler and durable, but cannot serve development updates and no release currently exists.
2. Latest CI artifact only: immediately serves a branch build, but artifacts expire and are easy to confuse with stable releases.
3. Stable plus Preview: selected by the user; covers daily releases and branch testing while preserving a stable default. The added selector and artifact validation are its cost.

## Boundaries

- Do not merge to `main`, create a release tag, or publish a release in this work.
- Do not add the Play in-app-update SDK: Klyro's current distribution is signed APKs from GitHub, not Google Play.
- Do not alter terminal input/keyboard behavior.
- No dependency or account token is required for the public repository.
- APK updates must retain the long-lived Klyro signing certificate; platform install confirmation remains authoritative.

## Revisit conditions

Revisit the preview source if Klyro later moves CI artifacts behind authentication, changes artifact retention, or publishes a dedicated preview release. Revisit install handoff if distribution moves to Google Play or Android changes the source-approval contract.
