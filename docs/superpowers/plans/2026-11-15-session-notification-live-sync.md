# Session lifecycle → foreground notification reconciliation

**Goal:** Klyro's foreground-service notification must track the actual number of running/pending-startup shells immediately when a terminal is opened, closed, or exits, including while the Activity is in the background. No stale session count or ghost persistent notification until the 60-second heartbeat.

## Root cause
`KlyroSessionService.onStartCommand` builds the notification at startup, and its 60 s timer only checks whether **any** PTYs remain. `TerminalSessionViewModel.closeTab` and `finishTab` update model state but never notify the service. Closing 2 of 3 sessions leaves "3 active" until a new call to start service; natural last-process exit leaves the foreground notification until periodic check even when the Activity is paused. Failed-shell tabs retained for post-mortem are not live PTYs (0.4.20-dev).

## Minimal approach (Ponytail)
Use one in-process observer registered by KlyroSessionService to the process-owned TerminalSessionViewModel. Call it after state mutations affecting runningPtyCount. The service coalesces callbacks on its existing main-thread Handler and refreshes the existing notification **without restarting foreground service**. Service-level 60s timer remains a backup. No new dependency, broadcast, persistence, or service ownership of a PTY.

## Contracts
1. Running count includes unstarted PID=0 tabs, excludes exited nonzero failed tabs and all normal exited sessions.
2. On close of any tab with other PTYs alive: notification number changes promptly.
3. On natural nonzero exit: keep read-only tab and update notification, or stop service if zero PTYs.
4. On normal exit zero: close tab and refresh service without claiming a new shell.
5. Multiple same-frame changes: coalesce to one notification refresh.
6. On Service destruction: unregister observer and cancel pending refresh, never kill PTYs.
7. While Service not yet foregrounded, onStartCommand is sole constructor of its first notification.
8. Remain START_NOT_STICKY; Android 14+ FGS type SPECIAL_USE preserved; no extra permission.
9. MainActivity may be backgrounded or recreated; no Activity reference held in process model.
10. Preserve 60s safety fallback and Debug signed APK.

## TDD tasks
- [ ] Create Python regression test for observer install/removal and event propagation; verify RED via Actions.
- [ ] Add in-process listener to session ViewModel, event on attach/close/failed exit.
- [ ] Add Service callback, scheduled reconciliation, notification update, stop when no running PTYs, unregister on destruction.
- [ ] Update contradictory legacy source assertions if necessary; maintain 0.4.21 baseline behavior.
- [ ] Release as 0.4.22-dev (code 29). CI JVM/Python, lint, ARM64 APK, metadata, 16 Kio, signing.
- [ ] Record in Create State, leave branch separate and main unmodified.

## Validation caveat
A green Android CI proves compilation/tests/artifact, not a Pixel-9 notification update observed on device. Do not re-run the already user-validated 1,500-line diagnostic without a new regression.
