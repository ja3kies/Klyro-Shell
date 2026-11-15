"""The extra-key row must scroll rather than shrink below minimum touch size."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class KeyTouchSizeTest(unittest.TestCase):
 def test_noncompact_touch_targets_have_minimum_width(self):
  self.assertIn(".coerceIn(48.dp, 56.dp)",S)
  self.assertNotIn(".coerceIn(40.dp, 56.dp)",S)
  self.assertIn('Row(modifier = Modifier.fillMaxWidth()\n                    .horizontalScroll(rememberScrollState()),\n                    horizontalArrangement = Arrangement.spacedBy(4.dp)) { firstRow() }',S)
 def test_compact_shortcut_row_still_scrolls(self):
  self.assertIn('val keyWidth = if (compact) 48.dp',S)
  self.assertIn('.horizontalScroll(rememberScrollState())',S)
if __name__=="__main__":unittest.main()
