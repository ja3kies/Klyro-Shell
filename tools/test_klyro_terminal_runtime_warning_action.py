"""Runtime warnings must offer a direct, accessible path to Diagnostics."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui"
PANE = (ROOT / "TerminalPane.kt").read_text()


class RuntimeWarningActionContract(unittest.TestCase):
    def test_warning_uses_a_dedicated_action_banner(self):
        self.assertIn("KlyroTerminalRuntimeWarningBanner(", PANE)

    def test_review_action_opens_diagnostics_directly(self):
        self.assertIn('initialSettingsSection = "diagnostics"', PANE)
        self.assertIn("onReviewDiagnostics = {", PANE)

    def test_warning_visibility_and_dismissal_contract_are_preserved(self):
        self.assertIn("var warningNoteDismissed by rememberSaveable(session, runtimeHasWarnings)", PANE)
        self.assertIn("runtimeHasWarnings && sessionFailureMessage == null && !warningNoteDismissed", PANE)


if __name__ == "__main__":
    unittest.main()
