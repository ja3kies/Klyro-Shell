"""Adversarial Compose UI architecture checks (not a substitute for device screenshots)."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/oryn/terminal/ui"
APP = ROOT / "app/src/main/java/com/oryn/terminal"


def src(path):
    return Path(path).read_text(encoding="utf-8")


class KlyroMobileDesignAudit(unittest.TestCase):
    def test_one_settings_information_architecture_across_build_types(self):
        settings = src(UI / "KlyroSettingsSheet.kt")
        gradle = src(ROOT / "app/build.gradle.kts")
        self.assertNotIn("KlyroExperimentalBento(", settings)
        self.assertNotIn("klyro_experimental_bento", gradle)
        self.assertFalse((UI / "KlyroExperimentalBento.kt").exists())
        for section in ("appearance", "keyboard", "files", "privacy",
                        "diagnostics", "about"):
            self.assertIn('"' + section + '" -> {', settings)
        self.assertNotIn("SettingsTile(", settings)

    def test_typography_controls_use_slider_not_oversized_steppers(self):
        settings = src(UI / "KlyroSettingsSheet.kt")
        self.assertIn("Slider(", settings)
        self.assertIn("valueRange = 9f..24f", settings)
        self.assertIn("steps = 29", settings)
        self.assertIn("SliderDefaults.colors(", settings)
        self.assertNotIn('Text("A−")', settings)
        self.assertNotIn('Text("A+")', settings)
        self.assertIn("KlyroUiMetrics.accentSwatch", settings)
        self.assertIn("Role.RadioButton", settings)

    def test_workspaces_use_compact_centered_rows_with_contextual_actions(self):
        sheet = src(UI / "KlyroWorkspaceSheet.kt")
        self.assertIn("KlyroUiMetrics.workspaceRow", sheet)
        self.assertIn("KlyroUiMetrics.minTouch", sheet)
        self.assertIn("textAlign = TextAlign.Center", sheet)
        self.assertIn("DropdownMenu(", sheet)
        self.assertIn("hugeicons_folder_01", sheet)
        self.assertNotIn('Text("Create project")', sheet)
        self.assertIn("onDeleteProject", sheet)
        self.assertIn("onRenameProject", sheet)

    def test_responsive_sheet_geometry_and_ime(self):
        for name in ("KlyroSettingsSheet.kt", "KlyroWorkspaceSheet.kt",
                     "KlyroCommandPalette.kt"):
            content = src(UI / name)
            with self.subTest(name=name):
                self.assertIn("KlyroUiMetrics.sheetContentWidth", content)
                self.assertIn("KlyroModalTheme", content)
                self.assertIn("imePadding()", content)
                self.assertIn("LocalKlyroColors", content)

    def test_command_palette_does_not_auto_run_storage_commands(self):
        catalog = src(UI / "KlyroCommandCatalog.java")
        pane = src(UI / "TerminalPane.kt")
        self.assertIn('"termux-setup-storage", "klyro-setup-storage"', catalog)
        self.assertIn('isAlternateBufferActive == false', pane)
        self.assertIn("session.emulator?.paste(snippet)", pane)
        self.assertNotIn('session.write("klyro-setup-storage\\n")', pane)

    def test_storage_bridge_uses_saf_and_no_broad_storage_permission(self):
        manifest = src(ROOT / "app/src/main/AndroidManifest.xml")
        activity = src(APP / "MainActivity.kt")
        bridge = src(APP / "termux/KlyroStorageCommandBridge.java")
        installer = src(APP / "termux/TermuxBootstrapInstaller.java")
        self.assertNotIn("MANAGE_EXTERNAL_STORAGE", manifest)
        self.assertNotIn("WRITE_EXTERNAL_STORAGE", manifest)
        self.assertIn("ActivityResultContracts.OpenDocumentTree()", activity)
        self.assertIn('uri, Intent.FLAG_GRANT_READ_URI_PERMISSION', activity)
        self.assertIn('completeStorageRequest("CANCELLED")', activity)
        self.assertIn('completeStorageRequest("OK")', activity)
        self.assertIn("Files.isSymbolicLink", bridge)
        self.assertIn("KlyroStorageCommandBridge.installHelper", installer)

    def test_private_cli_adapters_have_single_owner_and_no_implicit_privilege(self):
        installer = src(APP / "termux/KlyroShellHelperInstaller.java")
        info = src(APP / "termux/KlyroTermuxInfo.java")
        bridge = src(APP / "termux/KlyroStorageCommandBridge.java")
        self.assertIn('command.matches("klyro-[a-z0-9-]{1,48}")', installer)
        self.assertIn('Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)', installer)
        self.assertIn('if (!existing.contains("# " + marker)) return;', installer)
        self.assertIn('KlyroShellHelperInstaller.install', info)
        self.assertIn('KlyroShellHelperInstaller.install', bridge)
        self.assertIn('Usage: termux-info', info)
        self.assertNotIn('MANAGE_EXTERNAL_STORAGE', src(ROOT / "app/src/main/AndroidManifest.xml"))

    def test_legacy_actions_not_duplicated_in_palette_or_settings(self):
        palette = src(UI / "KlyroCommandPalette.kt")
        settings = src(UI / "KlyroSettingsSheet.kt")
        self.assertNotIn('"Copy output"', palette)
        self.assertNotIn('"Clear display"', palette)
        self.assertNotIn('"sessions" -> {', settings)
        self.assertNotIn("ActionTile(", settings)
        self.assertNotIn("Preview each typeface before choosing.", settings)


if __name__ == "__main__":
    unittest.main()
