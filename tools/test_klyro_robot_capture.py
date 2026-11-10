"""Device robot semantics: dedupe, safe expansion and full-sheet scroll coverage."""
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import klyro_ui_robot as robot

SAMPLE = ('<hierarchy><node class="android.view.View" bounds="[0,0][1080,2424]">'
          '<node content-desc="Fermer la feuille" bounds="[0,0][1080,1215]"/>'
          '<node content-desc="Poignée de déplacement" bounds="[498,1273][582,1284]"/>'
          '<node text="Settings"/></node></hierarchy>')

class CaptureTests(unittest.TestCase):
    def test_identical_sheet_saves_one_png_and_xml(self):
        screenshots = []
        def adb_stub(*args, **kwargs):
            if args[:2] == ("exec-out", "cat"):
                return SAMPLE
            if args[:2] == ("exec-out", "screencap"):
                screenshots.append(1)
                return b"fake-png"
            return ""
        with tempfile.TemporaryDirectory() as temp:
            r = robot.Robot(Path(temp), .1)
            with patch.object(robot, "active", return_value=True), \
                 patch.object(robot, "adb", side_effect=adb_stub):
                first = r.snap("first-settings")
                second = r.snap("second-settings")
            self.assertEqual(first["shot"], second["shot"])
            self.assertEqual(first["xml"], second["xml"])
            self.assertEqual(second["duplicate_of"], first["shot"])
            self.assertEqual(len(screenshots), 1)
            self.assertEqual(r.duplicates, 1)
            self.assertEqual(len(list(Path(temp).glob("*.png"))), 1)
            self.assertEqual(len(list(Path(temp).glob("*.xml"))), 1)

    def test_sheet_expand_uses_drag_handle_not_settings_controls(self):
        baseline = {"sheet_top": 1215, "handle": (498,1273,582,1284),
                    "screen_height": 2424, "stage": "Appearance",
                    "shot": "partial.png", "signature": "partial",
                    "scroll": None}
        expanded = {**baseline, "sheet_top": 250,
                    "shot": "full.png", "signature": "expanded"}
        r = robot.Robot(Path("."), .1)
        with patch.object(r, "swipe") as sw, patch.object(r, "snap", return_value=expanded):
            result = r.capture_full_sheet(baseline, "Appearance")
        self.assertEqual(result["shot"], "full.png")
        self.assertEqual(r.sheet_gallery["Appearance"]["expanded"], "verified")
        self.assertEqual(r.sheet_gallery["Appearance"]["images"], ["full.png"])
        self.assertEqual(sw.call_args.args[:2], (540, 1278))

    def test_sheet_scroll_captures_unique_views_and_returns_to_start(self):
        baseline = {"sheet_top": 140, "handle": None,
                    "screen_height": 2424, "stage": "Appearance",
                    "shot": "top.png", "signature": "top",
                    "scroll": (0,300,1080,2424)}
        middle = {**baseline, "shot": "bottom.png", "signature": "bottom"}
        r = robot.Robot(Path("."), .1)
        with patch.object(r, "swipe"), patch.object(
                r, "snap", side_effect=[middle, middle, baseline]):
            r.capture_full_sheet(baseline, "Appearance", max_scrolls=4)
        result = r.sheet_gallery["Appearance"]
        self.assertEqual(result["images"], ["top.png", "bottom.png"])
        self.assertEqual(result["scroll_segments"], 1)
        self.assertTrue(result["fully_captured"])

    def test_previous_gallery_prevents_repeated_expansion(self):
        r = robot.Robot(Path("."), .1)
        r.sheet_gallery["Settings"] = {"images": ["settings.png"]}
        state = {"sheet_top": 1200, "stage": "Settings"}
        with patch.object(r, "swipe") as swipe:
            self.assertIs(r.capture_full_sheet(state, "Settings"), state)
        swipe.assert_not_called()

if __name__ == "__main__":
    unittest.main()
