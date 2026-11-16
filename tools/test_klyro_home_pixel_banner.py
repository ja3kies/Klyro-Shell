"""Klyro Home cover: pixel-perfect native rendering, no scaled AI artwork."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
HOME = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
COVER = ROOT / "app/src/main/java/com/oryn/terminal/KlyroHomePixelCover.kt"
OLD_ART = ROOT / "app/src/main/res/drawable-nodpi/klyro_home_pixel_landscape.webp"


class HomePixelBannerContract(unittest.TestCase):
    def test_cover_is_hand_authored_and_resolution_independent(self):
        source = COVER.read_text()
        self.assertIn("internal fun KlyroHomePixelCover(", source)
        self.assertIn("Canvas(Modifier.fillMaxSize()", source)
        self.assertIn("drawKlyroMascot(", source)
        self.assertIn("drawRect(", source)
        self.assertNotIn("Image(", source)
        self.assertNotIn("ContentScale.FillBounds", source)
        self.assertNotIn("R.drawable.klyro_home_pixel_landscape", source)
        self.assertFalse(OLD_ART.exists(), "Remove obsolete blurred source asset")

    def test_art_only_renders_on_home_and_fades(self):
        self.assertIn("val homeVisible = !showPlugins &&", HOME)
        self.assertIn("if (homeVisible) {", HOME)
        self.assertIn("KlyroHomePixelCover(bannerHeight, screenChrome)", HOME)
        source = COVER.read_text()
        self.assertIn(".66f to Color.Transparent", source)
        self.assertIn("1f to screenChrome", source)
        self.assertIn("clearAndSetSemantics", source)

    def test_motion_is_optional_and_lightweight(self):
        source = COVER.read_text()
        self.assertIn("Settings.Global.ANIMATOR_DURATION_SCALE", source)
        self.assertIn("if (motionEnabled)", source)
        self.assertIn("rememberInfiniteTransition", source)
        self.assertNotIn("ImageDecoder", source)

    def test_profile_and_primary_action_follow_banner(self):
        home = HOME.split("// The primary action, runtime and tools", 1)[1]
        self.assertLess(home.index("Spacer(Modifier.fillMaxWidth().height(bannerSpace))"),
                        home.index('Text("KLYRO"'))
        self.assertLess(home.index('Text("KLYRO"'),
                        home.index("if (homeAction != KlyroHomeAction.NONE)"))
        self.assertLess(home.index("if (homeAction != KlyroHomeAction.NONE)"),
                        home.index("stateLabel,"))
        self.assertLess(home.index("stateLabel,"), home.index('title = "Plugins"'))

    def test_safe_areas_and_compact_height(self):
        self.assertIn("screenHeightDp < 660", HOME)
        self.assertIn("80.dp else 194.dp", HOME)
        self.assertIn("148.dp else 256.dp", HOME)
        self.assertIn(".verticalScroll(rememberScrollState())", HOME)
        self.assertIn("isAppearanceLightStatusBars = !homeVisible && !darkChrome", HOME)


if __name__ == "__main__":
    unittest.main()
