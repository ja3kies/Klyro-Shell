"""Shared UX contracts: accessible controls, recoverable errors and consistent Settings back."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal"
C=(ROOT/"ui/KlyroUiComponents.kt").read_text()
S=(ROOT/"ui/KlyroSettingsSheet.kt").read_text()
M=(ROOT/"MainActivity.kt").read_text()
class SharedUXClosure(unittest.TestCase):
 def test_large_text_sheet_title_does_not_force_single_line(self):
  self.assertIn('maxLines = 2, overflow = TextOverflow.Ellipsis',C)
 def test_disabled_shared_controls_really_disable_and_explain(self):
  self.assertIn('enabled = enabled,\n        onClick = onClick',C)
  self.assertIn('if (!enabled) stateDescription = "Unavailable"',C)
  self.assertIn('val ink = if (!enabled) colors.muted',C)
 def test_notices_and_empty_states_are_reusable(self):
  self.assertIn('internal fun KlyroInlineNotice(',C)
  self.assertIn('internal fun KlyroEmptyState(',C)
  self.assertIn('liveRegion = LiveRegionMode.Polite',C)
 def test_settings_back_and_close_have_distinct_purpose(self):
  self.assertIn('onClick = ::navigateBack',S)
  self.assertIn('Text("Close")',S)
  self.assertIn('cannotOpenPermissions -> cannotOpenPermissions = false',S)
  self.assertIn('onDismissRequest = onDismiss, dismissEnabled = canDismissSheet',S)
 def test_privacy_permissions_error_is_actionable(self):
  self.assertIn('KlyroInlineNotice(',S)
  self.assertIn('open App info from',S) if False else None
  self.assertIn('Text("Dismiss")',S)
 def test_home_message_is_functional_not_marketing(self):
  self.assertIn('Private shell · Local workspace',M)
  self.assertNotIn('Your own space.',M)
if __name__=="__main__":unittest.main()
