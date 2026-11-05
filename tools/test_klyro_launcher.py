"""Deterministic Android adaptive launcher identity smoke tests.

Run by the existing GitHub Actions python3 unittest discovery.
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
ANDROID = "{http://schemas.android.com/apk/res/android}"
AAPT = "{http://schemas.android.com/aapt}"


class KlyroLauncherIdentityTest(unittest.TestCase):
    def test_min_sdk_adaptive_icon_aliases(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text()
        self.assertIn("minSdk = 26", gradle)
        for name in ("ic_launcher", "ic_launcher_round"):
            file = RES / "mipmap-anydpi-v26" / (name + ".xml")
            root = ET.parse(file).getroot()
            self.assertEqual(root.tag, "adaptive-icon")
            for tag, suffix in (
                ("background", "ic_klyro_launcher_background"),
                ("foreground", "ic_klyro_launcher_foreground"),
                ("monochrome", "ic_klyro_mark_monochrome"),
            ):
                child = root.find(tag)
                self.assertIsNotNone(child)
                self.assertEqual(child.attrib[ANDROID + "drawable"], "@drawable/" + suffix)

    def test_vectors_are_scalable_full_resolution_layers(self):
        for name in ("ic_klyro_launcher_background",
                     "ic_klyro_launcher_foreground",
                     "ic_klyro_mark_monochrome"):
            root = ET.parse(RES / "drawable" / (name + ".xml")).getroot()
            self.assertEqual(root.tag, "vector")
            self.assertEqual(root.attrib[ANDROID + "width"], "108dp")
            self.assertEqual(root.attrib[ANDROID + "height"], "108dp")
            self.assertEqual(root.attrib[ANDROID + "viewportWidth"], "108")
            self.assertEqual(root.attrib[ANDROID + "viewportHeight"], "108")
            self.assertTrue(root.findall(".//path"))
        self.assertTrue(
            ET.parse(RES / "drawable/ic_klyro_launcher_foreground.xml")
            .getroot().findall(".//" + AAPT + "attr")
        )

    def test_foreground_and_monochrome_share_mask_safe_scale(self):
        for name in ("ic_klyro_launcher_foreground", "ic_klyro_mark_monochrome"):
            node = ET.parse(RES / "drawable" / (name + ".xml")).getroot()
            group = node.find("group")
            self.assertIsNotNone(group)
            self.assertEqual(group.attrib[ANDROID + "pivotX"], "54")
            self.assertEqual(group.attrib[ANDROID + "pivotY"], "54")
            self.assertEqual(float(group.attrib[ANDROID + "scaleX"]), 0.79)
            self.assertEqual(float(group.attrib[ANDROID + "scaleY"]), 0.79)

    def test_manifest_references_launcher_resources(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        application = manifest.find("application")
        self.assertEqual(application.attrib[ANDROID + "icon"], "@mipmap/ic_launcher")
        self.assertEqual(application.attrib[ANDROID + "roundIcon"], "@mipmap/ic_launcher_round")

    def test_monochrome_uses_solid_fill_not_gradients(self):
        root = ET.parse(RES / "drawable/ic_klyro_mark_monochrome.xml").getroot()
        self.assertFalse(root.findall(".//" + AAPT + "attr"))
        self.assertEqual(root.findall(".//path")[0].attrib[ANDROID + "fillColor"], "#FFFFFFFF")

    def test_reference_terminal_prompt_in_normal_and_themed_icons(self):
        foreground = ET.parse(
            RES / "drawable/ic_klyro_launcher_foreground.xml"
        ).getroot()
        strokes = [p for p in foreground.findall(".//path")
                   if p.attrib.get(ANDROID + "strokeWidth")]
        self.assertEqual(len(strokes), 2)
        self.assertTrue(all(float(p.attrib[ANDROID + "strokeWidth"]) >= 3.0
                            for p in strokes))
        mono = ET.parse(
            RES / "drawable/ic_klyro_mark_monochrome.xml"
        ).getroot()
        self.assertEqual(mono.findall(".//path")[1].attrib[ANDROID + "fillType"],
                         "evenOdd")
        self.assertTrue((ROOT / "design/klyro-brand-mark.svg").is_file())
        source = ET.parse(ROOT / "design/klyro-brand-mark.svg").getroot()
        self.assertEqual(source.attrib["viewBox"], "0 0 108 108")

    def test_compact_picker_preset_position_and_menu_order(self):
        ui = ROOT / "app/src/main/java/com/oryn/terminal/ui"
        picker = (ui / "KlyroColorPickerDialog.kt").read_text()
        terminal = (ui / "TerminalPane.kt").read_text()
        self.assertLess(picker.index('Text("Hue ·'), picker.index('Text("Presets"'))
        self.assertNotIn('Text("Saturation ·', picker)
        self.assertNotIn('Text("Brightness ·', picker)
        self.assertNotIn('Text("Paste", color = MaterialTheme.colorScheme.onSurface)', terminal)
        self.assertLess(terminal.index('Text("Files & storage"'),
                        terminal.index('Text("Home"'))


if __name__ == "__main__":
    unittest.main()
