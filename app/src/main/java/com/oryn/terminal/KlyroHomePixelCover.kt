package com.oryn.terminal

import android.annotation.TargetApi
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun KlyroHomePixelCover(height: Dp, screenChrome: Color) {
    val motionEnabled = rememberHomeMotionEnabled()

    Box(Modifier.fillMaxWidth().height(height).clearAndSetSemantics { }) {
        if (motionEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            KlyroAnimatedGifCover()
        } else {
            KlyroStaticGifCover()
        }

        // The Tumblr-style fade is identical for animated and still artwork.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    .50f to Color.Transparent,
                    .74f to screenChrome.copy(alpha = .28f),
                    .90f to screenChrome.copy(alpha = .76f),
                    1f to screenChrome
                )
            )
        }
    }
}

/** React immediately when accessibility settings change while Home is visible. */
@Composable
internal fun rememberHomeMotionEnabled(): Boolean {
    val context = LocalContext.current
    fun readMotionEnabled() = runCatching {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) > 0f
    }.getOrDefault(false)
    var enabled by remember(context) { mutableStateOf(readMotionEnabled()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = readMotionEnabled()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false, observer
        )
        // Close the read/register race without waiting for a second change.
        enabled = readMotionEnabled()
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}

/** API 28+ native GIF decoder, isolated from the Android 8 fallback for lint. */
@TargetApi(Build.VERSION_CODES.P)
@Composable
private fun KlyroAnimatedGifCover() {
    val context = LocalContext.current
    val decoded by produceState<AnimatedImageDrawable?>(null, context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(
                        context.resources, R.drawable.klyro_home_landscape_loop
                    )
                ) as? AnimatedImageDrawable
            }.getOrNull()?.apply {
                setFilterBitmap(false)
                repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            }
        }
    }
    val animated = decoded

    if (animated == null) {
        KlyroStaticGifCover()
        return
    }

    AndroidView(
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setImageDrawable(animated)
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    // A hidden screen or stopped Activity must not keep decoding GIF frames.
    val lifecycle = (context as? LifecycleOwner)?.lifecycle
    DisposableEffect(animated, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> animated.start()
                Lifecycle.Event.ON_STOP -> animated.stop()
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        if (lifecycle == null || lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) animated.start() else animated.stop()

        onDispose {
            lifecycle?.removeObserver(observer)
            animated.stop()
        }
    }
}

/** The same GIF's first frame is shown on Android 8 or with reduced motion. */
@Composable
private fun KlyroStaticGifCover() {
    val context = LocalContext.current
    val decoded by produceState<Bitmap?>(null, context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeResource(
                    context.resources, R.drawable.klyro_home_landscape_loop
                )
            }.getOrNull()
        }
    }
    val still = decoded
    if (still != null) {
        Image(
            bitmap = still.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize()
        )
    }
}
