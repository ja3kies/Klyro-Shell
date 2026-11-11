# Klyro-Shell — automated Android UX QA

The **same development branch** contains the Android app, the UI robot,
the accessibility auditor, and their regression tests. This replaces the
older split between the UX closure branch and the prototype QA branch.

## Run on Pixel 9 without root or a computer

Prerequisites: Termux with Python 3, ADB paired with the Pixel, storage
access configured. **One command in Termux, no checkout needed:**

```bash
bash <(curl -fsSL https://raw.githubusercontent.com/ja3kies/Klyro-Shell/p1/ux-cross-screen-final-pass-20261110/scripts/klyro-qa-bootstrap.sh)
```

This fetches the two current robot Python files from the same GitHub development
branch, validates their Python syntax, and runs the installed APK. It does not
reinstall the app. For a local repository checkout, the equivalent is
`bash scripts/klyro-qa.sh`.

The run opens the installed Klyro APK (without reinstalling it) and
performs this safe, deterministic smoke sequence:

1. Validate Home (including Bash launch/resume states).
2. Open Plugins; assert the Installed and Explore tabs are present, switch to the opposite tab, verify the selected tab, and restore the original selection.
3. Return Home and verify all main actions.
4. Open Settings and verify the root sections.
5. Visit **Appearance, Keyboard, Storage, Privacy, Diagnostics, and About**
   one by one, asserting distinctive visible content in every section.
6. Return to Settings after each subsection, verifying backward navigation.
7. Close Settings and verify Home.

This covers **up to 19 observed navigation and tab assertions** without installing
plugins, removing files, or changing settings.
The robot **does not** install plugins, remove files, close terminals, clear
session data, toggle themes, or execute shell commands by default. It
stops on an unverified navigation, rather than continuing blindly.

## Output and reports

Artifacts are created in a timestamped folder:
`Storage/Klyro-Shell/qa-YYYYMMDD-HHMMSS/`.

- `report.html`: navigation assertions, PASS / FAIL / SKIP and link to the code/UX review
- `results.json`: machine-readable steps, clicked-control metadata, warnings and status
- `*.png`, `*.xml`: per-step screenshot and accessibility hierarchy
- `accessibility-audit.md`: visible controls, missing markers, and 48dp
  target-size checks using the device's own `wm density` value
- `ux-review.html` / `ux-review.md`: clicked component details, real Kotlin source file, exact line numbers, accessible target dimensions in dp, UI/UX recommendations, and all captured sheet scroll segments
- `components.json`: machine-readable per-component audit with evidence, Kotlin provenance, overlapping hit regions and prioritized recommendations
- `performance.json`: read-only before/after Android `dumpsys gfxinfo` counters, with an observational janky-frame delta when both snapshots support it; these are cumulative counters, **not** per-screen animation performance
- `results.json` additionally distinguishes exact duplicate screenshots from revisits of the same structural screen with updated live text
- `qa-YYYYMMDD-HHMMSS.zip`: bundled evidence

If a snapshot or expected UI marker is missing, it **is not** a pass.
The report is not exhaustive functional coverage or visual validation.

The accessibility audit now flags independent touch targets whose hit
regions overlap by at least 30% of the smaller area (nested Compose controls
are excluded). These are **P2 review signals**, not confirmed visual defects.
It also checks clipped targets and 48dp minimums with device density.

When an audit revisits Settings with new runtime text, the structural layout is
recognized as a revisit, but the new screenshot is retained if its semantic
content differs. Identical screenshots are reused instead of being recaptured.

The standalone `robot-quality-gate.yml` validates Python syntax, test fixtures
and Kotlin source contracts without waiting for the full Android SDK setup.
The Android build workflow skips changes limited to robot scripts/tests/docs,
but still runs for source changes that can affect the signed APK.

The XML and screenshots may contain private terminal content. Keep report
archives private and inspect them before sharing.


## Screen deduplication and complete sheet evidence

The robot fingerprints the **visible UIAutomator hierarchy** (labels, roles,
bounds, selected states). Identical screens, such as Settings displayed on
successive Back actions, reuse the initial screenshot/XML rather than storing
near-duplicates. Pixel clock and notification changes do not defeat this
semantic deduplication. Visual color changes that are invisible to
UIAutomator do not count as independently verified screenshots.

For each Settings sheet, the robot checks the labeled drag handle, attempts
to drag the sheet upward, and records whether the accessible sheet top moved.
If content is scrollable, it safely scrolls through successive viewports,
captures each distinct view, then scrolls back. The full sequence appears
in the **Feuilles complètes et défilement** gallery of ux-review.html.
If expansion or end-of-scroll cannot be verified, it flags the limitation;
a PASS navigation does not imply visual QA completeness.

The component audit is evidence-based. It does NOT compile individual Kotlin
components on the device, provide AI-generated visual designs, or pretend
that generic heuristics are confirmed visual defects. Build/test/lint checks
remain in GitHub Actions; per-click checks cover accessibility labels,
48dp touch targets (using measured density), and near-edge clipping.
The zero-checkout bootstrap downloads the exact Kotlin UI files read-only
to provide real contextual excerpts. Source files and console transcripts
should be treated as sensitive when sharing the report.

## Automated CI

The `Android build` GitHub Actions workflow compiles the Android APK and
runs `tools/test_klyro_ui_robot.py`,
`tools/test_klyro_robot_report_audit.py` and
`tools/test_klyro_robot_accessibility_contract.py` alongside existing
tests. It checks both QA shell scripts and Python compilation.
**GitHub Actions cannot claim Pixel device acceptance.**

## Further QA

Use opt-in `--mode explore` for bounded, allowlisted navigation probes;
`--create-session` creates a real shell session and intentionally does not
close it. Avoid these flags on a work session with important foreground jobs.
Full interaction scenarios and visual comparisons require dedicated test
data and baseline screenshots before automated write actions are enabled.

The official zero-checkout QA entry point is
`scripts/klyro-qa-bootstrap.sh`; the local-repository entry point is `scripts/klyro-qa.sh`. Do not repeat manual ADB/XML extraction for ordinary
iteration; the robot captures these automatically.
