"""Source-level guards for the third Klyro UX cycle.

These contracts do not replace hands-on TalkBack and press/hold testing.
"""
from pathlib import Path
import unittest

UI = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui"


class CycleThreeUX(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pane = (UI / "TerminalPane.kt").read_text()
        cls.plugins = (UI / "KlyroPluginsPage.kt").read_text()
        cls.picker = (UI / "KlyroColorPickerDialog.kt").read_text()
        cls.sessions = (UI / "KlyroSessionSheets.kt").read_text()
        cls.workspaces = (UI / "KlyroWorkspaceSheet.kt").read_text()
        cls.palette = (UI / "KlyroCommandPalette.kt").read_text()
        cls.settings = (UI / "KlyroSettingsSheet.kt").read_text()

    def test_tooltip_appears_only_during_enabled_press(self):
        self.assertIn("if (held && enabled)", self.pane)

    def test_tooltip_does_not_capture_focus(self):
        self.assertIn("PopupProperties(focusable = false)", self.pane)

    def test_tooltip_uses_long_press_action_name(self):
        self.assertIn("longClickDescription ?: description ?: label", self.pane)

    def test_tooltip_uses_modal_semantic_colors(self):
        self.assertIn("color = colors.sheet", self.pane)
        self.assertIn("contentColor = colors.foreground", self.pane)

    def test_visual_all_labels_follow_context(self):
        self.assertIn('iconCaption = if (canSelect) "SELECT" else "PASTE"', self.pane)

    def test_visual_all_colors_follow_context(self):
        self.assertIn("pasteTone = !canSelect", self.pane)
        self.assertIn("if (pasteTone) colors.warningContainer else colors.key", self.pane)

    def test_all_action_behavior_is_preserved(self):
        self.assertIn(") { selectTerminalOutput(canSelect) }", self.pane)

    def test_end_and_arrow_navigation_kept(self):
        self.assertIn('TerminalExtraKey("END"', self.pane)
        self.assertIn('longClickDescription = "Go to end of line"', self.pane)

    def test_plugin_dialogs_share_radius(self):
        self.assertEqual(self.plugins.count("AlertDialog("),
                         self.plugins.count("shape = RoundedCornerShape(KlyroUiMetrics.modalCorner)"))

    def test_plugin_add_confirmation_retained(self):
        self.assertIn('pendingBundledId = plugin.id', self.plugins)
        self.assertIn('Text("Add pack")', self.plugins)

    def test_picker_hex_and_preview_touch_height(self):
        self.assertIn("Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch)", self.picker)
        self.assertIn("Modifier.size(KlyroUiMetrics.minTouch)", self.picker)

    def test_hex_invalid_state_has_live_feedback(self):
        self.assertIn("LiveRegionMode.Polite", self.picker)

    def test_favorite_name_limit_is_discoverable(self):
        self.assertIn("characters remaining", self.picker)

    def test_favorite_management_buttons_are_large(self):
        self.assertIn("modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)", self.picker)

    def test_workspace_empty_modal_is_rounded(self):
        self.assertEqual(self.workspaces.count("AlertDialog("),
                         self.workspaces.count("shape = RoundedCornerShape(KlyroUiMetrics.modalCorner)"))

    def test_back_cancels_workspace_editor(self):
        self.assertIn("BackHandler(enabled = editorId != null", self.workspaces)
        self.assertIn("editorId = null", self.workspaces)

    def test_back_in_palette_returns_from_guide(self):
        self.assertIn("BackHandler(enabled = showingGuide) { showingGuide = false }", self.palette)

    def test_appearance_sections_can_scroll(self):
        self.assertIn('rows = if (section == "home") 6 else 7', self.settings)

    def test_appearance_font_reset_is_accessible(self):
        self.assertIn("onClick = onResetFont,", self.settings)
        self.assertIn("modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)", self.settings)

    def test_launcher_selected_state_has_visual_check(self):
        self.assertIn("if (selectedBrand == choice)", self.settings)
        self.assertIn("R.drawable.hugeicons_checkmark_circle_01", self.settings)

    def test_launcher_error_can_recover_on_verified_choice(self):
        self.assertIn("brandSwitchError = false", self.settings)

    def test_terminal_errors_are_announced(self):
        self.assertGreaterEqual(self.pane.count("liveRegion = LiveRegionMode.Polite"), 2)

    def test_terminal_find_has_large_navigation_targets(self):
        self.assertIn("searchIndex = TranscriptSearch.nextIndex(position, -1", self.pane)
        self.assertIn("searchIndex = TranscriptSearch.nextIndex(position, 1", self.pane)

    def test_plugin_tabs_are_large(self):
        self.assertIn("modifier = Modifier.weight(1f)\n                        .heightIn(min = KlyroUiMetrics.minTouch)", self.plugins)

    def test_plugin_dialog_cancel_resets_error(self):
        self.assertIn("pendingImport = null; error = null", self.plugins)
        self.assertIn("removeId = null; error = null", self.plugins)

    def test_shell_picker_uses_standard_spacing(self):
        self.assertIn("Arrangement.spacedBy(KlyroUiMetrics.sectionGap)", self.sessions)

    def test_shell_picker_back_is_touch_accessible(self):
        self.assertIn("modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)", self.sessions)


if __name__ == "__main__":
    unittest.main()
