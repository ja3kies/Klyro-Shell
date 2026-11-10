"""Regression for bottom-sheet accessibility found in Pixel QA archive."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = (ROOT / "app/src/main/java/com/oryn/terminal/ui/KlyroSettingsSheet.kt").read_text()

class SettingsBottomInsets(unittest.TestCase):
    def test_sheet_keeps_bottom_actions_above_gesture_navigation(self):
        self.assertIn("import androidx.compose.foundation.layout.navigationBarsPadding", SETTINGS)
        fragment = SETTINGS.split('KlyroSheet(onDismissRequest = ::navigateBack)', 1)[1]
        self.assertIn('.navigationBarsPadding()', fragment.split('when (section)',1)[0])
        self.assertIn('.verticalScroll(remember(section)', fragment.split('when (section)',1)[0])

    def test_bottom_action_has_minimum_touch_height(self):
        section = SETTINGS.split('"diagnostics" -> {', 1)[1].split('"about" -> {', 1)[0]
        self.assertIn('onClick = onRecheckEnvironment', section)
        self.assertIn('.heightIn(min = KlyroUiMetrics.minTouch)', section)

if __name__ == "__main__":
    unittest.main()
