"""Avoid false-positive copy feedback when Android's clipboard is unavailable."""
from pathlib import Path
import unittest

MAIN = (Path(__file__).resolve().parents[1] /
        "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()

class HomeClipboardFeedbackTest(unittest.TestCase):
    def test_success_only_after_clipboard_write(self):
        section = MAIN.split("onCopyTechnicalDetails = {", 1)[1].split("bootstrapInstalled =", 1)[0]
        self.assertIn("clipboard.setPrimaryClip", section)
        self.assertIn("}.getOrDefault(false)", section)
        self.assertIn('if (copied) "Technical details copied" else "Clipboard unavailable"', section)
        self.assertNotIn('Toast.makeText(this, "Technical details copied"', section)

if __name__ == "__main__":
    unittest.main()
