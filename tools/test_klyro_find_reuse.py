"""Reopening Find retains the last query until the user clears it."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class FindReuseTest(unittest.TestCase):
 def test_find_reuses_query_without_resetting_search_result_index(self):
  part=S.split('TerminalExtraKey("FIND"',1)[1].split('TerminalExtraKey("CLEAR"',1)[0]
  self.assertNotIn('transcriptQuery = ""',part)
  self.assertIn('searchIndex = 0',part)
  self.assertIn('showTranscriptSearch = true',part)
if __name__=="__main__":unittest.main()
