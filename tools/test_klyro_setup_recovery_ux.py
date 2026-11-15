"""Home setup controls must not promise a destructive repair they do not perform."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal"
HOME = (ROOT / "MainActivity.kt").read_text(encoding="utf-8")

class SetupRecoveryUXContract(unittest.TestCase):
    def test_runtime_check_keeps_existing_terminal_failure_visible(self):
        section = HOME.split("private fun verifyRuntime()",1)[1].split(
            "private fun installEnvironment(",1)[0]
        self.assertNotIn("terminalState.clearSessionFailure()", section)
        self.assertIn("runtimeVerified = verification.isEnvironmentReady()", section)

    def test_live_sessions_block_runtime_mutations_not_checks(self):
        section = HOME.split("private fun handlePrimaryAction()",1)[1].split(
            "private fun verifyRuntime()",1)[0]
        self.assertIn("if (runtimeInstalled)", section)
        self.assertIn("terminalState.hasLiveSessions()", section)
        self.assertIn("installEnvironment()", section)
        self.assertIn("(!hasRunningSessions || runtimeInstalled)", HOME)

    def test_runtime_badge_ignores_unrelated_session_error(self):
        section = HOME.split("KlyroRuntimeBadgePolicy.resolve(",1)[1].split(")",1)[0]
        self.assertIn("technicalErrorDetails != null", section)
        self.assertNotIn("sessionFailureMessage", section)
        self.assertIn("KlyroHomeSetupLabelPolicy.label(", HOME)

if __name__ == "__main__":
    unittest.main()
