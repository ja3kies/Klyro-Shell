"""Primary setup and terminal actions must precede optional plugin marketing."""
from pathlib import Path
import unittest
MAIN = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
class HomeOrderingTest(unittest.TestCase):
    def test_runtime_status_and_primary_action_precede_plugins(self):
        self.assertLess(MAIN.index('Text("RUNTIME STATUS"'), MAIN.index('Text("Plugins"'))
        self.assertLess(MAIN.index('if (homeAction != KlyroHomeAction.NONE)'), MAIN.index('Text("Plugins"'))
if __name__ == "__main__":
    unittest.main()
