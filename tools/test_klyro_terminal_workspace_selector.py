"""The active workspace in terminal chrome is an accessible entry point."""
from pathlib import Path
import unittest

SOURCE = (Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()


class TerminalWorkspaceSelectorTest(unittest.TestCase):
    def test_header_selector_is_a_labeled_button_with_touch_target(self):
        header = SOURCE.split('if (!compact) {', 1)[1].split('// The new-session action', 1)[0]
        self.assertIn('onClickLabel = "Choose workspace"', header)
        self.assertIn('role = Role.Button', header)
        self.assertIn('.heightIn(min = KlyroUiMetrics.minTouch)', header)
        self.assertIn('.semantics(mergeDescendants = true)', header)

    def test_selector_opens_the_existing_workspace_sheet(self):
        header = SOURCE.split('if (!compact) {', 1)[1].split('// The new-session action', 1)[0]
        self.assertIn('contentDescription = "Workspace: $currentProject"', header)
        self.assertIn('onClick = { showWorkspaceSheet = true }', header)
        self.assertIn('if (showWorkspaceSheet) {', SOURCE)


if __name__ == "__main__":
    unittest.main()
