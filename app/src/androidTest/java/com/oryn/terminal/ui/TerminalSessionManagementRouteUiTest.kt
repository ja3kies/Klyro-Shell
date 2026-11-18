package com.oryn.terminal.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.oryn.terminal.session.OrynTerminalSessionClient
import com.oryn.terminal.workspace.KlyroWorkspaceCatalog
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import org.junit.Rule
import org.junit.Test

/** Verifies the visible More-menu route reaches the same active-session sheet. */
class TerminalSessionManagementRouteUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    @Test fun moreMenuCanOpenTheActiveSessionsManagementSheet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val client = OrynTerminalSessionClient(context, null)
        val viewClient = OrynTerminalViewClient(context)
        val session = TerminalSession("/system/bin/sh", "/", arrayOf("sh"), emptyArray(), 100, client)
        val emulator = TerminalEmulator(session, 80, 24, 100, client)
        TerminalSession::class.java.getDeclaredField("mEmulator")
            .apply { isAccessible = true }.set(session, emulator)
        try {
            ui.setContent {
                MaterialTheme {
                    TerminalPane(
                        session = session, darkChrome = true, themeMode = "dark", accentId = "mint",
                        onAccentIdChange = {}, onThemeModeChange = {}, title = "Build",
                        tabs = listOf(7L to "Build"), activeTabId = 7L, outputRevision = 0,
                        launchingBash = false,
                        workspaces = listOf(KlyroWorkspaceCatalog.Entry("general", "General")),
                        selectedWorkspaceId = "general", tabWorkspaces = mapOf(7L to "general"),
                        tabsWithUnreadOutput = emptySet(), finishedTabIds = emptySet(),
                        jobSessions = mapOf(7L to session), lastOutputElapsedMs = emptyMap(),
                        onSelectWorkspace = {}, onCreateWorkspace = { true },
                        onRenameWorkspace = { _, _ -> true }, onDeleteWorkspace = {},
                        onAssignWorkspace = { _, _ -> }, diagnosticReport = "Fixture", onRecheck = {},
                        currentSessionId = { 7L }, onSelectTab = {}, onRenameTab = { _, _ -> },
                        onCloseTab = {}, onNewTab = {}, onReturnHome = {}, onImportFile = {},
                        onExportFile = {}, onSetupStorage = {}, onImportStorageFolder = {},
                        sessionClient = client, viewClient = viewClient, sessionFailureMessage = null,
                        nativePasteRequest = null, onNativePasteConsumed = {}, onClose = {}
                    )
                }
            }

            ui.onNodeWithContentDescription("More actions").performClick()
            ui.onNodeWithText("Manage active session").assertIsDisplayed().performClick()
            ui.onNodeWithText("Rename").assertIsDisplayed()
            ui.onNodeWithText("Close terminal").assertIsDisplayed()
        } finally {
            client.dispose()
        }
    }
}
