"""Plugin operations cannot be silently dismissed before registry writes settle."""
from pathlib import Path
import unittest
FILE = Path(__file__).resolve().parents[1] / (
    "app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt")
CODE = FILE.read_text(encoding="utf-8")

class PluginOperationUXContract(unittest.TestCase):
    def test_back_and_home_are_blocked_while_writing(self):
        self.assertIn("fun navigateBack()", CODE)
        self.assertIn("if (mutationBusy) return", CODE)
        self.assertIn("BackHandler { navigateBack() }", CODE)
        self.assertIn("onClick = ::navigateBack,\n                enabled = !mutationBusy,", CODE)
        self.assertIn("enabled = !mutationBusy, role = Role.Tab,", CODE)

    def test_every_confirmation_cannot_dismiss_during_mutation(self):
        self.assertIn("if (!mutationBusy) { pendingBundledId = null; error = null }", CODE)
        self.assertIn("if (!mutationBusy) { removeId = null; error = null }", CODE)
        self.assertIn("if (!mutationBusy) {\n                    pendingImport = null;", CODE)
        self.assertGreaterEqual(CODE.count("enabled = !mutationBusy,"), 5)
        self.assertIn("manager.installPreview(record)", CODE)
        self.assertIn("manager.installBundled(id)", CODE)

    def test_live_feedback_is_neutral_and_accessible(self):
        self.assertIn('Text("Updating local plugins…"', CODE)
        self.assertIn("liveRegion = LiveRegionMode.Polite", CODE)
        self.assertIn("color = colors.key,", CODE)
        self.assertIn("withContext(Dispatchers.IO)", CODE)

if __name__ == "__main__":
    unittest.main()
