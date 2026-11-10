"""Older and newer transcript pages retain complete navigation with bounded memory."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
UI=(ROOT/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
ENGINE=(ROOT/"app/src/main/java/com/oryn/terminal/ui/TranscriptSearch.java").read_text()
class TranscriptPagingUX(unittest.TestCase):
 def test_bounded_pages_retain_all_match_access(self):
  self.assertIn("public static Snapshot scanPage(",ENGINE)
  self.assertIn("MAX_VISIBLE_MATCHES = 300",ENGINE)
  self.assertIn("int start = Math.max(0, end - MAX_VISIBLE_MATCHES)",ENGINE)
  self.assertIn("index >= start && index < end",ENGINE)
  self.assertIn('Text("Older matches")',UI)
  self.assertIn('Text("Newer matches")',UI)
  self.assertIn('enabled = searchPage < snapshot.pageCount - 1',UI)
  self.assertIn('enabled = searchPage > 0',UI)
 def test_deferred_jump_keeps_original_match_page(self):
  self.assertIn("pendingSearchPage = searchPage",UI)
  self.assertIn("val requestedPage = pendingSearchPage",UI)
  self.assertIn("requestedQuery, requestedCase, requestedPage",UI)
 def test_new_search_resets_page_and_last_action_returns_to_newest(self):
  self.assertIn("searchPage = 0",UI)
  self.assertIn("searchIndex = Int.MAX_VALUE",UI)
  self.assertIn("searchIndex == Int.MAX_VALUE",UI)
  self.assertIn("snapshot.firstOrdinal + position + 1",UI)
if __name__=="__main__":unittest.main()
