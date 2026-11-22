package com.oryn.terminal

import android.Manifest
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.oryn.terminal.ui.KlyroColors
import com.oryn.terminal.ui.LocalKlyroColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Runs against the real Android settings provider and native illustrations, without a PTY. */
@SdkSuppress(minSdkVersion = 29)
class HomeCoverUiTest {
    @get:Rule val ui = createComposeRule()

    private fun withAnimationScale(block: (setScale: (String) -> Unit) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val key = Settings.Global.ANIMATOR_DURATION_SCALE
        val original = Settings.Global.getString(resolver, key)
        instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.WRITE_SECURE_SETTINGS)
        try {
            block { value -> assertTrue(Settings.Global.putString(resolver, key, value)) }
        } finally {
            try {
                Settings.Global.putString(resolver, key, original)
            } finally {
                instrumentation.uiAutomation.dropShellPermissionIdentity()
            }
        }
    }

    @Test fun reducedMotionChangesWithoutRecreatingHome() = withAnimationScale { setScale ->
        val observed = AtomicReference<Boolean?>(null)
        setScale("0")
        ui.setContent {
            val enabled = rememberHomeMotionEnabled()
            SideEffect { observed.set(enabled) }
        }
        ui.waitUntil(5_000) { observed.get() == false }
        setScale("1")
        ui.waitUntil(5_000) { observed.get() == true }
        setScale("0")
        ui.waitUntil(5_000) { observed.get() == false }
    }

    @Test fun allHomeIllustrationsStayInsideBannerAndAnimateIndependently() = withAnimationScale { setScale ->
        setScale("1")
        val pattern = mutableStateOf(HomeIllustration.CHECKERBOARD)
        val palette = mutableStateOf(KlyroColors.create(true, "cyan"))
        val moving = mutableStateOf(true)
        val chrome = Color(0xFF172333)
        ui.mainClock.autoAdvance = false
        ui.setContent {
            CompositionLocalProvider(LocalKlyroColors provides palette.value,
                LocalKlyroMotionEnabled provides true) {
                Box(Modifier.width(240.dp).height(260.dp).background(chrome).testTag("home")) {
                    KlyroHomePixelCover(148.dp, chrome, animate = moving.value,
                        illustration = HomeIllustrationSettings(kind = pattern.value))
                }
            }
        }
        fun frame() = ui.onNodeWithTag("home").captureToImage().toPixelMap()
        fun changed(a: androidx.compose.ui.graphics.PixelMap, b: androidx.compose.ui.graphics.PixelMap): Int {
            var count = 0
            for (y in 0 until (a.height * .32f).toInt() step 4)
                for (x in 0 until a.width step 4)
                    if (a[x, y] != b[x, y]) count++
            return count
        }
        for (kind in HomeIllustration.entries) {
            ui.runOnIdle {
                pattern.value = kind
                palette.value = KlyroColors.create(true, "cyan")
                moving.value = true
            }
            ui.mainClock.advanceTimeBy(64)
            val before = frame()
            ui.mainClock.advanceTimeBy(1500)
            val after = frame()
            assertTrue("$kind must move when visible", changed(before, after) > 20)
            for (y in (after.height * .60f).toInt() until after.height step 4)
                for (x in 0 until after.width step 4)
                    assertEquals("$kind leaked outside the banner at $x,$y", chrome, after[x, y])
            ui.runOnIdle { moving.value = false }
            ui.mainClock.advanceTimeBy(64)
            val still = frame()
            ui.mainClock.advanceTimeBy(1500)
            assertEquals("$kind must stop when hidden", 0, changed(still, frame()))
            ui.runOnIdle { palette.value = KlyroColors.create(true, "lime") }
            ui.mainClock.advanceTimeBy(64)
            assertTrue("$kind must adapt to its theme", changed(still, frame()) > 20)
        }
    }
}
