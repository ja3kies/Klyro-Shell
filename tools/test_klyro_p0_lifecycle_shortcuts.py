"""Adversarial P0 gates for terminal service ownership and keyboard actions."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/oryn/terminal"


class P0LifecycleAndShortcuts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.service = (APP / "KlyroSessionService.kt").read_text()
        cls.runtime = (APP / "KlyroSessionRuntime.kt").read_text()
        cls.pane = (APP / "ui/TerminalPane.kt").read_text()
        cls.viewmodel = (APP / "TerminalSessionViewModel.kt").read_text()
        cls.activity = (APP / "MainActivity.kt").read_text()
        cls.clipboard_reader = (APP / "ui/ClipboardTextReader.java").read_text()
        cls.clipboard_policy = (APP / "ui/ClipboardPastePolicy.java").read_text()

    def test_service_destruction_does_not_kill_process_owned_ptys(self):
        body = self.service.split("override fun onDestroy()", 1)[1].split("override fun onBind", 1)[0]
        self.assertNotIn("closeAllSessions()", body)
        self.assertIn("handler.removeCallbacks(check)", body)

    def test_foreground_notification_reopens_terminal(self):
        self.assertIn("PendingIntent.getActivity(", self.service)
        self.assertIn("MainActivity::class.java", self.service)
        self.assertIn(".setContentIntent(openApp)", self.service)

    def test_notification_reports_actual_session_count(self):
        self.assertIn("KlyroSessionRuntime.sessions.sessions.size", self.service)
        self.assertIn('if (sessionCount == 1) "" else "s"', self.service)

    def test_foreground_notification_refresh_does_not_realert(self):
        self.assertIn(".setOnlyAlertOnce(true)", self.service)
        self.assertIn(".setCategory(NotificationCompat.CATEGORY_SERVICE)", self.service)

    def test_stop_without_sessions_uses_start_id(self):
        self.assertIn("stopSelf(startId)", self.service)

    def test_notification_uses_immutable_pending_intent(self):
        self.assertIn("PendingIntent.FLAG_IMMUTABLE", self.service)

    def test_inactivity_never_terminates_a_user_started_job(self):
        for source in (self.runtime, self.service, self.activity):
            self.assertNotIn("expireIfIdle", source)
        self.assertNotIn("closeAllSessions()", self.runtime)
        self.assertIn("val sessions = TerminalSessionViewModel()", self.runtime)

    def test_service_never_restarts_after_process_death(self):
        self.assertIn("START_NOT_STICKY", self.service)
        self.assertNotIn("START_STICKY", self.service)

    def test_service_stops_only_when_sessions_absent(self):
        self.assertIn("if (KlyroSessionRuntime.sessions.hasLiveSessions())", self.service)
        self.assertIn("else stopSelf()", self.service)

    def test_foreground_notification_is_silent(self):
        self.assertIn(".setOngoing(true)", self.service)
        self.assertIn(".setSilent(true)", self.service)

    def test_all_action_selects_only_when_output_is_copyable(self):
        self.assertIn("TerminalActionPolicy.hasCopyableOutput(output)", self.pane)
        self.assertIn("selectedOutput = TerminalActionPolicy.selectionTail(output)", self.pane)

    def test_all_action_falls_back_to_clipboard(self):
        self.assertIn("fun selectTerminalOutput(expectedSelection: Boolean)", self.pane)
        self.assertIn("pasteClipboard()", self.pane)

    def test_all_paste_icon_matches_paste_action(self):
        self.assertIn("else R.drawable.hugeicons_clipboard_paste", self.pane)
        self.assertIn('else "Paste clipboard"', self.pane)

    def test_all_select_icon_matches_select_action(self):
        self.assertIn("R.drawable.hugeicons_clipboard_list", self.pane)
        self.assertIn('"Select terminal output; hold to paste clipboard"', self.pane)

    def test_all_disabled_only_if_both_actions_unavailable(self):
        self.assertIn("TerminalActionPolicy.canUseSmartKey(canSelect, canSendKeys)", self.pane)

    def test_all_reports_active_selection(self):
        self.assertIn("active = selectedOutput != null", self.pane)

    def test_end_has_direct_touch_target(self):
        self.assertIn('TerminalExtraKey("END", description = "Go to end of line"', self.pane)

    def test_end_uses_shared_navigation_policy(self):
        self.assertIn("TerminalNavigationPolicy.horizontalLongPress(true,", self.pane)

    def test_end_respects_alternate_buffer(self):
        self.assertIn("session.emulator?.isAlternateBufferActive == true", self.pane)

    def test_arrow_long_press_retained(self):
        self.assertIn('longClickDescription = "Go to end of line"', self.pane)

    def test_paste_confirmation_is_retained(self):
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(content)", self.pane)

    def test_native_paste_confirmation_is_retained(self):
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(incoming)", self.pane)

    def test_clipboard_listener_is_disposed(self):
        self.assertIn("removePrimaryClipChangedListener(listener)", self.pane)

    def test_no_redundant_paste_key(self):
        self.assertNotIn('TerminalExtraKey(\n                "PASTE"', self.pane)

    def test_shortcut_has_four_visual_states(self):
        for state in ("ACTIVE", "HOLD", "INACTIVE", "DEFAULT"):
            self.assertIn("ShortcutState." + state, self.pane)

    def test_all_uses_realtime_clipboard_availability(self):
        self.assertIn("OnPrimaryClipChangedListener", self.pane)

    def test_end_remains_available_in_compact_row(self):
        self.assertIn("val secondRow: @Composable () -> Unit", self.pane)
        self.assertIn("if (compact)", self.pane)

    def test_native_paste_does_not_auto_run_unvalidated_multiline(self):
        self.assertIn("pendingMultilinePaste = incoming", self.pane)

    def test_back_handler_preserves_unsaved_dialog_flow(self):
        self.assertIn("pendingMultilinePaste == null && selectedOutput == null", self.pane)

    def test_active_output_updates_selectable_action(self):
        self.assertIn("outputRevision = terminalState.outputRevision(terminalState.activeTabId)", self.activity)
        self.assertIn("remember(session, outputRevision, terminalActionRevision)", self.pane)
        self.assertIn("queueOutputUpdate(tabId)", self.viewmodel)
        self.assertIn("outputRevisions[id] = (outputRevisions[id] ?: 0L) + 1L", self.viewmodel)
        self.assertIn("postDelayed(task, 120L)", self.viewmodel)

    def test_output_subscription_is_released_on_close(self):
        self.assertIn("outputRevisions.remove(id)", self.viewmodel)

    def test_clipboard_availability_uses_actual_validated_text(self):
        self.assertIn("return readPlainText(manager) != null;", self.clipboard_reader)

    def test_blank_clipboard_cannot_claim_paste_available(self):
        self.assertIn("isWhitespaceOnly(value)", self.clipboard_policy)

    def test_unicode_noncharacters_cannot_reach_shell(self):
        self.assertIn("Character.toCodePoint(c, value.charAt(i))", self.clipboard_policy)
        self.assertIn("(cp & 0xFFFE) == 0xFFFE", self.clipboard_policy)

    def test_multiline_paste_preview_not_truncated_at_500_chars(self):
        self.assertIn("pendingMultilinePaste.orEmpty(),", self.pane)
        self.assertNotIn("pendingMultilinePaste.orEmpty().take(500)", self.pane)

    def test_transcript_preview_does_not_split_surrogate_pairs(self):
        self.assertIn("Character.isLowSurrogate(text.charAt(start))", (APP / "ui/TerminalActionPolicy.java").read_text())
        self.assertIn("Character.isHighSurrogate(text.charAt(start - 1))", (APP / "ui/TerminalActionPolicy.java").read_text())

    def test_terminal_header_prioritizes_centered_workspace_without_duplicate_logo(self):
        self.assertIn("modifier = Modifier.align(Alignment.Center).widthIn(max = 136.dp)", self.pane)
        self.assertIn('"TERMINAL"', self.pane)
        self.assertNotIn("KlyroBrandChoice.selected(context)", self.pane)
        self.assertNotIn("R.drawable.ic_klyro_brand_b", self.pane)

    def test_service_start_is_guarded_by_live_sessions(self):
        self.assertIn("if (!KlyroSessionRuntime.sessions.hasLiveSessions()) return", self.service)


if __name__ == "__main__":
    unittest.main()
