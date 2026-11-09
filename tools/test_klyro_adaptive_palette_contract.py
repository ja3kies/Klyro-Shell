"""Only centralized palette seeds and HSV color-picker geometry may use fixed colors.

This is a source contract. Android screenshot checks remain necessary.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal"
SURFACES = [
    ROOT / "MainActivity.kt",
    *(ROOT / "ui" / name for name in (
        "TerminalPane.kt", "KlyroSettingsSheet.kt", "KlyroUiComponents.kt",
        "KlyroThemePreviewCard.kt", "KlyroPluginsPage.kt", "KlyroWorkspaceSheet.kt",
        "KlyroSessionSheets.kt", "KlyroCommandPalette.kt"
    ))
]

class AdaptivePaletteContract(unittest.TestCase):
    def test_ui_surfaces_do_not_embed_rgb_colors(self):
        for path in SURFACES:
            with self.subTest(file=path.name):
                code = path.read_text(encoding="utf-8")
                self.assertNotRegex(code, r'Color\\s*\\(\\s*0x[0-9a-fA-F]{6,8}\\b')
                self.assertNotRegex(code, r'android\\.graphics\\.Color\\.rgb\\s*\\(')

    def test_slider_and_brand_surfaces_use_theme_tokens(self):
        terminal = (ROOT / "ui/TerminalPane.kt").read_text(encoding="utf-8")
        settings = (ROOT / "ui/KlyroSettingsSheet.kt").read_text(encoding="utf-8")
        widgets = (ROOT / "ui/KlyroUiComponents.kt").read_text(encoding="utf-8")
        preview = (ROOT / "ui/KlyroThemePreviewCard.kt").read_text(encoding="utf-8")
        self.assertIn(".background(ui.tile)", terminal)
        self.assertIn("colors.key,", settings)
        self.assertIn("thumbColor = colors.foreground", widgets)
        self.assertIn("KlyroThemePreviewPalette.forMode(mode, accentId)", preview)
        self.assertIn("KlyroThemePreviewPalette.systemPair(accentId)", preview)
        self.assertNotIn("Triple(Color(", preview)

    def test_picker_and_palette_are_only_fixed_color_exceptions(self):
        palette = (ROOT / "ui/KlyroDesign.kt").read_text(encoding="utf-8")
        picker = (ROOT / "ui/KlyroColorPickerDialog.kt").read_text(encoding="utf-8")
        self.assertIn("fun create(dark: Boolean, accentId: String?)", palette)
        self.assertIn("Brush.horizontalGradient", picker)
        self.assertIn("Color.Red, Color.Yellow, Color.Green", picker)

if __name__ == "__main__":
    unittest.main()
