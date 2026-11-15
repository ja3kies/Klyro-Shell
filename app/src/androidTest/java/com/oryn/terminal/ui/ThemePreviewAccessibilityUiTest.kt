package com.oryn.terminal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ThemePreviewAccessibilityUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun themeCardGrowsWithLargeSystemFontScale() {
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 3f),
                LocalKlyroColors provides KlyroColors.create(false, "lime")
            ) {
                MaterialTheme {
                    KlyroThemePreviewCard(
                        mode = "system",
                        label = "System",
                        active = false,
                        accentId = "lime",
                        onClick = {}
                    )
                }
            }
        }

        ui.onNodeWithTag("theme-preview-system").assertHeightIsAtLeast(90.dp)
    }
}
