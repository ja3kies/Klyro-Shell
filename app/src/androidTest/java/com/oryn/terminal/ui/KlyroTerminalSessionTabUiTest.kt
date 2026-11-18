package com.oryn.terminal.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.unit.dp

/** Exercises the tab's actual accessibility node and separated actions. */
class KlyroTerminalSessionTabUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    @Test fun tappingSelectedTabOnlySelectsAndDoesNotOpenManagement() {
        var selected = 0
        var managed = 0
        showTab(active = true, unread = false, onSelect = { selected++ }, onManage = { managed++ })

        val tab = ui.onNodeWithContentDescription("Build, project General")
        tab.assertIsSelected().performClick()
        ui.runOnIdle {
            assertEquals(1, selected)
            assertEquals(0, managed)
        }
    }

    @Test fun tappingInactiveTabSelectsItWithoutOpeningManagement() {
        var selected = 0
        var managed = 0
        showTab(active = false, unread = true, onSelect = { selected++ }, onManage = { managed++ })

        val tab = ui.onNodeWithContentDescription("Build, project General")
        tab.assertIsNotSelected().performClick()
        ui.runOnIdle {
            assertEquals(1, selected)
            assertEquals(0, managed)
        }
    }

    @Test fun longPressOpensManagementForThisTab() {
        var managed = 0
        showTab(active = false, unread = false, onSelect = {}, onManage = { managed++ })

        ui.onNodeWithContentDescription("Build, project General")
            .performTouchInput { longClick() }
        ui.runOnIdle { assertEquals(1, managed) }
    }

    @Test fun customAccessibilityActionOpensManagementForThisTab() {
        var managed = 0
        showTab(active = true, unread = false, onSelect = {}, onManage = { managed++ })

        val action = ui.onNodeWithContentDescription("Build, project General")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "Manage session" }
        assertTrue(action.action())
        ui.runOnIdle { assertEquals(1, managed) }
    }

    @Test fun tabAnnouncesWorkspaceActivityAndUnreadStateWithoutChangingItsRole() {
        showTab(active = false, unread = true, onSelect = {}, onManage = {})

        val semantics = ui.onNodeWithContentDescription("Build, project General")
            .fetchSemanticsNode().config
        assertEquals(Role.Tab, semantics[SemanticsProperties.Role])
        assertEquals("Open terminal, Running, New terminal output",
            semantics[SemanticsProperties.StateDescription])
        assertTrue(semantics[SemanticsActions.CustomActions].any { it.label == "Manage session" })
        assertEquals("Select session", semantics[SemanticsActions.OnClick].label)
        assertEquals("Manage session", semantics[SemanticsActions.OnLongClick].label)
    }

    @Test fun tabKeepsMinimumTouchAreaForSmallTitles() {
        showTab(active = true, unread = false, onSelect = {}, onManage = {})

        ui.onNodeWithContentDescription("Build, project General")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    private fun showTab(
        active: Boolean,
        unread: Boolean,
        onSelect: () -> Unit,
        onManage: () -> Unit
    ) {
        ui.setContent {
            MaterialTheme {
                KlyroTerminalSessionTab(
                    title = "Build",
                    workspaceName = "General",
                    active = active,
                    activityTag = if (active) "JOB" else null,
                    activityAnnouncement = "Running",
                    showUnreadOutput = unread,
                    onSelect = onSelect,
                    onManage = onManage
                )
            }
        }
    }
}
