"""Terminal focus is restored only when the entire modal stack has closed."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class ModalFocusIsolation(unittest.TestCase):
 def test_main_terminal_does_not_take_focus_from_nested_dialogs(self):
  block=S.split('LaunchedEffect(\n        activeTabId, activeTerminalView, sessionSheetId',1)[1].split('val fontPreferences',1)[0]
  for state in ('showWorkspaceSheet','showCommandPalette','showTerminalSettings',
                'showTranscriptSearch','showEndSessionConfirmation',
                'selectedOutput','pendingMultilinePaste','sessionSheetId',
                'renameDialogId','shellChooserSourceId'):
   self.assertIn(state,block)
  self.assertIn('view.requestFocus()',block)
if __name__=="__main__":unittest.main()
