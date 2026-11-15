"""Destructive workspace actions must reject stale and protected targets."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroWorkspaceSheet.kt").read_text()
class WorkspaceDeletionTargetTest(unittest.TestCase):
    def test_general_and_removed_projects_cannot_be_deleted(self):
        block=S.split("deletingId?.let { id ->",1)[1]
        self.assertIn("it.id != KlyroWorkspaceCatalog.GENERAL",block)
        self.assertIn("enabled = target != null",block)
        self.assertIn("if (target != null) { onDeleteProject(id); deletingId = null }",block)
        self.assertIn("Workspace no longer available",block)
    def test_confirmation_is_still_required(self):
        self.assertIn('title = { Text("Delete " + name + "?") }',S)
        self.assertIn('onDismissRequest = { deletingId = null }',S)
if __name__=="__main__": unittest.main()
