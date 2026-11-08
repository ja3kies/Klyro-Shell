"""Release gates for B/A launcher identities, smart ALL and expanded safe plugin packs."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/oryn/terminal"
RES = ROOT / "app/src/main/res"
ANDROID = "{http://schemas.android.com/apk/res/android}"


class DualBrandSmartAllTest(unittest.TestCase):
    def test_launcher_aliases_have_exclusive_default(self):
        app = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot().find("application")
        aliases = {a.attrib[ANDROID + "name"]: a for a in app.findall("activity-alias")}
        self.assertEqual(aliases[".LauncherB"].attrib[ANDROID + "enabled"], "true")
        self.assertEqual(aliases[".LauncherA"].attrib[ANDROID + "enabled"], "false")
        for name, icon in ((".LauncherB", "b"), (".LauncherA", "a")):
            alias = aliases[name]
            self.assertEqual(alias.attrib[ANDROID + "targetActivity"], ".MainActivity")
            self.assertEqual(alias.attrib[ANDROID + "icon"], "@mipmap/ic_launcher_brand_" + icon)
            self.assertIsNotNone(alias.find("intent-filter"))
            root = ET.parse(RES / ("mipmap-anydpi-v26/ic_launcher_brand_" + icon + ".xml")).getroot()
            self.assertEqual(root.tag, "adaptive-icon")
            self.assertEqual(root.find("foreground").attrib[ANDROID + "drawable"],
                             "@drawable/ic_klyro_brand_" + icon)
            self.assertEqual(ET.parse(RES / ("drawable/ic_klyro_brand_" + icon + ".xml")).getroot().tag,
                             "vector")

    def test_brand_switch_is_persisted_only_on_success(self):
        source = (SRC / "ui/KlyroBrandChoice.kt").read_text()
        self.assertIn("setComponentEnabledSettings(", source)
        self.assertIn("PackageManager.DONT_KILL_APP", source)
        self.assertIn("if (ok) context.getSharedPreferences", source)
        self.assertIn("fun selected(context: Context)", source)
        self.assertIn("getComponentEnabledSetting(alternative)", source)
        self.assertIn("getComponentEnabledSetting(primary)", source)
        self.assertIn("val verified = ok && selected(context) == choice", source)

    def test_all_selects_transcript_or_pastes_and_paste_key_is_gone(self):
        source = (SRC / "ui/TerminalPane.kt").read_text()
        method = source.split("fun selectTerminalOutput()", 1)[1].split("// Fit seven", 1)[0]
        self.assertIn("selectedOutput = output", method)
        self.assertIn("pasteClipboard()", method)
        self.assertNotIn("No terminal output to select", method)
        self.assertNotIn('TerminalExtraKey(\n                "PASTE"', source)
        self.assertIn("active = selectedOutput != null", source)
        self.assertIn("clipboardAvailable", source)
        self.assertIn("ShortcutState { ACTIVE, HOLD, INACTIVE, DEFAULT }", source)

    def test_six_plugins_are_curated_and_whitelisted(self):
        source = (SRC / "plugins/KlyroPluginManager.kt").read_text()
        for name in ("app.klyro.git", "app.klyro.files", "app.klyro.system",
                     "app.klyro.git-review", "app.klyro.storage", "app.klyro.project"):
            self.assertIn('"' + name + '"', source)
        self.assertIn("readOnlyCommands.contains(value)", source)
        self.assertIn("safePackageSearch.matches(value)", source)
        self.assertIn("MAX_PLUGINS = 12", source)

    def test_settings_and_home_use_selected_mark(self):
        home = (SRC / "MainActivity.kt").read_text()
        settings = (SRC / "ui/KlyroSettingsSheet.kt").read_text()
        for mark in ("ic_klyro_brand_a", "ic_klyro_brand_b"):
            self.assertIn(mark, home)
            self.assertIn(mark, settings)
        self.assertIn("KlyroBrandChoice.select(context, choice)", settings)


if __name__ == "__main__":
    unittest.main()
