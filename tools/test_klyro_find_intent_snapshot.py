"""Delayed Find navigation uses the query accepted at tap time, not a later edit."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class FindIntentSnapshot(unittest.TestCase):
 def test_request_contains_query_and_case_before_wait(self):
  self.assertIn('var pendingSearchQuery by remember(session)',S)
  self.assertIn('var pendingSearchCase by remember(session)',S)
  fragment=S.split('LaunchedEffect(pendingSearch, session, activeTerminalView)',1)[1].split('fun openSessionSheet',1)[0]
  self.assertIn('val requestedQuery = pendingSearchQuery',fragment)
  self.assertIn('val requestedCase = pendingSearchCase',fragment)
  self.assertIn('requestedQuery, requestedCase',fragment)
  self.assertIn('if (view.isAttachedToWindow && currentSessionId() == activeTabId && view.getCurrentSession() === session)',fragment)
  self.assertNotIn('view.requestFocus()',fragment)
  self.assertIn('pendingSearchQuery = transcriptQuery\n                        pendingSearchCase = findMatchCase\n                        pendingSearchOffset = chosen.logicalOffset\n                        pendingSearchPage = searchPage\n                        pendingSearch = chosen.row',S)
  self.assertIn('TranscriptSearch.scanPage(',fragment)
  self.assertIn('TranscriptSearch.nearestIndex(result.newest, targetRow, targetOffset)',fragment)
if __name__=="__main__":unittest.main()
