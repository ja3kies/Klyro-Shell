"""Eight-session limit is explained by the visible disabled New button."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class SessionCapacityAffordance(unittest.TestCase):
 def test_new_button_explains_limit_without_adding_session(self):
  fragment=S.split('onClickLabel = if (tabs.size < 8)',1)[1].split('if (sessionFailureMessage != null)',1)[0]
  self.assertIn('"Maximum 8 terminal sessions"',fragment)
  self.assertIn('Text("8/8"',fragment)
  self.assertIn('enabled = tabs.size < 8',S)
  self.assertIn('onClick = onNewTab',fragment)
if __name__=="__main__":unittest.main()
