package com.oryn.terminal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import com.oryn.terminal.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Motion changes must never block actions or change semantic contracts.
 * Uses Compose interaction tests rather than animation screenshots.
 */
class KlyroMotionUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun sharedActionStillExecutesExactlyOnce() {
        var calls = 0
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                MaterialTheme {
                    KlyroActionRow(
                        label = "Open workspace", icon = R.drawable.hugeicons_folder_01,
                        onClick = { calls++ }
                    )
                }
            }
        }
        ui.onNodeWithContentDescription("Open workspace").performClick()
        ui.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun themePreviewPressStillSelectsRequestedTheme() {
        var selected by mutableStateOf("light")
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                MaterialTheme {
                    Column {
                        KlyroThemePreviewCard(
                            mode = "light", label = "Light", active = selected == "light",
                            accentId = "lime", onClick = { selected = "light" }
                        )
                        KlyroThemePreviewCard(
                            mode = "dark", label = "Dark", active = selected == "dark",
                            accentId = "lime", onClick = { selected = "dark" }
                        )
                    }
                }
            }
        }
        ui.onNodeWithTag("theme-preview-dark").performClick()
        ui.onNodeWithTag("theme-preview-dark").assertIsSelected()
        ui.runOnIdle { assertEquals("dark", selected) }
    }

    @Test fun animatedSliderPreservesExactValueAndProgressSemantics() {
        var current by mutableStateOf(13f)
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                MaterialTheme {
                    KlyroThinSlider(
                        value = current, onValueChange = { current = it },
                        valueRange = 10f..24f, modifier = Modifier.testTag("motion-font-slider")
                    )
                }
            }
        }
        ui.onNodeWithTag("motion-font-slider").performSemanticsAction(
            SemanticsActions.SetProgress
        ) { setProgress -> setProgress(20f) }
        ui.runOnIdle { assertEquals(20f, current, 0f) }
    }
}
