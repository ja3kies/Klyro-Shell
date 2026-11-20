"""P0 UX: 1,500-line diagnostic must acknowledge taps and never block the UI for setup."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
UI = (ROOT / "app/src/main/java/com/oryn/terminal/ui/FreezeReportsPane.kt").read_text()
SCRIPT = (ROOT / "app/src/main/java/com/oryn/terminal/diagnostics/StabilityScenario.java").read_text()


class StressLaunchUxContracts(unittest.TestCase):
    def main_launch(self):
        return MAIN.split("suspend fun startDiagnosticStress(): DiagnosticStressResult", 1)[1].split(
            "private fun launchBash()", 1
        )[0]

    def controls(self):
        return UI.split("private fun FreezeStabilityControls(", 1)[1]

    def test_launcher_prepares_terminal_without_blocking_main_thread(self):
        block = self.main_launch()
        preparation = block.split("withContext(Dispatchers.IO) {", 1)[1].split("\n            }", 1)[0]
        self.assertIn("TerminalSessionManager(applicationContext)", preparation)
        self.assertIn("manager.startBash(\"Stability test\", client)", preparation)
        self.assertIn("session.updateSize(80, 24)", block)
        self.assertLess(block.index("withContext(Dispatchers.IO)"), block.index("session.updateSize(80, 24)"))
        self.assertNotIn("session.updateSize(80, 24)", preparation)

    def test_rechecks_capacity_when_async_preparation_finishes(self):
        block = self.main_launch()
        self.assertGreaterEqual(block.count("terminalState.sessions.size >= 8"), 2)
        self.assertIn("terminalState.reserveTabId()", block)
        self.assertIn("terminalState.attachSession(", block)
        self.assertLess(block.index("terminalState.sessions.size >= 8", 20), block.index("terminalState.attachSession("))

    def test_failed_partial_launch_cleans_up_only_its_test_tab(self):
        block = self.main_launch()
        self.assertIn("terminalState.closeTab(id)", block)
        self.assertIn("KlyroSessionService.stopIfEmpty(this)", block)
        self.assertIn("existing sessions preserved", block)
        self.assertIn("Test input rejected; diagnostic tab retained.", block)
        self.assertIn("FreezeRecorder.recordStabilityRun(id)", block)

    def test_button_acknowledges_tap_and_ignores_double_taps(self):
        controls = self.controls()
        self.assertIn("var launching by remember", controls)
        self.assertIn("if (launching || scheduledSeconds != 0) return@TextButton", controls)
        self.assertIn('feedback = "Preparing isolated test…"', controls)
        self.assertIn("scope.launch {", controls)
        self.assertIn("startDiagnosticStress()", controls)
        self.assertIn("enabled = !launching", controls)
        self.assertIn('if (launching) "Preparing…" else "Start isolated 1,500-line test"', controls)

    def test_only_success_dismisses_diagnostic_sheet(self):
        controls = self.controls()
        self.assertIn("if (result?.started == true) onStabilityTestStarted()", controls)
        self.assertIn("finally { launching = false }", controls)
        self.assertIn("feedback", controls)
        self.assertIn("LiveRegionMode.Polite", controls)

    def test_command_unchanged_and_isolated(self):
        self.assertIn("sleep 3;", SCRIPT)
        self.assertIn("klyro_i<=1500", SCRIPT)
        self.assertIn("P0-STRESS 1500 OUTPUT TEST END", SCRIPT)
        self.assertIn("StabilityScenario.COMMAND", MAIN)


if __name__ == "__main__":
    unittest.main()
