"""All new-session entry points respect the eight PTY limit and sheet stack."""
from pathlib import Path
import unittest
P=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui"
S=(P/"KlyroSessionSheets.kt").read_text()
U=(P/"KlyroUiComponents.kt").read_text()
T=(P/"TerminalPane.kt").read_text()
class SessionLimitConsistency(unittest.TestCase):
 def test_action_row_has_disabled_semantics(self):
  self.assertIn('enabled: Boolean = true',U)
  self.assertIn('.clickable(enabled = enabled, role = Role.Button,',U)
  self.assertIn('if (!enabled) stateDescription = "Unavailable"',U)
 def test_session_details_disable_new_when_full(self):
  self.assertIn('canCreateSession = tabs.size < 8',T)
  self.assertIn('enabled = canCreateSession',S)
  self.assertIn('"Maximum 8 sessions"',S)
 def test_shell_chooser_disables_command_at_limit(self):
  self.assertIn('canStartBash = tabs.size < 8',T)
  self.assertIn('.clickable(enabled = canStartBash',S)
  self.assertIn('Maximum 8 sessions. Close a terminal first.',S)
 def test_chooser_dismiss_navigates_to_previous_sheet(self):
  self.assertIn('KlyroSheet(onDismissRequest = onBack)',S)
  self.assertIn('sessionSheetId = sourceId',T)
 def test_stale_session_modals_released(self):
  self.assertIn('LaunchedEffect(tabs, sessionSheetId, renameDialogId, shellChooserSourceId)',T)
  self.assertIn('if (renameDialogId != null && renameDialogId !in availableIds)',T)
  self.assertIn('if (sessionSheetId != null && sessionSheetId !in availableIds)',T)
if __name__=="__main__":unittest.main()
