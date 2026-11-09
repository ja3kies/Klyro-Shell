"""Rotation must keep review intent, not run installation from a lifecycle effect."""
from pathlib import Path
import unittest
SRC = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class ImportedPreviewRestorationTest(unittest.TestCase):
    def test_manifest_uri_is_saved_and_reparsed_in_io(self):
        self.assertIn("var pendingImportUri by rememberSaveable", SRC)
        effect = SRC.split("LaunchedEffect(pendingImportUri)",1)[1].split("\n\n    Column(",1)[0]
        self.assertIn("withContext(Dispatchers.IO)",effect)
        self.assertIn("manager.previewManifest(Uri.parse(selectedUri))",effect)
        self.assertNotIn("installPreview",effect)
        self.assertIn("pendingImportUri = null",SRC)
if __name__ == "__main__":
    unittest.main()
