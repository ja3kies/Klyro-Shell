"""Banner secondary actions stay neutral; the diagnostics route is primary."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class TerminalBannerActionColorsTest(unittest.TestCase):
    def test_secondary_actions_are_neutral_and_diagnostics_is_primary(self):
        self.assertIn('Text("Home", color = TerminalText, fontSize = 12.sp)',S)
        self.assertIn('Text("Dismiss", color = ui.foreground)',S)
        self.assertIn("val TerminalText = ui.foreground",S)
        self.assertIn("color = ui.warningContainer",S)
        self.assertIn("color = ui.dangerContainer",S)
        self.assertIn('Text("Review diagnostics")',S)
        self.assertIn("containerColor = ui.accent",S)
        self.assertIn("contentColor = ui.onAccent",S)
if __name__=="__main__": unittest.main()
