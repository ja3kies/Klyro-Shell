"""Settings are accessible from Home before Bash startup and share one diagnostic source."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal"
M=(ROOT/"MainActivity.kt").read_text()
P=(ROOT/"ui/TerminalPane.kt").read_text()
S=(ROOT/"ui/KlyroSettingsSheet.kt").read_text()
class HomeSettingsUX(unittest.TestCase):
 def test_home_has_settings_without_creating_pty(self):
  self.assertIn('onClickLabel = "Open Klyro settings"',M)
  self.assertIn('showHomeSettings = true',M)
  self.assertIn('if (showHomeSettings) {\n        KlyroSettingsSheet(',M)
  self.assertIn('onDismiss = { showHomeSettings = false }',M)
  settings=M.split('if (showHomeSettings) {',1)[1].split('if (showRepairConfirmation)',1)[0]
  self.assertNotIn('launchBash()',settings)
 def test_font_preferences_are_not_duplicated_in_app_state(self):
  self.assertIn('getSharedPreferences("klyro_terminal_preferences"',M)
  self.assertIn('homeFontPrefs.edit().putString("font_family", homeFontFamily).apply()',M)
  self.assertIn('homeFontPrefs.edit().remove("font_size_sp").apply()',M)
  self.assertIn('LaunchedEffect(showHomeSettings)',M)
 def test_diagnostics_available_from_both_surfaces(self):
  self.assertEqual(1,M.count('val diagnosticReport = KlyroHealthReport.format('))
  self.assertIn('diagnosticReport = diagnosticReport,',M)
  self.assertIn('Klyro health snapshot',M)
  self.assertIn('Share Klyro report',M)
 def test_verification_button_reflects_running_check(self):
  self.assertIn('verifyingRuntime = verifyingRuntime',M)
  self.assertIn('verifyingRuntime = verifyingRuntime',P)
  self.assertIn('enabled = !verifyingRuntime',S)
  self.assertIn('Text(if (verifyingRuntime) "Checking environment…"',S)
 def test_existing_terminal_modifier_release_preserved(self):
  self.assertIn('onResetModifiers = {\n                controlActive = false',P)
  self.assertIn('viewClient?.setFnDown(false)',M)
if __name__=="__main__":unittest.main()
