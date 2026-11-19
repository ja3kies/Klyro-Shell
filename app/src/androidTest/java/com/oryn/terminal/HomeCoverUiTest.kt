package com.oryn.terminal

import android.Manifest
import android.graphics.BitmapFactory
import android.provider.Settings
import android.graphics.drawable.AnimatedImageDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Runs against the real Android settings provider and decoded GIF, without a PTY. */
@SdkSuppress(minSdkVersion = 29)
class HomeCoverUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()

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

    @Test fun shortCoverCropsDecodedPixelsWithoutSquashingMascot() = withAnimationScale { setScale ->
        setScale("0")
        ui.setContent {
            Box(Modifier.width(320.dp).height(148.dp).background(Color.Black).testTag("cover")) {
                KlyroHomePixelCover(148.dp, Color.Black)
            }
        }
        ui.waitUntil(5_000) {
            ui.onNodeWithTag("cover").captureToImage().toPixelMap()[0, 0] != Color.Black
        }
        val rendered = ui.onNodeWithTag("cover").captureToImage()
        val pixels = rendered.toPixelMap()
        val source = BitmapFactory.decodeResource(
            InstrumentationRegistry.getInstrumentation().targetContext.resources,
            R.drawable.klyro_home_landscape_loop
        )
        try {
            val scale = max(rendered.width.toFloat() / source.width, rendered.height.toFloat() / source.height)
            var mismatches = 0
            var samples = 0
            // Stay above the deliberate bottom fade. Compare against uniform center-crop.
            for (y in 2 until (rendered.height * .6f).toInt() step 3) {
                for (x in 2 until rendered.width - 2 step 3) {
                    val sx = ((x + .5f - rendered.width / 2f) / scale + source.width / 2f)
                        .toInt().coerceIn(0, source.width - 1)
                    val sy = ((y + .5f - rendered.height / 2f) / scale + source.height / 2f)
                        .toInt().coerceIn(0, source.height - 1)
                    if (pixels[x, y].toArgb() != source.getPixel(sx, sy)) mismatches++
                    samples++
                }
            }
            assertTrue("Uniform crop differs at $mismatches/$samples pixels", mismatches < samples * .03f)
        } finally {
            source.recycle()
        }
    }

    @Test fun animatedCoverStopsAndResumesWithComposeLifecycleOwner() = withAnimationScale { setScale ->
        setScale("1")
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this)
        }
        ui.runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        ui.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                KlyroHomePixelCover(148.dp, Color.Black)
            }
        }

        var drawable: AnimatedImageDrawable? = null
        ui.waitUntil(10_000) {
            drawable = findAnimatedImage(ui.activity.window.decorView)
            drawable != null && drawable!!.isRunning
        }
        ui.runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        ui.waitUntil(5_000) { drawable?.isRunning == false }
        ui.runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        ui.waitUntil(5_000) { drawable?.isRunning == true }
    }

    private fun findAnimatedImage(view: View): AnimatedImageDrawable? {
        if (view is ImageView) return view.drawable as? AnimatedImageDrawable
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findAnimatedImage(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}
