"""Service notification stays in sync with in-process PTY inventory changes.

Source contracts cover wiring; a physical device is still needed for UX validation.
"""
import unittest
from pathlib import Path

APP = Path(__file__).resolve().parents[1] / "app/src/main/java/com/oryn/terminal"
MODEL = (APP / "TerminalSessionViewModel.kt").read_text()
SERVICE = (APP / "KlyroSessionService.kt").read_text()


class SessionNotificationLiveSync(unittest.TestCase):
    def test_model_offers_process_scoped_inventory_observer(self):
        self.assertIn("fun setRunningPtyInventoryListener(listener: (() -> Unit)?)", MODEL)
        self.assertIn("runningPtyInventoryListener = listener", MODEL)
        self.assertIn("private fun notifyRunningPtyInventoryChanged()", MODEL)

    def test_create_close_and_normal_exit_change_inventory(self):
        attach = MODEL.split("fun attachSession(", 1)[1].split("fun selectTab(", 1)[0]
        self.assertIn("notifyRunningPtyInventoryChanged()", attach)
        close = MODEL.split("fun closeTab(", 1)[1].split("fun closeSession(", 1)[0]
        self.assertIn("notifyRunningPtyInventoryChanged()", close)
        finish = MODEL.split("private fun finishTab(", 1)[1].split("// Legacy", 1)[0]
        self.assertIn("if (exitStatus == 0)", finish)
        self.assertIn("closeTab(id)", finish)

    def test_retained_failed_session_notifies_service_without_discarding_output(self):
        finish = MODEL.split("private fun finishTab(", 1)[1].split("// Legacy", 1)[0]
        self.assertIn("entry.copy(exitStatus = exitStatus)", finish)
        self.assertIn("notifyRunningPtyInventoryChanged()", finish)
        self.assertIn("sessionFailureMessage = exitMessage(exitStatus)", finish)

    def test_service_receives_inventory_events_even_in_background(self):
        create = SERVICE.split("override fun onCreate()", 1)[1].split("override fun onStartCommand(", 1)[0]
        self.assertIn("setRunningPtyInventoryListener", create)
        self.assertIn("scheduleInventoryRefresh()", create)
        self.assertIn("private fun scheduleInventoryRefresh()", SERVICE)
        self.assertIn("handler.post(inventoryRefresh)", SERVICE)
        self.assertIn("handler.removeCallbacks(inventoryRefresh)", SERVICE)

    def test_refresh_updates_notification_or_stops_last_pty_without_waiting_a_minute(self):
        refresh = SERVICE.split("private val inventoryRefresh = Runnable", 1)[1].split("override fun onCreate()", 1)[0]
        self.assertIn("KlyroSessionRuntime.sessions.runningPtyCount()", refresh)
        self.assertIn("stopSelf()", refresh)
        self.assertIn("showActiveSessionsNotification", refresh)
        self.assertIn("if (!foregroundReady)", refresh)
        self.assertIn("private fun showActiveSessionsNotification(sessionCount: Int)", SERVICE)
        self.assertIn("startForeground(NOTIFICATION_ID, notification", SERVICE)

    def test_service_destroy_unsubscribes_without_killing_ptys(self):
        cleanup = SERVICE.split("override fun onDestroy()", 1)[1].split("override fun onBind(", 1)[0]
        self.assertIn("setRunningPtyInventoryListener(null)", cleanup)
        self.assertIn("handler.removeCallbacks(inventoryRefresh)", cleanup)
        self.assertIn("handler.removeCallbacks(check)", cleanup)
        self.assertNotIn("closeAllSessions()", cleanup)
        self.assertNotIn("finishIfRunning()", cleanup)

    def test_initial_start_and_fallback_survive(self):
        self.assertIn("override fun onStartCommand(", SERVICE)
        self.assertIn("START_NOT_STICKY", SERVICE)
        self.assertIn("handler.postDelayed(check, 60_000L)", SERVICE)
        self.assertIn("it.exitStatus == null && (it.session.isRunning() || it.session.getPid() == 0)", MODEL)

if __name__ == "__main__":
    unittest.main()
