"""Users should understand why New is disabled when workspaces are full."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroWorkspaceSheet.kt").read_text()
class WorkspaceLimitMessage(unittest.TestCase):
 def test_limit_message_and_disabled_control_agree(self):
  self.assertIn('enabled = entries.size < KlyroWorkspaceCatalog.MAX_PROJECTS',S)
  self.assertIn('if (entries.size >= KlyroWorkspaceCatalog.MAX_PROJECTS)',S)
  self.assertIn('Workspace limit reached. Remove an unused workspace',S)
  self.assertIn('color = c.muted',S)
if __name__=="__main__":unittest.main()
