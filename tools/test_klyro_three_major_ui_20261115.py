"""UI regression contracts for three Klyro sheets/terminal surfaces.

Structural checks complement JVM and device tests; they cannot prove layout on Pixel.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui"
SETTINGS = (ROOT / "KlyroSettingsSheet.kt").read_text(encoding="utf-8")
PALETTE = (ROOT / "KlyroCommandPalette.kt").read_text(encoding="utf-8")
PANE = (ROOT / "TerminalPane.kt").read_text(encoding="utf-8")


class ThreeMajorUiRegressions(unittest.TestCase):
    def test_settings_navigation_stays_outside_scrollable_body(self):
        sheet = SETTINGS.split("KlyroSheet(onDismissRequest = onDismiss, dismissEnabled = canDismissSheet)", 1)[1]
        header = sheet.split("when (section)", 1)[0]
        self.assertIn("onClick = ::navigateBack", header)
        self.assertIn('Text("Close")', header)
        self.assertIn("Modifier.weight(1f, fill = false)", header)
        self.assertIn(".verticalScroll(remember(section)", header)
        self.assertLess(header.index('Text("Close")'), header.index(".verticalScroll(remember(section)"))
        self.assertIn(".navigationBarsPadding()", header)
        self.assertIn(".imePadding()", header)

    def test_launcher_change_is_async_and_disables_reentry(self):
        appearance = SETTINGS.split('"appearance" -> {', 1)[1].split('"keyboard" -> {', 1)[0]
        self.assertIn("enabled = brandReady && !brandUpdating", appearance)
        self.assertIn("brandUpdating = true", appearance)
        self.assertIn("brandUpdating = false", appearance)
        self.assertIn("scope.launch {", appearance)
        self.assertIn("withContext(Dispatchers.IO)", appearance)
        self.assertIn("KlyroBrandChoice.select(context, choice)", appearance)
        self.assertIn("KlyroBrandChoice.isVerifiedSelection(context, choice)", appearance)

    def test_palette_search_has_ime_action_and_keyboard_safe_list(self):
        self.assertIn("FocusRequester()", PALETTE)
        self.assertIn("LaunchedEffect(showingGuide)", PALETTE)
        self.assertIn("focusRequester(searchFocus)", PALETTE)
        self.assertIn("ImeAction.Search", PALETTE)
        self.assertIn("keyboardActions = KeyboardActions(", PALETTE)
        self.assertIn("keyboard?.hide()", PALETTE)
        self.assertIn("heightIn(max = KlyroUiMetrics.sheetContentMax)", PALETTE)
        self.assertIn(".weight(1f, fill = false)", PALETTE)
        self.assertIn("resultCount", PALETTE)

    def test_new_session_control_is_fixed_outside_tab_scroll(self):
        region = PANE.split('val tabStripScroll = rememberScrollState()', 1)[1].split(
            'if (sessionFailureMessage != null)', 1)[0]
        self.assertIn(".horizontalScroll(tabStripScroll)", region)
        self.assertIn("tabs.forEach { (id, tabTitle) ->", region)
        self.assertIn('onClick = onNewTab', region)
        self.assertIn("Modifier.weight(1f)", region)
        self.assertLess(region.index('tabs.forEach { (id, tabTitle) ->'),
                        region.index('onClick = onNewTab'))
        self.assertIn('contentDescription = if (tabs.size < 8) "New terminal"', region)


if __name__ == "__main__":
    unittest.main()
