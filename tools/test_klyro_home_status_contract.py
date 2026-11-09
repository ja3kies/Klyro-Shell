"""Source-level boundary contract; Android UI still requires device QA."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt"

class HomeRuntimeStatusContract(unittest.TestCase):
    def test_home_runtime_status_does_not_read_session_status(self):
        code = MAIN.read_text(encoding="utf-8")
        self.assertIn("status = statusMessage,", code)
        self.assertNotIn("status = terminalState.sessionFailureMessage ?:", code)
        self.assertIn("KlyroHomeStatusPolicy.resolve(status, sessionFailureMessage)", code)
        self.assertIn("homeStatus.runtimeMessage", code)
        self.assertIn("homeStatus.terminalFailure?.let", code)

    def test_terminal_lifecycle_does_not_overwrite_runtime_progress(self):
        code = MAIN.read_text(encoding="utf-8")
        self.assertNotIn('statusMessage = "Bash session closed."', code)
        self.assertNotIn('statusMessage = "Starting private Bash session…"', code)
        self.assertNotIn('runtimeVerified = runtimeVerified && terminalState.sessionFailureMessage == null,', code)

if __name__ == "__main__":
    unittest.main()
