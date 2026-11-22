package com.oryn.terminal.ui

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class KlyroAccentSwatchUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    @Test fun selectionChangesSilhouetteWithoutMovingTheTouchableTarget() {
        assumeTrue(Settings.Global.getFloat(ui.activity.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f)
        val selected = mutableStateOf("lime")
        ui.mainClock.autoAdvance = false
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides KlyroColors.create(true, selected.value)) {
                Row {
                    KlyroColors.primary.forEach { preset ->
                        KlyroAccentSwatch(
                            accent = KlyroColors.create(true, preset.id).accent,
                            active = selected.value == preset.id,
                            description = preset.label + " accent",
                            onClick = { selected.value = preset.id }
                        )
                    }
                }
            }
        }
        val node = ui.onNodeWithTag("accent-Orchid accent")
        ui.mainClock.advanceTimeBy(350)
        node.assertIsNotSelected()
        val initial = node.captureToImage().toPixelMap()
        node.performTouchInput { click() }
        ui.mainClock.advanceTimeBy(120)
        node.assertIsSelected()
        val middle = node.captureToImage().toPixelMap()
        ui.mainClock.advanceTimeBy(400)
        val final = node.captureToImage().toPixelMap()
        // Compare the edge band, excluding the centered glyph. A glyph-only change
        // cannot satisfy this check; the actual color-chip outline must transform.
        fun perimeterChanges(a: androidx.compose.ui.graphics.PixelMap,
                             b: androidx.compose.ui.graphics.PixelMap): Int {
            var count = 0
            for (y in 0 until a.height) for (x in 0 until a.width) {
                if (x in a.width / 3 until 2 * a.width / 3 &&
                    y in a.height / 3 until 2 * a.height / 3) continue
                if (a[x, y] != b[x, y]) count++
            }
            return count
        }
        assertTrue(initial.width == middle.width && middle.width == final.width)
        assertTrue(initial.height == middle.height && middle.height == final.height)
        assertTrue(perimeterChanges(initial, middle) > 10)
        assertTrue(perimeterChanges(middle, final) > 10)
        ui.onNodeWithTag("accent-Lime accent").assertIsNotSelected()
    }
}
