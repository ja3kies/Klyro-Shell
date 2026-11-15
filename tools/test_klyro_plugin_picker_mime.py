"""Only plausible JSON SAF document types should be offered to the user."""
from pathlib import Path
import unittest
SRC=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroPluginsPage.kt").read_text()
class PluginPickerUX(unittest.TestCase):
 def test_open_document_filters_manifest_types(self):
  self.assertIn('ActivityResultContracts.OpenDocument()',SRC)
  for mime in ('"application/json"','"text/plain"','"application/octet-stream"'):
   self.assertIn(mime,SRC)
  self.assertNotIn('importPicker.launch(arrayOf("*/*"))',SRC)
 def test_preview_and_install_remain_explicit(self):
  self.assertIn('manager.previewManifest(Uri.parse(selectedUri))',SRC)
  self.assertIn('pendingImport?.let { record ->',SRC)
  self.assertIn('manager.installPreview(record)',SRC)
  self.assertIn('confirmedImportId = record.id',SRC)
if __name__=="__main__":unittest.main()
