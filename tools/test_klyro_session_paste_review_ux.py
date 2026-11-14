"""UX regression contracts for session-bound and honest multiline paste review."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
PANE = (ROOT / "app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()

class PasteReviewUxContracts(unittest.TestCase):
    def test_pending_review_has_original_tab_identity(self):
        self.assertIn("var pendingMultilinePaste by remember(session) { mutableStateOf<TerminalPasteReview?>(null) }", PANE)
        self.assertIn("TerminalPasteReview(tabId, content)", PANE)
        self.assertIn("TerminalPasteReview(tabId, incoming)", PANE)

    def test_confirm_checks_tab_binding_and_reports_failure(self):
        self.assertIn("sendIfCurrent(activeTabId, currentSessionId()", PANE)
        self.assertIn("Paste not sent", PANE)

    def test_large_preview_discloses_omitted_text(self):
        self.assertIn("review.preview", PANE)
        self.assertIn("review.isPreviewTruncated", PANE)
        self.assertIn("Only the first", PANE)
        self.assertIn("review.lineCount", PANE)
        self.assertIn("review.utf8ByteCount", PANE)

if __name__ == "__main__":
    unittest.main()
