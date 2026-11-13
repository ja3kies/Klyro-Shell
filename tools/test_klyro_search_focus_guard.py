"""Find scrolling must not regain focus from subsequent dialogs."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class SearchFocusGuard(unittest.TestCase):
 def test_deferred_scroll_is_detached_safe_and_never_requests_focus(self):
  region=S.split("LaunchedEffect(pendingSearch, session, activeTerminalView)",1)[1].split("fun openSessionSheet",1)[0]
  self.assertIn("if (view.isAttachedToWindow && currentSessionId() == activeTabId && view.getCurrentSession() === session)",region)
  self.assertIn("pendingSearch = null",region)
  self.assertNotIn("view.requestFocus()",region)
  self.assertIn("withFrameNanos",region)
 def test_main_focus_effect_checks_all_modals(self):
  region=S.split("LaunchedEffect(\n        activeTabId, activeTerminalView, sessionSheetId",1)[1].split("BackHandler(",1)[0]
  for marker in ("!showTranscriptSearch", "!showCommandPalette",
                 "!showTerminalSettings", "!showWorkspaceSheet",
                 "!showEndSessionConfirmation", "pendingMultilinePaste == null"):
   self.assertIn(marker,region)
  self.assertIn("view.requestFocus()",region)
 def test_dismissed_warnings_reset_on_new_warning_state(self):
  self.assertIn("rememberSaveable(session, runtimeHasWarnings)",S)
if __name__=="__main__":unittest.main()
