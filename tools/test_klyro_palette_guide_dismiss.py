"""Swiping outside a nested palette guide should return to search, not close all."""
from pathlib import Path
import unittest
P=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroCommandPalette.kt").read_text()
class GuideDismissParity(unittest.TestCase):
 def test_back_and_sheet_dismiss_return_to_commands(self):
  self.assertIn("BackHandler(enabled = showingGuide) { showingGuide = false }",P)
  self.assertIn("if (showingGuide) showingGuide = false else onDismiss()",P)
  self.assertIn('Text(if (showingGuide) "Commands" else "Welcome guide")',P)
if __name__=="__main__":unittest.main()
