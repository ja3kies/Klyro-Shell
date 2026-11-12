"""ANR regression contracts for frequent PTY output while opening Settings.

Source-only guards. Passing these tests does NOT establish the cause of any
individual Pixel freeze or prove runtime freedom from deadlocks; capture an
Android ANR thread dump for that.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt").read_text()
PANE = (ROOT / "app/src/main/java/com/oryn/terminal/ui/TerminalPane.kt").read_text()
SETTINGS = (ROOT / "app/src/main/java/com/oryn/terminal/ui/KlyroSettingsSheet.kt").read_text()


class SettingsAnrContracts(unittest.TestCase):
    def test_main_diagnostic_stat_and_package_manager_run_on_io(self):
        area = MAIN.split('var appVersion by remember(context)', 1)[1].split(
            'val screenChrome =', 1)[0]
        self.assertIn('withContext(Dispatchers.IO)', area)
        self.assertIn('context.packageManager.getPackageInfo(', area)
        self.assertIn('context.filesDir.usableSpace', area)
        self.assertIn('val diagnosticReport = remember(', area)
        self.assertNotIn('val diagnosticReport = KlyroHealthReport.format(', MAIN)

    def test_status_bar_side_effect_only_runs_when_palette_changes(self):
        effect = MAIN.split('SideEffect {\n                if (appliedTerminalPalette != palette)', 1)[1].split(
            'CompositionLocalProvider(', 1)[0]
        self.assertIn('window.decorView.setBackgroundColor', effect)
        self.assertIn('WindowInsetsControllerCompat(', effect)
        self.assertIn('appliedTerminalPalette = palette', effect)

    def test_selector_does_not_read_entire_scrollback_per_output_revision(self):
        area = PANE.split('val canSelect = remember(session, outputRevision, terminalActionRevision)', 1)[1].split(
            'TerminalExtraKey(\n                "ALL"', 1)[0]
        self.assertIn('screen.activeTranscriptRows > 0', area)
        self.assertIn('screen.getSelectedText(', area)
        self.assertNotIn('transcriptText', area)
        # Full transcript is still available on explicit user selection.
        self.assertIn('val output = session.emulator?.screen?.transcriptText.orEmpty()', PANE)

    def test_launcher_identity_binder_lookup_is_not_on_settings_opening_frame(self):
        section = SETTINGS.split('LaunchedEffect(context) {', 1)[1].split(
            'fun navigateBack()', 1)[0]
        self.assertIn('withContext(Dispatchers.IO)', section)
        self.assertIn('KlyroBrandChoice.selected(context)', section)

    def test_storage_permission_binder_call_not_in_compose_files_case(self):
        prefix = SETTINGS.split('val context = LocalContext.current', 1)[1].split(
            'val version =', 1)[0]
        self.assertIn('LaunchedEffect(context, section)', prefix)
        self.assertIn('withContext(Dispatchers.IO)', prefix)
        self.assertIn('persistedUriPermissions.count {', prefix)
        block = SETTINGS.split('"files" -> {', 1)[1].split('"privacy" -> {', 1)[0]
        self.assertNotIn('persistedUriPermissions', block)
        self.assertIn('val authorizedCount = authorizedFolderCount.coerceAtLeast(0)', block)

    def test_storage_completion_does_not_write_on_activity_result_thread(self):
        block = MAIN.split('private fun completeStorageRequest(status: String)', 1)[1].split(
            'private val storageFolderLauncher', 1)[0]
        self.assertIn('lifecycleScope.launch(Dispatchers.IO)', block)
        self.assertIn('storageCommandBridge?.finish(token, status)', block)

    def test_existing_session_not_closed_when_settings_open(self):
        self.assertIn('if (showTerminalSettings) {', PANE)
        body = PANE.split('if (showTerminalSettings) {', 1)[1].split(
            'if (pendingMultilinePaste != null)', 1)[0]
        for forbidden in ('session.finishIfRunning(', 'sessionClient.dispose(', 'closeTab('):
            self.assertNotIn(forbidden, body)


if __name__ == "__main__":
    unittest.main()
