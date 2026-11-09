"""Workspace name drafts are not lost by rotation or accidental sheet dismiss."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroWorkspaceSheet.kt").read_text()
class WorkspaceDraftTest(unittest.TestCase):
    def test_draft_state_saved(self):
        for k in ("editorId", "editorText", "editorError", "deletingId", "menuId"):
            self.assertIn(f"var {k} by rememberSaveable",S)
    def test_dismiss_closes_editor_before_workspace(self):
        frag=S.split("KlyroSheet(onDismissRequest = {",1)[1].split("}) {",1)[0]
        self.assertIn("editorId != null -> { editorId = null; editorError = false }",frag)
        self.assertIn("else -> onDismiss()",frag)
    def test_save_requires_nonempty_name(self):
        self.assertIn("enabled = editorText.trim().isNotEmpty()", S)
if __name__=="__main__": unittest.main()
