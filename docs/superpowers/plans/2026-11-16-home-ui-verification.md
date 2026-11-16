# Home UI verification

Scope: continue PRs #32 and #33 in an isolated integration branch. Keep the
existing pixel GIF, horned fox, design tokens, Home actions and Klyro keyboard.
No merge into main. No visual completion claim without fresh Android captures.

## Inspected baseline

- PR #32: `6751baa8b9e8f323766d5b43891a8d315ff86537`, draft.
- PR #33: `b20427ad8ff580cfeabd80ced8d8252c84d74490`, draft, includes #32.
- Android build 43792033879 succeeded on #33, including JVM, lint and APK.
- Main is an ancestor of #33; no main changes need reconciliation.
- No Android SDK, adb, emulator or authorized device is present on this host.
  The repository's device robot requires a provisioned self-hosted ARM64 device.

## Home changes

- [x] Reproduce missing crop, background decoding and live reduced-motion guards.
- [x] Use matching center-crop for animated and still cover; preserve pixel filtering.
- [x] Decode both paths off the main thread using existing coroutines.
- [x] Observe animation scale changes and unregister on disposal.
- [x] Add Android behavioral tests for live settings and real decoded crop pixels.
- [ ] Compile APK, lint and Android tests at the resulting commit.
- [ ] Execute Android tests and capture Home: portrait, short/landscape, 200% font,
  light/dark, warning/error, setup/start/resume, reduced motion, background/resume.
- [ ] Inspect actual captures; adjust vertical composition, CTA and contrast if needed.

## Review gate

Simulated perspectives, not endorsements: Rams (hierarchy and redundancy), Ive
(spacing and proportions), Kare (pixel silhouette and clarity), Wroblewski
(reachable primary action and useful context), Norman (states and feedback).
Composition target: at least 8.5/10 after inspecting the actual render. No score yet.

Then continue Plugins, Settings/other screens, terminal, in that order. The terminal
reference guides visual hierarchy only: keep Klyro's current keyboard and do not
add the proposed replacement shortcut bar or a disconnected command input.

## Proof limits

Source contracts detect implementation regressions; they do not prove Android
rendering, accessibility, performance or device behavior. Instrumentation tests
must execute before those claims. A green build alone does not close Home.
