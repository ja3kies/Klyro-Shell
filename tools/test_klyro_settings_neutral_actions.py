"""Secondary Settings actions must use theme neutrals, not primary CTA accent."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroSettingsSheet.kt").read_text()
class SecondarySettingsHierarchyTest(unittest.TestCase):
    def test_release_modifiers_is_neutral_non_destructive(self):
        block=S.split('onClick = onResetModifiers,',1)[1].split('All modifiers released',1)[0]
        self.assertIn("ButtonDefaults.outlinedButtonColors",block)
        self.assertIn("contentColor = colors.foreground",block)
        self.assertIn("enabled = activeModifiers.isNotEmpty()",block)
    def test_settings_decorative_icons_neutral(self):
        fragment=S.split('private fun SettingsBentoTile',1)[1]
        self.assertIn("tint = colors.muted",fragment)
        self.assertNotIn("tint = colors.accent",fragment)
if __name__=="__main__": unittest.main()
