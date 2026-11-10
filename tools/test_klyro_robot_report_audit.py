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
            result = audit.inspect(p)
            self.assertEqual(len(result["unlabeled"]), 1)
            self.assertEqual(len(result["small"]), 1)
            self.assertIn("Launch Bash", result["missing"])

if __name__ == "__main__":
    unittest.main()
