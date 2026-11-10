"""Terminal chrome keeps off-screen tabs reachable and FIND compact without regressing search."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui"
S=(ROOT/"TerminalPane.kt").read_text()
D=(ROOT/"KlyroDesign.kt").read_text()
class TerminalUXClosure(unittest.TestCase):
 def test_tab_strip_tracks_workspace_selection_without_tty_side_effects(self):
  self.assertIn('val tabStripScroll = rememberScrollState()',S)
  self.assertIn('LaunchedEffect(activeTabId, tabs)',S)
  self.assertIn('tabStripScroll.animateScrollTo(target)',S)
  self.assertIn('.horizontalScroll(tabStripScroll)',S)
 def test_find_preview_scrolls_while_actions_remain_visible(self):
  block=S.split('if (showTranscriptSearch) {',1)[1].split('if (showWorkspaceSheet) {',1)[0]
  self.assertIn('Modifier.heightIn(max = findContentMax)',block)
  self.assertIn('.verticalScroll(rememberScrollState())',block)
  self.assertIn('KlyroUiMetrics.findDialogCompactMax',block)
  self.assertIn('Text("Older matches")',block)
  self.assertIn('Text("Newer matches")',block)
  self.assertIn('Text("Go to line")',block)
  self.assertIn('findDialogContentMax = 440.dp',D)
  self.assertIn('findDialogCompactMax = 310.dp',D)
 def test_find_remains_read_only(self):
  block=S.split('if (showTranscriptSearch) {',1)[1].split('if (showWorkspaceSheet) {',1)[0]
  self.assertIn('TranscriptSearch.scanPage(',S)
  self.assertNotIn('session.write(',block)
if __name__=="__main__":unittest.main()
