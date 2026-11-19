"""Behavioral source contracts for failed PTY output preservation and lifecycle.

Complementary to JVM tests: real Pixel 9 validation is not inferred.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/oryn/terminal"
MODEL = (SRC / "TerminalSessionViewModel.kt").read_text()
SERVICE = (SRC / "KlyroSessionService.kt").read_text()
MAIN = (SRC / "MainActivity.kt").read_text()
UI = (SRC / "ui/TerminalPane.kt").read_text()


class FailedPtyRecoveryContracts(unittest.TestCase):
    def test_failure_retains_scrollback_and_code_without_closing_tab(self):
        self.assertIn("val exitStatus: Int? = null", MODEL)
        action = MODEL.split("private fun finishTab(", 1)[1].split("// Legacy", 1)[0]
        self.assertIn("entry.exitStatus != null", action)
        self.assertNotIn("closeTab(id)", action)
        self.assertIn("entry.copy(exitStatus = exitStatus)", action)
        self.assertIn("Bash exited unexpectedly (code $exitStatus)", MODEL)
        self.assertIn("pendingNativePaste", action)
        self.assertIn("unreadActivity[id] = true", action)

    def test_switching_tabs_recovers_failed_tabs_exit_notice(self):
        block = MODEL.split("fun selectTab(id: Long)", 1)[1].split("fun renameTab(", 1)[0]
        self.assertIn("exitStatus", block)
        self.assertIn("sessionFailureMessage", block)
        self.assertIn("exitMessage(", MODEL)

    def test_service_tracks_running_ptys_not_just_open_tabs(self):
        self.assertIn("fun hasRunningPtys(): Boolean", MODEL)
        self.assertIn("session.isRunning()", MODEL)
        self.assertNotIn("KlyroSessionRuntime.sessions.hasLiveSessions()", SERVICE)
        self.assertIn("KlyroSessionRuntime.sessions.hasRunningPtys()", SERVICE)
        self.assertIn("terminalState.hasRunningPtys()", MAIN)

    def test_new_session_without_pid_still_owns_foreground_service(self):
        # TerminalSession starts its subprocess in updateSize(), not its constructor.
        self.assertIn("fun runningPtyCount(): Int", MODEL)
        self.assertIn("it.session.getPid() == 0", MODEL)
        self.assertIn("it.exitStatus == null", MODEL)
        self.assertIn("fun hasRunningPtys(): Boolean = runningPtyCount() > 0", MODEL)
        self.assertIn("KlyroSessionRuntime.sessions.runningPtyCount()", SERVICE)
        self.assertNotIn("sessions.count { it.session.isRunning() }", SERVICE)

    def test_finished_tab_hardware_back_allows_explicit_close(self):
        block = UI.split("if (showEndSessionConfirmation) {", 1)[1].split("internal enum class ShortcutState", 1)[0]
        self.assertIn('Text("Close finished tab",', block)
        self.assertIn('Text("Close tab",', UI)
        self.assertIn("onCloseTab", block)
        self.assertNotIn("session.isRunning()) {\n                            expected?.let(onCloseTab)", block)

    def test_failed_tab_visible_read_only_and_copyable(self):
        self.assertIn("val canSendKeys = session.isRunning() && session.emulator != null", UI)
        self.assertIn("enabled = TerminalActionPolicy.canUseSmartKey(canSelect, canSendKeys)", UI)
        self.assertIn("sessionFailureMessage", UI)
        self.assertIn("Paste not sent", UI)

    def test_successful_exit_retains_tab_without_failure_notice(self):
        block = MODEL.split("private fun finishTab(", 1)[1].split("// Legacy", 1)[0]
        self.assertNotIn("closeTab(id)", block)
        self.assertIn("if (exitStatus == 0) null", MODEL)

if __name__ == "__main__":
    unittest.main()
