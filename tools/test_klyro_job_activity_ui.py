"""Activity badges must reflect actual foreground PTY state, not only recent output."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal"
S=(ROOT/"ui/TerminalPane.kt").read_text()
class JobActivityUiContracts(unittest.TestCase):
 def test_proc_polling_is_throttled_visible_only_and_off_main(self):
  self.assertIn('LaunchedEffect(jobSessions, lifecycleOwner)',S)
  self.assertIn('Lifecycle.State.RESUMED',S)
  self.assertIn('withContext(Dispatchers.IO)',S)
  self.assertIn('KlyroJobProbe.sample(terminal)',S)
  self.assertIn('delay(if (foreground) 1_400L else 3_500L)',S)
 def test_status_is_per_session_and_releases_closed_tab(self):
  self.assertIn('jobSessions: Map<Long, TerminalSession>',S)
  self.assertIn('tabJobActivity = measured.mapValues',S)
  self.assertIn('old.sample.shellPid == sample.shellPid',S)
  self.assertIn('old.sample.foregroundGroup == sample.foregroundGroup',S)
 def test_foreground_job_not_conflated_with_recent_output(self):
  self.assertIn('sample?.hasForegroundJob() == true -> "JOB"',S)
  self.assertIn('recentOutput(id) -> "OUT"',S)
  self.assertIn('KlyroJobProbe.statusLabel',S)
  self.assertIn('KlyroJobProbe.elapsedLabel(',S)
 def test_accessibility_and_details_explain_uncertainty(self):
  self.assertIn('append(activityLabel(id))',S)
  self.assertIn('Android did not provide a readable foreground process group.',S)
  self.assertIn('Recent terminal output does not necessarily mean a job is running.',S)
  self.assertIn('activityStatus = activityLabel(id)',S)
if __name__=="__main__":unittest.main()
