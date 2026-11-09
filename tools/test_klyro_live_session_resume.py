"""A rotation must not replace a runtime that still has live terminal sessions."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal"
POLICY = (ROOT / "KlyroSetupResumePolicy.kt").read_text(encoding="utf-8")
ACTIVITY = (ROOT / "MainActivity.kt").read_text(encoding="utf-8")

class LiveSessionResumeSafety(unittest.TestCase):
    def test_resume_policy_stops_automatic_reinstall_with_live_sessions(self):
        self.assertIn("hasLiveSessions: Boolean = false", POLICY)
        self.assertIn("hasLiveSessions -> KlyroSetupResumeAction.IDLE", POLICY)
        self.assertLess(POLICY.index("busy -> KlyroSetupResumeAction.OBSERVE"),
                        POLICY.index("hasLiveSessions -> KlyroSetupResumeAction.IDLE"))

    def test_activity_passes_real_session_state_to_policy(self):
        self.assertIn("bootstrapInstalled, previousSetup, terminalState.hasLiveSessions()", ACTIVITY)
        self.assertIn('statusMessage = "Sessions preserved. Close them before installing PRoot."', ACTIVITY)
        self.assertIn("bootstrapInstalled && !runtimeInstalled -> installProotRuntime()", ACTIVITY)

if __name__ == "__main__":
    unittest.main()
