"""Shortcuts re-check PTY liveness when dispatched, not merely during composition."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class RaceSafeShortcutDispatch(unittest.TestCase):
 def test_arrow_and_end_guarded_at_dispatch(self):
  f=S.split("private fun writeToRunningSession(session: TerminalSession, value: String)",1)[1]
  self.assertIn("!session.isRunning() || session.emulator == null",f)
  self.assertIn("session.write(bytes, 0, bytes.size)",f)
 def test_esc_tab_guarded_at_dispatch(self):
  self.assertIn("sendCodePointIfRunning(session, 27)",S)
  self.assertIn("sendCodePointIfRunning(session, 9)",S)
  self.assertIn("private fun sendCodePointToRunningSession(",S)
  self.assertNotIn(") { session.writeCodePoint(false, 27) }",S)
 def test_exited_pty_releases_sticky_modifiers(self):
  self.assertIn("LaunchedEffect(canSendKeys, viewClient, outputRevision)",S)
  self.assertIn("if (!canSendKeys) {",S)
  for key in ("Control","Alt","Shift","Fn"):
   self.assertIn(f"viewClient.set{key}Down(false)",S)
if __name__=="__main__":unittest.main()
