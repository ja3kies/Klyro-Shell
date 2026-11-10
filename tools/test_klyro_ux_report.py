"""Regression tests for source-grounded component UI/UX reporting."""
import json
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET
import klyro_ux_report as review
import klyro_ui_robot as robot

class SheetEvidenceTests(unittest.TestCase):
    def test_repeated_settings_semantics_have_same_fingerprint(self):
        xml = ('<hierarchy><node class="android.view.View" bounds="[0,0][1080,2424]">'
               '<node content-desc="Fermer la feuille" bounds="[0,0][1080,1215]"/>'
               '<node text="Settings" bounds="[40,1368][260,1442]"/>'
               '<node text="Appearance" bounds="[42,1489][529,1715]"/>'
               '</node></hierarchy>')
        root1 = ET.fromstring(xml)
        root2 = ET.fromstring(xml)
        self.assertEqual(robot.xml_signature(root1), robot.xml_signature(root2))
        edited = ET.fromstring(xml.replace('text="Appearance"', 'text="Keyboard"'))
        self.assertNotEqual(robot.xml_signature(root1), robot.xml_signature(edited))

    def test_sheet_geometry_from_ui_automator_handle(self):
        root = ET.fromstring(
            '<hierarchy><node bounds="[0,0][1080,2424]">'
            '<node content-desc="Fermer la feuille" bounds="[0,0][1080,1215]"/>'
            '<node content-desc="Poignée de déplacement" bounds="[498,1273][582,1284]"/>'
            '<node text="Launcher icon"/>'
            '<node scrollable="true" bounds="[0,1339][1080,2424]"/>'
            '</node></hierarchy>')
        info = robot.geometry(root)
        self.assertEqual(info["sheet_top"], 1215)
        self.assertEqual(info["handle"], (498, 1273, 582, 1284))
        self.assertEqual(info["stage"], "Appearance")
        self.assertEqual(info["scroll"], (0, 1339, 1080, 2424))

    def test_invisible_node_cannot_affect_signature(self):
        basic = ET.fromstring('<hierarchy><node text="Settings"/></hierarchy>')
        hidden = ET.fromstring(
            '<hierarchy><node text="Settings"/><node visible-to-user="false">'
            '<node text="Destructive action"/></node></hierarchy>')
        self.assertEqual(robot.xml_signature(basic), robot.xml_signature(hidden))

    def test_exact_source_excerpt_without_guessing(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / review.MAIN
            source.parent.mkdir(parents=True)
            source.write_text(
                'fun app() {\n'
                '  val x = 4\n'
                '  .clickable(onClickLabel = "Open Klyro plugins") { launch() }\n'
                '}\n')
            found = review.source_for("Navigate: Plugins", "Home", folder)
            self.assertEqual(found["status"], "FOUND")
            self.assertEqual(found["line_start"], 1)
            self.assertIn('onClickLabel = "Open Klyro plugins"', found["code"])
            self.assertIn("#L3", found["url"])
            self.assertEqual(
                review.source_for("Navigate: Plugins", "Home", None)["status"],
                "UNAVAILABLE")

    def test_clicked_small_target_is_a_real_failure(self):
        target = {"label": "Recheck environment", "bounds": (42,2307,1038,2424)}
        result = review.examine_target(target, 2.625, 2424)
        checks = result["checks"]
        self.assertTrue(any(c["result"] == "FAIL" and c["check"] == "Zone tactile 48 dp"
                            for c in checks))
        self.assertTrue(any(c["result"] == "REVIEW" for c in checks))

    def test_source_report_escapes_html_and_writes_json(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder)
            (p / "initial.png").write_bytes(b"placeholder")
            step = {"name": "Navigate: Plugins", "status": "PASS",
                    "detail": "Expected Plugins", "before": "initial.png",
                    "after": "initial.png", "from_stage": "Home",
                    "destination": "Plugins",
                    "target": {"label": "<button>", "bounds": (0,0,150,150)}}
            result = review.make_report(
                p, [step], source_root=None, density=2.625,
                sheet_gallery={"Plugins": {"images": ["initial.png"],
                                          "expanded": "not attempted", "scroll_segments": 0}},
                duplicate_count=7, display_height=2424)
            text = (p / "ux-review.html").read_text()
            self.assertIn("&lt;button&gt;", text)
            self.assertNotIn("<button>", text)
            self.assertIn("7 doublons", text)
            self.assertIn("UNAVAILABLE", text)
            self.assertEqual(result[0]["source"]["status"], "UNAVAILABLE")
            self.assertEqual(json.loads((p/"components.json").read_text())
                             ["duplicate_screens_avoided"], 7)

if __name__ == "__main__":
    unittest.main()
