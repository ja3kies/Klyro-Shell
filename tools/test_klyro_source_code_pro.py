"""Source Code Pro must participate in the real font selector and packaged asset gate."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/oryn/terminal/ui"

class SourceCodeProContract(unittest.TestCase):
    def test_selectable_persisted_typeface_uses_its_own_asset(self):
        fonts = (UI / "KlyroTerminalFonts.kt").read_text()
        self.assertIn('Choice("source-code-pro", "Source Code Pro")', fonts)
        self.assertIn('"source-code-pro" -> "fonts/SourceCodePro-Regular.ttf"', fonts)
        self.assertIn("options.any { it.id == candidate }", fonts)
        settings = (UI / "KlyroSettingsSheet.kt").read_text()
        self.assertIn("KlyroTerminalFonts.options.forEach", settings)
        self.assertIn("onFontFamilyChange(option.id)", settings)

    def test_distribution_verifies_font_and_license_and_exposes_license(self):
        workflow = (ROOT / ".github/workflows/android-build.yml").read_text()
        for expected in ("803b7e23ec97ae58b6232ea76519a76d428ba268",
                         "10c73d7d901cf0dfa8ba03e45e3271d466291520",
                         "70288a864f8dfd1314aa0315748eece0cf80fdda",
                         "assets/fonts/SourceCodePro-Regular.ttf",
                         "assets/fonts/SourceCodePro-OFL.txt"):
            self.assertIn(expected, workflow)
        settings = (UI / "KlyroSettingsSheet.kt").read_text()
        self.assertIn('"Source Code Pro licence"', settings)
        self.assertIn('"fonts/SourceCodePro-OFL.txt"', settings)
