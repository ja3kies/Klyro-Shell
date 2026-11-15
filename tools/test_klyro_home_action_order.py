"""Home's main action must stay in the scrollable content group, not at the bottom."""
from pathlib import Path
import unittest

MAIN = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()

class HomeOrderingTest(unittest.TestCase):
    def test_primary_action_stays_with_home_content(self):
        home = MAIN.split('            // The primary action, runtime and tools', 1)[1]
        self.assertLess(home.index('if (homeAction != KlyroHomeAction.NONE)'),
                        home.index('stateLabel,'))
        self.assertLess(home.index('stateLabel,'), home.index('title = "Plugins"'))
        self.assertEqual(home.count('if (homeAction != KlyroHomeAction.NONE)'), 1)
        self.assertIn('modifier = Modifier.weight(1f).fillMaxWidth()', home)
        self.assertIn('.verticalScroll(rememberScrollState())', home)
        self.assertIn('enabled = homeAction != KlyroHomeAction.START_BASH || !launchingBash', home)
        self.assertIn('if (homeAction == KlyroHomeAction.RESUME_SESSION)', home)
        self.assertIn('KlyroRuntimeRepairPolicy.canOffer', home)

if __name__ == "__main__":
    unittest.main()
