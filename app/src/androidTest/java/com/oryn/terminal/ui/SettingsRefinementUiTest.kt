package com.oryn.terminal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalLayoutApi::class)
class SettingsRefinementUiTest {
    @get:Rule val ui = createComposeRule()

    @Composable private fun Fixture(
        initialSection: String,
        fontSize: Float = 13.5f,
        themeMode: String = "light",
        onThemeModeChange: (String) -> Unit = {},
        onFontChange: (Float) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        KlyroSettingsSheet(
            initialSection = initialSection, themeMode = themeMode, onThemeModeChange = onThemeModeChange,
            accentId = "cyan", onAccentIdChange = {}, fontSizeSp = fontSize,
            fontFamilyId = "system", onFontFamilyChange = {}, onFontChange = onFontChange,
            onResetFont = {}, onResetModifiers = {}, activeModifiers = emptySet(),
            onSetupStorage = {}, onImportStorageFolder = {}, onImportFile = {}, onExportFile = {},
            diagnosticReport = "Fixture report", onRecheckEnvironment = {}, onCopyDiagnostic = {},
            onShareDiagnostic = {}, onDismiss = onDismiss
        )
    }

    @Test fun largeTextThemeChoicesStackAndRemainClickable() {
        var chosen = ""
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme {
                    Box(Modifier.width(280.dp)) {
                        KlyroSettingsChoiceGrid(columns = 3) {
                            listOf("system", "light", "dark").forEach { mode ->
                                KlyroThemePreviewCard(mode, mode, false, "cyan",
                                    modifier = Modifier.weight(1f), onClick = { chosen = mode })
                            }
                        }
                    }
                }
            }
        }
        val first = ui.onNodeWithTag("theme-preview-system").fetchSemanticsNode().boundsInRoot
        val second = ui.onNodeWithTag("theme-preview-light").fetchSemanticsNode().boundsInRoot
        assertTrue(second.top >= first.bottom)
        ui.onNodeWithTag("theme-preview-dark").assertIsDisplayed().performClick()
        ui.runOnIdle { assertEquals("dark", chosen) }
        assertEquals(0, ui.onAllNodesWithText("› _").fetchSemanticsNodes().size)
    }

    @Test fun folderLoadingFailureEmptyAndConnectedAreDistinct() {
        val count = mutableStateOf(-1)
        ui.setContent { MaterialTheme { KlyroFolderAccessSummary(count.value) } }
        ui.onNodeWithText("Checking connected folders…").assertIsDisplayed()
        ui.onNodeWithText("No folder connected").assertDoesNotExist()
        ui.runOnIdle { count.value = -2 }
        ui.onNodeWithText("Folder status unavailable").assertIsDisplayed()
        ui.onNodeWithText("No folder connected").assertDoesNotExist()
        ui.runOnIdle { count.value = 0 }
        ui.onNodeWithText("No folder connected").assertIsDisplayed()
        ui.runOnIdle { count.value = 1 }
        ui.onNodeWithText("1 folder connected").assertIsDisplayed()
    }

    @Test fun fontPresetUsesExistingChangeCallbackAndSelectedState() {
        val size = mutableStateOf(13.5f)
        ui.setContent {
            MaterialTheme { Fixture("appearance", fontSize = size.value, onFontChange = { size.value = it }) }
        }
        ui.onNodeWithContentDescription("Font size 16 sp").performScrollTo().performClick()
        ui.onNodeWithContentDescription("Font size 16 sp").assertIsSelected()
        ui.runOnIdle { assertEquals(16f, size.value) }
    }

    @Test fun foldedMonochromeChoicesKeepExistingThemesAndCurrentValue() {
        val mode = mutableStateOf("light")
        ui.setContent {
            MaterialTheme {
                Fixture("appearance", themeMode = mode.value, onThemeModeChange = { mode.value = it })
            }
        }
        ui.onNodeWithTag("theme-preview-mono-black").assertDoesNotExist()
        ui.onNodeWithContentDescription("Monochrome themes").performScrollTo().performClick()
        ui.onNodeWithTag("theme-preview-mono-black").performScrollTo().performClick()
        ui.onNodeWithTag("theme-preview-mono-black").assertIsSelected()
        ui.runOnIdle { assertEquals("mono-black", mode.value) }
        ui.onNodeWithContentDescription("Monochrome themes").performScrollTo().performClick()
        ui.onNodeWithTag("theme-preview-mono-black").assertDoesNotExist()
        ui.onNodeWithText("Monochrome · OLED").assertIsDisplayed()
        ui.onNodeWithContentDescription("Choose custom accent").assertDoesNotExist()
    }

    @Test fun accentTargetsKeepTheirSizeInNarrowAppearance() {
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density * 2f, 1f)) {
                MaterialTheme { Fixture("appearance") }
            }
        }
        for (label in listOf("Lime accent", "Cyan accent", "Coral accent", "Orchid accent", "Choose custom accent")) {
            ui.onNodeWithContentDescription(label).performScrollTo()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
    }

    @Test fun labelledBackReturnsToRootAndCloseDismissesExactlyOnce() {
        var closes = 0
        val visible = mutableStateOf(true)
        ui.setContent {
            MaterialTheme { if (visible.value) Fixture("home", onDismiss = { closes++; visible.value = false }) }
        }
        ui.onNodeWithContentDescription("Appearance").performClick()
        ui.onNodeWithText("App theme").assertIsDisplayed()
        ui.onNodeWithText("Settings").performClick()
        ui.onNodeWithContentDescription("Appearance").assertIsDisplayed()
        ui.onNodeWithText("Close").performClick()
        ui.runOnIdle { assertEquals(1, closes) }
        ui.onNodeWithTag("klyro-modal-sheet").assertDoesNotExist()
    }
}
