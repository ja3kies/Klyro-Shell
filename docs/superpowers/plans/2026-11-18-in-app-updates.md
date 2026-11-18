# In-app updates implementation plan

## Scope
Stable GitHub releases by default and branch-aware Preview updates, entirely from Settings. The work stays on `feat/in-app-updates-20261118`; no release/tag/main merge.

## 45 substantive actions
- [x] 01. Add a Settings Updates destination and route identity.
- [x] 02. Add a restrained row explaining that updates are checked inside Klyro.
- [x] 03. Show installed version and selected channel.
- [x] 04. Add accessible Stable/Preview single-choice control.
- [x] 05. Persist channel choice across sheet dismissal and process recreation.
- [x] 06. Auto-check only when Updates opens, not on every Settings recomposition.
- [x] 07. Add explicit check/recheck control with clear loading semantics.
- [x] 08. Model loading, current, available, none, error, expired and installing states.
- [x] 09. Prevent duplicate concurrent checks and downloads.
- [x] 10. Add bounded GitHub API transport with timeouts and response limits.
- [x] 11. Parse latest non-draft, non-prerelease Stable release metadata.
- [x] 12. Select only the exact `klyro-shell.apk` release asset.
- [x] 13. Preserve release notes as plain text with safe length limits.
- [x] 14. Parse Preview runs for the branch embedded into this APK.
- [x] 15. Filter Preview runs to the correct workflow, branch, success and completed state.
- [x] 16. Select only the `klyro-shell-debug` signed artifact.
- [x] 17. Reject expired, absent, oversized or ambiguous artifacts.
- [x] 18. Verify downloaded Preview archive SHA-256 before extraction.
- [x] 19. Extract only one APK entry and reject path traversal/zip bombs.
- [x] 20. Verify Stable APK SHA-256 against GitHub asset digest when supplied.
- [x] 21. Verify package ID matches `com.klyro.shell` before install handoff.
- [x] 22. Verify candidate versionCode is strictly higher than installed version.
- [x] 23. Verify APK signing certificate matches the installed package.
- [x] 24. Keep downloads in private cache and delete partial/invalid APKs.
- [x] 25. Add a clear download CTA only when a valid update is available.
- [x] 26. Add accessible download progress and cancel behavior.
- [x] 27. Add retry and contextual network/API errors.
- [x] 28. Add specific integrity/package/signature rejection states.
- [x] 29. Request unknown-source approval only after an explicit install action.
- [x] 30. Recheck install permission after returning from Android Settings.
- [x] 31. Hand validated APK to Android's user-confirmed package installer.
- [x] 32. Add narrow FileProvider sharing limited to update APK cache paths.
- [x] 33. Add manifest permissions/components required by the installer handoff.
- [x] 34. Add `UPDATE_BRANCH` generated metadata with safe `main` local/tag fallback.
- [x] 35. Ensure CI pull request builds embed the source head branch.
- [x] 36. Ensure push builds embed their actual branch.
- [x] 37. Add stable tag build version override without changing ordinary dev versions.
- [x] 38. Add semantic-tag release workflow gated on commit reachability from main.
- [x] 39. Require the Klyro signing secret set before publishing a release.
- [x] 40. Publish the exact signed asset name `klyro-shell.apk` and release notes.
- [x] 41. Keep release job contents write permission scoped to that job.
- [ ] 42. Add unit tests for channel parsing, version ordering, workflow/artifact selection.
- [ ] 43. Add regression tests for digest mismatch, downgrade, package and signer rejection.
- [ ] 44. Run full CI, JVM, lint and APK packaging (Python passed locally); fix any regressions.
- [ ] 45. Install the delivered APK on Pixel 9, test both channels and capture real screens.

## Acceptance
- All code and workflow changes are committed on this feature branch only.
- No signed artifact is shown as installable before package, version, digest and signer checks.
- Android's installer remains user-confirmed.
- CI and source tests pass; real-device acceptance remains open unless the Pixel is available.
- No visual score is claimed until the real screen capture is inspected.
