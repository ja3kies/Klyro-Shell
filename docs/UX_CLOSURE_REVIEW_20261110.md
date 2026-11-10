# Klyro-Shell — UX end-to-end closure review

Branch: `p1/ux-cross-screen-final-pass-20261110`  
Baseline: `d2ac1a1` (previously green signed APK).  
Policy: preserve Bash, PTY, workspace files, bundled plugin safety, and the independent foreground service. No changes to `main`, no storage resets, no forced migrations.

## Acceptance matrix

| Surface | Completed change | Adversarial check | Manual test policy |
|---|---|---|---|
| Home | Replace decorative marketing copy with actionable local/privacy context | Home recovery, primary action, installation gating tests remain green | Final consolidated session only |
| Settings | Unify subpage back route; clear stale Android permissions error; separate Back and Close | Layered navigation, license and color-dialog restore tests | Final consolidated session only |
| Design components | Two-line accessible modal headings; disabled icons/menus; one notice component with live region | Existing theme/contrast/accessibility tests and closure contracts | Theme/large-text check at final session |
| Plugins installed | Persist recoverable errors; truthfully display updates; accessible empty-state action | Registry lock, enabled toggles, delete and rotation tests | Final consolidated session only |
| Plugins explore | Confirmed bundled installs tracked across rotation; no reentry; timed recovery if no confirmation | Missing bundle, preview, install, concurrent write tests | Final consolidated session only |
| Command palette | Available-only hides plugins with unverified binaries; explain limitation; unified failure notice | Pin/recents privacy and insertion-success tests | Final consolidated session only |
| Workspaces | Reset stale scroll on selection; clear stale context menus; disable New while editing; confirm assignment | Editor rotation, stale delete, General protection, capacity | Final consolidated session only |
| Sessions | Shell-choice Back has accessible name; close confirmation and PTY lifetime unchanged | Session persistence, limit, rename and close contracts | Final consolidated session only |
| Terminal tabs | Scroll selected tab into view on external navigation or session creation | Tab selection semantics, multi-session independent output | Final consolidated session only |
| Terminal FIND | Constrain content height on short screens; preserve previously validated exact paging and context | 720-line FIND probe already validated on Pixel 9; older/newer tests | No repeat of FIND functional test |
| Job activity | Existing per-tab foreground-status / output differentiation retained | /proc fail-closed parser, JVM tests | Final consolidated session only |

## UX behavior invariants

1. Never confuse command insertion with execution or recent output with a running job.
2. Every user-facing destructive action requires a visible confirmation unless the underlying action is reversible.
3. Screen-level Back and swipe dismissal unwind the topmost applicable UI layer.
4. No new dialog or stale state should strand the user after Activity recreation.
5. Every enabled control must have a reachable 48 dp target where space permits; disabled actions explain why.
6. Empty states have a direct recovery route; failures are announced without excessive toasts.
7. Application theme/foreground colors must be consistent in sheets, terminal chrome, dialogs and text controls.
8. No managed plugin may run unreviewed shell commands; no raw terminal text is stored in command recents.
9. A workspace operation must never kill its associated PTYs; changing project membership is metadata only.
10. Long terminal transcripts and sheets preserve navigation/accessibility without unbounded UI result lists.
11. Session close is explicit; `main` and signed baseline remain untouched.
12. No repeated device QA during UX implementation. One consolidated acceptance session follows a fully green APK.

## Validation and rollback

Run `tools/test_klyro_*.py`, Android JVM suite, Android lint/accessibility suite, ARM64 APK build, 16 KiB native-alignment verification and APK signing in GitHub Actions. Reconcile any brittle source-text assertions with the intended behavior, not by deleting coverage. Mark the work **automated-green** only after all stages pass; mark it **device-accepted** only after the final Pixel test session.

Rollback is `d2ac1a1ad330e8f450da733690c820a4a757f9be`, preserved on the previously validated `p1/ux-job-activity-20261110` branch; this closure is isolated from `main`.
