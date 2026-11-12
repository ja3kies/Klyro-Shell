#!/usr/bin/env python3
"""Non-device source contracts for Klyro's background/foreground repair.

These assertions protect regression-sensitive wiring. They are NOT evidence
that a Pixel 9 is free of ANRs; that requires a real-device lifecycle test.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
CLIENT = (ROOT / "app/src/main/java/com/oryn/terminal/session/OrynTerminalSessionClient.java").read_text()
VIEW_CLIENT = (ROOT / "app/src/main/java/com/oryn/terminal/ui/OrynTerminalViewClient.java").read_text()
PANE = (ROOT / "app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()

class BackgroundResumeContract(unittest.TestCase):
    def test_paused_activity_disables_terminal_ui_not_pty(self):
        callback = MAIN.split("override fun onPause()", 1)[1].split(
            "override fun onSaveInstanceState(", 1)[0]
        self.assertIn("it.client.setUiActive(false)", callback)
        self.assertNotRegex(callback, r"closeTab\(|finishIfRunning\(|closeSession\(")

    def test_resumed_activity_refreshes_live_sessions(self):
        callback = MAIN.split("override fun onResume()", 1)[1].split(
            "override fun onPause()", 1)[0]
        self.assertIn("it.client.setUiActive(true)", callback)
        self.assertIn("super.onResume()", callback)

    def test_output_is_not_queued_during_background(self):
        block = CLIENT.split("void onTextChanged(", 1)[1].split(
            "@Override", 1)[0]
        self.assertIn("!uiActive.get()", block)
        self.assertIn("outputReceivedWhilePaused.set(true)", block)
        self.assertIn("return;", block)
        self.assertIn("scheduleOutputRefresh()", block)

    def test_one_refresh_is_coalesced_on_main_looper(self):
        block = CLIENT.split("private void scheduleOutputRefresh()", 1)[1].split(
            "public void dispose()", 1)[0]
        self.assertIn("pendingOutputRefresh.compareAndSet(false, true)", block)
        self.assertIn("uiHandler.post(", block)
        self.assertIn("pendingOutputRefresh.set(false)", block)
        self.assertIn("!uiActive.get()", block)
        self.assertIn("view.isAttachedToWindow()", block)

    def test_ui_resumes_from_emulator_without_restarting_shell(self):
        callback = CLIENT.split("void setUiActive(", 1)[1].split(
            "private void scheduleOutputRefresh()", 1)[0]
        self.assertIn("outputReceivedWhilePaused.getAndSet(false)", callback)
        self.assertIn("scheduleOutputRefresh()", callback)
        self.assertNotIn("finishIfRunning", callback)

    def test_released_view_detaches_both_clients(self):
        area = PANE.split("AndroidView(\n            // Inset real terminal", 1)[1]
        area = area.split("        // Portrait: two fixed rows", 1)[0]
        self.assertIn("onRelease = { view ->", area)
        self.assertIn("sessionClient.detachViewIfCurrent(view)", area)
        self.assertIn("viewClient.detachViewIfCurrent(view)", area)
        self.assertIn("activeTerminalView = null", area)

    def test_conditional_view_detach_does_not_detach_replacement(self):
        self.assertIn("if (terminalView == view) terminalView = null;", CLIENT)
        self.assertIn("if (terminalView == oldView) terminalView = null;", VIEW_CLIENT)

    def test_storage_bridge_poll_runs_on_io_dispatcher(self):
        area = MAIN.split("private val storageBridgePoller = object : Runnable", 1)[1].split(
            "private fun completeStorageRequest", 1)[0]
        self.assertIn("withContext(Dispatchers.IO)", area)
        self.assertIn("storageCommandBridge?.poll()", area)
        self.assertIn("storageBridgePollInFlight", area)
        self.assertIn("launchPendingStoragePicker()", area)

    def test_picker_request_is_not_dropped_when_activity_pauses_or_recreates(self):
        self.assertIn("deferredStorageFolderPicker = true", MAIN)
        self.assertIn("if (deferredStorageFolderPicker) launchPendingStoragePicker()", MAIN)
        self.assertIn("klyro_deferred_storage_picker", MAIN)
        self.assertIn("savedInstanceState", MAIN)

if __name__ == "__main__":
    unittest.main()
