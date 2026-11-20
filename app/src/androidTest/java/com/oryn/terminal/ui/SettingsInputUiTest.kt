package com.oryn.terminal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsInputUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun savingAnExistingColorIdentifiesTheRenameAndDisablesRepeatSave() {
        val base = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val name = "color-fixture-${java.util.UUID.randomUUID()}"
        val prefs = base.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
        val isolated = object : android.content.ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) = prefs
        }
        try {
            prefs.edit().putString("custom_favorites", "#FF112233|Ocean").commit()
            ui.setContent {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides isolated,
                    LocalKlyroColors provides KlyroColors.create(false, "lime")) {
                    MaterialTheme {
                        KlyroColorPickerDialog(0xff112233.toInt(), onApply = {}, onDismiss = {})
                    }
                }
            }
            ui.onNodeWithText("Opacity & saved colors").performClick()
            ui.onNodeWithText("Rename “Ocean”").performScrollTo().performClick()
            ui.onNodeWithText("Color saved").assertIsNotEnabled()
            ui.onNodeWithContentDescription("Remove Custom, #FF112233").assertExists()
            ui.runOnIdle { assertEquals("#FF112233|Custom", prefs.getString("custom_favorites", null)) }
        } finally {
            base.deleteSharedPreferences(name)
        }
    }

    @Test fun rightToLeftHueTrackIsBlueOnItsLowXSide() {
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(false, "lime"),
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                MaterialTheme {
                    KlyroColorPickerDialog(0xffff0000.toInt(), onApply = {}, onDismiss = {})
                }
            }
        }
        val pixels = ui.onNodeWithContentDescription("Hue in degrees").captureToImage().toPixelMap()
        val sample = pixels[pixels.width / 4, pixels.height / 2]
        org.junit.Assert.assertTrue("Expected blue-violet near 270 degrees", sample.blue > sample.green)
    }

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
