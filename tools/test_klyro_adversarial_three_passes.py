"""Three static adversarial regression passes. Runtime CI and device QA are separate gates."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/oryn/terminal"
UI = APP / "ui"
READ = lambda p: p.read_text(encoding="utf-8")


class KlyroThreePassAdversarialAudit(unittest.TestCase):
    def test_pass_one_untrusted_plugin_file_cannot_execute_code(self):
        registry = READ(APP / "plugins/KlyroPluginManager.kt")
        page = READ(UI / "KlyroPluginsPage.kt")
        palette = READ(UI / "KlyroCommandPalette.kt")
        self.assertIn("MAX_MANIFEST_BYTES = 16 * 1024", registry)
        self.assertIn("output.size() + n <= MAX_MANIFEST_BYTES", registry)
        self.assertIn("MAX_PLUGINS = 12", registry)
        self.assertIn("MAX_COMMANDS = 8", registry)
        self.assertIn("safeInsertion(insert)", registry)
        self.assertIn("readOnlyCommands.contains(value)", registry)
        self.assertIn("safePackageSearch.matches(value)", registry)
        self.assertIn("manager.previewManifest(uri)", page)
        self.assertIn("manager.installPreview(record)", page)
        self.assertIn("pendingImport?.let", page)
        self.assertIn("ActivityResultContracts.OpenDocument()", page)
        self.assertIn("manager.setEnabled(plugin.id, checked)", page)
        self.assertIn("manager.uninstall(id)", page)
        self.assertIn("KlyroPluginManager.safeInsertion(command.insert)", palette)
        self.assertIn("onInsert(command.insert)", palette)
        self.assertNotIn("Runtime.getRuntime()", registry)
        self.assertNotIn("ProcessBuilder(", registry)
        self.assertNotIn("evaluateJavascript", registry)
        self.assertNotIn("WebView(", page)

    def test_pass_two_shortcuts_preserve_selection_without_accidental_paste(self):
        terminal = READ(UI / "TerminalPane.kt")
        actions = terminal.split("val firstRow:", 1)[1].split("if (compact)", 1)[0]
        self.assertIn('"ALL", description = "Select all terminal output"', actions)
        self.assertIn(") { selectTerminalOutput() }", actions)
        self.assertIn('"PASTE", description = "Paste clipboard text"', actions)
        self.assertIn(") { pasteClipboard() }", actions)
        self.assertNotIn('KlyroMenuItem("Select output"', terminal)
        selection = terminal.split("fun selectTerminalOutput()", 1)[1].split(
            "// Fit seven", 1
        )[0]
        self.assertIn("selectedOutput = output", selection)
        self.assertNotIn("paste(", selection)
        self.assertIn("ShortcutState { ACTIVE, HOLD, INACTIVE, DEFAULT }", terminal)
        self.assertIn("collectIsPressedAsState()", terminal)
        self.assertIn("disabledContainerColor = stateBackground", terminal)
        self.assertIn("stateDescription = keyState.name.lowercase()", terminal)
        self.assertIn("TerminalDisplayActions.clear(session)", terminal)
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(content)", terminal)

    def test_pass_three_compact_layout_and_launcher_consistency(self):
        settings = READ(UI / "KlyroSettingsSheet.kt")
        design = READ(UI / "KlyroDesign.kt")
        previews = READ(UI / "KlyroThemePreviewCard.kt")
        workspaces = READ(UI / "KlyroWorkspaceSheet.kt")
        home = READ(APP / "MainActivity.kt")
        self.assertIn("if (needsScroll) Modifier.verticalScroll(", settings)
        self.assertNotIn('section == "files" || section == "diagnostics"', settings)
        self.assertIn("heightIn(max = 260.dp)", settings)
        self.assertIn("verticalScroll(rememberScrollState())", settings)
        self.assertIn("density.fontScale > 1.2f", settings)
        self.assertIn("entries.size + projectTabs.size > 6", workspaces)
        self.assertIn("KlyroUiMetrics.minTouch", settings)
        self.assertIn("val bentoMinHeight = 86.dp", design)
        self.assertEqual(settings.count("KlyroThemePreviewCard("), 2)
        for key in ("mono-black", "mono-paper", "mono-contrast", "light", "dark"):
            self.assertIn('"' + key + '"', previews)
        self.assertIn('"system" to "System"', settings)
        self.assertIn("Color.Black", previews)
        self.assertIn("Color.White", previews)
        self.assertNotIn('Text("9 sp"', settings)
        self.assertNotIn('Text("24 sp"', settings)
        self.assertIn("KlyroPluginsPage(onBack =", home)
        self.assertIn("ic_klyro_launcher_foreground", home)
        self.assertIn('Text("ARM64"', home)
        fg = ROOT / "app/src/main/res/drawable/ic_klyro_launcher_foreground.xml"
        root = ET.parse(fg).getroot()
        android = "{http://schemas.android.com/apk/res/android}"
        self.assertEqual(root.attrib[android + "width"], "108dp")
        self.assertEqual(root.attrib[android + "height"], "108dp")


if __name__ == "__main__":
    unittest.main()
