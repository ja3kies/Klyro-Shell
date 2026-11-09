"""Banner secondary affordances use foreground neutral tokens."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class TerminalBannerActionColorsTest(unittest.TestCase):
    def test_auxiliary_banner_actions_are_neutral(self):
        self.assertIn('Text("Home", color = TerminalText, fontSize = 12.sp)',S)
        self.assertIn('Text("Dismiss", color = TerminalText, fontSize = 12.sp)',S)
        self.assertIn("val TerminalText = ui.foreground",S)
        self.assertIn("color = ui.warningContainer",S)
        self.assertIn("color = ui.dangerContainer",S)
if __name__=="__main__": unittest.main()
