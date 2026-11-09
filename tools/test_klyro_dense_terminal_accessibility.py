"""Clickable controls must remain accessible in dense terminal chrome."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class TouchAndNarrationUX(unittest.TestCase):
 def test_more_menu_has_minimum_touch_target(self):
  block=S.split('val toolsMenu: @Composable () -> Unit = {',1)[1].split('DropdownMenu(',1)[0]
  self.assertIn('Modifier.size(KlyroUiMetrics.minTouch)',block)
  self.assertIn('contentDescription = "More actions"',block)
 def test_session_pills_maintain_48dp_width_and_height(self):
  s=S.split('tabs.forEach { (id, tabTitle) ->',1)[1].split('Surface(',1)[0]
  self.assertIn('.height(48.dp)',s)
  self.assertIn('.widthIn(min = KlyroUiMetrics.minTouch)',s)
  self.assertIn('role = Role.Tab',s)
  self.assertIn('selected = active',s)
 def test_shortcuts_announce_states_not_internal_enum_names(self):
  self.assertEqual(2,S.count('stateDescription = spokenState'))
  self.assertNotIn('keyState.name.lowercase()',S)
  for word in ('"Unavailable"','"Selection available"','"Clipboard available"','"Active"'):
   self.assertIn(word,S)
if __name__=="__main__":unittest.main()
