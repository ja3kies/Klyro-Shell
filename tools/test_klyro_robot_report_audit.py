"""Regression tests for the Klyro robot report analyzer."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("klyro_robot_report_audit.py")
spec = importlib.util.spec_from_file_location("klyro_robot_report_audit", SCRIPT)
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)

class RobotReportAuditTests(unittest.TestCase):
    def test_labeled_parent_and_missing_marker(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-plugins.xml"
            p.write_text("""<?xml version="1.0" encoding="UTF-8"?>
<hierarchy><node text="" content-desc="" clickable="false" enabled="true" bounds="[0,0][500,900]">
<node text="" content-desc="" clickable="true" enabled="true" bounds="[0,100][400,250]">
<node text="Plugins" clickable="false" enabled="true" bounds="[20,110][200,150]"/>
</node>
</node></hierarchy>""")
            result = audit.inspect(p)
            self.assertEqual(result["stage"], "plugins")
            self.assertEqual(result["unlabeled"], [])
            self.assertIn("Installed", result["missing"])
            self.assertIn("Explore", result["missing"])
            self.assertIn("Installed", audit.report([result]))

    def test_unlabeled_target_and_small_bounds(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "000-initial.xml"
            p.write_text("""<hierarchy><node text="" content-desc="" clickable="true" enabled="true" bounds="[0,0][20,20]"/></hierarchy>""")
            result = audit.inspect(p, density=2.0)
            self.assertEqual(len(result["unlabeled"]), 1)
            self.assertEqual(len(result["small"]), 1)
            self.assertIn("RUNTIME STATUS", result["missing"])

    def test_descendant_description_survives_blank_text(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "002-home.xml"
            p.write_text("""<hierarchy><node clickable="true" enabled="true" bounds="[0,0][200,200]"><node text=" " content-desc="Launch Bash" clickable="false"/></node></hierarchy>""")
            result = audit.inspect(p)
            self.assertEqual(result["unlabeled"], [])
            self.assertIn("RUNTIME STATUS", result["missing"])

    def test_case_insensitive_markers_and_stage_tokens(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "003-plugins.xml"
            p.write_text("""<hierarchy><node text="PLUGINS"/><node text="installed (1)"/><node content-desc="EXPLORE"/></hierarchy>""")
            result = audit.inspect(p)
            self.assertEqual(result["stage"], "plugins")
            self.assertEqual(result["missing"], [])

    def test_size_is_unknown_without_density(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-home.xml"
            p.write_text("""<hierarchy><node clickable="true" enabled="true" text="Launch Bash" bounds="[0,0][20,20]"/></hierarchy>""")
            self.assertEqual(audit.inspect(p)["small"], [])
            self.assertEqual(len(audit.inspect(p, density=2)["small"]), 1)

    def test_invalid_bounds_are_reported(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-home.xml"
            p.write_text("""<hierarchy><node bounds="[20,20][10,10]"/><node bounds="broken"/><node bounds="[0,0][100,100]"/></hierarchy>""")
            result = audit.inspect(p)
            self.assertEqual(len(result["invalid_bounds"]), 2)
            self.assertIn("malformed", audit.report([result]))

    def test_empty_snapshot_is_inconclusive(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "000-initial.xml"
            p.write_text("<hierarchy/>")
            result = audit.inspect(p)
            self.assertTrue(result["empty"])
            self.assertIn("inconclusive", audit.report([result]))

    def test_invisible_control_is_not_reported_as_clickable(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-home.xml"
            p.write_text("""<hierarchy><node clickable="true" enabled="true" visible-to-user="false" bounds="[0,0][10,10]"/></hierarchy>""")
            result = audit.inspect(p, density=2)
            self.assertEqual(result["interactive"], 0)
            self.assertEqual(result["small"], [])
            self.assertEqual(result["unlabeled"], [])

    def test_hidden_markers_do_not_satisfy_navigation(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-plugins.xml"
            p.write_text("""<hierarchy><node text="Plugins"/><node text="Installed" visible-to-user="false"/><node text="Explore" visible-to-user="false"/></hierarchy>""")
            result = audit.inspect(p)
            self.assertIn("Installed", result["missing"])
            self.assertIn("Explore", result["missing"])

    def test_hidden_child_does_not_label_control(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-home.xml"
            p.write_text("""<hierarchy><node clickable="true" bounds="[0,0][200,200]"><node content-desc="Invisible" visible-to-user="false"/></node></hierarchy>""")
            self.assertEqual(len(audit.inspect(p)["unlabeled"]), 1)

    def test_fractional_density_threshold_not_rounded(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-home.xml"
            p.write_text("""<hierarchy><node clickable="true" text="Tap" bounds="[0,0][49,49]"/></hierarchy>""")
            self.assertEqual(len(audit.inspect(p, density=1.025)["small"]), 1)

    def test_home_is_valid_when_terminal_is_running(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "002-home.xml"
            p.write_text("""<hierarchy><node text="KLYRO"/><node text="RUNTIME STATUS"/><node text="Return to terminal"/><node text="Plugins"/><node text="Settings"/></hierarchy>""")
            self.assertEqual(audit.inspect(p)["missing"], [])

    def test_first_install_home_has_no_launch_button(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "002-home.xml"
            p.write_text("""<hierarchy><node text="KLYRO"/><node text="RUNTIME STATUS"/><node text="Plugins"/><node text="Settings"/></hierarchy>""")
            self.assertEqual(audit.inspect(p)["missing"], [])

    def test_hidden_ancestor_does_not_satisfy_navigation_markers(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-plugins.xml"
            p.write_text("""<hierarchy><node text="Plugins"/><node visible-to-user="false"><node text="Installed"/><node text="Explore"/></node></hierarchy>""")
            result = audit.inspect(p)
            self.assertIn("Installed", result["missing"])
            self.assertIn("Explore", result["missing"])

    def test_hidden_ancestor_does_not_label_clickable_control(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-home.xml"
            p.write_text("""<hierarchy><node clickable="true" bounds="[0,0][200,200]"><node visible-to-user="false"><node content-desc="Hidden label"/></node></node></hierarchy>""")
            self.assertEqual(len(audit.inspect(p)["unlabeled"]), 1)

    def test_small_touch_target_has_label_and_measured_dimensions(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "014-after-diagnostics.xml"
            p.write_text(
                '<hierarchy><node clickable="true" bounds="[42,2307][1038,2424]">'
                '<node text="Recheck environment" /></node></hierarchy>'
            )
            result = audit.inspect(p, density=2.625)
            self.assertEqual(result["small_details"][0]["label"], "Recheck environment")
            self.assertEqual(result["small_details"][0]["height_dp"], 44.6)
            text = audit.report([result])
            self.assertIn("Recheck environment", text)
            self.assertIn("48dp", text)

    def test_outside_display_click_target_is_reported(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "005-after-settings.xml"
            p.write_text(
                '<hierarchy><node bounds="[0,0][1080,2424]">'
                '<node clickable="true" enabled="true" text="Close" '
                'bounds="[940,2340][1140,2470]"/></node></hierarchy>'
            )
            result = audit.inspect(p, density=2.625)
            self.assertEqual(len(result["clipped"]), 1)
            self.assertEqual(result["clipped"][0]["label"], "Close")
            self.assertIn("outside the captured display", audit.report([result]))

    def test_hidden_geometry_is_not_reported_as_clipping(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "003-after-settings.xml"
            p.write_text(
                '<hierarchy><node bounds="[0,0][1080,2424]">'
                '<node visible-to-user="false" bounds="[0,0][1080,2424]">'
                '<node clickable="true" text="Remove" bounds="[1200,2500][1500,2800]"/>'
                '</node></node></hierarchy>'
            )
            result = audit.inspect(p, density=2.625)
            self.assertEqual(result["clipped"], [])
            self.assertEqual(result["interactive"], 0)

    def test_valid_target_at_screen_boundary_is_not_clipped(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "001-after-home.xml"
            p.write_text(
                '<hierarchy><node bounds="[0,0][1080,2424]">'
                '<node clickable="true" text="Plugins" bounds="[900,2290][1080,2424]"/>'
                '</node></hierarchy>'
            )
            self.assertEqual(audit.inspect(p)["clipped"], [])

    def test_long_sheet_expected_markers_accumulate_across_scrolls(self):
        with tempfile.TemporaryDirectory() as d:
            top = Path(d) / "007-after-appearance.xml"
            lower = Path(d) / "009-full-appearance-scroll-1.xml"
            top.write_text(
                '<hierarchy><node text="Appearance"/><node text="Launcher icon"/></hierarchy>'
            )
            lower.write_text('<hierarchy><node text="App theme"/></hierarchy>')
            raw = [audit.inspect(top), audit.inspect(lower)]
            self.assertIn("App theme", raw[0]["missing"])
            combined = audit.reconcile_expected_markers(raw)
            self.assertEqual(combined[0]["missing"], [])
            self.assertEqual(combined[1]["missing"], [])
            self.assertNotIn("expected visible marker not found", audit.report(raw))

    def test_missing_across_all_scroll_views_is_reported_only_once(self):
        with tempfile.TemporaryDirectory() as d:
            top = Path(d) / "007-after-appearance.xml"
            lower = Path(d) / "009-full-appearance-scroll-1.xml"
            top.write_text(
                '<hierarchy><node text="Appearance"/><node text="Launcher icon"/></hierarchy>'
            )
            lower.write_text('<hierarchy><node text="Terminal font"/></hierarchy>')
            merged = audit.reconcile_expected_markers(
                [audit.inspect(top), audit.inspect(lower)]
            )
            self.assertEqual(merged[0]["missing"], ["App theme"])
            self.assertEqual(merged[1]["missing"], [])
            self.assertEqual(audit.report(merged).count(
                "expected visible marker not found"), 1)

    def test_stage_requires_complete_token(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "003-pluginsettings.xml"
            p.write_text("<hierarchy/>")
            self.assertEqual(audit.inspect(p)["stage"], "unknown")

if __name__ == "__main__":
    unittest.main()
