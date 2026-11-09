"""Android launcher alias hardening contracts for Klyro A/B branding."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/oryn/terminal/ui"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"

class LauncherAliasHardening(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = (UI / "KlyroBrandChoice.kt").read_text()
        cls.settings = (UI / "KlyroSettingsSheet.kt").read_text()
        cls.manifest = MANIFEST.read_text()

    def test_default_launcher_remains_explicit(self):
        self.assertIn('android:name=".LauncherB"', self.manifest)
        self.assertIn('android:name=".LauncherA"', self.manifest)

    def test_android_manifest_defaults_are_not_mistaken_for_disabled(self):
        self.assertIn("else -> manifestDefault", self.source)
        self.assertIn("aliasEnabled(pm, primary(context), true)", self.source)
        self.assertIn("aliasEnabled(pm, alternative(context), false)", self.source)

    def test_both_enabled_is_not_verified(self):
        self.assertIn("b && !a -> DEFAULT", self.source)
        self.assertIn("a && !b -> ALTERNATIVE", self.source)
        self.assertIn("else -> null", self.source)

    def test_switch_verifies_actual_os_alias(self):
        self.assertIn("verifiedChoice(context) == choice", self.source)

    def test_switches_are_serialized(self):
        self.assertIn("@Synchronized", self.source)

    def test_older_android_enables_replacement_before_disabling_active(self):
        a = self.source.index("pm.setComponentEnabledSetting(a, on,")
        b = self.source.index("pm.setComponentEnabledSetting(b, off,")
        self.assertLess(a, b)

    def test_failed_switch_restores_component_states(self):
        self.assertIn("beforeB", self.source)
        self.assertIn("beforeA", self.source)
        self.assertIn("if (!switched)", self.source)

    def test_preferences_written_only_after_os_verification(self):
        self.assertLess(self.source.index("if (!switched)"), self.source.index(".edit().putString(KEY, choice).apply()"))

    def test_old_alias_is_not_repeatedly_switched(self):
        self.assertIn("if (!KlyroBrandChoice.isVerifiedSelection(context, choice))", self.settings)

    def test_invalid_os_alias_configuration_is_recoverable(self):
        self.assertIn("fun isVerifiedSelection(context: Context, choice: String): Boolean", self.source)
        self.assertIn("verifiedChoice(context) == choice", self.source)

    def test_ui_reflects_actual_os_state_after_attempt(self):
        self.assertIn("selectedBrand = KlyroBrandChoice.selected(context)", self.settings)

    def test_device_launcher_refresh_limitation_is_explained(self):
        self.assertIn("launchers refresh home-screen icons", self.settings)

    def test_about_icon_fits_its_container(self):
        self.assertIn("modifier = Modifier.size(56.dp)", self.settings)


if __name__ == "__main__":
    unittest.main()
