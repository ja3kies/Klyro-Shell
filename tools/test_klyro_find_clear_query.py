"""Find has a discoverable way to clear reused queries."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class FindClearActionTest(unittest.TestCase):
 def test_clear_button_is_safely_scoped_to_find_dialog(self):
  find=S.split('title = { Text("Find in terminal"',1)[1].split('onDismissRequest = { showEndSessionConfirmation',1)[0]
  self.assertIn('if (transcriptQuery.isNotEmpty())',find)
  self.assertIn('transcriptQuery = ""',find)
  self.assertIn('searchIndex = 0',find)
  self.assertIn('searchSnapshot = null',find)
  self.assertIn('Text("Clear")',find)
if __name__=="__main__":unittest.main()
