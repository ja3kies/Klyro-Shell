package com.oryn.terminal.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Reproduces a saved favorite that offered Edit/Remove but no direct reuse. */
class KlyroSavedAccentUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    private val prefs get() = ui.activity.getSharedPreferences("klyro_ui_theme", Context.MODE_PRIVATE)

    @After fun cleanup() {
        prefs.edit().remove("custom_favorites").commit()
    }

    @Test fun tappingSavedSwatchAppliesExactArgbAndClosesPicker() {
        prefs.edit().putString("custom_favorites",
            "#804488CC|Ocean\\n#FFBDCC64|Lime").commit()
        val applied = mutableStateOf<String?>(null)
        val visible = mutableStateOf(true)
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(true, applied.value)) {
                if (visible.value) KlyroColorPickerDialog(
                    initialArgb = AndroidColor.parseColor("#FFBDCC64"),
                    onApply = { applied.value = it; visible.value = false },
                    onDismiss = { visible.value = false }
                )
            }
        }
        ui.onNodeWithText("Opacity & saved colors").performClick()
        ui.onNodeWithTag("saved-color-use-#804488CC").performClick()
        ui.waitForIdle()
        ui.runOnIdle {
            assertEquals("custom:#804488CC", applied.value)
            assertFalse(visible.value)
            assertEquals(2, KlyroFavoriteColors.decode(
                prefs.getString("custom_favorites", null)).size)
        }
    }

    @Test fun editingFavoriteDoesNotApplyItUntilConfirmed() {
        prefs.edit().putString("custom_favorites", "#FF3377AA|Ocean").commit()
        val applied = mutableStateOf<String?>(null)
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(true, "lime")) {
                KlyroColorPickerDialog(AndroidColor.parseColor("#FFBDCC64"),
                    onApply = { applied.value = it }, onDismiss = {})
            }
        }
        ui.onNodeWithText("Opacity & saved colors").performClick()
        ui.onNodeWithText("Edit").performClick()
        ui.waitForIdle()
        ui.runOnIdle { assertNull(applied.value) }
        ui.onNodeWithText("Custom accent").assertExists()
    }
}
