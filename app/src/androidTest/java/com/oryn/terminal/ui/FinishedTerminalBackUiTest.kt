package com.oryn.terminal.ui

import android.accessibilityservice.AccessibilityService
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.oryn.terminal.session.OrynTerminalSessionClient
import com.oryn.terminal.workspace.KlyroWorkspaceCatalog
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Real TerminalPane/Android Back; emulator is isolated and no PTY is launched. */
class FinishedTerminalBackUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    @Test fun backCanExplicitlyRemoveRetainedOutputWithoutSendingInput() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val client = OrynTerminalSessionClient(context, null)
        val viewClient = OrynTerminalViewClient(context)
        val session = TerminalSession("/system/bin/sh", "/", arrayOf("sh"), emptyArray(), 100, client)
        val emulator = TerminalEmulator(session, 80, 24, 100, client)
        TerminalSession::class.java.getDeclaredField("mEmulator").apply { isAccessible = true }.set(session, emulator)
        var removals = 0
        val visible = mutableStateOf(true)
        try {
            ui.setContent {
                MaterialTheme {
                    if (!visible.value) Text("Retained tab removed") else TerminalPane(
                        session=session, darkChrome=true, themeMode="dark", accentId="mint",
                        onAccentIdChange={}, onThemeModeChange={}, title="Finished",
                        tabs=listOf(7L to "Finished"), activeTabId=7L, outputRevision=0,
                        workspaces=listOf(KlyroWorkspaceCatalog.Entry("general","General")),
                        selectedWorkspaceId="general", tabWorkspaces=mapOf(7L to "general"),
                        tabsWithUnreadOutput=emptySet(), finishedTabIds=setOf(7L),
                        jobSessions=mapOf(7L to session), lastOutputElapsedMs=emptyMap(),
                        onSelectWorkspace={}, onCreateWorkspace={true}, onRenameWorkspace={_,_->true},
                        onDeleteWorkspace={}, onAssignWorkspace={_,_->}, diagnosticReport="Fixture",
                        onRecheck={}, currentSessionId={7L}, onSelectTab={}, onRenameTab={_,_->},
                        onCloseTab={id -> assertEquals(7L,id);removals++;visible.value=false},
                        onNewTab={}, onReturnHome={}, onImportFile={}, onExportFile={},
                        onSetupStorage={}, onImportStorageFolder={}, sessionClient=client,
                        viewClient=viewClient, sessionFailureMessage=null,
                        nativePasteRequest=null, onNativePasteConsumed={}, onClose={})
                }
            }
            ui.runOnIdle {
                androidx.core.view.WindowCompat.getInsetsController(ui.activity.window, ui.activity.window.decorView)
                    .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
            }
            ui.waitUntil(5000) {
                androidx.core.view.ViewCompat.getRootWindowInsets(ui.activity.window.decorView)
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
            }
            InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            ui.waitForIdle()
            ui.onNodeWithText("This removes the retained output. Its process has already exited.").assertExists()
            ui.onNodeWithText("Keep session").performClick()
            ui.runOnIdle { assertEquals(0,removals) }
            InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            ui.waitForIdle();ui.onNodeWithText("End session").performClick()
            ui.onNodeWithText("Retained tab removed").assertExists()
            ui.runOnIdle { assertEquals(1,removals) }
        } finally { client.dispose() }
    }
}
