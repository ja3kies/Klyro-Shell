"""Regression gates for the lifecycle/optimization D+E pair.

Source invariants supplement JVM and real on-device QA; not a substitute for
real UI interaction.
"""
from pathlib import Path
import unittest
from klyro_version_contract import assert_supported_klyro_version

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/oryn/terminal"


class KlyroPairQaTest(unittest.TestCase):
    def test_close_requires_explicit_confirmation(self):
        session = (APP / "ui/KlyroSessionSheets.kt").read_text()
        self.assertIn('title = { Text("Close ', session)
        self.assertIn('Text("Keep session")', session)
        self.assertIn('onDismissRequest = { closeTargetId = null }', session)
        self.assertIn('TerminalActionPolicy.runIfCurrent(expected, sessionId, true, onCloseSession)', session)
        self.assertIn('onDismissRequest = onDismiss, dismissEnabled = !confirmClose', session)

    def test_terminal_transient_ui_is_scoped_by_session(self):
        pane = (APP / "ui/TerminalPane.kt").read_text()
        for name in ("sessionSheetId", "renameDialogId", "shellChooserSourceId",
                     "pendingMultilinePaste", "outputSelectionView"):
            self.assertIn(f"var {name} by remember(session)", pane)
        for name in ("showTranscriptSearch",
                     "transcriptQuery", "findMatchCase", "searchIndex"):
            self.assertIn(f"var {name} by rememberSaveable(session)", pane)
        self.assertIn("var endSessionTargetId by rememberSaveable", pane)
        self.assertIn("var warningNoteDismissed by rememberSaveable(session, runtimeHasWarnings)", pane)
        model = (APP / "TerminalSessionViewModel.kt").read_text()
        self.assertIn("pendingNativePaste = null", model)
        self.assertIn("isSessionActive(tabId)", model)

    def test_rotation_preserves_home_intent(self):
        main = (APP / "MainActivity.kt").read_text()
        self.assertIn('getBoolean("klyro_show_welcome"', main)
        self.assertIn('putBoolean("klyro_show_welcome"', main)

    def test_import_export_share_bounded_safe_rules(self):
        main = (APP / "MainActivity.kt").read_text()
        self.assertGreaterEqual(main.count("KlyroFileSafety.verifyDestination("), 2)
        self.assertGreaterEqual(main.count("KlyroFileSafety.copyLimited("), 3)
        self.assertIn("KlyroFileSafety.exportFile(", main)
        safety = (APP / "KlyroFileSafety.kt").read_text()
        self.assertIn("verifyExportSource(home, it)", safety)
        self.assertIn("MAX_COLLISIONS = 1000", safety)
        self.assertIn("Files.isSymbolicLink", safety)

    def test_version_and_qa_document_present(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text()
        assert_supported_klyro_version(self, gradle)
        self.assertTrue((ROOT / "docs/OBJECTIVES_H_I_2026-11-06.md").is_file())
        self.assertTrue((ROOT / "docs/QA_PIXEL9_0_4_1_DEV.md").is_file())
        self.assertTrue((ROOT / "docs/OBJECTIVES_D_E_2026-11-05.md").is_file())


if __name__ == "__main__":
    unittest.main()
