package com.oryn.terminal.ui

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.oryn.terminal.KlyroHomePixelCover
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class KlyroHomeMotionUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

    private fun requireMotion() {
        assumeTrue(Settings.Global.getFloat(ui.activity.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f)
    }

    @Test fun visibleCheckerChangesPixelsAndPausedCheckerStaysStill() {
        requireMotion()
        val moving = mutableStateOf(true)
        val palette = KlyroColors.create(true, "lime")
        ui.mainClock.autoAdvance = false
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides palette) {
                Box(Modifier.width(192.dp).testTag("checker")) {
                    KlyroHomePixelCover(128.dp, palette.chrome, animate = moving.value)
                }
            }
        }
        fun frame() = ui.onNodeWithTag("checker").captureToImage().toPixelMap()
        ui.mainClock.advanceTimeBy(64)
        val first = frame()
        ui.mainClock.advanceTimeBy(1000)
        val second = frame()
        var changed = 0
        for (y in 0 until first.height step 4) {
            for (x in 0 until first.width step 4) {
                if (first[x, y] != second[x, y]) changed++
            }
        }
        assertTrue("Visible checker pixels must change", changed > 20)
        ui.runOnIdle { moving.value = false }
        ui.mainClock.advanceTimeBy(64)
        val paused = frame()
        ui.mainClock.advanceTimeBy(1000)
        val later = frame()
        for (y in 0 until paused.height step 4) {
            for (x in 0 until paused.width step 4) {
                assertEquals(paused[x, y], later[x, y])
            }
        }
    }

    @Test fun accentTransitionHasAnIntermediateFrameAndFinalColor() {
        requireMotion()
        val lime = KlyroColors.create(true, "lime")
        val cyan = KlyroColors.create(true, "cyan")
        val target = mutableStateOf(lime)
        var rendered = lime
        ui.mainClock.autoAdvance = false
        ui.setContent {
            val palette = rememberAnimatedKlyroPalette(target.value)
            SideEffect { rendered = palette }
        }
        ui.runOnIdle { target.value = cyan }
        ui.mainClock.advanceTimeBy(160)
        ui.runOnIdle {
            assertNotEquals(lime.accent, rendered.accent)
            assertNotEquals(cyan.accent, rendered.accent)
        }
        ui.mainClock.advanceTimeBy(500)
        ui.runOnIdle { assertEquals(cyan.accent, rendered.accent) }
    }
}
