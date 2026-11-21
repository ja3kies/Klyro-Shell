// From Uiverse.io by moin_7638 - https://uiverse.io/moin_7638/stupid-grasshopper-46
// MIT License, Copyright (c) moin_7638 (Jami)
package com.oryn.terminal

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.ceil
import kotlin.math.cos
import com.oryn.terminal.ui.LocalKlyroColors

internal const val HOME_PATTERN_PERIOD_MS = 20_000

/** CSS conic layers, sampled on a 0.25em grid (4dp at the original 16px em).
 * The 4em tile is 16 cells wide; each repeating-conic tile is two cells wide.
 * CSS conic gradients start at the top and advance clockwise.
 */
internal fun homePatternColoredCell(column: Int, row: Int): Boolean {
    val right = column % 16 >= 8
    val bottom = row % 16 >= 8
    return when {
        right && bottom -> true
        !right && !bottom -> false
        else -> (column % 2) != (row % 2)
    }
}

/** Animate luminance within the selected theme, without rotating into unrelated hues. */
internal fun homePatternTint(accent: Color, degrees: Float): Color {
    val intensity = (.63 + .21 * cos(degrees.toDouble() * Math.PI / 180.0)).toFloat()
    return Color(
        red = accent.red * intensity,
        green = accent.green * intensity,
        blue = accent.blue * intensity,
    )
}

/** Native replacement for the old GIF, preserving Home's existing banner layout. */
@Composable
internal fun KlyroHomePixelCover(height: Dp, screenChrome: Color, animate: Boolean = true) {
    val accent = LocalKlyroColors.current.accent
    val motionEnabled = rememberHomeMotionEnabled()
    val started = rememberCoverStarted()
    val phase = if (animate && motionEnabled && started) {
        val transition = rememberInfiniteTransition(label = "home-pattern-pulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(HOME_PATTERN_PERIOD_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "home-pattern-phase"
        )
    } else null

    Box(Modifier.fillMaxWidth().height(height).clearAndSetSemantics { }) {
        Box(Modifier.fillMaxSize().drawWithCache {
            val cell = 4.dp.toPx()
            val columns = ceil(size.width / cell).toInt()
            val rows = ceil(size.height / cell).toInt()
            val pattern = Path()
            // Geometry is cached. Only the color changes on animation frames.
            // Merge neighboring colored cells horizontally to reduce path complexity.
            for (row in 0 until rows) {
                var column = 0
                while (column < columns) {
                    if (!homePatternColoredCell(column, row)) {
                        column++
                        continue
                    }
                    val first = column
                    while (column < columns && homePatternColoredCell(column, row)) column++
                    pattern.addRect(Rect(
                        first * cell, row * cell,
                        (column * cell).coerceAtMost(size.width),
                        ((row + 1) * cell).coerceAtMost(size.height)
                    ))
                }
            }
            onDrawBehind {
                drawRect(Color.Black)
                drawPath(pattern, homePatternTint(accent, phase?.value ?: 0f))
            }
        })

        // Preserve the existing fade into the Home content and navigation.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .45f),
                    .16f to Color.Transparent,
                    .50f to Color.Transparent,
                    .74f to screenChrome.copy(alpha = .28f),
                    .90f to screenChrome.copy(alpha = .76f),
                    1f to screenChrome
                )
            )
        }
    }
}

/** Stop decorative animation when the hosting screen is no longer started. */
@Composable
private fun rememberCoverStarted(): Boolean {
    val lifecycle = (LocalContext.current as? LifecycleOwner)?.lifecycle
    var started by remember(lifecycle) {
        mutableStateOf(lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: false)
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { owner, _ ->
            started = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle?.addObserver(observer)
        started = lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: false
        onDispose { lifecycle?.removeObserver(observer) }
    }
    return started
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
