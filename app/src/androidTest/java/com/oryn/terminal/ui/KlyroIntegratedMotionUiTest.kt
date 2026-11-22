package com.oryn.terminal.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.oryn.terminal.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class KlyroIntegratedMotionUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    @Test fun pointerActionsRemainCorrectAfterMotionPort() {
        var calls = 0
        val selected = mutableStateOf("light")
        ui.setContent {
            MaterialTheme {
                KlyroTransparentTouchFeedback {
                    CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                        Column {
                            KlyroActionRow(
                                label = "Open workspace",
                                icon = R.drawable.hugeicons_folder_01,
                                onClick = { calls++ }
                            )
                            KlyroActionRow(
                                label = "Unavailable action",
                                icon = R.drawable.hugeicons_folder_01,
                                enabled = false,
                                onClick = { calls += 100 }
                            )
                            KlyroThemePreviewCard(
                                mode = "dark", label = "Dark",
                                active = selected.value == "dark", accentId = "lime",
                                onClick = { selected.value = "dark" }
                            )
                        }
                    }
                }
            }
        }
        ui.onNodeWithContentDescription("Open workspace").performTouchInput { click() }
        ui.onNodeWithContentDescription("Unavailable action")
            .assertIsNotEnabled()
            .performTouchInput { click() }
        ui.onNodeWithTag("theme-preview-dark").performTouchInput { click() }
        ui.onNodeWithTag("theme-preview-dark").assertIsSelected()
        ui.runOnIdle {
            assertEquals(1, calls)
            assertEquals("dark", selected.value)
        }
    }
}
