"""Imported documents must not block Android's main thread."""
from pathlib import Path
import unittest
FILE = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class PluginSlowProviderContract(unittest.TestCase):
    def test_preview_runs_on_io_dispatcher_and_shows_busy(self):
        fragment = FILE.split("val importPicker =",1)[1].split("\n\n    Column(",1)[0]
        self.assertIn("withContext(Dispatchers.IO)",fragment)
        self.assertIn("manager.previewManifest(Uri.parse(selectedUri))",fragment)
        self.assertIn("previewBusy = false",fragment)
        self.assertIn('Text(if (previewBusy) "Reading manifest…" else "Import JSON")',FILE)
if __name__ == "__main__":
    unittest.main()
