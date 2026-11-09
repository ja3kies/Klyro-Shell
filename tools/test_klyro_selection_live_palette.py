"""Live selection dialog tracks adaptive themes without resetting text."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
class LiveSelectionPaletteTest(unittest.TestCase):
    def test_selection_recolors_existing_android_view(self):
        part=S.split('title = { Text("Select terminal output") }',1)[1].split('confirmButton = {',1)[0]
        self.assertIn("update = { view ->",part)
        self.assertIn("view.setTextColor(ui.terminalInk.toArgb())",part)
        self.assertIn("view.setBackgroundColor(ui.terminal.toArgb())",part)
        self.assertNotIn("view.setText(",part)
        self.assertIn("keyListener = null",part)
if __name__=="__main__": unittest.main()
