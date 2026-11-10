# Klyro-Shell — automated Android UX QA

The **same development branch** contains the Android app, the UI robot,
the accessibility auditor, and their regression tests. This replaces the
older split between the UX closure branch and the prototype QA branch.

## Run on Pixel 9 without root or a computer

Prerequisites: Termux with Python 3, ADB paired with the Pixel, storage
access configured. Check out this branch in the Klyro-Shell repository:

```bash
git fetch origin p1/ux-cross-screen-final-pass-20261110
git switch p1/ux-cross-screen-final-pass-20261110
bash scripts/klyro-qa.sh
```

The run opens the installed Klyro APK (without reinstalling it) and
performs this safe, deterministic smoke sequence:

1. Validate Home (including Bash launch/resume states).
2. Open Plugins; assert the Installed and Explore tabs are present.
3. Return Home and verify all main actions.
4. Open Settings and verify the root sections.
5. Visit **Appearance, Keyboard, Storage, Privacy, Diagnostics, and About**
   one by one, asserting distinctive visible content in every section.
6. Return to Settings after each subsection, verifying backward navigation.
7. Close Settings and verify Home.

This covers **at least 16 observed navigation assertions** without installing
plugins, removing files, or changing settings.
The robot **does not** install plugins, remove files, close terminals, clear
session data, toggle themes, or execute shell commands by default. It
stops on an unverified navigation, rather than continuing blindly.

## Output and reports

Artifacts are created in a timestamped folder:
`Storage/Klyro-Shell/qa-YYYYMMDD-HHMMSS/`.

- `report.html`: before/after screenshots and PASS/FAIL assertions
- `results.json`: machine-readable steps and warnings
- `*.png`, `*.xml`: per-step screenshot and accessibility hierarchy
- `accessibility-audit.md`: visible controls, missing markers, and 48dp
  target-size checks using the device's own `wm density` value
- `qa-YYYYMMDD-HHMMSS.zip`: bundled evidence

If a snapshot or expected UI marker is missing, it **is not** a pass.
The report is not exhaustive functional coverage or visual validation.

The XML and screenshots may contain private terminal content. Keep report
archives private and inspect them before sharing.

## Automated CI

The `Android build` GitHub Actions workflow compiles the Android APK and
runs `tools/test_klyro_ui_robot.py`,
`tools/test_klyro_robot_report_audit.py` and
`tools/test_klyro_robot_accessibility_contract.py` alongside existing
tests. It also checks `scripts/klyro-qa.sh` syntax and Python compilation.
**GitHub Actions cannot claim Pixel device acceptance.**

## Further QA

Use opt-in `--mode explore` for bounded, allowlisted navigation probes;
`--create-session` creates a real shell session and intentionally does not
close it. Avoid these flags on a work session with important foreground jobs.
Full interaction scenarios and visual comparisons require dedicated test
data and baseline screenshots before automated write actions are enabled.

The official QA entry point for this branch is
`scripts/klyro-qa.sh`. Do not repeat manual ADB/XML extraction for ordinary
iteration; the robot captures these automatically.
