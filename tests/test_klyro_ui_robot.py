import importlib.util
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

spec=importlib.util.spec_from_file_location("klyro_ui_robot",Path(__file__).resolve().parents[1]/"tools"/"klyro_ui_robot.py")
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class RobotParsingTests(unittest.TestCase):
    def test_compose_descendant_accessibility_label(self):
        root=ET.fromstring('<hierarchy><node clickable="true" enabled="true" bounds="[0,0][100,100]"><node content-desc="New terminal" /></node></hierarchy>')
        self.assertEqual(mod.controls(root)[0]["label"],"New terminal")
    def test_dialog_state(self):
        root=ET.fromstring('<hierarchy><node text="End Bash session?" /><node text="Keep session" /><node text="End session" /></hierarchy>')
        self.assertTrue(mod.state(root)["dialog"])
    def test_session_deduplication(self):
        root=ET.fromstring('<hierarchy><node content-desc="Session 1, project General" /><node text="Session 1" /><node text="Session 2" /></hierarchy>')
        self.assertEqual(mod.state(root)["sessions"],["1","2"])
    def test_no_unlabeled_action(self):
        root=ET.fromstring('<hierarchy><node clickable="true" enabled="true" bounds="[0,0][100,100]" /></hierarchy>')
        self.assertEqual(mod.controls(root),[])
    def test_safe_allowlist(self):
        self.assertTrue(mod.SAFE.fullmatch("More actions"))
        self.assertFalse(bool(mod.SAFE.fullmatch("End session")))
        self.assertTrue(bool(mod.DANGER.search("End session")))

if __name__=="__main__":
    unittest.main()
