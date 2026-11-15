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

    def test_priority_issue_carries_real_diagnostics_kotlin_code(self):
        repo_root = Path(__file__).resolve().parents[1]
        evidence = [
            {"stage": "diagnostics", "small_details": [
                {"label": "Recheck environment", "bounds": "[42,2307][1038,2424]",
                 "width_dp": 379.4, "height_dp": 44.6}],
             "unlabeled": [], "missing": []}
        ]
        issues = review.findings_from_accessibility(evidence, repo_root)
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0]["priority"], "P1")
        self.assertEqual(issues[0]["source"]["status"], "FOUND")
        self.assertIn("onClick = onRecheckEnvironment", issues[0]["source"]["code"])

    def test_repeated_same_defect_is_listed_once(self):
        one = {"stage": "diagnostics", "small_details": [
            {"label": "Recheck environment", "width_dp": 379.4,
             "height_dp": 44.6}], "missing": [], "unlabeled": []}
        issues = review.findings_from_accessibility([one, one], None)
        self.assertEqual(len(issues), 1)

    def test_navigation_pass_does_not_hide_ux_defects(self):
        with tempfile.TemporaryDirectory() as d:
            robot.report(Path(d), [
                {"name": "Initial Home", "status": "PASS",
                 "before": "initial.png", "after": "initial.png", "detail": "Home"}
            ], [], ux_findings=[{"priority": "P1", "label": "Recheck environment"}])
            payload = json.loads((Path(d) / "results.json").read_text())
            self.assertEqual(payload["overall"], "NEEDS_UX_REVIEW")
            self.assertEqual(payload["summary"]["PASS"], 1)
            self.assertEqual(payload["ux_findings_count"], 1)

    def test_two_scrolled_positions_of_accent_plus_are_one_issue(self):
        repo_root = Path(__file__).resolve().parents[1]
        snapshots = [
            {"stage": "appearance",
             "unlabeled": ["android.widget.Button [912,2207][1038,2333]"],
             "small_details": [], "missing": []},
            {"stage": "appearance",
             "unlabeled": ["android.widget.Button [912,1698][1038,1824]"],
             "small_details": [], "missing": []}
        ]
        issues = review.findings_from_accessibility(snapshots, repo_root)
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0]["label"], "Custom accent (+)")
        self.assertEqual(issues[0]["source_match"], "inferred-from-position-and-code")
        self.assertEqual(issues[0]["source"]["status"], "FOUND")
        self.assertIn('onClickLabel = "Choose custom accent"', issues[0]["source"]["code"])

    def test_back_route_maps_to_actual_compose_backhandler(self):
        root = Path(__file__).resolve().parents[1]
        source = review.source_for("Back to Settings: Appearance", "Appearance", root)
        self.assertEqual(source["status"], "FOUND")
        self.assertIn("fun navigateBack()", source["code"])

    def test_unrelated_unlabeled_control_has_no_false_accent_mapping(self):
        snapshots = [{"stage": "diagnostics",
                      "unlabeled": ["android.widget.Button [912,2207][1038,2333]"],
                      "small_details": [], "missing": []}]
        issue = review.findings_from_accessibility(snapshots, None)[0]
        self.assertEqual(issue["source"]["status"], "UNMAPPED")

    def test_offscreen_control_is_prioritized_without_inventing_source(self):
        sample = {"stage": "appearance",
                  "clipped": [{"label": "Close",
                               "bounds": "[930,2330][1120,2510]"}],
                  "small_details": [], "unlabeled": [], "missing": []}
        issues = review.findings_from_accessibility([sample], None)
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0]["priority"], "P1")
        self.assertEqual(issues[0]["kind"], "clipped-touch-target")
        self.assertEqual(issues[0]["source"]["status"], "UNMAPPED")

    def test_duplicate_offscreen_observation_not_reported_twice(self):
        sample = {"stage": "appearance",
                  "clipped": [{"label": "Close",
                               "bounds": "[930,2330][1120,2510]"}],
                  "small_details": [], "unlabeled": [], "missing": []}
        self.assertEqual(len(review.findings_from_accessibility(
            [sample, sample], None)), 1)

    def test_bootstrap_pins_every_source_file_to_same_revision(self):
        root = Path(__file__).resolve().parents[1]
        script = (root / "scripts/klyro-qa-bootstrap.sh").read_text()
        self.assertIn('export KLYRO_QA_SOURCE_COMMIT="$REV"', script)
        self.assertIn('SOURCE="https://raw.githubusercontent.com/ja3kies/Klyro-Shell/$REV/tools"', script)
        self.assertIn('Klyro-Shell/$REV/$REL', script)
        self.assertIn('printf', script)
        self.assertIn('source-commit.txt', script)

    def test_overlap_is_a_review_finding_not_a_claimed_visual_failure(self):
        snapshot = {
            "stage": "settings", "small_details": [], "unlabeled": [], "missing": [],
            "overlaps": [{"first": "Appearance", "second": "Keyboard",
                          "first_bounds": "[0,0][200,200]",
                          "second_bounds": "[100,100][300,300]",
                          "overlap_pct": 25.0}],
        }
        findings = review.findings_from_accessibility([snapshot], None)
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0]["kind"], "overlapping-touch-targets")
        self.assertEqual(findings[0]["priority"], "P2")
        self.assertEqual(findings[0]["source"]["status"], "UNMAPPED")

    def test_same_overlap_across_scrolled_snapshots_is_one_finding(self):
        snapshot = {
            "stage": "appearance", "small_details": [], "unlabeled": [], "missing": [],
            "overlaps": [{"first": "Theme", "second": "System",
                          "first_bounds": "[20,100][300,300]",
                          "second_bounds": "[120,100][400,300]",
                          "overlap_pct": 64.0}],
        }
        self.assertEqual(
            len(review.findings_from_accessibility([snapshot, snapshot], None)), 1)

    def test_navigation_report_tracks_repeated_layouts_separately(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder)
            robot.report(p, [], [], exact_duplicates=4, layout_revisits=6)
            result = json.loads((p / "results.json").read_text())
            self.assertEqual(result["exact_duplicate_screens"], 4)
            self.assertEqual(result["revisited_screen_layouts"], 6)
            self.assertIn("6", (p / "report.html").read_text())

    def test_gfxinfo_observation_is_reported_but_not_an_animation_benchmark(self):
        with tempfile.TemporaryDirectory() as folder:
            out = Path(folder)
            perf = {"status": "OBSERVED", "rendered_frames_during_robot": 200,
                    "janky_frames_during_robot": 12,
                    "janky_percent_during_robot": 6.0,
                    "caveat": "Aggregate, not per-route animation"}
            robot.report(out, [], [], performance=perf)
            results = json.loads((out / "results.json").read_text())
            self.assertEqual(results["performance"]["janky_percent_during_robot"], 6.0)
            self.assertIn("6.0%", (out / "report.html").read_text())
            review.make_report(out, [], performance=perf)
            self.assertIn("OBSERVED", (out / "ux-review.html").read_text())
            components = json.loads((out / "components.json").read_text())
            self.assertEqual(components["performance"], perf)

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
                duplicate_count=7, layout_revisits=9,
                display_height=2424, display_width=1080,
                installed_app={"versionName": "0.3.3-dev", "versionCode": "3",
                               "source_matches_installed_apk": "UNVERIFIED"})
            text = (p / "ux-review.html").read_text()
            self.assertIn("&lt;button&gt;", text)
            self.assertNotIn("<button>", text)
            self.assertIn("7 doublons", text)
            self.assertIn("9 revisites", text)
            self.assertIn("UNAVAILABLE", text)
            self.assertIn("target-overlay", text)
            self.assertIn("Écran avant le clic", text)
            self.assertEqual(result[0]["source"]["status"], "UNAVAILABLE")
            contents = json.loads((p / "components.json").read_text())
            self.assertEqual(contents["duplicate_screens_avoided"], 7)
            self.assertEqual(contents["layout_revisits"], 9)
            self.assertEqual(contents["source_revision"], review.SOURCE_REV)
            self.assertIn(review.SOURCE_REV, text)
            self.assertIn("0.3.3-dev", text)
            self.assertIn("NON VÉRIFIÉE", text)
            self.assertEqual(contents["installed_app"]["versionCode"], "3")

if __name__ == "__main__":
    unittest.main()
