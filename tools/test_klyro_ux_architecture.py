"""Static guardrails for Klyro action ownership and visual consistency.

These checks supplement, not replace, device interaction testing.
"""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/oryn/terminal/ui"
TERMUX = ROOT / "app/src/main/java/com/oryn/terminal/termux"


def read(path):
    return pathlib.Path(path).read_text(encoding="utf-8")


class KlyroUxArchitectureTest(unittest.TestCase):
    def test_single_diagnostics_destination(self):
        pane = read(UI / "TerminalPane.kt")
        settings = read(UI / "KlyroSettingsSheet.kt")
        self.assertIn('"diagnostics" -> {', settings)
        self.assertNotIn('showDiagnosticsSheet', pane)
        self.assertNotIn('KlyroDiagnosticsSheet(', pane)
        self.assertFalse((UI / "KlyroDiagnosticsSheet.kt").exists())
        self.assertNotIn('text = { Text("Diagnostics") }', pane)

    def test_command_palette_is_not_a_duplicate_app_action_menu(self):
        pane = read(UI / "TerminalPane.kt")
        palette = read(UI / "KlyroCommandPalette.kt")
        catalog = read(UI / "KlyroCommandCatalog.java")
        self.assertIn('KlyroCommandPalette(', pane)
        self.assertNotIn('KlyroPaletteAction(', pane)
        self.assertNotIn('"Copy output"', palette)
        self.assertNotIn('"Clear display"', palette)
        self.assertIn('KlyroCommandCatalog.search(query)', palette)
        self.assertIn('KlyroCommandCatalog.safeInsertion', palette)
        self.assertIn('termux-setup-storage', catalog)

    def test_contextual_terminal_actions_are_not_duplicated_in_settings(self):
        settings = read(UI / "KlyroSettingsSheet.kt")
        self.assertNotIn('"sessions" -> {', settings)
        self.assertNotIn('Select terminal output', settings)
        self.assertIn('SettingsRowLink(', settings)

    def test_theme_and_workspace_contract(self):
        main = read(ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt")
        workspaces = read(UI / "KlyroWorkspaceSheet.kt")
        palette = read(UI / "KlyroCommandPalette.kt")
        self.assertEqual(main.count("surfaceVariant = palette.tile"), 2)
        self.assertIn("textAlign = TextAlign.Center", workspaces)
        self.assertIn("fontWeight = FontWeight.Bold", workspaces)
        self.assertIn("KlyroModalTheme", palette)
        self.assertIn("KlyroModalTheme", workspaces)

    def test_welcome_profile_does_not_require_replacing_custom_rc(self):
        welcome = read(TERMUX / "KlyroWelcomeGuide.java")
        installer = read(TERMUX / "TermuxBootstrapInstaller.java")
        self.assertIn("augmentGeneratedProfile", installer)
        self.assertIn("KLYRO_WELCOME_GUIDE_V1", welcome)
        self.assertIn(".klyro-welcome-shown", welcome)
        self.assertIn("case ", welcome)
        self.assertIn("termux.dev/docs", welcome)


if __name__ == "__main__":
    unittest.main()
