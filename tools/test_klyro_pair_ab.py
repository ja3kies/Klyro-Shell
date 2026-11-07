"""A+B release source regressions, supplementing JVM and Android Lint."""
from pathlib import Path
import unittest
from klyro_version_contract import assert_supported_klyro_version

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/oryn/terminal/ui"

class KlyroPairABTest(unittest.TestCase):
    def test_color_is_staged_until_apply(self):
        picker = (UI/"KlyroColorPickerDialog.kt").read_text()
        self.assertIn("var pendingPreset by remember(initialArgb)", picker)
        self.assertIn("pendingPreset = preset.id", picker)
        self.assertIn('onApply(pendingPreset ?: KlyroColors.customId(argb()))', picker)
        self.assertIn('pendingPreset = null', picker)
        self.assertNotIn('onApply(preset.id)', picker)
        # Preview was a duplicate of Edit; editing still stages until Apply.
        self.assertIn('Text("Edit")', picker)
        self.assertNotIn('Text("Preview")', picker)
        self.assertIn('keyboardType = KeyboardType.Ascii', picker)
        self.assertIn('LocalConfiguration.current.screenHeightDp', picker)
        self.assertIn('Brush.horizontalGradient(', picker)
        self.assertIn('if (!showAdvanced && pendingFavoriteDeletion == null)', picker)
        self.assertIn('if (showAdvanced && pendingFavoriteDeletion == null)', picker)

    def test_modal_geometry_and_settings_density_are_shared(self):
        design = (UI/"KlyroDesign.kt").read_text()
        self.assertIn("object KlyroUiMetrics", design)
        self.assertIn("val modalCorner = 28.dp", design)
        self.assertIn("val cardCorner = 22.dp", design)
        for name in ("KlyroSettingsSheet.kt","KlyroSessionSheets.kt",
                     "KlyroColorPickerDialog.kt","TerminalPane.kt"):
            self.assertIn("KlyroUiMetrics.",(UI/name).read_text(),name)
        settings=(UI/"KlyroSettingsSheet.kt").read_text()
        self.assertIn("KlyroUiMetrics.sheetContentWidth", settings)
        self.assertIn("KlyroUiMetrics.sheetContentMax", settings)
        self.assertIn("KlyroUiMetrics.minTouch", settings)
        self.assertIn("role = Role.Button", settings)
        self.assertIn("KlyroThinSlider(", settings)
        shared = (UI/"KlyroUiComponents.kt").read_text()
        self.assertIn("Slider(", shared)
        self.assertFalse((UI/"KlyroExperimentalBento.kt").exists())

    def test_alerts_and_errors_do_not_destroy_sessions(self):
        pane=(UI/"TerminalPane.kt").read_text()
        self.assertIn('TextButton(onClick = onReturnHome)',pane)
        self.assertNotIn('Text("Back to setup"',pane)
        self.assertIn('color = ui.warningContainer',pane)
        self.assertIn('color = ui.dangerContainer',pane)
        self.assertIn('Text("End session", color = ui.danger',pane)
        root=(ROOT/"app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
        self.assertIn("LinearProgressIndicator(",root)
        self.assertNotIn('val ui.accent = ui.accent',root)
        self.assertIn('BOOTSTRAP INSTALLED',root)

    def test_build_version_bumped(self):
        build=(ROOT/"app/build.gradle.kts").read_text()
        assert_supported_klyro_version(self, build)
        self.assertTrue((ROOT / "docs/OBJECTIVES_H_I_2026-11-06.md").is_file())
        self.assertTrue((ROOT/"docs/QA_PIXEL9_0_4_2_DEV.md").is_file())


if __name__ == "__main__":
    unittest.main()
