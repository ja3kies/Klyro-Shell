"""Adversarial source contracts for contextual terminal SELECT/PASTE.
These checks complement JVM policy tests; Pixel device QA is still required.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/oryn/terminal/ui"

class SelectPasteAdversarialAudit(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pane = (UI / "TerminalPane.kt").read_text()
        cls.reader = (UI / "ClipboardTextReader.java").read_text()
        cls.clip = (UI / "ClipboardPastePolicy.java").read_text()
        cls.policy = (UI / "TerminalActionPolicy.java").read_text()

    def test_select_paste_matches_displayed_mode(self):
        self.assertIn("fun selectTerminalOutput(expectedSelection: Boolean)", self.pane)
        self.assertIn("selectTerminalOutput(canSelect)", self.pane)
        self.assertIn('iconCaption = if (canSelect) "SELECT" else "PASTE"', self.pane)

    def test_native_select_context_menu_never_pastes(self):
        self.assertIn('menu.add("Select all")', self.pane)
        self.assertIn("selectTerminalOutput(expectedSelection = true)", self.pane)

    def test_lifecycle_observer_uses_supported_activity_owner(self):
        self.assertIn("context as? LifecycleOwner", self.pane)
        self.assertIn("Lifecycle.Event.ON_RESUME", self.pane)

    def test_select_mode_never_silently_switches_to_paste(self):
        part = self.pane.split("fun selectTerminalOutput(expectedSelection: Boolean)", 1)[1].split("// Fit seven", 1)[0]
        self.assertIn("if (!expectedSelection)", part)
        self.assertIn("Terminal output changed; nothing to select", part)
        self.assertIn("TerminalActionPolicy.hasCopyableOutput(output)", part)

    def test_selection_snapshot_is_bounded(self):
        self.assertIn("TerminalActionPolicy.selectionTail(output)", self.pane)
        self.assertIn("MAX_SELECTION_UTF16_UNITS = 2 * 1024 * 1024", self.policy)
        self.assertIn("Character.isLowSurrogate(text.charAt(start))", self.policy)

    def test_finished_session_can_still_be_copied(self):
        self.assertIn("enabled = canSelect || (session.isRunning() && session.emulator != null && clipboardAvailable)", self.pane)

    def test_finished_session_cannot_receive_direct_paste(self):
        self.assertIn("Terminal unavailable; start or reopen a session to paste", self.pane)

    def test_finished_session_cannot_receive_native_paste(self):
        self.assertIn("Terminal unavailable; paste cancelled", self.pane)
        self.assertIn("onNativePasteConsumed()", self.pane)

    def test_finished_session_cannot_receive_confirmed_multiline_paste(self):
        self.assertIn('if (!session.isRunning() || emulator == null) {\n                        Toast.makeText(context, "Terminal unavailable; paste cancelled"', self.pane)

    def test_failed_clipboard_reads_do_not_crash_terminal(self):
        self.assertIn("catch (SecurityException | IllegalStateException denied)", self.reader)

    def test_clipboard_availability_refreshes_on_resume(self):
        self.assertIn("Lifecycle.Event.ON_RESUME", self.pane)
        self.assertIn("clipboardAvailable = ClipboardTextReader.hasPlainText(clipboardManager)", self.pane)

    def test_clipboard_listener_unregisters(self):
        self.assertIn("removePrimaryClipChangedListener(listener)", self.pane)
        self.assertIn("removeObserver(observer)", self.pane)

    def test_clipboard_rejects_uris_and_intents(self):
        self.assertIn("item.getIntent() != null || item.getUri() != null", self.reader)
        self.assertIn("clip.getItemCount() != 1", self.reader)

    def test_copy_handles_unavailable_clipboard(self):
        self.assertIn('as? ClipboardManager ?: error("Clipboard unavailable")', self.pane)
        self.assertIn("if (copied) closeSelection()", self.pane)

    def test_copy_handles_reversed_selection(self):
        self.assertIn("val left = minOf(start, end)", self.pane)
        self.assertIn("val right = maxOf(start, end)", self.pane)

    def test_copy_feedback_explains_copy_all(self):
        self.assertIn("All visible output copied", self.pane)

    def test_multiline_paste_still_requires_confirmation(self):
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(content)", self.pane)
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(incoming)", self.pane)

    def test_no_invisible_control_text_can_be_pasted(self):
        self.assertIn("Character.getType(c) == Character.FORMAT", self.clip)
        self.assertIn("MAX_UTF8_BYTES", self.clip)
        self.assertIn("Character.isHighSurrogate(c)", self.clip)

    def test_shortcut_colors_are_differentiated_without_removing_context(self):
        self.assertIn(
            "tone = if (canSelect) KlyroShortcutTone.SELECT else KlyroShortcutTone.PASTE",
            self.pane
        )
        self.assertIn("KlyroShortcutTonePolicy.resolve(colors, tone, keyState)", self.pane)
        self.assertNotIn("pasteTone", self.pane)
        self.assertNotIn("colors.warningContainer else colors.key", self.pane)

if __name__ == "__main__":
    unittest.main()
