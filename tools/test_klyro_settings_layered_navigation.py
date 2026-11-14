"""Android Back, sheet swipe and nested Settings dialogs share one stack."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroSettingsSheet.kt").read_text()
class LayeredSettingsNavigation(unittest.TestCase):
 def test_nested_dialogs_are_dismissed_before_parent_subsection(self):
  fragment=S.split("fun navigateBack()",1)[1].split("val density =",1)[0]
  keys=["showColorPicker -> showColorPicker = false",
        "showFontLicense -> showFontLicense = false",
        "showHugeiconsLicense -> showHugeiconsLicense = false",
        'section != "home" -> section = "home"',
        "else -> onDismiss()"]
  for key in keys: self.assertIn(key,fragment)
  self.assertEqual([fragment.index(k) for k in keys], sorted(fragment.index(k) for k in keys))
 def test_external_dismiss_and_system_back_close_owner(self):
  self.assertIn("KlyroSheet(onDismissRequest = onDismiss, dismissEnabled = canDismissSheet)",S)
  self.assertIn("BackHandler(enabled = canDismissSheet) { onDismiss() }",S)
  self.assertIn('BackHandler(enabled = canDismissSheet) { onDismiss() }',S)
if __name__=="__main__":unittest.main()
