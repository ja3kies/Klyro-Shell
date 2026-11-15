# Modal campaign evidence

Source/CI/APK fingerprints: artifacts.json (added after verified CI).
Test outcomes: test-results.json. UI scenarios: dismissal-matrix.json.
Raw JVM XML, build logs, exact library sources and lint are retained in the local
`.local/modal/modal-touch-evidence.zip` delivery and GitHub CI artifacts.
No terminal content, clipboard, secrets or user documents are included.
`native-final.log` measures the isolated native harness, not Compose rendering.
Device attempts show the actual missing-device blocker; no UI PASS is inferred.

Committed command dumps normalize trailing whitespace only; originals remain in
the local evidence archive. No outcome or error message is changed.
