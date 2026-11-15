"""Regression guard: Settings sheets and errors must survive configuration changes."""
import unittest
from pathlib import Path
P = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui/KlyroSettingsSheet.kt"
S = P.read_text()
class SettingsOverlayRotationTest(unittest.TestCase):
    def test_licenses_and_permission_error_are_saveable(self):
        for k in ("showFontLicense", "showHugeiconsLicense", "cannotOpenPermissions", "brandSwitchError"):
            self.assertIn(f"var {k} by rememberSaveable", S)
    def test_settings_back_routes_to_parent_and_keeps_dialog_open(self):
        self.assertIn('BackHandler(enabled = canDismissSheet) { onDismiss() }', S)
        self.assertIn('showFontLicense -> showFontLicense = false', S)
        self.assertIn('showHugeiconsLicense -> showHugeiconsLicense = false', S)
        self.assertIn('KlyroSheet(onDismissRequest = onDismiss, dismissEnabled = canDismissSheet)', S)
if __name__=="__main__": unittest.main()
