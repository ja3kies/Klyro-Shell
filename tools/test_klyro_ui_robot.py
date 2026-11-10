"""Deterministic Klyro robot regression suite (no physical device required)."""
import importlib.util
from unittest.mock import patch
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "klyro_ui_robot", Path(__file__).with_name("klyro_ui_robot.py"))
robot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(robot)


def tree(label, nested=True):
    if nested:
        return ET.fromstring(
            '<hierarchy><node clickable="true" enabled="true" bounds="[0,0][300,300]">'
            '<node text="' + label + '" /></node></hierarchy>')
    return ET.fromstring(
        '<hierarchy><node clickable="true" enabled="true" text="' + label +
        '" bounds="[0,0][300,300]"/></hierarchy>')


class RobotSemanticTests(unittest.TestCase):
    def test_compose_container_uses_child_title(self):
        self.assertEqual(robot.controls(tree("Plugins"))[0]["label"], "Plugins")

    def test_ancestor_onclick_label_does_not_hide_plugins_title(self):
        root = ET.fromstring(
            '<hierarchy><node clickable="true" enabled="true" '
            'content-desc="Open Klyro plugins" bounds="[0,0][300,200]">'
            '<node text="Plugins"/><node text="Extend the command palette"/>'
            '</node></hierarchy>')
        controls = robot.controls(root)
        self.assertEqual(controls[0]["label"], "Plugins")
        self.assertIn("Open Klyro plugins", controls[0]["aliases"])
        self.assertEqual(robot.Robot(Path("."), .1).find(
            robot.state(root), "Plugins")["label"], "Plugins")

    def test_ancestor_onclick_label_does_not_hide_settings_title(self):
        root = ET.fromstring(
            '<hierarchy><node clickable="true" enabled="true" '
            'content-desc="Open Klyro settings" bounds="[0,0][300,200]">'
            '<node text="Settings"/></node></hierarchy>')
        self.assertEqual(robot.Robot(Path("."), .1).find(
            robot.state(root), "Settings")["label"], "Settings")

    def test_selected_tabs_keep_label_if_description_differs(self):
        root = ET.fromstring(
            '<hierarchy><node selected="true" clickable="true" enabled="true" '
            'content-desc="Show Installed (1)" bounds="[0,0][300,200]">'
            '<node text="Installed (1)"/></node></hierarchy>')
        self.assertIn("Installed (1)", robot.state(root)["selected_tabs"])

    def test_invisible_and_disabled_controls_never_tapped(self):
        root = ET.fromstring(
            '<hierarchy><node clickable="true" enabled="true" visible-to-user="false" '
            'text="Settings" bounds="[0,0][300,200]"/>'
            '<node clickable="true" enabled="false" text="Plugins" '
            'bounds="[0,0][300,200]"/></hierarchy>')
        self.assertEqual(robot.controls(root), [])

    def test_non_destructive_skip_records_missing_semantics(self):
        r = robot.Robot(Path("."), .1)
        before = {"shot": "initial.png", "selected_tabs": []}
        self.assertIs(r.probe_plugin_tabs(before), before)
        self.assertEqual(r.steps[0]["status"], "SKIP")
        self.assertTrue(r.warnings)

    def test_missing_navigation_is_fail_not_silent(self):
        r = robot.Robot(Path("."), .1)
        before = {"controls": [], "shot": "initial.png"}
        self.assertIsNone(r.navigate(before, "Settings", ("Settings",)))
        self.assertEqual(r.steps[0]["status"], "FAIL")

    def test_current_control_bounds_parser_and_visibility(self):
        root = ET.fromstring(
            '<hierarchy><node clickable="true" enabled="true" '
            'visible-to-user="true" text="Settings" bounds="[2,4][302,204]"/></hierarchy>')
        result = robot.controls(root)
        self.assertEqual(result[0]["x"], 152)
        self.assertEqual(result[0]["y"], 104)

    def test_malformed_bounds_do_not_create_control(self):
        root = ET.fromstring(
            '<hierarchy><node clickable="true" enabled="true" '
            'text="Settings" bounds="x:y"/></hierarchy>')
        self.assertEqual(robot.controls(root), [])

    def test_first_install_home_requires_no_bash_button(self):
        r = robot.Robot(Path("."), .1)
        self.assertTrue(r.has_markers(
            {"labels": ["KLYRO", "RUNTIME STATUS", "Install runtime",
                        "Plugins", "Settings"]}, robot.HOME_MARKERS))

    def test_hidden_parent_hides_nested_clickable_child(self):
        root = ET.fromstring(
            '<hierarchy><node visible-to-user="false">'
            '<node clickable="true" enabled="true" text="Settings" '
            'bounds="[0,0][300,200]"/></node></hierarchy>')
        self.assertEqual(robot.controls(root), [])
        self.assertNotIn("Settings", robot.state(root)["labels"])

    def test_visible_parent_with_hidden_description_child_is_unlabeled(self):
        root = ET.fromstring(
            '<hierarchy><node clickable="true" enabled="true" '
            'bounds="[0,0][300,200]">'
            '<node visible-to-user="false"><node text="Settings"/></node>'
            '</node></hierarchy>')
        self.assertEqual(robot.controls(root), [])

    def test_direct_button_label(self):
        self.assertEqual(robot.controls(tree("Home", False))[0]["label"], "Home")

    def test_semantic_find_is_not_substring(self):
        r = robot.Robot(Path("."), .1)
        controls = [{"label": "Home"}, {"label": "Homepage settings"}]
        self.assertEqual(r.find({"controls": controls}, "Home")["label"], "Home")
        self.assertIsNone(r.find({"controls": controls}, "Settings"))

    def test_required_stages_are_non_destructive(self):
        self.assertEqual([step[0] for step in robot.ROUTES],
                         ["Plugins", "Home", "Settings"])
        self.assertEqual([step[0] for step in robot.SETTINGS_SUBPAGES],
                         ["Appearance", "Keyboard", "Storage", "Privacy",
                          "Diagnostics", "About"])
        for name, _ in robot.ROUTES + robot.SETTINGS_SUBPAGES:
            self.assertIsNone(robot.DANGER.search(name))

    def test_smoke_assertions_use_visible_labels(self):
        r = robot.Robot(Path("."), .1)
        self.assertTrue(r.has_markers(
            {"labels": ["Installed (1)", "Explore", "Plugins"]},
            ("Plugins", "Installed", "Explore")))
        self.assertFalse(r.has_markers(
            {"labels": ["Plugins", "Installed (1)"]},
            ("Plugins", "Installed", "Explore")))

    def test_unlabeled_control_ignored(self):
        self.assertEqual(robot.controls(tree("")), [])

    def test_bash_resume_home_is_supported(self):
        r = robot.Robot(Path("."), .1)
        self.assertTrue(r.has_markers(
            {"labels": ["KLYRO", "RUNTIME STATUS", "Return to terminal", "Plugins", "Settings"]},
            robot.HOME_MARKERS))

    def test_default_home_is_supported(self):
        r = robot.Robot(Path("."), .1)
        self.assertTrue(r.has_markers(
            {"labels": ["KLYRO", "RUNTIME STATUS", "Launch Bash", "Plugins", "Settings"]},
            robot.HOME_MARKERS))

    def test_density_uses_android_override(self):
        with patch.object(robot, "adb", return_value="Physical density: 420\\nOverride density: 440"):
            self.assertEqual(robot.measured_density(), 2.75)

    def test_density_without_device_value_is_unknown(self):
        with patch.object(robot, "adb", return_value="No density information"):
            self.assertIsNone(robot.measured_density())

    def test_selected_plugin_tab_is_detected(self):
        root = ET.fromstring(
            '<hierarchy><node selected="true" clickable="true" enabled="true" bounds="[0,0][200,100]">'
            '<node text="Installed (1)" /></node>'
            '<node selected="false" clickable="true" enabled="true" bounds="[200,0][400,100]">'
            '<node text="Explore" /></node></hierarchy>')
        snapshot = robot.state(root)
        self.assertEqual(snapshot["selected_tabs"], ["Installed (1)"])
        self.assertEqual(robot.Robot(Path("."), .1).find(snapshot, r"Installed [(][0-9]+[)]")["label"], "Installed (1)")

    def test_unselected_plugin_tab_does_not_count_as_selected(self):
        root = ET.fromstring(
            '<hierarchy><node selected="false"><node text="Installed (0)"/></node>'
            '<node selected="true"><node text="Explore"/></node></hierarchy>')
        self.assertEqual(robot.state(root)["selected_tabs"], ["Explore"])

    def test_no_destructive_action_allowlist(self):
        for unsafe in ("Install", "Remove", "End Bash session", "Clear", "Reset"):
            self.assertFalse(bool(robot.SAFE.fullmatch(unsafe)))

    def test_dialog_state_detected(self):
        root = ET.fromstring('<hierarchy><node text="End Bash session?" /></hierarchy>')
        self.assertTrue(robot.state(root)["dialog"])

    def test_sessions_are_deduplicated(self):
        root = ET.fromstring(
            '<hierarchy><node text="Session 1" />'
            '<node content-desc="Session 1, project General" />'
            '<node text="Session 2" /></hierarchy>')
        self.assertEqual(robot.state(root)["sessions"], ["1", "2"])


if __name__ == "__main__":
    unittest.main()
