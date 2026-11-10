"""Stateful Settings and command discovery must survive configuration changes."""
from pathlib import Path
import unittest
APP = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui"
SETTINGS = (APP / "KlyroSettingsSheet.kt").read_text(encoding="utf-8")
PALETTE = (APP / "KlyroCommandPalette.kt").read_text(encoding="utf-8")

class NavigationStateUX(unittest.TestCase):
    def test_settings_section_and_color_dialog_survive_rotation(self):
        self.assertIn("var section by rememberSaveable(initialSection)", SETTINGS)
        self.assertIn("var showColorPicker by rememberSaveable", SETTINGS)
        self.assertIn('enabled = section != "home"', SETTINGS)

    def test_command_query_and_guide_state_survive_rotation(self):
        self.assertIn('var query by rememberSaveable { mutableStateOf("") }', PALETTE)
        self.assertIn("var showingGuide by rememberSaveable", PALETTE)
        self.assertIn("KlyroCommandSearch.MAX_QUERY_LENGTH", PALETTE)
        self.assertIn("BackHandler(enabled = showingGuide)", PALETTE)

    def test_non_error_feedback_is_neutral(self):
        self.assertIn("if (unsafeFeedback) colors.warningContainer else colors.key", PALETTE)
        self.assertIn("unsafeFeedback = true", PALETTE)
        self.assertIn("unsafeFeedback = false", PALETTE)
        self.assertIn('else "No matching commands."', PALETTE)
        self.assertIn('else if (availableOnly) "No available commands match."', PALETTE)

if __name__ == "__main__":
    unittest.main()
