"""Guard the Home decoder boundaries; device tests exercise live settings changes."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
COVER = (ROOT / "app/src/main/java/com/oryn/terminal/KlyroHomePixelCover.kt").read_text()


class HomeCoverResponsivenessTest(unittest.TestCase):
    def test_still_and_animation_preserve_the_same_aspect_ratio(self):
        self.assertIn("ImageView.ScaleType.CENTER_CROP", COVER)
        self.assertIn("contentScale = ContentScale.Crop", COVER)
        self.assertNotIn("ImageView.ScaleType.FIT_XY", COVER)
        self.assertNotIn("ContentScale.FillBounds", COVER)

    def test_decoding_does_not_block_composition(self):
        self.assertIn("produceState", COVER)
        self.assertGreaterEqual(COVER.count("withContext(Dispatchers.IO)"), 2)

    def test_reduced_motion_is_observed_and_observer_is_released(self):
        self.assertIn("Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)", COVER)
        self.assertIn("registerContentObserver", COVER)
        self.assertIn("unregisterContentObserver", COVER)


if __name__ == "__main__":
    unittest.main()
