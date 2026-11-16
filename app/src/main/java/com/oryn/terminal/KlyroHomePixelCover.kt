package com.oryn.terminal

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable
internal fun KlyroHomePixelCover(height: Dp, screenChrome: Color) {
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) > 0f
        }.getOrDefault(false)
    }

    // Android 9+ decodes the actual GIF on a framework decoder thread.
    // Older Android and reduced-motion users retain a crisp, static scene.
    val animated = remember(context, motionEnabled) {
        if (motionEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(
                        context.resources, R.drawable.klyro_home_landscape_loop
                    )
                ) as? AnimatedImageDrawable
            }.getOrNull()?.apply {
                setFilterBitmap(false) // Preserve hard pixel edges when scaled.
                repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            }
        } else null
    }

    Box(Modifier.fillMaxWidth().height(height).clearAndSetSemantics { }) {
        if (animated != null) {
            AndroidView(
                factory = { viewContext ->
                    ImageView(viewContext).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        setImageDrawable(animated)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Do not keep decoding frames after Home is left or the Activity stops.
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
                if (lifecycle == null ||
                    lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                ) animated.start()
                onDispose {
                    lifecycle?.removeObserver(observer)
                    animated.stop()
                }
            }
        } else {
            // Decode one frame without scheduling an animation on older Android
            // or when the system has requested reduced motion.
            val still = remember(context) {
                runCatching {
                    BitmapFactory.decodeResource(
                        context.resources, R.drawable.klyro_home_landscape_loop
                    )
                }.getOrNull()
            }
            if (still != null) {
                Image(
                    bitmap = still.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Canvas(Modifier.fillMaxSize()) { drawRect(screenChrome) }
            }
        }

        // Tumblr-like banner fade is identical in animated and static modes.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    .66f to Color.Transparent,
                    1f to screenChrome
                )
            )
        }
    }
}

