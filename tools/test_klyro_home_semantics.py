"""Accessibility static contract; instrumented TalkBack checks remain necessary."""
import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()

class HomeSemanticsTest(unittest.TestCase):
    def test_plugins_card_has_button_role(self):
        self.assertIn('.clickable(role = Role.Button, onClickLabel = "Open Klyro plugins")', MAIN)

    def test_runtime_text_is_a_polite_live_region(self):
        text = MAIN.split("homeStatus.runtimeMessage,", 1)[1][:170]
        self.assertIn("liveRegion = LiveRegionMode.Polite", text)

if __name__ == "__main__":
    unittest.main()
