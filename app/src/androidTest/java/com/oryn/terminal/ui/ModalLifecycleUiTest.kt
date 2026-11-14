package com.oryn.terminal.ui

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.oryn.terminal.workspace.KlyroWorkspaceCatalog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Test-only input injection goes through Android InputDispatcher, not a semantics click.
 * Coordinates are transient and never stored in application diagnostics. No PTYs are created. */
class ModalLifecycleUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()
    private var taps = 0
    private var closes = 0
    private var priorNotice: String? = null
    private lateinit var visible: MutableState<Boolean>

    private var fixtureDark = false
    private fun host(
        restoration: androidx.compose.ui.test.junit4.StateRestorationTester? = null,
        dark: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable (() -> Unit) -> Unit
    ) {
        fixtureDark = dark
        val tree: @Composable () -> Unit = {
            visible = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(dark,"lime"),
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density,fontScale)) {
            MaterialTheme(colorScheme = if(dark) darkColorScheme() else lightColorScheme()) {
                Box(Modifier.fillMaxSize()) {
                    Button(onClick = { taps++ }, modifier = Modifier.align(androidx.compose.ui.Alignment.TopStart)
                        .testTag("underlying-control")) { Text("Underlying action") }
                    if (visible.value) content { closes++; visible.value = false }
                }
            }
            }
        }
        if(restoration == null) ui.setContent(tree) else restoration.setContent(tree)
        ui.waitForIdle()
    }

    @Composable private fun SettingsFixture(close: () -> Unit) {
        KlyroSettingsSheet(initialSection = "home", themeMode = if(fixtureDark) "dark" else "light", onThemeModeChange = {},
            accentId = "sage", onAccentIdChange = {}, fontSizeSp = 13.5f, fontFamilyId = "default",
            onFontFamilyChange = {}, onFontChange = {}, onResetFont = {}, onResetModifiers = {},
            activeModifiers = emptySet(), onSetupStorage = {}, onImportStorageFolder = {},
            onImportFile = {}, onExportFile = {}, diagnosticReport = "Fixture report",
            onRecheckEnvironment = {}, onCopyDiagnostic = {}, onShareDiagnostic = {}, onDismiss = close)
    }

    private fun swipeAway() {
        // Start at the drag handle, not in a nested scrolling list or slider.
        ui.onNodeWithTag("klyro-modal-sheet").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(center.x, 12f),
                end = androidx.compose.ui.geometry.Offset(center.x, height + 300f), durationMillis = 350)
        }
        ui.waitForIdle()
    }

    private fun assertRemovedAndTapUnderlying(
        expectedTaps: Int = 1,
        checkOwner: () -> Unit = { assertFalse(visible.value); assertEquals(1, closes) }
    ) {
        ui.onNodeWithTag("klyro-modal-sheet").assertDoesNotExist()
        ui.waitUntil(5000) { ui.activity.hasWindowFocus() }
        ui.runOnIdle { checkOwner(); assertTrue(ui.activity.hasWindowFocus()) }
        val center = ui.onNodeWithTag("underlying-control").fetchSemanticsNode().boundsInRoot.center
        val offset = IntArray(2)
        ui.runOnIdle { ui.activity.findViewById<View>(android.R.id.content).getLocationOnScreen(offset) }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val time = SystemClock.uptimeMillis()
        for ((action, at) in listOf(MotionEvent.ACTION_DOWN to time, MotionEvent.ACTION_UP to time + 30)) {
            val event = MotionEvent.obtain(time, at, action, center.x + offset[0], center.y + offset[1], 0)
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            try { assertTrue(automation.injectInputEvent(event, true)) } finally { event.recycle() }
        }
        ui.waitForIdle()
        ui.runOnIdle { assertEquals(expectedTaps, taps); checkOwner() }
    }

    private fun settingsSwipe(section: String) {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText(section).performClick()
        swipeAway()
        assertRemovedAndTapUnderlying()
    }

    @Test fun appearanceSwipeRemovesWindowAndUnderlyingTapExecutes() = settingsSwipe("Appearance")
    @Test fun keyboardSwipeRemovesWindowAndUnderlyingTapExecutes() = settingsSwipe("Keyboard")
    @Test fun filesSwipeRemovesWindowAndUnderlyingTapExecutes() = settingsSwipe("Storage")
    @Test fun privacySwipeRemovesWindowAndUnderlyingTapExecutes() = settingsSwipe("Privacy")
    @Test fun diagnosticsSwipeRemovesWindowAndUnderlyingTapExecutes() = settingsSwipe("Diagnostics")
    @Test fun aboutSwipeRemovesWindowAndUnderlyingTapExecutes() = settingsSwipe("About")
    @Test fun reportsSwipeRemovesWindowAndUnderlyingTapExecutes() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("Diagnostics").performClick()
        ui.onNodeWithText("Freeze reports").performScrollTo().performClick()
        swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun internalBackKeepsSheetThenCloseRemovesIt() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick()
        ui.onNodeWithText("Settings", substring = false).performClick()
        ui.onNodeWithTag("klyro-modal-sheet").assertExists()
        ui.runOnIdle { assertEquals(0, closes) }
        ui.onNodeWithText("Close").performClick()
        assertRemovedAndTapUnderlying()
    }
    @Test fun closeFromAppearanceRemovesWindow() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick()
        ui.onNodeWithText("Close").performClick(); assertRemovedAndTapUnderlying()
    }
    @Test fun systemBackFromAppearanceClosesOwner() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick()
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.waitForIdle(); assertRemovedAndTapUnderlying()
    }
    @Test fun scrimTapFromAppearanceRemovesWindow() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick()
        // Scrim is outside the sheet; use WindowManager injection for this path too.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val time = SystemClock.uptimeMillis()
        val width = ui.activity.resources.displayMetrics.widthPixels.toFloat()
        val sheetTop = ui.onNodeWithTag("klyro-modal-sheet").fetchSemanticsNode().boundsInRoot.top
        val status = android.graphics.Rect()
        ui.runOnIdle { ui.activity.window.decorView.getWindowVisibleDisplayFrame(status) }
        org.junit.Assume.assumeTrue("Outside tap requires a visible scrim region", sheetTop > status.top+4f)
        for ((action, at) in listOf(MotionEvent.ACTION_DOWN to time, MotionEvent.ACTION_UP to time+30)) {
            val event = MotionEvent.obtain(time,at,action,width/2,status.top+2f,0)
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            try { assertTrue(automation.injectInputEvent(event,true)) } finally { event.recycle() }
        }
        ui.waitForIdle(); assertRemovedAndTapUnderlying()
    }
    @Test fun workspaceSwipeClosesOwner() {
        host { close -> KlyroWorkspaceSheet(entries = listOf(KlyroWorkspaceCatalog.Entry("general","General")),
            selectedId = "general", tabs = emptyList(), tabWorkspaces = emptyMap(), activeTabId = null,
            onSelectProject = {}, onCreateProject = { true }, onRenameProject = { _,_ -> true },
            onDeleteProject = {}, onAssignCurrentTab = {}, onSwitchTab = {}, onDismiss = close) }
        swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun workspaceDraftRejectsSwipeUntilExplicitCancel() {
        host { close -> KlyroWorkspaceSheet(entries = listOf(KlyroWorkspaceCatalog.Entry("general","General")),
            selectedId = "general", tabs = emptyList(), tabWorkspaces = emptyMap(), activeTabId = null,
            onSelectProject = {}, onCreateProject = { true }, onRenameProject = { _,_ -> true },
            onDeleteProject = {}, onAssignCurrentTab = {}, onSwitchTab = {}, onDismiss = close) }
        ui.onNodeWithText("New").performClick()
        ui.onNodeWithText("Workspace name").performTextInput("Unsaved draft")
        swipeAway()
        ui.onNodeWithText("Unsaved draft").assertExists()
        ui.runOnIdle { assertTrue(visible.value); assertEquals(0,closes) }
        ui.onNodeWithText("Cancel").performClick(); swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun sessionActionsSwipeDoesNotCloseSession() {
        var terminated=0
        host { close -> KlyroSessionSheet(sessionId=42,sessionName="Fixture",isActive=true,
            onDismiss=close,onRename={},onChooseShell={},onActivate={},onCloseSession={terminated++}) }
        swipeAway(); assertRemovedAndTapUnderlying(); assertEquals(0,terminated)
    }
    @Test fun paletteGuideSwipeClosesWholePalette() {
        host { close -> KlyroCommandPalette(installedPrefix=null,pluginCommands=emptyList(),onInsert={true},onDismiss=close) }
        ui.onNodeWithText("Welcome guide").performClick()
        swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun interruptedDragKeepsVisibleSheetThenRealSwipeCloses() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick()
        ui.onNodeWithTag("klyro-modal-sheet").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(center.x,12f)); moveBy(androidx.compose.ui.geometry.Offset(0f,5f)); cancel()
        }
        ui.waitForIdle(); ui.runOnIdle { assertTrue(visible.value) }
        swipeAway(); assertRemovedAndTapUnderlying()
    }
    private fun reportsHost() {
        priorNotice = com.oryn.terminal.diagnostics.FreezeRecorder.notice.value
        com.oryn.terminal.diagnostics.FreezeRecorder.notice.value = null
        com.oryn.terminal.diagnostics.FreezeRecorder.settingsVisible.value = false
        com.oryn.terminal.diagnostics.FreezeRecorder.openReports.value = true
        ui.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    Button(onClick = { taps++ }, modifier = Modifier.testTag("underlying-control")) { Text("Underlying action") }
                    FreezeRecoveryNotice()
                }
            }
        }
        ui.waitForIdle()
    }
    @Test fun standaloneReportsSwipeClearsRequestAndRestoresTouch() {
        try {
            reportsHost(); swipeAway()
            assertRemovedAndTapUnderlying { assertFalse(com.oryn.terminal.diagnostics.FreezeRecorder.openReports.value) }
        } finally {
            com.oryn.terminal.diagnostics.FreezeRecorder.openReports.value = false
            com.oryn.terminal.diagnostics.FreezeRecorder.notice.value = priorNotice
        }
    }
    @Test fun standaloneReportsCloseClearsRequestAndRestoresTouch() {
        try {
            reportsHost(); ui.onNodeWithText("Close diagnostics").performClick()
            assertRemovedAndTapUnderlying { assertFalse(com.oryn.terminal.diagnostics.FreezeRecorder.openReports.value) }
        } finally {
            com.oryn.terminal.diagnostics.FreezeRecorder.openReports.value = false
            com.oryn.terminal.diagnostics.FreezeRecorder.notice.value = priorNotice
        }
    }

    @Test fun darkModeDismissRestoresTouch() {
        host(dark=true) { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick(); swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun largeFontDismissRestoresTouch() {
        host(fontScale=2f) { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick(); swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun closedOwnerCannotResurrectOnSavedStateRestore() {
        val restore=androidx.compose.ui.test.junit4.StateRestorationTester(ui)
        host(restoration=restore) { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick(); swipeAway()
        restore.emulateSavedInstanceStateRestore(); ui.waitForIdle()
        assertRemovedAndTapUnderlying()
    }
    @Test fun restoredSubpageCanStillBeDismissed() {
        val restore=androidx.compose.ui.test.junit4.StateRestorationTester(ui)
        host(restoration=restore) { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick()
        restore.emulateSavedInstanceStateRestore(); ui.waitForIdle()
        ui.onAllNodesWithTag("klyro-modal-sheet").assertCountEquals(1)
        swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun stoppedAndResumedActivityCanDismissWithoutRestart() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick()
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        ui.waitForIdle(); swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun immediateReopenStartsWithFreshNavigation() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick(); swipeAway(); assertRemovedAndTapUnderlying()
        ui.runOnIdle { visible.value=true }; ui.waitForIdle()
        ui.onNodeWithText("Storage").assertExists()
        ui.onNodeWithText("Close").performClick()
        assertRemovedAndTapUnderlying(expectedTaps=2) { assertFalse(visible.value);assertEquals(2,closes) }
    }
    @Test fun protectedRenameDraftSurvivesBackUntilCancel() {
        host { close -> KlyroRenameSessionDialog(name="Fixture",onSave={},onDismiss=close) }
        ui.onNode(hasSetTextAction()).performTextReplacement("Unsaved name")
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.waitForIdle(); ui.onNodeWithText("Unsaved name").assertExists()
        ui.onNodeWithText("Cancel").performClick(); assertRemovedAndTapUnderlying()
    }
    @Test fun protectedColorDraftSurvivesBackUntilCancel() {
        host { close -> KlyroColorPickerDialog(initialArgb=0xff112233.toInt(),onApply={},onDismiss=close) }
        ui.onNodeWithContentDescription("Hex color value").performTextReplacement("#445566")
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.waitForIdle(); ui.onNodeWithText("Custom accent").assertExists()
        ui.onNodeWithText("Cancel").performClick(); assertRemovedAndTapUnderlying()
    }
    @Test fun sessionConfirmationCancelThenSwipeNeverTerminates() {
        var terminated=0
        host { close -> KlyroSessionSheet(sessionId=42,sessionName="Fixture",isActive=true,
            onDismiss=close,onRename={},onChooseShell={},onActivate={},onCloseSession={terminated++}) }
        ui.onNodeWithContentDescription("Close terminal").performClick()
        ui.onNodeWithText("Keep session").performClick()
        swipeAway(); assertRemovedAndTapUnderlying();assertEquals(0,terminated)
    }
    @Test fun guideInternalCommandsButtonDoesNotClosePalette() {
        host { close -> KlyroCommandPalette(installedPrefix=null,pluginCommands=emptyList(),onInsert={true},onDismiss=close) }
        ui.onNodeWithText("Welcome guide").performClick(); ui.onNodeWithText("Commands").performClick()
        ui.runOnIdle { assertEquals(0,closes) }
        swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun licenseDialogClosesBeforeParentAndLeavesNoWindow() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("About").performClick()
        ui.onNodeWithText("JetBrains Mono licence").performScrollTo().performClick()
        ui.onNode(hasText("Close") and hasAnyAncestor(hasTestTag("klyro-license-dialog"))).performClick()
        ui.waitForIdle(); ui.onNodeWithTag("klyro-modal-sheet").assertExists()
        swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun nextModalCanOpenImmediatelyAsPreviousOwnerCloses() {
        lateinit var mode: MutableState<Int>
        ui.setContent {
            mode=remember { mutableStateOf(1) }
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    Button(onClick={taps++},modifier=Modifier.testTag("underlying-control")) { Text("Underlying action") }
                    when(mode.value) {
                        1 -> SettingsFixture { closes++;mode.value=2 }
                        2 -> KlyroCommandPalette(installedPrefix=null,pluginCommands=emptyList(),onInsert={true},
                            onDismiss={closes++;mode.value=0})
                    }
                }
            }
        }
        ui.onNodeWithText("Appearance").performClick(); swipeAway()
        ui.onNodeWithText("Command Palette").assertExists()
        ui.onAllNodesWithTag("klyro-modal-sheet").assertCountEquals(1)
        swipeAway(); assertRemovedAndTapUnderlying { assertEquals(0,mode.value);assertEquals(2,closes) }
    }

    @Test fun removingOwnerDuringClosingAnimationStillRestoresTouch() {
        host { close -> SettingsFixture(close) }
        ui.onNodeWithText("Appearance").performClick()
        ui.mainClock.autoAdvance=false
        ui.onNodeWithTag("klyro-modal-sheet").performTouchInput {
            swipe(start=androidx.compose.ui.geometry.Offset(center.x,12f),
                end=androidx.compose.ui.geometry.Offset(center.x,height+300f),durationMillis=200)
        }
        ui.runOnIdle { visible.value=false }
        ui.mainClock.autoAdvance=true; ui.waitForIdle()
        assertRemovedAndTapUnderlying { assertFalse(visible.value);assertTrue(closes<=1) }
    }
    @Test fun imeBackPreservesDraftThenCancelAllowsRealDismiss() {
        host { close -> KlyroWorkspaceSheet(entries=listOf(KlyroWorkspaceCatalog.Entry("general","General")),
            selectedId="general",tabs=emptyList(),tabWorkspaces=emptyMap(),activeTabId=null,
            onSelectProject={},onCreateProject={true},onRenameProject={_,_->true},onDeleteProject={},
            onAssignCurrentTab={},onSwitchTab={},onDismiss=close) }
        ui.onNodeWithText("New").performClick()
        ui.onNodeWithText("Workspace name").performClick()
        try {
            ui.waitUntil(5000) {
                androidx.core.view.ViewCompat.getRootWindowInsets(ui.activity.window.decorView)
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
            }
        } catch (_: androidx.compose.ui.test.ComposeTimeoutException) {
            org.junit.Assume.assumeTrue("Software IME must be available",false)
        }
        ui.onNodeWithText("Workspace name").performTextInput("IME draft")
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.waitForIdle(); ui.onNodeWithText("IME draft").assertExists()
        ui.onNodeWithText("Cancel").performClick(); swipeAway(); assertRemovedAndTapUnderlying()
    }

}
