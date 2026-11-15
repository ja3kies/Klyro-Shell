"""Cautious catalog commands must require confirmation before insertion."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroCommandPalette.kt").read_text()
class CautionReview(unittest.TestCase):
 def test_caution_does_not_insert_directly(self):
  self.assertIn('if (entry.caution) {',S)
  self.assertIn('pendingCautionId = entry.command',S)
  self.assertIn('pendingCautionCommand = snippet',S)
  self.assertIn('pendingCautionCommand?.let { command ->',S)
  self.assertIn('title = { Text("Review command") }',S)
  self.assertIn('pendingCautionCommand = null',S)
  self.assertIn('pendingCautionId = null',S)
  self.assertIn('Text("Insert command")',S)
  self.assertIn('if (id != null) insertCurated(id, command)',S)
  self.assertIn('if (onInsert(snippet))',S)
  self.assertIn('not run automatically',S)
 def test_neutral_warning_copy_and_adaptive_tokens(self):
  self.assertIn('Surface(color = colors.key',S)
  self.assertIn('containerColor = colors.accent, contentColor = colors.onAccent',S)
if __name__=="__main__":unittest.main()
