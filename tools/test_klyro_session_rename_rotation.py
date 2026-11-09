"""Session rename text and selection survive rotation, without auto-saving."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroSessionSheets.kt").read_text()
class SessionRenameTest(unittest.TestCase):
    def test_draft_is_saveable_text_field_value(self):
        self.assertIn("rememberSaveable(name, stateSaver = TextFieldValue.Saver)",S)
        self.assertIn("TextFieldValue(name, selection = TextRange(0, name.length))",S)
        self.assertIn("cleaned.length <= 32",S)
        self.assertIn("enabled = value.text.trim().isNotBlank()",S)
    def test_no_implicit_write(self):
        self.assertIn("onClick = { onSave(value.text.trim()) }",S)
if __name__=="__main__": unittest.main()
