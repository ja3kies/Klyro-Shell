"""Settings swipe dismissal and physical Back must preserve subsection navigation."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroSettingsSheet.kt").read_text()
class SettingsDismissNavigation(unittest.TestCase):
 def test_swipe_unwinds_subpage_not_entire_sheet(self):
  self.assertIn('KlyroSheet(onDismissRequest = ::navigateBack)',S)
  self.assertIn('showColorPicker -> showColorPicker = false',S)
  self.assertIn('showFontLicense -> showFontLicense = false',S)
  self.assertIn('showHugeiconsLicense -> showHugeiconsLicense = false',S)
  self.assertIn('section != "home" -> section = "home"',S)
if __name__=="__main__":unittest.main()
