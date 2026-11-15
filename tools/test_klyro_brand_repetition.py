"""Brand imagery is reserved for welcome identity and explicit icon selection."""
from pathlib import Path
import unittest

SRC = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal"
HOME = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
PANE = (SRC / "ui/TerminalPane.kt").read_text(encoding="utf-8")
SETTINGS = (SRC / "ui/KlyroSettingsSheet.kt").read_text(encoding="utf-8")

class BrandRepetitionContract(unittest.TestCase):
    def test_home_keeps_brand_and_terminal_chrome_does_not_repeat_it(self):
        for name in ("ic_klyro_brand_a", "ic_klyro_brand_b"):
            self.assertIn(name, HOME)
            self.assertNotIn(name, PANE)
        self.assertIn('"TERMINAL"', PANE)
        self.assertIn("currentProject", PANE)

    def test_settings_has_only_icon_picker_previews_not_an_about_logo(self):
        for name in ("ic_klyro_brand_a", "ic_klyro_brand_b"):
            self.assertEqual(1, SETTINGS.count(name))
        about = SETTINGS.split('"about" -> {', 1)[1]
        self.assertNotIn("ic_klyro_brand", about)
        self.assertNotIn('contentDescription = "Klyro Shell logo"', about)
        self.assertIn('color = colors.muted', about)

if __name__ == "__main__":
    unittest.main()
