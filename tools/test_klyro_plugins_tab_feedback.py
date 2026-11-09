"""Errors from one plugin tab must not masquerade as failures on the other."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class PluginTabFeedbackIsolation(unittest.TestCase):
 def test_switching_sections_resets_stale_error_but_not_installed_data(self):
  block=S.split('onClickLabel = "Show $label"',1)[1].split('.semantics { this.selected = selected }',1)[0]
  self.assertIn('page = id',block)
  self.assertIn('error = null',block)
  self.assertNotIn('installed =',block)
  self.assertIn('enabled = !mutationBusy',S)
if __name__=="__main__":unittest.main()
