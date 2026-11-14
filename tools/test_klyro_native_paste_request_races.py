"""Cross-layer UX regression gates for native clipboard request ownership.

The source gates supplement the executable JUnit identity checks. Physical
Android navigation/IME timing remains a separate device test.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/oryn/terminal"
VIEWMODEL = (APP / "TerminalSessionViewModel.kt").read_text()
ACTIVITY = (APP / "MainActivity.kt").read_text()
PANE = (APP / "ui/TerminalPane.kt").read_text()
CLIENT = (APP / "session/OrynTerminalSessionClient.java").read_text()

class NativePasteRaceContracts(unittest.TestCase):
    def test_request_has_unique_identity_for_duplicate_payloads(self):
        self.assertIn("val requestId: Long", VIEWMODEL)
        self.assertIn("nextNativePasteRequestId++", VIEWMODEL)
        self.assertIn("NativePasteRequest(tabId, text, nextNativePasteRequestId++)", VIEWMODEL)

    def test_only_the_exact_request_can_be_consumed(self):
        self.assertIn("fun clearNativePaste(request: NativePasteRequest)", VIEWMODEL)
        self.assertIn("pendingNativePaste === request", VIEWMODEL)
        self.assertNotIn("fun clearNativePaste(tabId: Long)", VIEWMODEL)

    def test_main_activity_transfers_whole_request_not_text(self):
        self.assertIn("nativePasteRequest = terminalState.pendingNativePaste", ACTIVITY)
        self.assertIn("onNativePasteConsumed = terminalState::clearNativePaste", ACTIVITY)
        self.assertNotIn("?.takeIf { it.tabId == terminalState.activeTabId }?.text", ACTIVITY)

    def test_pane_keeps_origin_tab_and_consumes_exact_request(self):
        self.assertIn("nativePasteRequest: TerminalSessionViewModel.NativePasteRequest?", PANE)
        self.assertIn("onNativePasteConsumed: (TerminalSessionViewModel.NativePasteRequest) -> Unit", PANE)
        self.assertIn("val incoming = request.text", PANE)
        self.assertIn("request.tabId != activeTabId", PANE)
        self.assertIn("request.tabId != currentSessionId()", PANE)
        self.assertIn("onNativePasteConsumed(request)", PANE)
        self.assertIn("TerminalPasteReview(request.tabId, incoming)", PANE)
        self.assertNotIn("onNativePasteConsumed()", PANE)

    def test_native_view_is_revalidated_when_handler_delivers(self):
        self.assertIn("private boolean isNativePasteTargetCurrent(", CLIENT)
        self.assertIn("terminalView == source", CLIENT)
        self.assertIn("source.isAttachedToWindow()", CLIENT)
        self.assertIn("source.getCurrentSession() == session", CLIENT)
        self.assertIn("isNativePasteTargetCurrent(session, source)", CLIENT)
        block = CLIENT.split("void onPasteTextFromClipboard(", 1)[1].split("@Override", 1)[0]
        self.assertGreaterEqual(block.count("isNativePasteTargetCurrent(session, source)"), 2)

    def test_native_single_line_reports_failed_enqueue_and_retains_bracket_mode(self):
        block = CLIENT.split("void onPasteTextFromClipboard(", 1)[1].split("@Override", 1)[0]
        self.assertIn("pasteWithResult(pasted)", block)
        self.assertNotIn(".paste(pasted)", block)
        self.assertIn('Paste not sent', block)
        self.assertIn("session.isRunning()", block)

if __name__ == "__main__":
    unittest.main()
