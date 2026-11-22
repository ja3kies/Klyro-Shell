"""Regression wiring for motion ported from PR #52; rendered behavior is tested on Android."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal"
def read(name):
    return (ROOT / name).read_text()

class MotionRestorationTest(unittest.TestCase):
    def test_home_keeps_immediate_primary_action_and_motion_on_visual_content(self):
        home = read("MainActivity.kt")
        for marker in ('"home-brand"', '"home-runtime-status"', '"home-$title"', 'klyroPressScale(launchPressed)'):
            self.assertIn(marker, home)
        self.assertIn("onClick = onLaunch", home)
        self.assertIn("interactionSource = launchSource", home)

    def test_entrance_never_hides_touchable_content(self):
        motion = read("ui/motion/KlyroMotion.kt").split("fun Modifier.klyroStaggeredEntrance", 1)[1]
        self.assertNotIn("alpha =", motion)
        self.assertIn("index.coerceIn(0, 5) * 24L", motion)
        self.assertIn("if (motion)", motion)
        self.assertIn("6.dp.toPx()", motion)

    def test_settings_and_plugins_and_workspaces_restore_motion(self):
        self.assertIn("slider-drag-feedback", read("ui/KlyroUiComponents.kt"))
        self.assertIn("workspace-selection", read("ui/KlyroWorkspaceSheet.kt"))
        self.assertIn("theme-preview-label", read("ui/KlyroThemePreviewCard.kt"))
        plugins = read("ui/KlyroPluginsPage.kt")
        for marker in ("plugin-tab-background", "plugin-tab-ink", "installed-${plugin.id}", "explore-${plugin.id}"):
            self.assertIn(marker, plugins)

    def test_shape_transitions_preserve_selection_semantics_and_reduced_motion(self):
        preview = read("ui/KlyroThemePreviewCard.kt")
        self.assertIn("theme-preview-selection", preview)
        self.assertIn("theme-preview-corner", preview)
        self.assertIn("selected = active", preview)
        self.assertIn("if (motion) tween(240) else snap()", preview)
        settings = read("ui/KlyroSettingsSheet.kt")
        for marker in ("monochrome-chevron", "font-family-fill", "font-family-shape"):
            self.assertIn(marker, settings)
        self.assertIn("selected = active", settings)
        self.assertIn("else androidx.compose.animation.core.snap()", settings)
        plugins = read("ui/KlyroPluginsPage.kt")
        self.assertIn("plugin-tab-corner", plugins)
        self.assertIn("RoundedCornerShape(tabCorner)", plugins)
        self.assertIn("this.selected = selected", plugins)

    def test_slider_stretches_only_visual_thumb_on_drag(self):
        shared = read("ui/KlyroUiComponents.kt")
        self.assertIn("scaleX = thumbScale", shared)
        self.assertIn("scaleY = 1f - (thumbScale - 1f) * .35f", shared)
        self.assertIn("interactionSource = interactionSource", shared)
        self.assertIn("modifier.heightIn(min = KlyroUiMetrics.minTouch)", shared)

if __name__ == "__main__":
    unittest.main()
