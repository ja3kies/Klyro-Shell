"""Adversarial Compose UI architecture checks (not a substitute for device screenshots)."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

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

    def test_legacy_actions_not_duplicated_in_palette_or_settings(self):
        palette = src(UI / "KlyroCommandPalette.kt")
        settings = src(UI / "KlyroSettingsSheet.kt")
        self.assertNotIn('"Copy output"', palette)
        self.assertNotIn('"Clear display"', palette)
        self.assertNotIn('"sessions" -> {', settings)
        self.assertNotIn("ActionTile(", settings)
        self.assertNotIn("Preview each typeface before choosing.", settings)


    def test_hugeicons_assets_are_vectorized_and_ui_actions_use_real_icons(self):
        drawable = ROOT / "app/src/main/res/drawable"
        names = (
            "colors", "keyboard", "folder_01", "shield_01",
            "download_01", "upload_01", "chevron_right",
            "more_vertical", "pen_01", "trash", "plus_sign", "arrow_left_01",
            "arrow_up_right_01", "cancel_circle", "terminal",
        )
        ns = "{http://schemas.android.com/apk/res/android}"
        for name in names:
            with self.subTest(icon=name):
                path = drawable / ("hugeicons_" + name + ".xml")
                self.assertTrue(path.is_file(), str(path))
                vector = ET.parse(path).getroot()
                self.assertEqual(vector.tag, "vector")
                paths = vector.findall("path")
                self.assertGreaterEqual(len(paths), 1)
                self.assertTrue(all(p.get(ns + "pathData") for p in paths))

        settings = src(UI / "KlyroSettingsSheet.kt")
        self.assertIn("private fun SettingsBentoTile(", settings)
        self.assertIn("icon: Int,", settings)
        self.assertIn("Icon(painterResource(icon)", settings)
        for token in ('"⌨"', '"▣"', '"◇"', 'glyph = "↓"', 'glyph = "↑"'):
            self.assertNotIn(token, settings)
        self.assertNotIn('Text("‹  Settings")', settings)
        self.assertIn("hugeicons_arrow_left_01", settings)
        self.assertIn("hugeicons-MIT.txt", settings)
        self.assertTrue((ROOT / "app/src/main/assets/licenses/hugeicons-MIT.txt").is_file())

        workspaces = src(UI / "KlyroWorkspaceSheet.kt")
        self.assertIn("R.drawable.hugeicons_pen_01", workspaces)
        self.assertIn("R.drawable.hugeicons_trash", workspaces)
        self.assertIn("R.drawable.hugeicons_more_vertical", workspaces)
        self.assertNotIn('leadingIcon = { Text(', workspaces)
        self.assertNotIn('Text("+ New")', workspaces)

        sessions = src(UI / "KlyroSessionSheets.kt")
        self.assertIn("R.drawable.hugeicons_plus_sign", sessions)
        self.assertIn("R.drawable.hugeicons_cancel_circle", sessions)
        self.assertNotIn('SheetAction("×"', sessions)
        self.assertNotIn('Text("←")', sessions)

        pane = src(UI / "TerminalPane.kt")
        menu = pane.split("val toolsMenu: @Composable () -> Unit =", 1)[1].split(
            "\n    Column(", 1
        )[0]
        self.assertEqual(menu.count("DropdownMenuItem("), 4)
        self.assertEqual(menu.count("leadingIcon ="), 4)
        self.assertIn("R.drawable.hugeicons_more_vertical", menu)
        self.assertNotIn('Text("+", fontSize', pane)
        palette = src(UI / "KlyroCommandPalette.kt")
        self.assertIn("R.drawable.hugeicons_terminal", palette)
        self.assertNotIn('Text(">_"', palette)

if __name__ == "__main__":
    unittest.main()
