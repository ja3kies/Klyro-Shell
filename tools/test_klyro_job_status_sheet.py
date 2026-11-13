"""Session sheet displays read-only activity and avoids pretending unknown is idle."""
from pathlib import Path
import unittest
P=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroSessionSheets.kt").read_text()
class SessionActivitySheet(unittest.TestCase):
 def test_status_and_uncertainty_visible_without_touching_pty(self):
  self.assertIn('activityStatus: String? = null',P)
  self.assertIn('activityDetail: String? = null',P)
  self.assertIn('rows = if (activityStatus.isNullOrBlank()) 4 else 5',P)
  self.assertIn('if (!activityStatus.isNullOrBlank())',P)
  self.assertIn('Text(activityDetail, color = colors.muted',P)
  self.assertNotIn('session.write(',P)
 def test_existing_close_confirmation_preserved(self):
  self.assertIn('closeTargetId by rememberSaveable',P)
  self.assertIn('This stops its Bash process and running child commands',P)
  self.assertIn('Text("Keep session")',P)
if __name__=="__main__":unittest.main()
