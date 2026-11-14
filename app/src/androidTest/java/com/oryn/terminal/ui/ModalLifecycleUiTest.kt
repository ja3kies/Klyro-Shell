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

    private fun host(content: @Composable (() -> Unit) -> Unit) {
        visible = mutableStateOf(true)
        ui.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    Button(onClick = { taps++ }, modifier = Modifier.align(androidx.compose.ui.Alignment.TopStart)
                        .testTag("underlying-control")) { Text("Underlying action") }
                    if (visible.value) content { closes++; visible.value = false }
                }
            }
        }
        ui.waitForIdle()
    }

    @Composable private fun settings(close: () -> Unit) {
        KlyroSettingsSheet(initialSection = "home", themeMode = "dark", onThemeModeChange = {},
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
        ui.runOnIdle { assertEquals(1, taps); checkOwner() }
    }

    private fun settingsSwipe(section: String) {
        host { close -> settings(close) }
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
        host { close -> settings(close) }
        ui.onNodeWithText("Diagnostics").performClick()
        ui.onNodeWithText("Freeze reports").performClick()
        swipeAway(); assertRemovedAndTapUnderlying()
    }
    @Test fun internalBackKeepsSheetThenCloseRemovesIt() {
        host { close -> settings(close) }
        ui.onNodeWithText("Appearance").performClick()
        ui.onNodeWithText("Settings", substring = false).performClick()
        ui.onNodeWithTag("klyro-modal-sheet").assertExists()
        ui.runOnIdle { assertEquals(0, closes) }
        ui.onNodeWithText("Close").performClick()
        assertRemovedAndTapUnderlying()
    }
    @Test fun closeFromAppearanceRemovesWindow() {
        host { close -> settings(close) }
        ui.onNodeWithText("Appearance").performClick()
        ui.onNodeWithText("Close").performClick(); assertRemovedAndTapUnderlying()
    }
    @Test fun systemBackFromAppearanceClosesOwner() {
        host { close -> settings(close) }
        ui.onNodeWithText("Appearance").performClick()
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.waitForIdle(); assertRemovedAndTapUnderlying()
    }
    @Test fun scrimTapFromAppearanceRemovesWindow() {
        host { close -> settings(close) }
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
        host { close -> settings(close) }
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

}
