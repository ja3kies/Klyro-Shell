"""The terminal keys share the available width without horizontal scrolling."""
from pathlib import Path
import unittest


class KeyLayoutTest(unittest.TestCase):
    def test_rows_fit_and_page_switch_is_explicit(self):
        root = Path(__file__).resolve().parents[1]
        pane = (root / "app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
        keyboard = pane.split("HorizontalPager(state = keyboardPager", 1)[1].split(
            "KlyroKeyboardPagerIndicator(", 1)[0]
        self.assertNotIn("horizontalScroll", keyboard)
        self.assertIn("userScrollEnabled = false", keyboard)
        self.assertEqual(pane.count("modifier = Modifier.weight(1f), darkChrome"), 14)
        self.assertIn("compact && maxWidth >= 736.dp", pane)
        self.assertIn("heightIn(min = KlyroUiMetrics.minTouch)", pane)
