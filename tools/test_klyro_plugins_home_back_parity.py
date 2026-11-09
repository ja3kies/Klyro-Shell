"""The visible Home action must not bypass a pending plugin confirmation."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class PluginBackParityTest(unittest.TestCase):
 def test_home_and_system_back_share_navigation(self):
  self.assertIn("fun navigateBack()",S)
  self.assertIn("BackHandler { navigateBack() }",S)
  self.assertIn("onClick = ::navigateBack",S)
  fn=S.split("fun navigateBack()",1)[1].split("fun applyChange",1)[0]
  self.assertIn("if (mutationBusy) return",fn)
  self.assertIn("pendingImport != null || pendingImportUri != null",fn)
  self.assertIn("pendingBundledId != null",fn)
  self.assertIn("removeId != null",fn)
if __name__=="__main__":unittest.main()
