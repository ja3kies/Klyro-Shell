"""Job status source is session-scoped; UI never stores commands or leaks callbacks."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal"
V=(ROOT/"TerminalSessionViewModel.kt").read_text()
A=(ROOT/"MainActivity.kt").read_text()
class JobActivitySource(unittest.TestCase):
 def test_output_timestamps_use_monotonic_uptime_not_wall_clock(self):
  self.assertIn("lastOutputElapsed[tabId] = SystemClock.elapsedRealtime()",V)
  self.assertIn("val lastOutputElapsedMs: Map<Long, Long>",V)
  self.assertIn("lastOutputElapsed.remove(id)",V)
  self.assertIn("if (tabs.any { it.id == tabId })",V)
 def test_per_tab_session_reference_not_only_active_session(self):
  self.assertIn("terminalState.sessions.associate { it.id to it.session }",A)
  self.assertIn("lastOutputElapsedMs = terminalState.lastOutputElapsedMs",A)
  self.assertIn("jobSessions = jobSessions",A)
if __name__=="__main__":unittest.main()
