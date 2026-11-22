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

    def test_native_pty_refresh_is_bounded_to_a_frame(self):
        client = (APP / "session/OrynTerminalSessionClient.java").read_text()
        refresh = client.split("private void scheduleOutputRefresh()", 1)[1].split(
            "/** Drop stale UI work", 1)[0]
        self.assertIn("uiHandler.postDelayed(() -> {", refresh)
        self.assertIn("}, 32L);", refresh)

    def test_service_destruction_does_not_kill_process_owned_ptys(self):
        body = self.service.split("override fun onDestroy()", 1)[1].split("override fun onBind", 1)[0]
        self.assertNotIn("closeAllSessions()", body)
        self.assertIn("handler.removeCallbacks(check)", body)

    def test_foreground_notification_reopens_terminal(self):
        self.assertIn("PendingIntent.getActivity(", self.service)
        self.assertIn("MainActivity::class.java", self.service)
        self.assertIn(".setContentIntent(openApp)", self.service)

    def test_notification_reports_actual_session_count(self):
        self.assertIn("KlyroSessionRuntime.sessions.runningPtyCount()", self.service)
        self.assertIn('if (sessionCount == 1) "" else "s"', self.service)

    def test_foreground_notification_refresh_does_not_realert(self):
        refresh = self.service.split("fun refreshSessionNotification", 1)[1].split("fun ensureRunning", 1)[0]
        self.assertIn("FreezeRecorder.serviceActive", refresh)
        self.assertIn(".notify(", refresh)
        self.assertNotIn("startForegroundService", refresh)
        self.assertNotIn("startForeground(", refresh)
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

    def test_service_stops_when_no_running_ptys_remain(self):
        self.assertIn("if (KlyroSessionRuntime.sessions.hasRunningPtys())", self.service)
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
        self.assertIn("ClipboardPastePolicy.requiresConfirmation(incoming)", self.pane)
        self.assertIn("pendingMultilinePaste = TerminalPasteReview(request.tabId, incoming)", self.pane)
        self.assertIn("request.tabId != activeTabId", self.pane)
        self.assertIn("if (tabId != null && currentSessionId() == tabId)", self.pane)

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

    def test_empty_clipboard_cannot_claim_paste_available(self):
        self.assertIn("value == null || value.isEmpty()", self.clipboard_policy)
        self.assertIn("return ClipboardPastePolicy.canPaste(text) ? text : null;", self.clipboard_reader)

    def test_unicode_noncharacters_cannot_reach_shell(self):
        self.assertIn("Character.toCodePoint(c, value.charAt(i))", self.clipboard_policy)
        self.assertIn("(cp & 0xFFFE) == 0xFFFE", self.clipboard_policy)

    def test_multiline_paste_preview_is_bounded_and_disclosed(self):
        review = (APP / "ui/TerminalPasteReview.java").read_text()
        self.assertIn("private static final int PREVIEW_LIMIT = 3072", review)
        self.assertIn("review.preview,", self.pane)
        self.assertIn("if (review.isPreviewTruncated)", self.pane)
        self.assertIn("The entire script will be pasted.", self.pane)
        self.assertIn("Character.isHighSurrogate(text.charAt(end - 1))", review)
        self.assertNotIn("pendingMultilinePaste.orEmpty().take(500)", self.pane)

    def test_transcript_preview_does_not_split_surrogate_pairs(self):
        self.assertIn("Character.isLowSurrogate(text.charAt(start))", (APP / "ui/TerminalActionPolicy.java").read_text())
        self.assertIn("Character.isHighSurrogate(text.charAt(start - 1))", (APP / "ui/TerminalActionPolicy.java").read_text())

    def test_terminal_header_prioritizes_centered_workspace_without_duplicate_logo(self):
        self.assertIn("Box(Modifier.weight(1f), contentAlignment = Alignment.Center)", self.pane)
        self.assertIn(".widthIn(max = 160.dp)", self.pane)
        self.assertIn('onClickLabel = "Choose workspace"', self.pane)
        self.assertIn('"TERMINAL"', self.pane)
        self.assertNotIn("KlyroBrandChoice.selected(context)", self.pane)
        self.assertNotIn("R.drawable.ic_klyro_brand_b", self.pane)

    def test_apk_version_advances_past_0412_without_downgrade(self):
        import re
        gradle = (ROOT / "app/build.gradle.kts").read_text()
        code_match = re.search(r"val baseKlyroVersionCode\s*=\s*(\d+)", gradle)
        version_match = re.search(r'val baseKlyroVersionName\s*=\s*"0\.4\.(\d+)-dev"', gradle)
        self.assertIsNotNone(code_match)
        self.assertIsNotNone(version_match)
        code, patch = int(code_match.group(1)), int(version_match.group(1))
        self.assertGreaterEqual(code, 31, "Standalone APK must not downgrade 0.4.23-dev")
        self.assertGreaterEqual(patch, 24)
        self.assertEqual(code, patch + 7)
        self.assertIn("versionCode = finalVersionCode", gradle)

    def test_failed_bootstrap_cannot_modify_existing_shell_profile(self):
        installer = (APP / "termux/TermuxBootstrapInstaller.java").read_text()
        activation = installer.index("activatePrefix(staging, prefix, previous);")
        profile_write = installer.index("writeKlyroBashProfile(new File(paths.getHome(), \".bashrc\"));", activation)
        self.assertGreater(profile_write, activation)
        self.assertNotIn("writeKlyroBashProfile", installer[installer.index("KlyroStorageCommandBridge.installHelper(staging);"):activation])

    def test_service_start_is_guarded_by_running_ptys(self):
        self.assertIn("if (!KlyroSessionRuntime.sessions.hasRunningPtys()) return", self.service)


if __name__ == "__main__":
    unittest.main()
