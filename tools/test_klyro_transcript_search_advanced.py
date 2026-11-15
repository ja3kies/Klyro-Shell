"""Advanced Find: bounded live scan, honest counts, stable navigation and read-only copies."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
S=(ROOT/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
ENGINE=(ROOT/"app/src/main/java/com/oryn/terminal/ui/TranscriptSearch.java").read_text()
class AdvancedFindUX(unittest.TestCase):
 def test_live_search_debounces_and_refreshes_on_output(self):
  self.assertIn('showTranscriptSearch, transcriptQuery, findMatchCase, searchPage,',S)
  self.assertIn('delay(170)',S)
  self.assertIn('searchSnapshot = snapshot',S)
  self.assertIn('searchSnapshot = null',S)
 def test_results_are_memory_bounded_with_exact_total(self):
  self.assertIn('MAX_VISIBLE_MATCHES = 300',ENGINE)
  self.assertIn('tail.size() == MAX_VISIBLE_MATCHES',ENGINE)
  self.assertIn('totalCount',ENGINE)
  self.assertIn('snapshot.totalCount',S)
  self.assertIn('Showing ${snapshot.firstOrdinal + 1}',S)
 def test_nearest_selection_survives_live_update(self):
  self.assertIn('previous != null -> TranscriptSearch.nearestIndex(',S)
  self.assertIn('snapshot.newest, previous.row, previous.logicalOffset',S)
  self.assertIn('TranscriptSearch.nearestIndex(result.newest, targetRow, targetOffset)',S)
 def test_preview_highlights_exact_match_even_after_wrap(self):
  self.assertIn('selectedMatch.preview',S)
  self.assertIn('selectedMatch.highlightStart, selectedMatch.highlightEnd',S)
  self.assertIn('background = ui.accent',S)
  self.assertIn('color = ui.onAccent',S)
 def test_navigation_has_previous_next_latest_with_scroll_safety(self):
  for x in ('Text("Previous")','Text("Next")','Text("Latest")'):
   self.assertIn(x,S)
  self.assertIn('match.row, currentEmulator.mRows',S)
  self.assertIn('isAttachedToWindow',S)
 def test_preview_copy_is_explicit_and_never_injects_shell_input(self):
  block=S.split('Text("Copy excerpt")',1)[0].split('if (selectedMatch != null)',1)[1]
  self.assertIn('ClipData.newPlainText("Klyro search excerpt", selectedMatch.preview)',block)
  self.assertNotIn('session.write(',block)
 def test_invalid_search_is_clearable(self):
  self.assertIn('Text("Clear search")',S)
  self.assertIn('Search text limit reached (128 characters)',S)
  self.assertIn('enabled = selectedMatch != null && !searchingTranscript',S)
 def test_original_unbounded_method_kept_for_backwards_compatibility(self):
  self.assertIn('public static List<Integer> findRows(',ENGINE)
  self.assertIn('public static Snapshot scanRecent(',ENGINE)
  self.assertIn('public static Snapshot scanPage(',ENGINE)
if __name__=="__main__":unittest.main()
