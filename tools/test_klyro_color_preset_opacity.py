"""Preset opacity edits must not silently revert to the original preset."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroColorPickerDialog.kt").read_text()
class PresetOpacityTest(unittest.TestCase):
    def test_opacity_change_clears_preset_before_apply(self):
        self.assertIn("onValueChange = { alpha = it; pendingPreset = null }",S)
        self.assertIn("onApply(pendingPreset ?: KlyroColors.customId(argb()))",S)
        self.assertIn("pendingPreset = preset.id",S)
if __name__=="__main__": unittest.main()
