"""Native Uiverse Home pattern and existing layout contracts."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
HOME = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
COVER = (ROOT / "app/src/main/java/com/oryn/terminal/KlyroHomePixelCover.kt").read_text()


class HomePatternContract(unittest.TestCase):
    def test_native_pattern_replaces_decoder(self):
        self.assertIn("From Uiverse.io by moin_7638", COVER)
        self.assertIn("MIT License, Copyright (c) moin_7638 (Jami)", COVER)
        self.assertIn("homePatternColoredCell", COVER)
        self.assertIn("4.dp.toPx()", COVER)
        self.assertIn("drawWithCache", COVER)
        self.assertIn("drawRect(Color.Black)", COVER)
        self.assertIn("LocalKlyroColors.current.accent", COVER)
        self.assertIn("drawPath(pattern, homePatternTint(accent, angle))", COVER)
        self.assertNotIn("ImageDecoder", COVER)
        self.assertNotIn("AndroidView", COVER)
        self.assertNotIn("R.drawable.klyro_home_landscape_loop", COVER)

    def test_every_renderer_is_clipped_before_the_fade(self):
        self.assertIn("Box(Modifier.fillMaxWidth().height(height).clipToBounds()", COVER)

    def test_animation_contract(self):
        self.assertIn("HOME_PATTERN_PERIOD_MS = 20_000", COVER)
        self.assertIn("targetValue = 360f", COVER)
        self.assertIn("LinearEasing", COVER)
        self.assertIn("RepeatMode.Restart", COVER)
        self.assertIn("if (animate && motionEnabled && started)", COVER)
        self.assertIn("Settings.Global.ANIMATOR_DURATION_SCALE", COVER)
        self.assertIn("Lifecycle.State.STARTED", COVER)
        self.assertIn("lifecycle?.removeObserver(observer)", COVER)
        self.assertIn("clearAndSetSemantics", COVER)

    def test_cover_still_fades_into_profile_and_actions(self):
        self.assertIn("val homeVisible = !showPlugins &&", HOME)
        self.assertIn("if (homeVisible) {", HOME)
        self.assertIn("KlyroHomePixelCover(bannerHeight, screenChrome,", HOME)
        self.assertIn("animate = (!showHomeSettings && !showHomeWorkspaces) || homePatternVisibility.shouldAnimate", HOME)
        self.assertIn("LocalHomePatternVisibility provides homePatternVisibility", HOME)
        self.assertIn(".50f to Color.Transparent", COVER)
        self.assertIn(".74f to screenChrome.copy(alpha = .28f)", COVER)
        self.assertIn(".90f to screenChrome.copy(alpha = .76f)", COVER)
        self.assertIn("1f to screenChrome", COVER)
        home = HOME.split("// The primary action, runtime and tools", 1)[1]
        self.assertLess(home.index("Spacer(Modifier.fillMaxWidth().height(bannerSpace))"),
                        home.index('Text("KLYRO"'))
        self.assertLess(home.index('Text("KLYRO"'),
                        home.index("if (homeAction != KlyroHomeAction.NONE)"))
        self.assertLess(home.index("if (homeAction != KlyroHomeAction.NONE)"),
                        home.index("stateLabel,"))

    def test_compact_screen_does_not_hide_primary_action(self):
        self.assertIn("screenHeightDp < 660", HOME)
        self.assertIn("80.dp else 194.dp", HOME)
        self.assertIn("148.dp else 256.dp", HOME)
        self.assertIn(".verticalScroll(rememberScrollState())", HOME)


if __name__ == "__main__":
    unittest.main()
