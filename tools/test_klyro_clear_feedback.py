"""CLEAR must provide truthful feedback and never scroll if the action was refused."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui"
S=(ROOT/"TerminalPane.kt").read_text()
D=(ROOT/"TerminalDisplayActions.java").read_text()
class ClearFeedbackTest(unittest.TestCase):
 def test_clear_returns_success_from_emulator_only(self):
  self.assertIn("public static boolean clear(TerminalSession session)",D)
  self.assertIn("if (prompt.isEmpty()) return false;",D)
  self.assertIn("return true;",D)
  self.assertNotIn("session.write(",D)
 def test_ui_only_scrolls_on_success_and_announces_refusal(self):
  self.assertIn("if (TerminalDisplayActions.clear(session))",S)
  self.assertIn("input preserved",S)
  self.assertIn("enabled = session.emulator != null",S)
if __name__=="__main__":unittest.main()
