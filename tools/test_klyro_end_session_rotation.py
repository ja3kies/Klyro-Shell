"""Rotating while confirming End Session must not implicitly kill or dismiss it."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class EndSessionRotationTest(unittest.TestCase):
 def test_confirmation_state_survives_rotation(self):
  self.assertIn("var endSessionTargetId by rememberSaveable",S)
  self.assertIn("if (showEndSessionConfirmation)",S)
  self.assertIn("endSessionTargetId = activeTabId",S)
  self.assertIn('onDismissRequest = { endSessionTargetId = null }',S)
 def test_destruction_is_only_user_confirmed(self):
  dialog=S.split("if (showEndSessionConfirmation)",1)[1]
  self.assertIn("expected?.let(onCloseTab)",dialog)
  self.assertIn("runIfCurrent(expected, currentSessionId()",dialog)
  self.assertIn("Keep session",dialog)
if __name__=="__main__":unittest.main()
