"""Structural contract: guide navigation must not consume external dismissal."""
from pathlib import Path
import unittest
P=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroCommandPalette.kt").read_text()
class GuideDismissParity(unittest.TestCase):
 def test_external_dismiss_closes_palette_even_from_guide(self):
  self.assertIn("KlyroSheet(onDismissRequest = onDismiss,",P)
  self.assertIn("dismissEnabled = pendingCautionCommand == null && !confirmClearRecent",P)
  self.assertNotIn("showingGuide -> showingGuide = false",P)
  self.assertIn('Text(if (showingGuide) "Commands" else "Welcome guide")',P)
if __name__=="__main__":unittest.main()
