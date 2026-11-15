"""Rotation should not re-announce an already dismissed runtime warning."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class WarningNoteRotationTest(unittest.TestCase):
 def test_warning_dismissal_saved_per_session(self):
  self.assertIn("var warningNoteDismissed by rememberSaveable(session, runtimeHasWarnings)",S)
  self.assertIn("runtimeHasWarnings && sessionFailureMessage == null && !warningNoteDismissed",S)
  self.assertIn("onClick = { warningNoteDismissed = true }",S)
if __name__=="__main__":unittest.main()
