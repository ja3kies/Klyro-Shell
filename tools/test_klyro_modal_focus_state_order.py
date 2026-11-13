"""Dialog state must be initialized before it is accessed by focus side-effects."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class FocusInitializationOrdering(unittest.TestCase):
 def test_dialog_state_precedes_terminal_focus_effect(self):
  effect=S.index('LaunchedEffect(\n        activeTabId, activeTerminalView, sessionSheetId')
  for k in ('val showEndSessionConfirmation =','var showTranscriptSearch by',
            'var warningNoteDismissed by','var selectedOutput by',
            'var pendingMultilinePaste by'):
   self.assertLess(S.index(k),effect,k)
if __name__=="__main__":unittest.main()
