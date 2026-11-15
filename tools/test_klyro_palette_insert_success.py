"""Accepted insertions alone enter the recent list; failed deliveries keep the palette open."""
from pathlib import Path
import unittest
UI=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui"
P=(UI/"KlyroCommandPalette.kt").read_text()
T=(UI/"TerminalPane.kt").read_text()
class InsertSuccessContract(unittest.TestCase):
 def test_insert_callback_reports_delivery(self):
  self.assertIn('onInsert: (String) -> Boolean',P)
  self.assertIn('if (onInsert(snippet)) {\n            recordInsertion(id)\n            onDismiss()',P)
  self.assertIn('Return to the Bash prompt and retry',P)
  self.assertIn('val emulator = session.emulator',T)
  self.assertIn('TerminalActionPolicy.pasteIfCurrent(activeTabId, currentSessionId(), session.isRunning(), snippet)',T)
 def test_plugin_errors_never_look_like_success(self):
  self.assertIn('if (onInsert(command.insert))',P)
  self.assertIn('Terminal unavailable for insertion. Retry at the Bash prompt.',P)
  self.assertNotIn('recordInsertion(command.insert)',P)
 def test_empty_results_can_recover_from_both_filters(self):
  self.assertIn('query = ""; pinnedOnly = false; availableOnly = false',P)
if __name__=="__main__":unittest.main()
