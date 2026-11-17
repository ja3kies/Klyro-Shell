"""Settings layout/state contracts; real rendering still requires Android QA."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / 'app/src/main/java/com/oryn/terminal/ui'
S = (UI / 'KlyroSettingsSheet.kt').read_text()
P = (UI / 'KlyroThemePreviewCard.kt').read_text()
B = (UI / 'KlyroBrandChoice.kt').read_text()


class SettingsRefinement(unittest.TestCase):
    def test_frequent_appearance_controls_precede_launcher_identity(self):
        appearance = S.split('"appearance" -> {', 1)[1].split('"keyboard" -> {', 1)[0]
        self.assertLess(appearance.index('KlyroAppearanceGroup("App theme"'), appearance.index('Text("Terminal font"'))
        self.assertLess(appearance.index('Text("Terminal font"'), appearance.index('KlyroAppearanceGroup("Launcher icon"'))

    def test_same_responsive_layout_serves_all_choice_groups(self):
        self.assertGreaterEqual(S.count('KlyroSettingsChoiceGrid('), 5)
        grid = UI / 'KlyroSettingsChoiceGrid.kt'
        self.assertTrue(grid.exists())
        source = grid.read_text()
        self.assertIn('maxWidth < minimumInlineWidth || LocalDensity.current.fontScale >= 1.3f', source)
        self.assertIn('maxItemsInEachRow = if (stacked) 1 else columns', source)
        self.assertIn('minimumInlineWidth: Dp = 340.dp', source)
        self.assertNotIn('maxLines = 1', P)

    def test_accent_presets_are_exposed_as_one_radio_group(self):
        appearance = S.split('"appearance" -> {', 1)[1].split('"keyboard" -> {', 1)[0]
        swatches = appearance.split('KlyroColors.primary.forEach { preset ->', 1)[0]
        self.assertIn('Modifier.selectableGroup()', swatches[-500:])
        self.assertIn('clickable(role = Role.RadioButton', appearance)
        self.assertIn('selected = active', appearance)

    def test_unknown_launcher_state_is_not_selected_as_default(self):
        self.assertIn('fun confirmedSelection(context: Context): String?', B)
        self.assertIn('brandReady = actual != null', S)
        self.assertIn('selected = brandReady && selectedBrand == choice', S)
        self.assertIn('Text("Retry icon check")', S)

    def test_storage_loading_and_failure_are_not_reported_as_zero(self):
        self.assertIn('.getOrDefault(-2)', S)
        self.assertIn('"Checking connected folders…"', S)
        self.assertIn('"Folder status unavailable"', S)
        self.assertIn('Text("Retry folder check")', S)

    def test_diagnostics_title_precedes_report_navigation(self):
        diagnostics = S.split('"diagnostics" -> {', 1)[1].split('"about" -> {', 1)[0]
        self.assertLess(diagnostics.index('KlyroSectionHeading("Diagnostics"'),
                        diagnostics.index('SettingsRowLink("Freeze reports"'))


if __name__ == '__main__':
    unittest.main()
