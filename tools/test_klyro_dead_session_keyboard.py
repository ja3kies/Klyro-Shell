"""Dead PTYs must not present modifiers, navigation or ESC as live actions."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class DeadSessionKeyboardTest(unittest.TestCase):
 def test_character_and_navigation_actions_are_disabled_on_exit(self):
  keys=S.split("val firstRow:",1)[1].split("if (compact) {",1)[0]
  self.assertIn("val canSendKeys = session.isRunning() && session.emulator != null",S)
  self.assertEqual(11,keys.count("enabled = canSendKeys"))
 def test_post_mortem_find_and_copy_remain_available(self):
  keys=S.split("val firstRow:",1)[1].split("if (compact) {",1)[0]
  for label in ('"ALL"','"FIND"','"CLEAR"'):
   self.assertIn(label, keys)
  self.assertIn("enabled = TerminalActionPolicy.canUseSmartKey(canSelect, canSendKeys)",keys)
  self.assertIn("TerminalActionPolicy.hasCopyableOutput",S)
if __name__=="__main__":unittest.main()
