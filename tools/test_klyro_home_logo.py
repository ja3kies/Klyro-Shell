"""Home logo must not be larger than its clipping container."""
from pathlib import Path
import re
import unittest
MAIN = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
class HomeLogoTest(unittest.TestCase):
    def test_logo_does_not_overshoot_header_bounds_or_force_dark_fill(self):
        part = MAIN.split("contentDescription = \"Klyro Shell logo\"", 1)[0][-600:]
        self.assertIn(".background(ui.tile)", part)
        around = MAIN.split('contentDescription = "Klyro Shell logo"', 1)[1][:150]
        self.assertIn("Modifier.size(60.dp)", around)
        self.assertNotIn("Modifier.size(80.dp)", around)
if __name__ == "__main__":
    unittest.main()
