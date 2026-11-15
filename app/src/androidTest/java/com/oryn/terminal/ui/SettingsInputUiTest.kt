package com.oryn.terminal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsInputUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun fontSliderReportsSelectedSizeNotDelta() {
        var selectedSize = 13.5f
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                MaterialTheme {
                    KlyroSettingsSheet(
                        initialSection = "appearance", themeMode = "system",
                        onThemeModeChange = {}, accentId = "lime", onAccentIdChange = {},
                        fontSizeSp = selectedSize, fontFamilyId = "default",
                        onFontFamilyChange = {}, onFontChange = { selectedSize = it },
                        onResetFont = {}, onResetModifiers = {}, activeModifiers = emptySet(),
                        onSetupStorage = {}, onImportStorageFolder = {}, onImportFile = {},
                        onExportFile = {}, diagnosticReport = "", onRecheckEnvironment = {},
                        onCopyDiagnostic = {}, onShareDiagnostic = {}, onDismiss = {}
                    )
                }
            }
        }

        ui.onNodeWithContentDescription("Terminal font size").performSemanticsAction(
            SemanticsActions.SetProgress
        ) { setProgress -> setProgress(20f) }

        ui.runOnIdle { assertEquals(20f, selectedSize) }
    }

    @Test fun hexDoneActionHidesSoftwareKeyboard() {
        var hideRequests = 0
        val keyboard = object : SoftwareKeyboardController {
            override fun show() = Unit
            override fun hide() { hideRequests++ }
        }
        ui.setContent {
            CompositionLocalProvider(
                LocalKlyroColors provides KlyroColors.create(false, "lime"),
                LocalSoftwareKeyboardController provides keyboard
            ) {
                MaterialTheme {
                    KlyroColorPickerDialog(0xff112233.toInt(), onApply = {}, onDismiss = {})
                }
            }
        }

        ui.onNodeWithContentDescription("Hex color value").performImeAction()
        ui.runOnIdle { assertEquals(1, hideRequests) }
    }
}
