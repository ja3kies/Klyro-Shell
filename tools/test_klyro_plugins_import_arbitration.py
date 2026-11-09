"""SAF importer must not overlap another preview or registry transaction."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class PluginImportArbitrationTest(unittest.TestCase):
 def test_document_result_cannot_replace_pending_mutation(self):
  self.assertIn("if (uri != null && !previewBusy && !mutationBusy)",S)
  self.assertIn("enabled = !previewBusy && !mutationBusy &&\n                            pendingImportUri == null",S)
  self.assertIn("LaunchedEffect(pendingImportUri)",S)
  self.assertIn("withContext(Dispatchers.IO)",S)
  self.assertIn("pendingImportUri = uri.toString()",S)
if __name__=="__main__":unittest.main()
