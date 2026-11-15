"""Registry writes are serialized off the main Compose thread."""
from pathlib import Path
import unittest
SRC = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class PluginPersistenceTest(unittest.TestCase):
    def test_mutations_run_in_io_and_disable_competing_controls(self):
        f = SRC.split("fun applyChange",1)[1].split("\n    val importPicker",1)[0]
        self.assertIn("if (mutationBusy) return",f)
        self.assertIn("withContext(Dispatchers.IO + NonCancellable)",f)
        self.assertIn("manager.installed()",f)
        self.assertIn("mutationBusy = false",f)
        self.assertIn("enabled = !mutationBusy",SRC)
if __name__ == "__main__":
    unittest.main()
