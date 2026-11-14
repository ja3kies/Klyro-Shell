"""Structural contract: external dismissal closes; labelled Back navigates."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroSettingsSheet.kt").read_text()
class SettingsDismissNavigation(unittest.TestCase):
 def test_swipe_closes_owner_instead_of_unwinding_subpage(self):
  self.assertIn('KlyroSheet(onDismissRequest = onDismiss, dismissEnabled = canDismissSheet)',S)
  self.assertIn('showColorPicker -> showColorPicker = false',S)
  self.assertIn('showFontLicense -> showFontLicense = false',S)
  self.assertIn('showHugeiconsLicense -> showHugeiconsLicense = false',S)
  self.assertIn('section != "home" -> section = "home"',S)
if __name__=="__main__":unittest.main()
