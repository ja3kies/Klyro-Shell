"""Session tabs must keep selection and session management as distinct actions."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
PANE = (ROOT / "app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
TAB_PATH = ROOT / "app/src/main/java/com/oryn/terminal/ui/KlyroTerminalSessionTab.kt"


class SessionTabActionContract(unittest.TestCase):
    def test_compose_long_press_test_uses_the_supported_touch_injection_extension(self):
        ui_test = (ROOT / "app/src/androidTest/java/com/oryn/terminal/ui/"
                   "KlyroTerminalSessionTabUiTest.kt").read_text()
        self.assertIn("import androidx.compose.ui.test.longClick", ui_test)
        self.assertIn("performTouchInput { longClick() }", ui_test)

    def test_tab_is_a_separate_component_with_a_stable_semantics_contract(self):
        self.assertTrue(TAB_PATH.exists())
        tab = TAB_PATH.read_text()
        self.assertIn("role = Role.Tab", tab)
        self.assertIn("selected = active", tab)
        self.assertIn("contentDescription =", tab)
        self.assertIn("stateDescription =", tab)
        self.assertIn("CustomAccessibilityAction(\"Manage session\")", tab)

    def test_every_tab_tap_selects_even_when_it_is_already_selected(self):
        tab = TAB_PATH.read_text()
        self.assertIn('onClickLabel = "Select session"', tab)
        self.assertIn("onClick = onSelect", tab)
        self.assertNotIn("if (active)", tab.split(".combinedClickable(", 1)[1].split(".semantics", 1)[0])

    def test_management_keeps_long_press_and_is_exposed_as_a_custom_action(self):
        tab = TAB_PATH.read_text()
        self.assertIn('onLongClickLabel = "Manage session"', tab)
        self.assertIn("onLongClick = onManage", tab)
        self.assertIn("customActions = listOf(", tab)
        self.assertIn("onManage()", tab)

    def test_more_menu_routes_to_the_actual_active_session(self):
        menu = PANE.split("val toolsMenu:", 1)[1].split("val tabStripScroll", 1)[0]
        self.assertIn('KlyroMenuItem("Manage active session"', menu)
        self.assertIn('R.drawable.hugeicons_terminal', menu)
        self.assertIn("tabs.firstOrNull { it.first == activeTabId }", PANE)
        self.assertIn("activeSession != null", menu)
        self.assertIn("showToolsMenu = false", menu)
        self.assertIn("openSessionSheet", menu)

    def test_existing_visual_and_attention_indicators_remain_bound_to_the_tab(self):
        tab = TAB_PATH.read_text()
        for contract in (
            "colors.accent", "colors.key", "KlyroUiMetrics.minTouch",
            "height(48.dp)", "widthIn(max = 140.dp)", "activityTag",
            "showUnreadOutput", "horizontalArrangement = Arrangement.spacedBy(6.dp)",
            "TextOverflow.Ellipsis",
        ):
            with self.subTest(contract=contract):
                self.assertIn(contract, tab)

    def test_terminal_menu_and_keyboard_contract_are_not_replaced(self):
        self.assertIn('KlyroMenuItem("Paste clipboard"', PANE)
        self.assertIn('KlyroMenuItem("Home"', PANE)
        self.assertIn("KlyroTerminalSessionTab(", PANE)
        self.assertNotIn('KlyroMenuItem("Session shortcuts"', PANE)

    def test_real_terminal_menu_opens_the_active_session_management_sheet(self):
        ui_test = (ROOT / "app/src/androidTest/java/com/oryn/terminal/ui/"
                   "TerminalSessionManagementRouteUiTest.kt").read_text()
        for action in (
            'onNodeWithContentDescription("More actions")',
            'onNodeWithText("Manage active session").assertIsDisplayed().performClick()',
            'onNodeWithText("Rename").assertIsDisplayed()',
            'onNodeWithText("Close terminal").assertIsDisplayed()',
        ):
            with self.subTest(action=action):
                self.assertIn(action, ui_test)

    def test_more_menu_stays_pinned_outside_the_horizontally_scrolling_tab_group(self):
        strip = PANE.split("// The new-session action remains", 1)[1].split(
            "Box(\n                modifier = Modifier\n                    .size(48.dp)", 1
        )[0]
        self.assertLess(strip.index("if (compact) toolsMenu()"),
                        strip.index(".horizontalScroll(tabStripScroll)"))
        tab_items = strip.split(".horizontalScroll(tabStripScroll)", 1)[1]
        self.assertIn(".selectableGroup()", tab_items)
        self.assertNotIn("toolsMenu()", tab_items)
        route_test = (ROOT / "app/src/androidTest/java/com/oryn/terminal/ui/"
                      "TerminalSessionManagementRouteUiTest.kt").read_text()
        self.assertIn("(1L..8L).map", route_test)
        self.assertIn('onNodeWithContentDescription("More actions")\n                .assertIsDisplayed()', route_test)


if __name__ == "__main__":
    unittest.main()
