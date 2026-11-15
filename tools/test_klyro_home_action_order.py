"""The primary terminal action stays below scrollable diagnostics and tools."""
from pathlib import Path
import unittest
MAIN = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
class HomeOrderingTest(unittest.TestCase):
    def test_terminal_action_anchors_below_scrollable_navigation(self):
        self.assertLess(MAIN.index('stateLabel,'), MAIN.index('title = "Plugins"'))
        self.assertLess(MAIN.index('title = "Plugins"'), MAIN.index('if (homeAction != KlyroHomeAction.NONE)'))
        self.assertIn('modifier = Modifier.weight(1f).fillMaxWidth()', MAIN)
        self.assertIn('enabled = homeAction != KlyroHomeAction.START_BASH || !launchingBash', MAIN)
        self.assertIn('if (homeAction == KlyroHomeAction.RESUME_SESSION)', MAIN)
if __name__ == "__main__":
    unittest.main()
