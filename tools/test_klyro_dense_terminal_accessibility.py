"""Clickable controls must remain accessible in dense terminal chrome."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
S=(ROOT/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
TAB=(ROOT/"app/src/main/java/com/oryn/terminal/ui/KlyroTerminalSessionTab.kt").read_text()
class TouchAndNarrationUX(unittest.TestCase):
 def test_more_menu_has_minimum_touch_target(self):
  block=S.split('val toolsMenu: @Composable () -> Unit = {',1)[1].split('DropdownMenu(',1)[0]
  self.assertIn('Modifier.size(KlyroUiMetrics.minTouch)',block)
  self.assertIn('contentDescription = "More actions"',block)
 def test_session_pills_maintain_48dp_width_and_height(self):
  self.assertIn('.heightIn(min = KlyroUiMetrics.minTouch)',TAB)
  self.assertIn('.widthIn(min = KlyroUiMetrics.minTouch)',TAB)
  self.assertIn('role = Role.Tab',TAB)
  self.assertIn('selected = active',TAB)
 def test_shortcuts_announce_states_not_internal_enum_names(self):
  self.assertEqual(2,S.count('stateDescription = spokenState'))
  self.assertNotIn('keyState.name.lowercase()',S)
  for word in ('"Unavailable"','"Selection available"','"Clipboard available"','"Active"'):
   self.assertIn(word,S)
if __name__=="__main__":unittest.main()
