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
        self.assertIn("KlyroThinSlider(", settings)
        self.assertIn("valueRange = 9f..24f", settings)
        self.assertIn("steps = 29", settings)
        shared = src(UI / "KlyroUiComponents.kt")
        self.assertIn("SliderDefaults.colors(", shared)
        self.assertIn("thumb = {", shared)
        self.assertIn("KlyroUiMetrics.iosSliderTrack", shared)
        self.assertNotIn('Text("A−")', settings)
        self.assertNotIn('Text("A+")', settings)
        self.assertIn("KlyroUiMetrics.accentSwatch", settings)
        self.assertIn("Role.RadioButton", settings)

    def test_workspaces_use_compact_left_aligned_rows_with_contextual_actions(self):
        sheet = src(UI / "KlyroWorkspaceSheet.kt")
        self.assertIn("KlyroUiMetrics.workspaceRow", sheet)
        self.assertIn("KlyroUiMetrics.minTouch", sheet)
        self.assertIn("textAlign = TextAlign.Start", sheet)
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


    def test_home_and_terminal_copy_avoid_repeated_runtime_metadata(self):
        home = src(APP / "MainActivity.kt")
        pane = src(UI / "TerminalPane.kt")
        commands = src(UI / "KlyroCommandPalette.kt")
        self.assertIn('Text("RUNTIME STATUS"', home)
        self.assertNotIn('Text("PRIVATE ENVIRONMENT"', home)
        self.assertNotIn('A standalone Android shell with its own private environment', home)
        self.assertNotIn('Text("ARM64"', pane)
        self.assertIn("KlyroUiMetrics.menuWidth", pane)
        self.assertIn("containerColor = ui.sheet", pane)
        self.assertNotIn("DropdownMenuItem(", pane)
        self.assertIn('"Insert only; press Enter to run."', commands)
        self.assertNotIn('"Not available · " + entry.note', commands)
        self.assertNotIn('ModalBottomSheet(', commands)

    def test_shared_sheet_menu_action_and_license_components_prevent_style_drift(self):
        shared = src(UI / "KlyroUiComponents.kt")
        for component in ("KlyroSheet(", "KlyroSectionHeading(",
                          "KlyroActionRow(", "KlyroMenuItem(",
                          "KlyroLicenseDialog("):
            self.assertIn(component, shared)
        self.assertEqual(shared.count("ModalBottomSheet("), 1)
        self.assertEqual(shared.count("DropdownMenuItem("), 1)
        self.assertIn("internal fun KlyroSheetTitle(", shared)
        for page, expected in (("KlyroSettingsSheet.kt", 1),
                               ("KlyroCommandPalette.kt", 1),
                               ("KlyroWorkspaceSheet.kt", 1),
                               ("KlyroSessionSheets.kt", 2)):
            self.assertEqual(src(UI / page).count("KlyroSheetTitle("),
                             expected, page)
        for name, count in (("KlyroSettingsSheet.kt", 1),
                            ("KlyroWorkspaceSheet.kt", 1),
                            ("KlyroCommandPalette.kt", 1),
                            ("KlyroSessionSheets.kt", 2)):
            page = src(UI / name)
            with self.subTest(page=name):
                self.assertEqual(page.count("KlyroSheet("), count)
                self.assertNotIn("ModalBottomSheet(", page)
        settings = src(UI / "KlyroSettingsSheet.kt")
        self.assertEqual(settings.count("KlyroLicenseDialog("), 2)
        self.assertNotIn("private fun SectionHeading(", settings)
        self.assertNotIn("Klyro SAF access · no Unix mount", settings)
        self.assertNotIn("Terminal settings and Android integration", settings)
        self.assertIn("KlyroActionRow(", settings)
        self.assertNotIn("SliderDefaults.colors(", settings)
        picker = src(UI / "KlyroColorPickerDialog.kt")
        self.assertEqual(picker.count("KlyroThinSlider("), 2)
        self.assertNotIn("Slider(", picker.replace("KlyroThinSlider(", ""))
        self.assertIn("CornerRadius(track / 2f)", picker)
        workspace = src(UI / "KlyroWorkspaceSheet.kt")
        self.assertEqual(workspace.count("KlyroMenuItem("), 2)
        self.assertNotIn("Sessions · ", workspace)
        sessions = src(UI / "KlyroSessionSheets.kt")
        self.assertEqual(sessions.count("KlyroActionRow("), 4)
        self.assertNotIn("private fun SheetAction(", sessions)
        picker = src(UI / "KlyroColorPickerDialog.kt")
        self.assertNotIn('Text("Preview")', picker)
        self.assertIn('Text("Edit")', picker)

    def test_operational_release_gates_for_storage_clipboard_and_workspace_accessibility(self):
        activity = src(APP / "MainActivity.kt")
        terminal = src(UI / "TerminalPane.kt")
        settings = src(UI / "KlyroSettingsSheet.kt")
        workspace = src(UI / "KlyroWorkspaceSheet.kt")

        # One inaccessible provider may not abort remaining SAF grants.
        self.assertIn("if (imported >= 30) break", activity)
        self.assertIn("readableFolders == 0", activity)
        self.assertIn("skippedFolders++", activity)
        self.assertIn("folders skipped", activity)
        self.assertIn("Copy up to 30 files", settings)
        self.assertIn("Export klyro-export.txt", settings)

        # The Select all action must never be a paste alias.
        self.assertIn('"ALL", description = "Select all terminal output"', terminal)
        self.assertIn('selectTerminalOutput()', terminal)
        # User can always paste irrespective of terminal transcript state.
        self.assertIn('TerminalExtraKey(\n                "PASTE"', terminal)
        self.assertIn("enabled = clipboardAvailable", terminal)
        self.assertNotIn('KlyroMenuItem("Select output"', terminal)
        self.assertNotIn('if (hasCopyableOutput) "SELECT" else "PASTE"', terminal)
        self.assertNotIn("sessionClient.setOutputObserver(observer)", terminal)

        # Each workspace has a single full-row selectable action; its
        # independent overflow remains a separate TalkBack target.
        self.assertEqual(workspace.count('onClickLabel = "Select workspace "'), 1)
        self.assertIn("role = Role.RadioButton", workspace)
        self.assertIn("Options for ", workspace)
        self.assertIn('if (!themeMode.startsWith("mono-"))', settings)

    def test_adversarial_visual_semantics_and_screen_readability(self):
        settings = src(UI / "KlyroSettingsSheet.kt")
        workspace = src(UI / "KlyroWorkspaceSheet.kt")
        terminal = src(UI / "TerminalPane.kt")
        drawable = ROOT / "app/src/main/res/drawable"

        # When switching from the Bento home grid to a subsection, reset
        # scroll: the title/back button must not remain above the viewport.
        self.assertIn("remember(section) { ScrollState(0) }", settings)
        self.assertIn('Text("Settings")', settings)
        self.assertNotIn("prominent = true", settings)
        self.assertIn('R.drawable.hugeicons_palette', settings)
        self.assertIn('R.drawable.ic_klyro_keyboard_keys', settings)
        self.assertIn('R.drawable.hugeicons_pulse_rectangle_01', settings)
        self.assertIn("KlyroThemePreviewCard(", settings)
        self.assertNotIn('Text("9 sp"', settings)
        self.assertNotIn('Text("24 sp"', settings)
        for glyph in ('"◩"', '"●"', '"○"'):
            self.assertNotIn(glyph, settings)
        self.assertNotIn('Text("Selected: " + chosen', settings)
        self.assertTrue((drawable / "ic_klyro_keyboard_keys.xml").is_file())
        keyboard = ET.parse(drawable / "ic_klyro_keyboard_keys.xml").getroot()
        self.assertEqual(keyboard.tag, "vector")
        self.assertGreaterEqual(len(keyboard.findall("path")), 3)

        # Distinct destination icons: search, workspace grid, home and gear.
        menu = terminal.split("val toolsMenu: @Composable () -> Unit =", 1)[1].split(
            "\n    Column(", 1
        )[0]
        for label, icon in (("Command Palette", "search_01"),
                            ("Workspaces", "grid2x2"),
                            ("Settings", "settings_01"),
                            ("Home", "home_03")):
            self.assertIn('KlyroMenuItem("' + label +
                          '", R.drawable.hugeicons_' + icon, menu)
        self.assertNotIn(
            'KlyroMenuItem("Home", R.drawable.hugeicons_terminal)', menu)

        # Icon-only CTAs have TalkBack names and explicit interaction states.
        self.assertNotIn("Text(label, fontSize = KlyroUiMetrics.shortcutCaption", terminal)
        self.assertIn("ShortcutState.HOLD", terminal)
        self.assertIn("stateDescription = keyState.name.lowercase()", terminal)
        self.assertIn('TerminalExtraKey("CLEAR"', terminal)
        self.assertNotIn('KlyroMenuItem("Select output"', terminal)
        self.assertIn('TerminalExtraKey(\n                "PASTE"', terminal)
        self.assertNotIn('if (hasCopyableOutput) "SELECT" else "PASTE"', terminal)
        self.assertNotIn('sessionClient.setOutputObserver(observer)', terminal)
        self.assertNotIn(' · Selected', workspace)
        self.assertIn('textAlign = TextAlign.Start', workspace)
        self.assertIn('Text(\n                    "Sessions"', workspace)
        self.assertNotIn('"Sessions (" + projectTabs.size', workspace)

    def test_hugeicons_assets_are_vectorized_and_ui_actions_use_real_icons(self):
        drawable = ROOT / "app/src/main/res/drawable"
        names = (
            "colors", "keyboard", "folder_01", "shield_01",
            "download_01", "upload_01", "chevron_right",
            "more_vertical", "pen_01", "trash", "plus_sign", "arrow_left_01",
            "arrow_up_right_01", "cancel_circle", "terminal",
            "palette", "pulse_rectangle_01", "home_03", "grid2x2",
            "contrast", "moon_01", "file_01", "search_01",
            "checkmark_circle_01",
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
        self.assertEqual(menu.count("KlyroMenuItem("), 4)
        shared = src(UI / "KlyroUiComponents.kt")
        self.assertEqual(shared.count("DropdownMenuItem("), 1)
        self.assertIn("leadingIcon =", shared)
        self.assertIn("R.drawable.hugeicons_more_vertical", menu)
        self.assertNotIn('Text("+", fontSize', pane)
        palette = src(UI / "KlyroCommandPalette.kt")
        self.assertIn("R.drawable.hugeicons_terminal", palette)
        self.assertNotIn('Text(">_"', palette)

if __name__ == "__main__":
    unittest.main()
