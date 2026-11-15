"""Find preview follows the Klyro accent and uses a neutral adaptive tile."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class SearchPreviewColors(unittest.TestCase):
 def test_custom_accent_and_theme_tokens_used(self):
  modal=S.split('if (showTranscriptSearch)',1)[1].split('if (showWorkspaceSheet)',1)[0]
  self.assertIn('background = ui.accent',modal)
  self.assertIn('color = ui.onAccent',modal)
  self.assertIn('color = ui.tile,',modal)
  self.assertNotIn('MaterialTheme.colorScheme.primary',modal)
if __name__=="__main__":unittest.main()
