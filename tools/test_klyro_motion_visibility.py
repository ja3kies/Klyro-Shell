"""Regression contract: interactive cards remain visible during decorative stagger."""
from pathlib import Path
import unittest

MOTION = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui/motion/KlyroMotion.kt"


class MotionVisibilityContract(unittest.TestCase):
    def test_stagger_does_not_hide_clickable_cards(self):
        source = MOTION.read_text()
        body = source.split("fun Modifier.klyroStaggeredEntrance(", 1)[1]
        self.assertIn("translationY = (1f - progress) * 14f", body)
        self.assertNotIn("alpha = progress", body)

    def test_stagger_remains_bounded(self):
        source = MOTION.read_text()
        self.assertIn("index.coerceIn(0, KlyroMotion.MAX_STAGGER_ITEMS)", source)


if __name__ == "__main__":
    unittest.main()
