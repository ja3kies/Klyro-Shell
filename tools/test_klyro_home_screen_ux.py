"""Home-screen UX regression contracts.
Source checks are not a substitute for Android large-font, landscape, or TalkBack QA.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "app/src/main/java/com/oryn/terminal/MainActivity.kt"

class HomeScreenUX(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.src = HOME.read_text()
        cls.ui = cls.src.split("private fun OrynTerminalApp(", 1)[1]

    def test_home_primary_task_before_plugins(self):
        self.assertLess(self.ui.index("Your shell."), self.ui.index("Open Klyro plugins"))
        self.assertLess(self.ui.index("RUNTIME STATUS"), self.ui.index("Open Klyro plugins"))

    def test_home_logo_fits_in_rounded_identity_container(self):
        self.assertIn("Modifier.size(60.dp)", self.ui)
        self.assertIn("modifier = Modifier.size(52.dp)", self.ui)
        self.assertIn(".clip(RoundedCornerShape(KlyroUiMetrics.cardCorner))", self.ui)

    def test_brand_title_shrinks_before_device_badge(self):
        self.assertIn("modifier = Modifier.weight(1f), maxLines = 1", self.ui)
        self.assertIn('Text("ARM64"', self.ui)

    def test_brand_device_badge_uses_shared_corner_token(self):
        self.assertIn("shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)", self.ui)

    def test_home_logo_uses_current_os_selection(self):
        self.assertIn("KlyroBrandChoice.selected(context)", self.ui)
        self.assertIn("R.drawable.ic_klyro_brand_a", self.ui)
        self.assertIn("R.drawable.ic_klyro_brand_b", self.ui)

    def test_home_header_not_fixed_dark_square(self):
        home = self.ui.split("Your shell.", 1)[0]
        self.assertNotIn("Color(0xFF0C0B13)", home)
        self.assertIn(".background(ui.tile)", home)

    def test_hero_has_session_context_and_length_limits(self):
        self.assertIn("if (hasRunningSessions)", self.ui)
        self.assertIn("open ${if (tabs.size == 1)", self.ui)
        self.assertIn("overflow = TextOverflow.Ellipsis", self.ui)

    def test_session_resume_available_even_after_verification_failure(self):
        home = self.ui.split('Text("Return to terminal"', 1)[0]
        self.assertIn("if (hasRunningSessions)", home)
        self.assertIn("onClick = onLaunch", home)

    def test_no_duplicate_resume_and_launch_cta(self):
        self.assertIn("if (runtimeVerified && !hasRunningSessions)", self.ui)
        self.assertIn('Text("Launch Bash", fontWeight', self.ui)

    def test_session_failure_has_priority_status(self):
        self.assertIn('sessionFailureMessage != null -> "SESSION ERROR"', self.ui)
        self.assertIn("sessionFailureMessage != null -> ui.danger", self.ui)

    def test_unverified_session_is_not_called_setup(self):
        self.assertIn('hasRunningSessions && !runtimeVerified -> "SESSION AVAILABLE"', self.ui)

    def test_installer_status_is_not_stale_terminal_status(self):
        self.assertIn("isInstalling || isVerifyingRuntime || technicalErrorDetails != null", self.src)
        self.assertIn("terminalState.sessions.isEmpty() -> statusMessage", self.src)

    def test_status_component_uses_distinct_bento_surface(self):
        self.assertIn("color = ui.tile,\n                    shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)", self.ui)

    def test_runtime_accessible_status_announcement(self):
        self.assertIn("liveRegion = LiveRegionMode.Polite", self.ui)

    def test_all_runtime_warnings_are_discoverable(self):
        self.assertIn("showAllWarnings", self.ui)
        self.assertIn("warningLines.joinToString", self.ui)
        self.assertIn('Text(if (showAllWarnings) "Show less" else "Show all warnings")', self.ui)

    def test_runtime_warning_variants_not_hidden(self):
        self.assertIn('it.startsWith("update-alternatives: warning:", ignoreCase = true)', self.ui)

    def test_diagnostics_preview_bounded_but_copy_complete(self):
        self.assertIn("technicalErrorDetails.take(16_000)", self.ui)
        self.assertIn("Preview shortened.", self.ui)
        self.assertIn('ClipData.newPlainText("Klyro technical details", details)', self.src)

    def test_copy_diagnostics_handles_unavailable_clipboard(self):
        self.assertIn('as? ClipboardManager ?: error("Clipboard unavailable")', self.src)
        self.assertIn("if (copied) \"Technical details copied\" else \"Clipboard unavailable\"", self.src)

    def test_technical_controls_stack_for_large_fonts(self):
        self.assertIn("Column(\n                                modifier = Modifier.fillMaxWidth(),", self.ui)
        self.assertIn("onClick = onToggleTechnicalDetails", self.ui)
        self.assertIn("onClick = onCopyTechnicalDetails", self.ui)

    def test_progress_distinguishes_unknown_from_known(self):
        self.assertIn("if (progress > 0)", self.ui)
        self.assertIn('contentDescription = "Preparing installation"', self.ui)
        self.assertIn('contentDescription = "Installation progress"', self.ui)

    def test_progress_never_unbounded(self):
        self.assertIn('(progress / 100f).coerceIn(0f, 1f)', self.ui)

    def test_installation_retry_does_not_claim_cleanup(self):
        self.assertIn('"Retry installation"', self.ui)
        self.assertNotIn('"Clean & retry installation"', self.ui)

    def test_installation_disabled_while_busy(self):
        self.assertIn("enabled = !installing && !verifyingRuntime", self.ui)

    def test_session_resume_prioritized_over_reinstallation(self):
        self.assertIn("containerColor = if (hasRunningSessions) ui.tile else ui.accent", self.ui)

    def test_plugin_card_has_explicit_navigation_role(self):
        self.assertIn('.clickable(role = Role.Button,', self.ui)
        self.assertIn('onClickLabel = "Open Klyro plugins"', self.ui)

    def test_plugin_route_survives_rotation(self):
        self.assertIn("var showPlugins by rememberSaveable", self.ui)

    def test_home_ctas_use_tokenized_rounded_shapes(self):
        self.assertIn("shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)", self.ui)
        self.assertIn("heightIn(min = KlyroUiMetrics.minTouch)", self.ui)

    def test_main_branch_not_changed_by_source_policy(self):
        self.assertIn("KlyroSessionService.ensureRunning(this)", self.src)


if __name__ == "__main__":
    unittest.main()
