package com.oryn.terminal.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The quiet-touch theme must preserve interactive and accessible controls. */
@OptIn(ExperimentalMaterial3Api::class)
class KlyroTransparentTouchFeedbackUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    @Test fun noVisibleRippleDoesNotDisableMaterialOrFoundationActions() {
        var foundationClicks = 0
        var materialClicks = 0
        var rippleColor: Color? = null
        ui.setContent {
            MaterialTheme {
                KlyroTransparentTouchFeedback {
                    rippleColor = LocalRippleConfiguration.current?.color
                    Column {
                        Box(
                            modifier = Modifier.size(56.dp)
                                .testTag("quiet-foundation-action")
                                .clickable(role = Role.Button, onClickLabel = "Open plugins") {
                                    foundationClicks++
                                }
                        ) {
                            Text("Plugins")
                        }
                        TextButton(onClick = { materialClicks++ }) {
                            Text("Settings")
                        }
                    }
                }
            }
        }
        ui.onNodeWithTag("quiet-foundation-action")
            .assertHasClickAction()
            .performClick()
        ui.onNodeWithText("Settings")
            .assertHasClickAction()
            .performClick()
        ui.runOnIdle {
            assertEquals(Color.Transparent, rippleColor)
            assertEquals(1, foundationClicks)
            assertEquals(1, materialClicks)
        }
    }
}
