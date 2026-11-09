"""Confirmation BackHandler must not reference undeclared Compose local state."""
from pathlib import Path
import unittest
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroCommandPalette.kt").read_text()
class PaletteInitializationOrdering(unittest.TestCase):
 def test_pending_command_declaration_precedes_back_handler(self):
  self.assertLess(S.index('var pendingCautionCommand by rememberSaveable'),
    S.index('BackHandler(enabled = pendingCautionCommand != null)'))
if __name__=="__main__":unittest.main()
