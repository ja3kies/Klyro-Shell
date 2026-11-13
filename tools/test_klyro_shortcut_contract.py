#!/usr/bin/env python3
"""Source wiring contracts for Clear and Select/Paste.

These guard against the exact regression observed on the Pixel. They do not
claim to replace instrumented Android UI tests.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PANE = (ROOT / "app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
CLEAR = (ROOT / "app/src/main/java/com/oryn/terminal/ui/TerminalDisplayActions.java").read_text()
POLICY = (ROOT / "app/src/main/java/com/oryn/terminal/ui/TerminalActionPolicy.java").read_text()


class ShortcutRegressionContracts(unittest.TestCase):
    def test_clear_accepts_partial_prompt_without_writing_to_shell(self):
        self.assertIn("emulator.getScreen().clearTranscript()", CLEAR)
        self.assertIn("!session.isRunning()", CLEAR)
        self.assertIn("ClearResult.SCROLLBACK_ONLY", CLEAR)
        self.assertNotIn("emulator.append(", CLEAR)
        self.assertNotIn("session.write(", CLEAR)
        self.assertNotIn("session.writeCodePoint(", CLEAR)

    def test_clear_refreshes_local_transcript_action_mode(self):
        self.assertIn("terminalActionRevision++", PANE)
        self.assertIn("remember(session, outputRevision, terminalActionRevision)", PANE)
        self.assertIn("TerminalDisplayActions.clearSafely(session)", PANE)

    def test_clear_warns_when_only_history_was_removed(self):
        self.assertIn("Scrollback cleared; current program and input preserved", PANE)
        self.assertNotIn("only at an idle Klyro prompt", PANE)

    def test_paste_available_when_selection_mode_appears(self):
        self.assertIn("onLongClick = {", PANE)
        self.assertIn("TerminalActionPolicy.shouldSelect(canSelect, true)", PANE)
        self.assertIn("TerminalActionPolicy.shouldSelect(canSelect, false)", PANE)
        self.assertIn("longClickDescription = \"Paste clipboard\"", PANE)
        self.assertIn("!longPress", POLICY)

    def test_clipboard_read_is_not_based_on_stale_cached_state(self):
        self.assertIn("TerminalActionPolicy.canUseSmartKey(canSelect, canSendKeys)", PANE)
        self.assertIn("ClipboardTextReader.readPlainText(clipboardManager)", PANE)

    def test_more_menu_always_offers_explicit_paste(self):
        self.assertIn('KlyroMenuItem("Paste clipboard"', PANE)
        self.assertIn("pasteClipboard()", PANE)

    def test_copy_selection_works_even_before_native_view_attaches(self):
        self.assertIn("view?.text?.toString() ?: visibleOutput", PANE)


if __name__ == "__main__":
    unittest.main()
