# Home intensive UX — evidence checkpoint, 17 November 2026

Status: inspection complete; Home acceptance BLOCKED. No application changes or
claim of UX completion. Do not advance to Plugins before Home's device gate.

## Baseline and preserved work

- Main: `4ccc95d485b42fb996e71e8480c7446987eb080b`.
- Latest appearance branch inspected: `integration/appearance-minimal-20261116`,
  `8a0e35b6afa703a400b1ee0455ee33a77a8b959e`. Main is its ancestor.
- Working branch: `ux/home-intensive-20261117`; no merge authorized.
- This baseline includes Home cover/crop/reduced motion, Plugins refinement,
  Settings hierarchy and Appearance simplification. Preserve these changes.
- GitHub run https://github.com/ja3kies/Klyro-Shell/actions/runs/43906186541
  completed successfully at that exact SHA. Both jobs and their step conclusions
  were retrieved. JVM tests, lint, debug/release APK and test APK compilation
  succeeded. No step executes instrumented tests on Android.
- Locally executed: `python3 -m unittest discover -s tools -p 'test_klyro_home*.py'`:
  21 tests, OK. These inspect source contracts, not device behavior.
- Local build attempt `./gradlew :app:assembleDebug :app:testDebugUnitTest
  :app:lintDebug --console=plain` failed because there is no wrapper. The existing
  CI deliberately provisions Gradle 8.11.1. No local Gradle, SDK, adb or /dev/kvm
  is available. Do not describe this as an application compilation failure.

## Visual evidence

Inspected `ui-refinement-20261116/home-before.png`. It is an older real Android
capture with unknown source SHA. The ARM64 chip and prominent ready panel shown
there have already been changed in the baseline code. It cannot establish the
current rendering, and no fabricated after image is supplied.

## Findings requiring reproduction

| Priority | Finding / cause suggested by source | Components and dependent paths | Acceptance |
| --- | --- | --- | --- |
| P1 | Runtime status keeps icon, weighted text and Recheck in one Row; large text may squeeze feedback | MainActivity Home runtime panel, checking and error states | At 320dp / 200% text all status text and Recheck remain readable and reachable |
| P1 | Show technical details and Copy share one non-wrapping Row | Home failure/recovery, clipboard feedback | Both actions remain visible, distinct and at least 48dp at large text |
| P2 | Home scroll state is created inside its conditional branch; navigation removes that composition | Home → Plugins → Back and Home → Terminal → Home | Restore previous scroll position without blocking primary action |
| P2 | Cover runs while Home Settings overlays Home; lifecycle only stops at Activity stop | Home → Settings, background/resume | Measure whether covered animation costs frames; pause only if warranted |
| P2 | First warning is capped at three lines; full details path must remain discoverable | Home warning → details → copy | Every warning can be read and copied without loss |

These are source-based risks, not reproduced device failures. Do not change
layout speculatively or call them fixed before measuring the real states.

## Device acceptance matrix (all pending)

1. Fresh install: setup is the clear enabled primary action.
2. Setup in progress: honest progress and no duplicate install.
3. Ready: launch Bash once, with immediate pending feedback.
4. Existing sessions: resume preserves the same PTY.
5. Failed runtime with live session: existing session remains reachable.
6. Recheck: pending feedback; repeated activation does not overlap work.
7. Warning: read every warning and copy complete details.
8. Error: expand/collapse/copy and recovery remain reachable.
9. Repair: cancel leaves state untouched; confirm respects live-session guard.
10. Plugins round trip: correct Android Back and restored Home position.
11. Settings round trip: internal Back, sheet dismissal, no touch freeze.
12. Portrait 320dp and 200% fonts: no clipped actions or overlapping targets.
13. Landscape / short viewport: every action reachable by scrolling.
14. Light/dark and all four accents: measured text/icon contrast.
15. TalkBack: logical traversal, meaningful actions, polite status feedback.
16. Reduced motion changes while visible: static artwork immediately applied.
17. Background/resume and rotation: no duplicate work or session loss.
18. Real before/after captures tagged with APK SHA, size, theme and font scale.

Execute HomeCoverUiTest as well as relevant runtime and navigation tests on an
authorized ARM64 device. The existing manual device workflow requires a trusted
self-hosted runner; its presence has not been verified. Compilation of the test
APK is not execution. Do not replace native runtime acceptance with a UI mock.

## Simulated expert review — source and old capture only

- Apple ergonomics: preserve the clear primary action; check large-text recovery
  layout and return-position continuity before further visual simplification.
- Material: preserve existing colors/shapes and adaptive navigation tiles;
  verify actual contrast and minimum 48dp interactive targets.
- Motion: existing reduced-motion observation is valuable; test live changes
  and covered/background animation rather than introducing extra motion.
- Accessibility: prioritize non-truncated recovery content, TalkBack traversal
  and 200% text over decorative changes.
- Android: successful build does not establish configuration restoration,
  lifecycle or instrumented acceptance.
- Terminal user: keep the keyboard, session persistence and recovery access;
  no speculative shortcuts or new dashboard widgets.

Reference: https://developer.android.com/develop/ui/compose/accessibility/api-defaults
and https://developer.android.com/develop/ui/compose/accessibility/scalable-content.
No user research or designer endorsement is implied; no quality score assigned.

Next action: provide an authorized ARM64 Android execution surface, capture the
baseline matrix, reproduce the ranked risks, then implement the smallest complete
fixes and re-run their device regressions. Plugins remains the next screen.
