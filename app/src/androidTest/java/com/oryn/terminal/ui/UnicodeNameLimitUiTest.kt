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

class UnicodeNameLimitUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun workspaceEditorAcceptsThirtyTwoCodePointsIncludingBoundaryEmoji() {
        val expected = "a".repeat(31) + "😀"
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
        ui.onNodeWithText("Workspace name").performTextReplacement(expected + "z")
        ui.onNodeWithText("Save").performClick()
        ui.runOnIdle { assertEquals(expected, saved) }
    }

    @Test fun workspaceRenameSavesTheSameTrimmedNameShownByTheCatalog() {
        val original = KlyroWorkspaceCatalog.Entry("12345678-1234-1234-1234-123456789012", "Work")
        var saved: String? = null
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                MaterialTheme {
                    KlyroWorkspaceSheet(entries = listOf(
                        KlyroWorkspaceCatalog.Entry("general", "General"), original
                    ), selectedId = original.id, tabs = emptyList(), tabWorkspaces = emptyMap(),
                        activeTabId = null, onSelectProject = {}, onCreateProject = { false },
                        onRenameProject = { _, name -> saved = KlyroWorkspaceCatalog.cleanName(name); true },
                        onDeleteProject = {}, onAssignCurrentTab = {}, onSwitchTab = {}, onDismiss = {})
                }
            }
        }
        ui.onNodeWithText("Work").performClick()
        ui.onNodeWithText("Rename workspace").performClick()
        ui.onNodeWithText("Workspace name").performTextReplacement("  New work  ")
        ui.onNodeWithText("Save").performClick()
        ui.runOnIdle { assertEquals("New work", saved) }
    }

    @Test fun sessionRenameSavesSameSanitizedNameUsedBySessionModel() {
        var saved: String? = null
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                MaterialTheme {
                    KlyroRenameSessionDialog(name = "Fixture",
                        onSave = { saved = KlyroWorkspaceCatalog.cleanName(it) }, onDismiss = {})
                }
            }
        }
        ui.onNodeWithText("Session name").performTextReplacement("  New name  ")
        ui.onNodeWithText("Save").performClick()
        ui.runOnIdle { assertEquals("New name", saved) }
    }

    @Test fun sessionRenameAcceptsThirtyTwoCodePointsIncludingBoundaryEmoji() {
        val expected = "a".repeat(31) + "😀"
        var saved: String? = null
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                MaterialTheme {
                    KlyroRenameSessionDialog(name = "Fixture", onSave = { saved = it }, onDismiss = {})
                }
            }
        }

        ui.onNodeWithText("Session name").performTextReplacement(expected + "z")
        ui.onNodeWithText("Save").performClick()
        ui.runOnIdle { assertEquals(expected, saved) }
    }
}
