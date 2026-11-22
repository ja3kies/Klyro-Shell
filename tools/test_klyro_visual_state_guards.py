"""Guard the actual shared UI call sites against stale visual state."""
from pathlib import Path
import unittest

UI = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui"

class VisualStateGuards(unittest.TestCase):
    def test_disabled_motion_does_not_create_a_hidden_palette_transition(self):
        source = (UI / "KlyroAnimatedPalette.kt").read_text()
        self.assertLess(source.index("if (!motion) return target"), source.index("val transition = updateTransition"))
        self.assertIn('key(target.dark, target.accentId.takeIf { it.startsWith("mono-") })', source)

    def test_workspace_selection_honors_reduced_motion(self):
        source = (UI / "KlyroWorkspaceSheet.kt").read_text()
        self.assertIn('if (motionEnabled) tween(200) else snap(), label = "workspace-selection"', source)

    def test_incomplete_hex_and_pending_presets_cannot_be_dismissed_as_clean(self):
        source = (UI / "KlyroColorPickerDialog.kt").read_text()
        draft = source.split("val hasUnsavedColor =", 1)[1].split("KlyroModalTheme", 1)[0]
        self.assertIn("argb() != initialArgb", draft)
        self.assertIn("pendingPreset != null", draft)
        self.assertIn('hexInput != "#%06X".format(initialArgb and 0xFFFFFF)', draft)
        self.assertIn("dismissOnBackPress = !hasUnsavedColor", source)
        self.assertIn("dismissOnClickOutside = !hasUnsavedColor", source)
