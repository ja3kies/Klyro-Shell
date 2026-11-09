"""Cautious catalog commands must require confirmation before insertion."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroCommandPalette.kt").read_text()
class CautionReview(unittest.TestCase):
 def test_caution_does_not_insert_directly(self):
  self.assertIn('if (entry.caution) {\n                                                    keyboard?.hide()\n                                                    pendingCautionCommand = snippet',S)
  self.assertIn('pendingCautionCommand?.let { command ->',S)
  self.assertIn('title = { Text("Review command") }',S)
  self.assertIn('onDismissRequest = { pendingCautionCommand = null }',S)
  self.assertIn('Text("Insert command")',S)
  self.assertIn('onInsert(command)',S)
  self.assertIn('not run automatically',S)
 def test_neutral_warning_copy_and_adaptive_tokens(self):
  self.assertIn('Surface(color = colors.key',S)
  self.assertIn('containerColor = colors.accent, contentColor = colors.onAccent',S)
if __name__=="__main__":unittest.main()
