package com.oryn.terminal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KlyroKeyboardPagerIndicatorUiTest {
    @get:Rule val ui = createComposeRule()

    private lateinit var pager: PagerState

    private fun render(motionEnabled: Boolean = true) {
        ui.setContent {
            pager = rememberPagerState(pageCount = { 2 })
            Column {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.width(300.dp).height(80.dp).testTag("keyboard-pages")
                ) { page ->
                    Text("Page ${page + 1}")
                }
                KlyroKeyboardPagerIndicator(
                    pagerState = pager, accent = Color.Cyan,
                    muted = Color.Gray, motionEnabled = motionEnabled
                )
            }
        }
    }

    @Test fun indicatorsReplaceVisualKeysAndCommandsLabels() {
        render()
        ui.onNodeWithTag("terminal-keyboard-pager-indicator").assertExists()
        ui.onNodeWithText("Keys").assertDoesNotExist()
        ui.onNodeWithText("Commands").assertDoesNotExist()
        ui.onNodeWithContentDescription("Show terminal keys").assertIsSelected()
    }

    @Test fun accessibleDotsCanSwitchPagesWithoutMotion() {
        render(motionEnabled = false)
        ui.onNodeWithContentDescription("Show pinned commands").performClick()
        ui.waitForIdle()
        ui.runOnIdle { assertEquals(1, pager.currentPage) }
        ui.onNodeWithContentDescription("Show pinned commands").assertIsSelected()
        ui.onNodeWithContentDescription("Show terminal keys").performClick()
        ui.waitForIdle()
        ui.runOnIdle { assertEquals(0, pager.currentPage) }
    }

    @Test fun swipingPagerUpdatesTheActiveDot() {
        render()
        ui.onNodeWithTag("keyboard-pages").performTouchInput { swipeLeft() }
        ui.waitForIdle()
        ui.runOnIdle { assertEquals(1, pager.currentPage) }
        ui.onNodeWithContentDescription("Show pinned commands").assertIsSelected()
    }
}
