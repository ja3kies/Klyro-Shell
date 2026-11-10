"""Find dialog query, position and case options survive rotation."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class FindRotationTest(unittest.TestCase):
 def test_only_presentation_is_restored_not_terminal_commands(self):
  for k in ("showTranscriptSearch","transcriptQuery","findMatchCase","searchIndex"):
   self.assertIn(f"var {k} by rememberSaveable(session)",S)
  self.assertIn("var pendingSearch by remember(session)",S)
  self.assertIn("TranscriptSearch.scanRecent(",S)
  self.assertIn("val snapshot = if (emulator == null)",S)
  self.assertIn("findMatchCase = it",S)
if __name__=="__main__":unittest.main()
