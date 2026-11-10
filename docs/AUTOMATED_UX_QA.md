# Automated Klyro UX QA — initial integration

This branch introduces two complementary paths. Neither has yet been run on a device by CI.

## Local Pixel 9 / Termux: evidence-producing UI robot

From a Termux clone of this branch, with wireless ADB connected:

```bash
python3 tools/klyro_ui_robot.py --output "$HOME/storage/shared/Klyro-Shell/qa-robot" --max-actions 18
```

Open `Stockage interne/Klyro-Shell/qa-robot/report.html` in a browser.
The run produces `report.html`, `results.json`, PNG screenshots, and XML hierarchies.
It only explores a conservative allowlist of labeled navigation controls and
returns using Android Back. It intentionally skips destructive actions and
does **not** prove full functional correctness, exhaustive coverage, or visual
quality. It can still change navigation state and must be run with disposable
test data. The first screen must be Klyro, not Termux.

## Firebase Test Lab Robo: autonomous device exploration

GitHub Actions workflow: `Klyro automated UX QA`, manual dispatch.
Prerequisites:
1. Configure repository secret `FIREBASE_TEST_LAB_SA_JSON` with a Google Cloud
   service account JSON authorized to run Firebase Test Lab tests.
2. Configure repository variable `FIREBASE_PROJECT_ID`.
3. Supply `apk_run_id` for a successful Android build containing artifact
   `klyro-shell-debug-key`.
4. Check Firebase Test Lab pricing, quotas, and device availability first.
   The device selector in the workflow is an initial candidate and may need
   updating for the project catalog.

The Robo job runs on Google's devices, not on the GitHub Actions emulator.
It saves the gcloud JSON metadata as a GitHub Actions artifact. Detailed
screenshots, videos and logs are stored in Firebase Test Lab's results
location, not yet bundled into an HTML report. The workflow has not been
executed and should not be described as passing.

## Next engineering increments

- Add deterministic Compose instrumentation tests with stable test tags.
- Cover confirmation dialogs, plugin lifecycle, session persistence, terminal
  PTY interactions, search, keyboard and accessibility.
- Fetch Firebase result artifacts from Cloud Storage into the Actions artifact.
- Add screenshot baseline comparison and fail-on-regression policies.
- Run both pipelines and publish a consolidated pass/fail report.
