# Klyro Shell — branch and release policy

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
