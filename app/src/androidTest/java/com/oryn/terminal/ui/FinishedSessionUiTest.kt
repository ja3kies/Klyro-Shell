package com.oryn.terminal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FinishedSessionUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun finishedTabRemovalExplainsTranscriptLossAndRequiresConfirmation() {
        var removals = 0
        ui.setContent {
            MaterialTheme {
                KlyroSessionSheet(sessionId = 7, sessionName = "Finished", isActive = true,
                    sessionRunning = false, onDismiss = {}, onRename = {}, onChooseShell = {},
                    onActivate = {}, onCloseSession = { removals++ })
            }
        }
        ui.onNodeWithContentDescription("Close terminal").performClick()
        ui.onNodeWithText("This removes the retained output. Its process has already exited. Other sessions remain open.")
            .assertExists()
        ui.onNodeWithText("Keep session").performClick()
        ui.runOnIdle { assertEquals(0, removals) }
        ui.onNodeWithContentDescription("Close terminal").performClick()
        ui.onNodeWithText("Close session").performClick()
        ui.runOnIdle { assertEquals(1, removals) }
    }
}
