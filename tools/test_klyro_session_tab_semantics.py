"""TalkBack should announce session pills as selected or unselected tabs."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class AccessibleSessionPills(unittest.TestCase):
 def test_tabs_have_structural_role_and_active_selection(self):
  block=S.split('tabs.forEach { (id, tabTitle) ->',1)[1].split('Box(\n                modifier = Modifier\n                    .size(48.dp)',1)[0]
  self.assertIn('role = Role.Tab',block)
  self.assertIn('selected = active',block)
  self.assertIn('stateDescription = if (id in tabsWithUnreadOutput',block)
  self.assertIn('onLongClickLabel = "Edit session"',block)
if __name__=="__main__":unittest.main()
