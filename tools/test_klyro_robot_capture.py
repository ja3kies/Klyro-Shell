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
                    "scroll": None, "touches_bottom": True}
        expanded = {**baseline, "sheet_top": 250,
                    "shot": "full.png", "signature": "expanded"}
        r = robot.Robot(Path("."), .1)
        with patch.object(r, "swipe") as sw, patch.object(r, "snap", return_value=expanded):
            result = r.capture_full_sheet(baseline, "Appearance")
        self.assertEqual(result["shot"], "full.png")
        self.assertEqual(r.sheet_gallery["Appearance"]["expanded"], "verified")
        self.assertEqual(r.sheet_gallery["Appearance"]["images"], ["full.png"])
        self.assertEqual(sw.call_args.args[:2], (540, 1278))

    def test_compact_sheet_with_handle_requires_no_drag(self):
        baseline = {"sheet_top": 1215, "handle": (498,1273,582,1284),
                    "screen_height": 2424, "stage": "Settings",
                    "shot": "compact.png", "signature": "compact",
                    "scroll": None, "touches_bottom": False}
        r = robot.Robot(Path("."), 0)
        with patch.object(r, "swipe") as swipe:
            observed = r.capture_full_sheet(baseline, "Settings")
        swipe.assert_not_called()
        self.assertEqual(observed["shot"], "compact.png")
        self.assertTrue(r.sheet_gallery["Settings"]["fully_captured"])
        self.assertEqual(r.warnings, [])

    def test_disappearing_sheet_never_reuses_stale_navigation_state(self):
        baseline = {"sheet_top": 1215, "handle": (498,1273,582,1284),
                    "screen_height": 2424, "stage": "Appearance",
                    "shot": "partial.png", "signature": "partial",
                    "scroll": None, "touches_bottom": True}
        gone = {**baseline, "sheet_top": None, "stage": "Home",
                "shot": "home.png", "signature": "home"}
        r = robot.Robot(Path("."), 0)
        with patch.object(r, "swipe"), patch.object(r, "snap", return_value=gone):
            with self.assertRaisesRegex(RuntimeError, "Sheet disappeared"):
                r.capture_full_sheet(baseline, "Appearance")
        self.assertEqual(
            r.sheet_gallery["Appearance"]["expanded"],
            "unexpected navigation; capture stopped")

    def test_sheet_scroll_captures_unique_views_without_dragging_down(self):
        baseline = {"sheet_top": 140, "handle": None,
                    "screen_height": 2424, "stage": "Appearance",
                    "shot": "top.png", "signature": "top",
                    "scroll": (0,300,1080,2424)}
        middle = {**baseline, "shot": "bottom.png", "signature": "bottom"}
        r = robot.Robot(Path("."), .1)
        with patch.object(r, "swipe") as swipe, patch.object(
                r, "snap", side_effect=[middle, middle]):
            result = r.capture_full_sheet(baseline, "Appearance", max_scrolls=4)
        data = r.sheet_gallery["Appearance"]
        self.assertEqual(data["images"], ["top.png", "bottom.png"])
        self.assertEqual(data["scroll_segments"], 1)
        self.assertTrue(data["fully_captured"])
        self.assertEqual(data["exit_strategy"], "android-back")
        self.assertEqual(result["shot"], "bottom.png")
        self.assertEqual(swipe.call_count, 2)
        # A downwards swipe on this scrollable area collapses/dismisses
        # the Material bottom sheet on the real Pixel 9.
        for args, _ in swipe.call_args_list:
            self.assertGreater(args[1], args[3])

    def test_no_downward_scroll_restoration_on_actual_pixel_failure(self):
        baseline = {"sheet_top": 606, "handle": None,
                    "screen_height": 2424, "stage": "Appearance",
                    "shot": "007-after-appearance.png", "signature": "top",
                    "scroll": (0,733,1080,2361)}
        bottom = {**baseline, "shot": "009-full-appearance-scroll-1.png",
                  "signature": "bottom"}
        r = robot.Robot(Path("."), .1)
        with patch.object(r, "swipe") as swipe, patch.object(
                r, "snap", side_effect=[bottom, bottom]):
            result = r.capture_full_sheet(baseline, "Appearance")
        self.assertEqual(result["shot"], "009-full-appearance-scroll-1.png")
        self.assertTrue(all(call.args[1] > call.args[3]
                            for call in swipe.call_args_list))
        self.assertEqual(r.warnings, [])

    def test_failed_tap_cannot_pass_on_changing_live_text(self):
        before = {"shot": "home-before.png", "stage": "Home",
                  "screen": ("KLYRO", "Clock 20:00"),
                  "controls": [{"label": "Plugins", "aliases": ["Plugins"],
                                "x": 120, "y": 250}],
                  "labels": ["Plugins", "KLYRO"]}
        after = {"shot": "home-after.png", "stage": "Home",
                 "screen": ("KLYRO", "Clock 20:01", "Plugins",
                            "Installed", "Explore"),
                 "labels": ["Plugins", "Installed", "Explore", "Clock 20:01"],
                 "sheet_top": None}
        r = robot.Robot(Path("."), 0)
        with patch.object(r, "tap"), \
             patch.object(r, "wait_for_markers", return_value=after):
            result = r.navigate(before, "Plugins",
                                ("Plugins", "Installed", "Explore"))
        self.assertIsNone(result)
        self.assertEqual(r.steps[-1]["status"], "FAIL")

    def test_subpage_routes_remain_identifiable_after_title_scrolls_away(self):
        before = {"shot": "settings.png", "stage": "Settings",
                  "screen": ("Settings",),
                  "controls": [{"label": "Appearance",
                                "aliases": ["Appearance"],
                                "x": 220, "y": 340}]}
        opened = {"shot": "appearance-top.png", "stage": "Appearance",
                  "screen": ("Appearance", "Launcher icon", "App theme"),
                  "labels": ["Appearance", "Launcher icon", "App theme"],
                  "sheet_top": 500}
        scrolled = {"shot": "appearance-bottom.png", "stage": "Settings",
                    "screen": ("Accent", "App theme"),
                    "labels": ["App theme", "Accent"], "sheet_top": 140}
        r = robot.Robot(Path("."), 0)
        def capture(snapshot, title):
            r.sheet_gallery[title] = {"observed_labels": opened["labels"]}
            return scrolled
        with patch.object(r, "tap"), \
             patch.object(r, "wait_for_markers", return_value=opened), \
             patch.object(r, "capture_full_sheet", side_effect=capture):
            result = r.navigate(before, "Appearance",
                                ("Appearance", "Launcher icon", "App theme"))
        self.assertIsNotNone(result)
        self.assertEqual(result["route_stage"], "Appearance")
        self.assertEqual(r.steps[-1]["destination"], "Appearance")
        self.assertEqual(r.steps[-1]["status"], "PASS")

    def test_below_fold_diagnostics_marker_valid_after_full_capture(self):
        before = {"shot": "settings.png", "screen": ("Settings", "Appearance"),
                  "controls": [{"label": "Diagnostics", "aliases": ["Diagnostics"],
                                "x": 540, "y": 200, "bounds": (40,100,1040,300)}],
                  "stage": "Settings"}
        partial = {"shot": "diagnostics-top.png", "screen": ("Diagnostics",),
                   "labels": ["Diagnostics"], "stage": "Diagnostics", "sheet_top": 700}
        bottom = {"shot": "diagnostics-bottom.png",
                  "screen": ("Recheck environment",),
                  "labels": ["Recheck environment"], "stage": "Diagnostics",
                  "sheet_top": 100}
        r = robot.Robot(Path("."), 0)
        def full_capture(state, page):
            self.assertEqual(page, "Diagnostics")
            r.sheet_gallery[page] = {
                "observed_labels": ["Diagnostics", "Recheck environment"]}
            return bottom
        with patch.object(r, "tap"), \
             patch.object(r, "wait_for_markers", return_value=partial), \
             patch.object(r, "capture_full_sheet", side_effect=full_capture):
            result = r.navigate(before, "Diagnostics",
                                ("Diagnostics", "Recheck environment"))
        self.assertEqual(result, bottom)
        self.assertEqual(r.steps[-1]["status"], "PASS")
        self.assertEqual(r.steps[-1]["target"]["label"], "Diagnostics")

    def test_deeper_marker_missing_after_full_sheet_is_real_failure(self):
        before = {"shot": "settings.png", "screen": ("Settings",),
                  "controls": [{"label": "About", "aliases": ["About"],
                                "x": 500, "y": 300, "bounds": (40,100,1000,350)}],
                  "stage": "Settings"}
        partial = {"shot": "about-top.png", "screen": ("About Klyro Shell",),
                   "labels": ["About Klyro Shell"], "stage": "About",
                   "sheet_top": 500}
        r = robot.Robot(Path("."), 0)
        def full_capture(state, page):
            r.sheet_gallery[page] = {"observed_labels": ["About Klyro Shell"]}
            return state
        with patch.object(r, "tap"), \
             patch.object(r, "wait_for_markers", return_value=partial), \
             patch.object(r, "capture_full_sheet", side_effect=full_capture):
            result = r.navigate(before, "About",
                                ("About Klyro Shell", "JetBrains Mono licence"))
        self.assertIsNone(result)
        self.assertEqual(r.steps[-1]["status"], "FAIL")

    def test_backhandler_restores_settings_root_after_scrolled_appearance(self):
        detail = {"shot": "bottom.png", "screen": ("Terminal font", "Accent"),
                  "stage": "Appearance", "sheet_top": 606}
        root = {"shot": "settings.png",
                "screen": ("Settings", "Appearance", "Keyboard", "Storage", "Privacy"),
                "labels": ["Settings", "Appearance", "Keyboard", "Storage", "Privacy"],
                "stage": "Settings", "sheet_top": 1215}
        r = robot.Robot(Path("."), 0)
        with patch.object(robot, "active", return_value=True), \
             patch.object(robot, "adb", return_value="") as adb, \
             patch.object(r, "wait_for_markers", return_value=root):
            after = r.back_to_settings(detail, "Appearance")
        self.assertEqual(after, root)
        adb.assert_called_once_with("shell", "input", "keyevent", "KEYCODE_BACK")
        self.assertEqual(r.steps[0]["status"], "PASS")

    def test_backhandler_failure_if_sheet_was_dismissed(self):
        detail = {"shot": "bottom.png", "screen": ("Accent",),
                  "stage": "Appearance", "sheet_top": 606}
        dismissed = {"shot": "home.png", "screen": ("KLYRO",), "labels": ["KLYRO"],
                     "stage": "Home", "sheet_top": None}
        r = robot.Robot(Path("."), 0)
        with patch.object(robot, "active", return_value=True), \
             patch.object(robot, "adb", return_value=""), \
             patch.object(r, "wait_for_markers", return_value=dismissed):
            self.assertIsNone(r.back_to_settings(detail, "Appearance"))
        self.assertEqual(r.steps[0]["status"], "FAIL")

    def test_half_expanded_sheet_without_scroll_is_incomplete(self):
        baseline = {"sheet_top": 1400, "handle": None,
                    "screen_height": 2424, "stage": "Settings",
                    "shot": "partial.png", "signature": "partial",
                    "scroll": None, "touches_bottom": True}
        r = robot.Robot(Path("."), .1)
        with patch.object(r, "swipe") as swipe:
            r.capture_full_sheet(baseline, "Settings")
        self.assertFalse(r.sheet_gallery["Settings"]["fully_captured"])
        self.assertTrue(r.warnings)
        swipe.assert_not_called()

    def test_compact_sheet_that_fits_does_not_trigger_false_warning(self):
        baseline = {"sheet_top": 1215, "handle": None,
                    "screen_height": 2424, "stage": "Settings",
                    "shot": "complete.png", "signature": "complete",
                    "scroll": None, "touches_bottom": False}
        r = robot.Robot(Path("."), .1)
        with patch.object(r, "swipe") as swipe:
            r.capture_full_sheet(baseline, "Settings")
        self.assertTrue(r.sheet_gallery["Settings"]["fully_captured"])
        self.assertEqual(r.warnings, [])
        swipe.assert_not_called()

    def test_previous_gallery_prevents_repeated_expansion(self):
        r = robot.Robot(Path("."), .1)
        r.sheet_gallery["Settings"] = {"images": ["settings.png"]}
        state = {"sheet_top": 1200, "stage": "Settings"}
        with patch.object(r, "swipe") as swipe:
            self.assertIs(r.capture_full_sheet(state, "Settings"), state)
        swipe.assert_not_called()

if __name__ == "__main__":
    unittest.main()
