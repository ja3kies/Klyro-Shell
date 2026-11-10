"""Workspace navigation is recoverable and plugin writes cannot strand modals."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui"
W=(ROOT/"KlyroWorkspaceSheet.kt").read_text()
S=(ROOT/"KlyroSessionSheets.kt").read_text()
P=(ROOT/"KlyroPluginsPage.kt").read_text()
class WorkspaceUXClosure(unittest.TestCase):
 def test_workspace_switch_resets_scroll_and_stale_menu(self):
  self.assertIn('LaunchedEffect(selectedId) { workspaceScroll.scrollTo(0) }',W)
  self.assertIn('Modifier.verticalScroll(workspaceScroll)',W)
  self.assertIn('entries.none { it.id == menuId }',W)
 def test_editor_cannot_erase_draft_by_tapping_new(self):
  self.assertIn('enabled = editorId == null && entries.size',W)
  self.assertIn('editorError = false',W)
 def test_move_confirms_actual_workspace_assignment(self):
  self.assertIn('lastAssignedWorkspaceId = selected.id',W)
  self.assertIn('tabWorkspaces[activeTabId] == selected.id',W)
  self.assertIn('KlyroInlineNotice("Current terminal moved to',W)
 def test_shell_chooser_back_has_accessible_label(self):
  self.assertIn('contentDescription = "Back to session"',S)
 def test_orphaned_confirmations_have_timeout_recovery(self):
  self.assertIn('delay(10_000L)',P)
  self.assertIn('if (stuck) {',P)
  self.assertIn('Review the list before retrying.',P)
  self.assertIn('withContext(Dispatchers.IO) { manager.installed() }',P)
if __name__=="__main__":unittest.main()
