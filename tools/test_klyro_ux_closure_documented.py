"""The UX closure checklist covers every surface without erasing previous acceptance."""
from pathlib import Path
import unittest
BASE=Path(__file__).resolve().parents[1]
DOC=(BASE/"docs/UX_CLOSURE_REVIEW_20261110.md").read_text()
class ClosureCoverage(unittest.TestCase):
 def test_routes_and_components_are_accounted_for(self):
  for subject in ("Home","Settings","Design components","Plugins installed",
   "Plugins explore","Command palette","Workspaces","Sessions",
   "Terminal tabs","Terminal FIND","Job activity"):
   self.assertIn(subject,DOC,subject)
 def test_no_manual_test_before_consolidated_build(self):
  self.assertIn("No repeated device QA during UX implementation",DOC)
  self.assertIn("device-accepted",DOC)
  self.assertIn("automated-green",DOC)
 def test_rollback_is_explicit(self):
  self.assertIn("d2ac1a1ad330e8f450da733690c820a4a757f9be",DOC)
  self.assertIn("isolated from `main`",DOC)
if __name__=="__main__":unittest.main()
