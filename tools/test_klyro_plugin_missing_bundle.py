"""Stale bundle IDs should not leave an invisible blocking confirmation state."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class MissingBundledPackRecovery(unittest.TestCase):
 def test_stale_bundle_provides_dismissible_fallback(self):
  block=S.split('pendingBundledId?.let { id ->',1)[1].split('pendingImport?.let { record ->',1)[0]
  self.assertIn('if (record != null)',block)
  self.assertIn('} else {\n            AlertDialog(',block)
  self.assertIn('title = { Text("Pack unavailable") }',block)
  self.assertIn('pendingBundledId = null; error = null',block)
  self.assertIn('manager.installBundled(id)',block)
if __name__=="__main__":unittest.main()
