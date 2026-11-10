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
            self.assertIn("Launch Bash", result["missing"])

    def test_descendant_description_survives_blank_text(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "002-home.xml"
            p.write_text("""<hierarchy><node clickable="true" enabled="true" bounds="[0,0][200,200]"><node text=" " content-desc="Launch Bash" clickable="false"/></node></hierarchy>""")
            result = audit.inspect(p)
            self.assertEqual(result["unlabeled"], [])
            self.assertNotIn("Launch Bash", result["missing"])

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

    def test_stage_requires_complete_token(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "003-pluginsettings.xml"
            p.write_text("<hierarchy/>")
            self.assertEqual(audit.inspect(p)["stage"], "unknown")

if __name__ == "__main__":
    unittest.main()
