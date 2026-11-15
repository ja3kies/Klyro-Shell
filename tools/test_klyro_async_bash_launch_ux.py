"""All normal Bash launch entry points stay responsive, capacity-safe and honest during startup.

Source-contract regression. Android UI behavior still requires hardware verification.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
PANE = (ROOT / "app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
SHEETS = (ROOT / "app/src/main/java/com/oryn/terminal/ui/KlyroSessionSheets.kt").read_text()
MANAGER = (ROOT / "app/src/main/java/com/oryn/terminal/session/TerminalSessionManager.java").read_text()
SESSION = (ROOT / "terminal-emulator/src/main/java/com/termux/terminal/TerminalSession.java").read_text()

class AsyncBashLaunchUX(unittest.TestCase):
    def launch(self):
        return MAIN.split("private fun launchBash()", 1)[1].split("private fun closeBash()", 1)[0]

    def test_terminal_parser_handler_is_bound_to_main_even_if_created_on_io(self):
        # A Handler() without an explicit Looper would fail on Dispatchers.IO.
        self.assertIn("import android.os.Looper;", SESSION)
        self.assertIn("final Handler mMainThreadHandler = new MainThreadHandler();", SESSION)
        handler = SESSION.split("class MainThreadHandler extends Handler", 1)[1].split(
            "final byte[] mReceiveBuffer", 1
        )[0]
        self.assertIn("MainThreadHandler() {", handler)
        self.assertIn("super(Looper.getMainLooper());", handler)
        diagnostic = MAIN.split("suspend fun startDiagnosticStress()", 1)[1].split(
            "private fun launchBash()", 1
        )[0]
        self.assertIn("withContext(Dispatchers.IO)", diagnostic)
        self.assertIn("withContext(Dispatchers.IO)", self.launch())

    def test_root_cause_file_layout_is_not_repeated_on_main(self):
        self.assertIn("this.paths.prepareLayout();", MANAGER)
        launch = self.launch()
        self.assertIn("lifecycleScope.launch {", launch)
        self.assertIn("withContext(Dispatchers.IO) {", launch)
        io = launch.split("withContext(Dispatchers.IO) {", 1)[1].split("\n            }", 1)[0]
        self.assertIn("TerminalSessionManager(applicationContext)", io)
        self.assertIn('manager.startBash("Bash", newSessionClient)', io)
        self.assertNotIn("TerminalSessionManager(this)", launch)
        self.assertNotIn("newSession.updateSize(", io)

    def test_dispatch_gates_duplicate_taps_before_suspension(self):
        launch = self.launch()
        self.assertIn("if (bashLaunchInProgress) return", launch)
        self.assertIn("bashLaunchInProgress = true", launch)
        self.assertIn("finally {", launch)
        self.assertIn("bashLaunchInProgress = false", launch)
        self.assertLess(launch.index("bashLaunchInProgress = true"), launch.index("lifecycleScope.launch {"))

    def test_capacity_is_rechecked_after_delayed_setup(self):
        launch = self.launch()
        self.assertGreaterEqual(launch.count("terminalState.sessions.size >= 8"), 2)
        self.assertLess(launch.index("withContext(Dispatchers.IO)"), launch.rindex("terminalState.sessions.size >= 8"))
        self.assertLess(launch.rindex("terminalState.sessions.size >= 8"), launch.index("terminalState.attachSession("))

    def test_workspace_context_is_captured_before_suspension(self):
        launch = self.launch()
        self.assertIn("val targetWorkspaceId = workspaceStore.activeId", launch)
        self.assertIn("workspaceId = targetWorkspaceId", launch)
        self.assertLess(launch.index("val targetWorkspaceId"), launch.index("withContext(Dispatchers.IO)"))

    def test_cancellation_and_exception_only_clean_attempted_session(self):
        launch = self.launch()
        self.assertIn("catch (cancelled: kotlinx.coroutines.CancellationException)", launch)
        self.assertIn("newSessionClient.dispose()", launch)
        self.assertIn("terminalState.closeTab(tabId)", launch)
        self.assertIn("KlyroSessionService.stopIfEmpty(this@MainActivity)", launch)
        self.assertIn("technicalErrorDetails = KlyroDiagnostics.summarize(error)", launch)
        self.assertNotIn("terminalState.closeAllSessions()", launch)

    def test_home_button_shows_progress_without_disabling_resume(self):
        self.assertIn("launchingBash = bashLaunchInProgress", MAIN)
        self.assertIn("launchingBash: Boolean", MAIN)
        self.assertIn("homeAction != KlyroHomeAction.START_BASH || !launchingBash", MAIN)
        self.assertIn('"Starting Bash…"', MAIN)
        self.assertIn("homeAction == KlyroHomeAction.RESUME_SESSION", MAIN)

    def test_tab_plus_and_session_sheet_respect_pending_launch(self):
        self.assertIn("launchingBash: Boolean", PANE)
        self.assertIn("enabled = tabs.size < 8 && !launchingBash", PANE)
        self.assertIn("canCreateSession = tabs.size < 8 && !launchingBash", PANE)
        self.assertIn("canStartBash = tabs.size < 8 && !launchingBash", PANE)
        self.assertIn("launchingBash = launchingBash", PANE)
        self.assertIn("launchingBash: Boolean", SHEETS)
        self.assertIn('"Starting Bash…"', SHEETS)
        self.assertIn("enabled = canCreateSession", SHEETS)
        self.assertIn(".clickable(enabled = canStartBash", SHEETS)

    def test_entrypoints_still_route_through_single_launcher(self):
        self.assertIn("onNewTab = ::launchBash", MAIN)
        self.assertIn("else launchBash()", MAIN)
        self.assertIn("onNewBash = {", PANE)
        self.assertIn("onNewTab()", PANE)

if __name__ == "__main__":
    unittest.main()
