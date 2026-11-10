"""Warnings from rejected plugin commands must never leak into subsequent searches."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroCommandPalette.kt").read_text()
class CommandPaletteWarningResetTest(unittest.TestCase):
 def test_warning_clear_on_navigation_search_and_reset(self):
  self.assertIn("showingGuide = !showingGuide\n                        info = null\n                        unsafeFeedback = false",S)
  self.assertIn("query = it.take(KlyroCommandSearch.MAX_QUERY_LENGTH)\n                            info = null\n                            unsafeFeedback = false",S)
  self.assertEqual(1,S.count('query = ""; info = null; unsafeFeedback = false'))
  self.assertIn('query = ""; pinnedOnly = false; availableOnly = false',S)
  self.assertIn("unsafeFeedback = true",S)
  self.assertIn("if (unsafeFeedback) colors.warningContainer else colors.key",S)
if __name__=="__main__":unittest.main()
