"""Home banner stays offline, decorative, and grouped with functional content."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
HOME = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
ART = ROOT / "app/src/main/res/drawable-nodpi/klyro_home_pixel_landscape.webp"

class HomePixelBannerContract(unittest.TestCase):
    def test_image_is_valid_small_local_webp(self):
        data = ART.read_bytes()
        self.assertEqual(data[:4], b"RIFF")
        self.assertEqual(data[8:12], b"WEBP")
        self.assertEqual(int.from_bytes(data[4:8], "little") + 8, len(data))
        self.assertLess(len(data), 16_000)

    def test_art_only_renders_on_home_and_fades(self):
        self.assertIn('val homeVisible = !showPlugins &&', HOME)
        self.assertIn('if (homeVisible) {', HOME)
        self.assertIn('R.drawable.klyro_home_pixel_landscape', HOME)
        self.assertIn('contentDescription = null,', HOME)
        self.assertIn('.66f to Color.Transparent', HOME)
        self.assertIn('1f to screenChrome', HOME)

    def test_animation_stops_when_system_reduces_motion(self):
        cover = HOME.split('private fun KlyroHomePixelCover(', 1)[1]
        self.assertIn('LocalMotionDurationScale.current.scaleFactor > 0f', cover)
        self.assertIn('rememberInfiniteTransition(label = "Klyro night stars")', cover)
        self.assertIn('Canvas(Modifier.fillMaxSize())', cover)
        self.assertIn('Image(', cover)
        self.assertIn('contentDescription = null', cover)

    def test_profile_header_and_primary_action_follow_banner(self):
        home = HOME.split('// The primary action, runtime and tools', 1)[1]
        self.assertLess(home.index('Spacer(Modifier.fillMaxWidth().height(bannerSpace))'),
                        home.index('Text("KLYRO"'))
        self.assertLess(home.index('Text("KLYRO"'),
                        home.index('if (homeAction != KlyroHomeAction.NONE)'))
        self.assertLess(home.index('if (homeAction != KlyroHomeAction.NONE)'),
                        home.index('stateLabel,'))
        self.assertLess(home.index('stateLabel,'),
                        home.index('title = "Plugins"'))

    def test_small_height_loses_art_before_it_loses_actions(self):
        self.assertIn('screenHeightDp < 660', HOME)
        self.assertIn('80.dp else 194.dp', HOME)
        self.assertIn('148.dp else 256.dp', HOME)
        self.assertIn('.verticalScroll(rememberScrollState())', HOME)

if __name__ == "__main__":
    unittest.main()
