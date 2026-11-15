"""CLEAR must preserve visible shell input, never write PTY input, and report partial clears honestly."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui"
S=(ROOT/"TerminalPane.kt").read_text()
D=(ROOT/"TerminalDisplayActions.java").read_text()

class ClearFeedbackTest(unittest.TestCase):
    def test_clear_preserves_current_bash_edit_buffer_and_never_writes_child_input(self):
        self.assertIn("public static ClearResult clearSafely(TerminalSession session)", D)
        self.assertIn("emulator.getScreen().clearTranscript()", D)
        self.assertNotIn("emulator.append(", D)
        self.assertIn("!session.isRunning()", D)
        self.assertIn("return ClearResult.SCROLLBACK_ONLY;", D)
        self.assertNotIn("session.write(", D)
        self.assertNotIn("session.writeCodePoint(", D)

    def test_ui_repaints_after_full_or_partial_clear_and_announces_partial(self):
        self.assertIn("TerminalDisplayActions.clearSafely(session)", S)
        self.assertNotIn("TerminalDisplayActions.ClearResult.CLEARED ->", S)
        self.assertIn("TerminalDisplayActions.ClearResult.SCROLLBACK_ONLY ->", S)
        self.assertIn("Scrollback cleared; current program and input preserved", S)
        self.assertIn("enabled = session.emulator != null", S)
        self.assertIn("terminalActionRevision++", S)

if __name__ == "__main__":
    unittest.main()
