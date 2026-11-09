"""Cross-surface adversarial UX checks. Not a substitute for physical device QA."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/oryn/terminal"
UI = APP / "ui"
READ = lambda path: path.read_text(encoding="utf-8")


class KlyroCrossSurfaceUxAudit(unittest.TestCase):
    def test_home_uses_launcher_identity_and_plugins_have_system_back(self):
        home = READ(APP / "MainActivity.kt")
        plugins = READ(UI / "KlyroPluginsPage.kt")
        self.assertIn('ic_klyro_brand_b', home)
        self.assertIn('Text("ARM64"', home)
        self.assertIn('ic_klyro_plugins', home)
        self.assertIn('rememberSaveable { mutableStateOf(false) }', home)
        self.assertIn("KlyroPluginsPage(onBack =", home)
        self.assertIn("BackHandler {", plugins)
        self.assertIn("pendingImport != null || pendingImportUri != null -> {", plugins)
        self.assertIn("var pendingImportUri by rememberSaveable", plugins)
        self.assertIn("removeId != null -> { removeId = null; error = null }", plugins)
        self.assertIn("else -> onBack()", plugins)
        self.assertIn('var page by rememberSaveable', plugins)

    def test_plugins_confirm_import_and_protect_against_unsafe_commands(self):
        plugins = READ(UI / "KlyroPluginsPage.kt")
        manager = READ(APP / "plugins/KlyroPluginManager.kt")
        palette = READ(UI / "KlyroCommandPalette.kt")
        self.assertIn("manager.previewManifest(Uri.parse(selectedUri))", plugins)
        self.assertIn("withContext(Dispatchers.IO)", plugins)
        self.assertIn("manager.installPreview(record)", plugins)
        self.assertIn("pendingImport?.let", plugins)
        self.assertIn('Text("Install")', plugins)
        self.assertIn("distinctBy { it.label to it.insert }", manager)
        self.assertIn("readOnlyCommands.contains(value)", manager)
        self.assertIn("safePackageSearch.matches(value)", manager)
        self.assertIn('"git diff --stat"', manager)
        self.assertNotIn('value.startsWith("$it ")', manager)
        self.assertIn("KlyroPluginManager.safeInsertion(command.insert)", palette)
        self.assertIn("isAlternateBufferActive == false", READ(UI / "TerminalPane.kt"))

    def test_plugins_and_settings_screen_reader_navigation(self):
        plugins = READ(UI / "KlyroPluginsPage.kt")
        components = READ(UI / "KlyroUiComponents.kt")
        palette = READ(UI / "KlyroCommandPalette.kt")
        workspaces = READ(UI / "KlyroWorkspaceSheet.kt")
        self.assertIn("this.selected = selected", plugins)
        self.assertIn('if (plugin.enabled) "Disable " else "Enable "', plugins)
        self.assertIn("maxLines = 1, overflow = TextOverflow.Ellipsis", components)
        self.assertIn("modifier = Modifier.weight(1f)", palette)
        self.assertIn('KlyroSheetTitle("Workspaces", modifier = Modifier.weight(1f))', workspaces)
        self.assertIn('R.drawable.hugeicons_folder_01', plugins)
        self.assertIn('R.drawable.hugeicons_pulse_rectangle_01', plugins)
        self.assertIn('R.drawable.ic_klyro_plugins', plugins)

    def test_terminal_selection_is_read_only_and_never_pastes(self):
        pane = READ(UI / "TerminalPane.kt")
        selection = pane.split("fun selectTerminalOutput(expectedSelection: Boolean)", 1)[1].split("val keyWidth =", 1)[0]
        self.assertIn("selectedOutput = TerminalActionPolicy.selectionTail(output)", selection)
        self.assertNotIn(".paste(", selection)
        self.assertIn('"ALL",\n                description = if (canSelect) "Select all terminal output" else "Paste clipboard"', pane)
        self.assertNotIn('"PASTE", description = "Paste clipboard text"', pane)
        self.assertIn("keyListener = null", pane)
        self.assertIn("ui.terminalInk.toArgb()", pane)
        self.assertIn("setTextIsSelectable(true)", pane)
        self.assertNotIn('KlyroMenuItem("Select output"', pane)

    def test_native_paste_and_clipboard_are_consistently_guarded(self):
        pane = READ(UI / "TerminalPane.kt")
        section = pane.split("LaunchedEffect(nativePasteRequest, session)", 1)[1].split(
            "var activeTerminalView", 1
        )[0]
        self.assertIn("ClipboardPastePolicy.canPaste(incoming)", section)
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(incoming)", section)
        self.assertIn("pendingMultilinePaste = incoming", section)
        self.assertIn("emulator.paste(incoming)", section)
        self.assertIn("onNativePasteConsumed()", section)
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(content)", pane)

    def test_terminal_toolbar_and_shortcuts_share_identity_and_states(self):
        pane = READ(UI / "TerminalPane.kt")
        self.assertNotIn("R.drawable.ic_klyro_brand_b", pane)
        self.assertIn('Text(\n                    "TERMINAL",', pane)
        self.assertIn("ShortcutState { ACTIVE, HOLD, INACTIVE, DEFAULT }", pane)
        self.assertIn("collectIsPressedAsState()", pane)
        self.assertIn("stateDescription = keyState.name.lowercase()", pane)
        self.assertIn("TerminalDisplayActions.clear(session)", pane)
        self.assertIn('R.drawable.hugeicons_eraser', pane)

    def test_search_checkbox_has_single_full_width_toggle_target(self):
        pane = READ(UI / "TerminalPane.kt")
        self.assertIn(".toggleable(value = findMatchCase, role = Role.Checkbox)", pane)
        self.assertIn("Checkbox(checked = findMatchCase, onCheckedChange = null)", pane)
        self.assertIn('Text("Match case"', pane)
        self.assertIn("TranscriptSearch.findRows(", pane)

    def test_settings_are_compact_without_losing_scroll_escape(self):
        settings = READ(UI / "KlyroSettingsSheet.kt")
        design = READ(UI / "KlyroDesign.kt")
        spaces = READ(UI / "KlyroWorkspaceSheet.kt")
        self.assertIn("val needsScroll =", settings)
        self.assertIn("KlyroSheetLayoutPolicy.needsScroll(", settings)
        self.assertIn("if (needsScroll) Modifier.verticalScroll(", settings)
        self.assertIn("heightIn(max = 260.dp)", settings)
        self.assertIn("rows = entries.size + projectTabs.size", spaces)
        self.assertIn("editing = editorId != null", spaces)
        self.assertIn("val bentoMinHeight = 86.dp", design)
        self.assertIn("val minTouch = 48.dp", design)

    def test_keyboard_and_privacy_actions_are_meaningful(self):
        settings = READ(UI / "KlyroSettingsSheet.kt")
        terminal = READ(UI / "TerminalPane.kt")
        self.assertIn("activeModifiers: Set<String>", settings)
        self.assertIn("activeModifiers = buildSet", terminal)
        self.assertIn("onClick = onResetModifiers", settings)
        self.assertIn("enabled = activeModifiers.isNotEmpty()", settings)
        self.assertIn('"All modifiers released"', settings)
        self.assertIn('stateDescription = if (active) "Active" else "Inactive"', settings)
        self.assertIn('Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)', settings)
        self.assertIn('Uri.parse("package:', settings)
        self.assertIn("if (cannotOpenPermissions)", settings)

    def test_theme_cards_are_distinct_and_six_modes_persist(self):
        settings = READ(UI / "KlyroSettingsSheet.kt")
        design = READ(UI / "KlyroDesign.kt")
        previews = READ(UI / "KlyroThemePreviewCard.kt")
        main = READ(APP / "MainActivity.kt")
        self.assertEqual(settings.count("KlyroThemePreviewCard("), 2)
        self.assertIn('if (mode == "system")', previews)
        self.assertIn('"mono-contrast"', previews)
        self.assertIn('"mono-black"', previews)
        self.assertIn('"mono-paper"', previews)
        self.assertIn("normalizedThemeMode(", main)
        self.assertIn("selected in KlyroColors.themeModes", main)
        self.assertIn("val themeModes = setOf(", design)
        self.assertIn("KlyroThinSlider(", settings)
        self.assertNotIn('Text("9 sp"', settings)
        self.assertNotIn('Text("24 sp"', settings)

    def test_color_picker_is_responsive_and_talkback_adjustable(self):
        picker = READ(UI / "KlyroColorPickerDialog.kt")
        for action in ("Increase saturation", "Decrease saturation",
                       "Increase brightness", "Decrease brightness"):
            self.assertIn('CustomAccessibilityAction("' + action + '"', picker)
        self.assertIn('density.fontScale > 1.15f', picker)
        self.assertIn('stateDescription =', picker)
        self.assertIn("WindowInsets.ime.getBottom(density) > 0", picker)
        self.assertIn("pendingPreset = null", picker)
        self.assertIn("KlyroHexInput.isComplete(hexInput)", picker)

    def test_diagnostics_privacy_and_about_show_truthful_actions(self):
        settings = READ(UI / "KlyroSettingsSheet.kt")
        pane = READ(UI / "TerminalPane.kt")
        self.assertIn("Use the ALL shortcut", settings)
        self.assertNotIn("Select output in the terminal menu", settings)
        self.assertIn('R.drawable.ic_klyro_brand_b', settings)
        self.assertIn('Text("Copy")', settings)
        self.assertIn('Text("Share")', settings)
        self.assertIn("if (copied) \"Report copied\" else \"Clipboard unavailable\"", pane)
        self.assertIn('Toast.makeText(context, "No sharing app available"', pane)

    def test_android_backup_and_device_migration_cannot_export_shell_data(self):
        a = "{http://schemas.android.com/apk/res/android}"
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        app = manifest.find("application")
        self.assertEqual(app.attrib[a + "allowBackup"], "false")
        self.assertEqual(app.attrib[a + "fullBackupContent"], "false")
        self.assertEqual(app.attrib[a + "dataExtractionRules"],
                         "@xml/klyro_data_extraction_rules")
        path = ROOT / "app/src/main/res/xml/klyro_data_extraction_rules.xml"
        root = ET.parse(path).getroot()
        self.assertEqual(root.tag, "data-extraction-rules")
        for transfer in ("cloud-backup", "device-transfer"):
            rules = root.find(transfer)
            self.assertIsNotNone(rules)
            excluded = {(e.attrib.get("domain"), e.attrib.get("path"))
                        for e in rules.findall("exclude")}
            self.assertIn(("root", "."), excluded)
            self.assertIn(("device_root", "."), excluded)

    def test_sessions_shell_picker_and_launcher_remain_safe(self):
        sessions = READ(UI / "KlyroSessionSheets.kt")
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        self.assertIn('Text("Keep session")', sessions)
        self.assertIn('onCloseSession()', sessions)
        self.assertIn('KlyroSheetTitle("Choose shell")', sessions)
        self.assertIn('Text("/bin/bash"', sessions)
        app_tag = manifest.find("application")
        a = "{http://schemas.android.com/apk/res/android}"
        self.assertEqual(app_tag.attrib[a + "icon"], "@mipmap/ic_launcher")
        plugin = ET.parse(ROOT / "app/src/main/res/drawable/ic_klyro_plugins.xml").getroot()
        self.assertEqual(plugin.tag, "vector")


if __name__ == "__main__":
    unittest.main()
