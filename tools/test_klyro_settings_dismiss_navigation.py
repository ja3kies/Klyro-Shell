"""Settings swipe dismissal and physical Back must preserve subsection navigation."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroSettingsSheet.kt").read_text()
class SettingsDismissNavigation(unittest.TestCase):
 def test_swipe_unwinds_subpage_not_entire_sheet(self):
  fragment=S.split("KlyroSheet(onDismissRequest = {",1)[1].split("}) {",1)[0]
  self.assertIn('section != "home"',fragment)
  self.assertIn('section = "home" else onDismiss()',fragment)
  self.assertIn('!showColorPicker',fragment)
  self.assertIn('!showFontLicense',fragment)
  self.assertIn('!showHugeiconsLicense',fragment)
if __name__=="__main__":unittest.main()
