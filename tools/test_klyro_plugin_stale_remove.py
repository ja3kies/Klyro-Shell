"""A stale saved removal ID is never treated as a valid plugin."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class PluginStaleRemovalTest(unittest.TestCase):
 def test_missing_item_has_safe_dialog_and_no_uninstall(self):
  d=S.split("removeId?.let { id ->",1)[1]
  self.assertIn("Plugin unavailable",d)
  self.assertIn("enabled = item != null && !mutationBusy",d)
  self.assertIn("if (item == null) return@TextButton",d)
  self.assertIn("This plugin is no longer installed",d)
  self.assertIn("onDismissRequest = {",d)
if __name__=="__main__":unittest.main()
