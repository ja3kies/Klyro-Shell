"""Active workspace can use an accent outline while session counts stay neutral."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroWorkspaceSheet.kt").read_text()
class WorkspaceMetadataHierarchy(unittest.TestCase):
 def test_session_count_is_neutral_and_selection_keeps_accent_border(self):
  block=S.split('if (count == 1) "1 session"',1)[1].split('Box(\n                                modifier = Modifier.size',1)[0]
  self.assertIn('color = c.muted',block)
  self.assertNotIn('color = if (active) c.accent',block)
  self.assertIn('c.accent.copy(alpha = .68f)',S)
if __name__=="__main__":unittest.main()
