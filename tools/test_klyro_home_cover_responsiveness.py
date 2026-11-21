"""Guard the native Home pattern rendering boundaries; device tests exercise live settings changes."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
COVER = (ROOT / "app/src/main/java/com/oryn/terminal/KlyroHomePixelCover.kt").read_text()


class HomeCoverResponsivenessTest(unittest.TestCase):
    def test_square_tiles_follow_density_and_clip_to_banner(self):
        self.assertIn("val cell = 4.dp.toPx()", COVER)
        self.assertIn("ceil(size.width / cell)", COVER)
        self.assertIn("ceil(size.height / cell)", COVER)
        self.assertIn("clipRect {", COVER)
        self.assertIn("translate(left = -offset, top = -offset)", COVER)

    def test_geometry_is_cached_and_frames_only_change_color(self):
        self.assertIn("drawWithCache", COVER)
        geometry, draw = COVER.split("onDrawBehind {", 1)
        self.assertIn("val pattern = Path()", geometry)
        self.assertNotIn("phase?.value", geometry)
        self.assertIn("homePatternTint(accent, angle)", draw)
        self.assertIn("phase?.value", draw)
        self.assertIn("findViewTreeLifecycleOwner()", COVER)
        self.assertNotIn("ImageDecoder", COVER)
        self.assertNotIn("BitmapFactory", COVER)

    def test_reduced_motion_is_observed_and_observer_is_released(self):
        self.assertIn("Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)", COVER)
        self.assertIn("registerContentObserver", COVER)
        self.assertIn("unregisterContentObserver", COVER)


if __name__ == "__main__":
    unittest.main()
