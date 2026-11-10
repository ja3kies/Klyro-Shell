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

    def test_settings_close_and_back_actions_exist(self):
        self.assertIn('Text("Close")', SETTINGS)
        self.assertIn('Text("Settings")', SETTINGS)

if __name__ == "__main__":
    unittest.main()
