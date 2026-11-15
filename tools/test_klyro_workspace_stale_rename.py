"""A workspace removed while its editor is open cannot be renamed by stale ID."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroWorkspaceSheet.kt").read_text()
class WorkspaceStaleEditTest(unittest.TestCase):
 def test_renaming_requires_live_target(self):
  self.assertIn("val editTarget = entries.firstOrNull { it.id == editorId }",S)
  self.assertIn("(!editing || (editTarget != null &&",S)
  self.assertIn("onRenameProject(editTarget.id, editorText)",S)
  self.assertNotIn("onRenameProject(editorId!!",S)
  self.assertIn("Workspace no longer exists. Cancel this edit.",S)
if __name__=="__main__":unittest.main()
