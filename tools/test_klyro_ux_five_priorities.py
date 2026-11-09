"""Adversarial source contracts; runtime interactions still need Android acceptance."""
from pathlib import Path
import unittest

UI = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui"


class UXFivePriorities(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.shared = (UI / "KlyroUiComponents.kt").read_text()
        cls.sessions = (UI / "KlyroSessionSheets.kt").read_text()
        cls.palette = (UI / "KlyroCommandPalette.kt").read_text()
        cls.workspaces = (UI / "KlyroWorkspaceSheet.kt").read_text()
        cls.settings = (UI / "KlyroSettingsSheet.kt").read_text()
        cls.plugins = (UI / "KlyroPluginsPage.kt").read_text()
        cls.terminal = (UI / "TerminalPane.kt").read_text()

    def test_shared_actions_have_button_semantics(self):
        self.assertIn(".clickable(role = Role.Button", self.shared)

    def test_shared_actions_expose_subtitle_to_screen_readers(self):
        self.assertIn('else "$label. $subtitle"', self.shared)

    def test_destructive_actions_expose_a_warning(self):
        self.assertIn('stateDescription = "Destructive action"', self.shared)

    def test_menu_items_are_large_enough(self):
        self.assertIn("Modifier.heightIn(min = KlyroUiMetrics.minTouch)", self.shared)

    def test_session_rename_field_explains_constraints(self):
        self.assertIn("Maximum 32 characters.", self.sessions)

    def test_session_rename_rejects_blank_inputs(self):
        self.assertIn("value.text.trim().isNotBlank()", self.sessions)

    def test_shell_selection_has_role_and_hit_target(self):
        self.assertIn('onClickLabel = "Start a Bash terminal"', self.sessions)
        self.assertIn(".heightIn(min = KlyroUiMetrics.minTouch)", self.sessions)

    def test_search_can_be_cleared(self):
        self.assertIn('Text("Clear")', self.palette)
        self.assertIn('query = ""; info = null', self.palette)

    def test_no_results_has_escape(self):
        self.assertIn('Text("Show all commands")', self.palette)

    def test_unsafe_plugin_insertion_reports_failure(self):
        self.assertIn("This plugin command cannot be inserted safely.", self.palette)

    def test_palette_error_is_announced(self):
        self.assertIn("liveRegion = LiveRegionMode.Polite", self.palette)

    def test_command_rows_have_hit_target(self):
        self.assertIn("min = KlyroUiMetrics.minTouch", self.palette)

    def test_workspace_catalog_has_empty_state(self):
        self.assertIn("Workspaces unavailable", self.workspaces)

    def test_workspace_name_length_visible(self):
        self.assertIn("characters remaining", self.workspaces)

    def test_workspace_invalid_save_is_disabled(self):
        self.assertIn("enabled = editorText.trim().isNotEmpty()", self.workspaces)

    def test_workspace_sessions_have_hit_targets(self):
        self.assertIn(".heightIn(min = KlyroUiMetrics.minTouch)", self.workspaces)

    def test_settings_rejects_invalid_deep_links(self):
        self.assertIn('} ?: "home")', self.settings)

    def test_settings_back_returns_to_root(self):
        self.assertIn('BackHandler(', self.settings)
        self.assertIn(') { section = "home" }', self.settings)

    def test_settings_back_does_not_override_dialogs(self):
        route = self.settings.split("fun navigateBack()", 1)[1].split("BackHandler(", 1)[0]
        self.assertIn('showColorPicker -> showColorPicker = false', route)
        self.assertIn('showFontLicense -> showFontLicense = false', route)
        self.assertIn('showHugeiconsLicense -> showHugeiconsLicense = false', route)
        self.assertIn('section != "home" -> section = "home"', route)

    def test_launcher_action_uses_verified_os_state(self):
        self.assertIn("KlyroBrandChoice.isVerifiedSelection(context, choice)", self.settings)

    def test_launcher_and_permission_errors_announced(self):
        self.assertGreaterEqual(self.settings.count("liveRegion = LiveRegionMode.Polite"), 2)

    def test_bundled_add_only_opens_confirmation(self):
        section = self.plugins.split('KlyroPluginManager.bundled.forEach', 1)[1].split('pendingBundledId?.let', 1)[0]
        self.assertIn('pendingBundledId = plugin.id', section)
        self.assertNotIn('manager.installBundled(plugin.id)', section)

    def test_bundled_confirm_is_explicit(self):
        self.assertIn('title = { Text("Add ${record.name}?") }', self.plugins)
        self.assertIn('Text("Add pack")', self.plugins)
        self.assertIn('applyChange({ manager.installBundled(id) }) {', self.plugins)

    def test_bundled_cancel_does_not_install(self):
        self.assertIn('Text("Cancel")', self.plugins)
        self.assertIn('pendingBundledId = null; error = null', self.plugins)

    def test_bundled_back_dismisses_before_navigation(self):
        self.assertIn('pendingBundledId != null -> { pendingBundledId = null; error = null }', self.plugins)

    def test_bundled_failed_install_keeps_confirmation_open(self):
        self.assertIn('applyChange({ manager.installBundled(id) }) {\n                                pendingBundledId = null', self.plugins)
        self.assertIn('result.onSuccess { updated ->', self.plugins)
        self.assertIn('onSuccess()', self.plugins)

    def test_plugins_keep_import_open_on_failure(self):
        self.assertIn('applyChange({ manager.installPreview(record) }) {', self.plugins)

    def test_plugins_keep_remove_open_on_failure(self):
        self.assertIn('applyChange({ manager.uninstall(id) }) {', self.plugins)

    def test_plugins_have_state_specific_accessibility(self):
        self.assertIn('if (plugin.enabled) "Disable " else "Enable "', self.plugins)

    def test_plugins_announce_errors(self):
        self.assertIn('liveRegion = LiveRegionMode.Polite', self.plugins)

    def test_contextual_all_behavior_unchanged(self):
        self.assertIn('description = if (canSelect) "Select all terminal output" else "Paste clipboard"', self.terminal)


if __name__ == "__main__":
    unittest.main()
