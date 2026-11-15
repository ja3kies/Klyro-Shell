"""A rotation must not discard custom HSV, HEX or saved-color drafts."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroColorPickerDialog.kt").read_text()
class ColorDraftRotationTest(unittest.TestCase):
    def test_state_survives_rotation_with_selected_color_input(self):
        for name in ("hue","saturation","brightness","alpha","pendingPreset","hexInput"):
            self.assertIn(f"var {name} by rememberSaveable(initialArgb)",S)
        for name in ("favoriteName","showAdvanced"):
            self.assertIn(f"var {name} by rememberSaveable",S)
    def test_changes_not_applied_until_user_confirms(self):
        self.assertIn("onApply(pendingPreset ?: KlyroColors.customId(argb()))",S)
        self.assertIn("Text(\"Cancel\")",S)
if __name__=="__main__": unittest.main()
