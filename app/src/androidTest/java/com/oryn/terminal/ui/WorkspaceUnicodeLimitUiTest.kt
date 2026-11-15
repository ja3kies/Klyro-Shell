package com.oryn.terminal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.oryn.terminal.workspace.KlyroWorkspaceCatalog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkspaceUnicodeLimitUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun pastedWorkspaceNameDoesNotEndWithHalfAnEmoji() {
        val wanted = "a".repeat(31) + "😀"
        var saved: String? = null
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                MaterialTheme {
                    KlyroWorkspaceSheet(
                        entries = listOf(KlyroWorkspaceCatalog.Entry("general", "General")),
                        selectedId = "general", tabs = emptyList(), tabWorkspaces = emptyMap(),
                        activeTabId = null, onSelectProject = {},
                        onCreateProject = { saved = it; true }, onRenameProject = { _, _ -> true },
                        onDeleteProject = {}, onAssignCurrentTab = {}, onSwitchTab = {}, onDismiss = {}
                    )
                }
            }
        }

        ui.onNodeWithText("New").performClick()
        ui.onNodeWithText("Workspace name").performTextReplacement(wanted + "z")
        ui.onNodeWithText("Save").performClick()
        ui.runOnIdle { assertEquals(wanted, saved) }
    }

    @Test fun sessionNameLimitCountsEmojiAsOneCharacter() {
        val wanted = "a".repeat(31) + "😀"
        var saved: String? = null
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                MaterialTheme {
                    KlyroRenameSessionDialog(name = "Fixture", onSave = { saved = it }, onDismiss = {})
                }
            }
        }

        ui.onNodeWithText("Session name").performTextReplacement(wanted + "z")
        ui.onNodeWithText("Save").performClick()
        ui.runOnIdle { assertEquals(wanted, saved) }
    }
}
