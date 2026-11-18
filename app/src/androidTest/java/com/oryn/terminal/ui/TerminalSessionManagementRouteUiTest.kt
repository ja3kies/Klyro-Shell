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

    @Test fun pinnedMoreMenuRemainsReachableAfterEightTabsScrollToTheActiveSession() {
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
                        tabs = (1L..8L).map { it to "Terminal $it" },
                        activeTabId = 8L, outputRevision = 0,
                        launchingBash = false,
                        workspaces = listOf(KlyroWorkspaceCatalog.Entry("general", "General")),
                        selectedWorkspaceId = "general",
                        tabWorkspaces = (1L..8L).associateWith { "general" },
                        tabsWithUnreadOutput = emptySet(), finishedTabIds = emptySet(),
                        jobSessions = mapOf(8L to session), lastOutputElapsedMs = emptyMap(),
                        onSelectWorkspace = {}, onCreateWorkspace = { true },
                        onRenameWorkspace = { _, _ -> true }, onDeleteWorkspace = {},
                        onAssignWorkspace = { _, _ -> }, diagnosticReport = "Fixture", onRecheck = {},
                        currentSessionId = { 8L }, onSelectTab = {}, onRenameTab = { _, _ -> },
                        onCloseTab = {}, onNewTab = {}, onReturnHome = {}, onImportFile = {},
                        onExportFile = {}, onSetupStorage = {}, onImportStorageFolder = {},
                        sessionClient = client, viewClient = viewClient, sessionFailureMessage = null,
                        nativePasteRequest = null, onNativePasteConsumed = {}, onClose = {}
                    )
                }
            }

            ui.onNodeWithContentDescription("More actions")
                .assertIsDisplayed().performClick()
            ui.onNodeWithText("Manage active session").assertIsDisplayed().performClick()
            ui.onNodeWithText("Rename").assertIsDisplayed()
            ui.onNodeWithText("Close terminal").assertIsDisplayed()
        } finally {
            client.dispose()
        }
    }
}
