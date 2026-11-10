"""Ensure every declared UI click maps to source that exists on this branch."""
from pathlib import Path
import unittest
import klyro_ux_report as report

ROOT = Path(__file__).resolve().parents[1]

class KotlinProvenanceContract(unittest.TestCase):
    def test_click_anchors_exist_in_real_kotlin_files(self):
        for label in report.SOURCE_ANCHORS:
            with self.subTest(action=label):
                source = report.source_for(label, None, ROOT)
                self.assertEqual(source["status"], "FOUND", source)
                self.assertIn("code", source)
                self.assertIn("#L", source["url"])

    def test_settings_entry_and_back_use_separate_real_handlers(self):
        entry = report.source_for("Navigate: Settings", "Home", ROOT)
        back = report.source_for("Navigate: Settings", "Appearance", ROOT)
        self.assertEqual(entry["status"], "FOUND")
        self.assertEqual(back["status"], "FOUND")
        self.assertNotEqual(entry["path"], back["path"])

    def test_bento_and_action_row_implementations_are_present(self):
        for route in ("Navigate: Appearance", "Navigate: Diagnostics"):
            sources = report.related_code_for(route, ROOT)
            self.assertTrue(sources)
            for source in sources:
                with self.subTest(route=route, name=source["name"]):
                    self.assertEqual(source["status"], "FOUND", source)

    def test_actual_style_tokens_extracted_from_code(self):
        source = report.source_for("Navigate: Plugins", "Home", ROOT)
        tokens = report.style_tokens(source.get("code"))
        self.assertTrue(any(x.startswith("ui.") or x.startswith("KlyroUiMetrics.")
                            for x in tokens), tokens)

if __name__ == "__main__":
    unittest.main()
