"""Pending IDs survive Activity recreation without installing anything."""
from pathlib import Path
import unittest
PAGE = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class PluginConfirmationRotationTest(unittest.TestCase):
    def test_pending_ids_saveable_and_confirmed_only_by_click(self):
        self.assertIn("var removeId by rememberSaveable", PAGE)
        self.assertIn("var pendingBundledId by rememberSaveable", PAGE)
        self.assertIn("onClick = {\n                            applyChange({ manager.installBundled(id) }) {", PAGE)
        self.assertIn("applyChange({ manager.uninstall(id) }) {", PAGE)
if __name__ == "__main__":
    unittest.main()
