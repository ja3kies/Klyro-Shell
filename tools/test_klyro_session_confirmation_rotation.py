"""Rotation does not skip an already-requested destructive confirmation."""
import unittest
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/"app/src/main/java/com/oryn/terminal/ui/KlyroSessionSheets.kt").read_text()
class SessionConfirmTest(unittest.TestCase):
    def test_close_confirm_survives_rotation_not_autoexecuted(self):
        self.assertIn("var confirmClose by rememberSaveable(sessionName)",S)
        self.assertIn("if (confirmClose) {\n            AlertDialog(",S)
        self.assertIn("onCloseSession()",S.split("confirmButton = {",1)[1])
        self.assertIn("onDismissRequest = { if (!confirmClose) onDismiss() }",S)
if __name__=="__main__": unittest.main()
