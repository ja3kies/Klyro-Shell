"""Static accessibility contract for routes used by the Pixel 9 QA robot."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal"
MAIN = (ROOT / "MainActivity.kt").read_text(encoding="utf-8")
SETTINGS = (ROOT / "ui/KlyroSettingsSheet.kt").read_text(encoding="utf-8")

class RobotAccessibleRoutesTests(unittest.TestCase):
    def test_home_plugin_semantics_remain_stable(self):
        start = MAIN.index('onClickLabel = "Open Klyro plugins"')
        fragment = MAIN[start:start + 180]
        self.assertIn('contentDescription = "Plugins"', fragment)

    def test_home_settings_semantics_remain_stable(self):
        start = MAIN.index('onClickLabel = "Open Klyro settings"')
        fragment = MAIN[start:start + 220]
        self.assertIn('contentDescription = "Settings"', fragment)

    def test_settings_section_tiles_have_semantic_names(self):
        function = SETTINGS.split("private fun SettingsBentoTile(", 1)[1]
        fragment = function.split("private fun ", 1)[0]
        self.assertIn('contentDescription = title', fragment)
        self.assertIn('onClickLabel = "Open " + title', fragment)

    def test_icon_only_custom_accent_has_accessible_semantic_description(self):
        anchor = 'onClickLabel = "Choose custom accent"'
        self.assertIn(anchor, SETTINGS)
        scope = SETTINGS.split(anchor, 1)[1].split('Icon(painterResource(R.drawable.hugeicons_plus_sign)', 1)[0]
        self.assertIn('.semantics {', scope)
        self.assertIn('contentDescription = "Choose custom accent"', scope)
        self.assertIn('showColorPicker = true', scope)

    def test_settings_close_and_back_actions_exist(self):
        self.assertIn('Text("Close")', SETTINGS)
        self.assertIn('Text("Settings")', SETTINGS)

if __name__ == "__main__":
    unittest.main()
