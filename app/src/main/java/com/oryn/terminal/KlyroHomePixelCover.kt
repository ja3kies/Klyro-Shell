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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.compose.ui.platform.LocalView
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import com.oryn.terminal.ui.LocalKlyroColors

internal const val HOME_PATTERN_PERIOD_MS = 20_000

/** Four tile translations per color cycle, so motion is visible within a second. */
internal fun homePatternOffsetCells(degrees: Float): Float = (degrees / 360f * 64f) % 16f

/** A full spin every five seconds, synchronized with the repeating translation. */
internal fun homePatternRotation(degrees: Float): Float = (degrees * 4f) % 360f

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
internal fun KlyroHomePixelCover(
    height: Dp,
    screenChrome: Color,
    animate: Boolean = true,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    illustration: HomeIllustrationSettings = rememberHomeIllustrationSettings().value
) {
    val accent = LocalKlyroColors.current.accent
    val motionEnabled = rememberHomeMotionEnabled()
    val started = rememberCoverStarted()
    val phase = if (animate && motionEnabled && started) key(illustration.kind, illustration.effectiveSpeed) {
        val transition = rememberInfiniteTransition(label = "home-pattern-pulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween((illustration.kind.periodMs / illustration.effectiveSpeed).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "home-pattern-phase"
        )
    } else null

    Box(Modifier.fillMaxWidth().height(height).clipToBounds()
        .then(if (onBoundsChanged != null) Modifier.onGloballyPositioned { coordinates ->
            coordinates.homePatternScreenBounds()?.let(onBoundsChanged)
        } else Modifier)
        .clearAndSetSemantics { }) {
        Box(Modifier.fillMaxSize().drawWithCache {
            val cell = 4.dp.toPx()
            val columns = ceil(size.width / cell).toInt() + 16
            val rows = ceil(size.height / cell).toInt() + 16
            val warmLionMask = if (illustration.kind == HomeIllustration.WARM_LION) {
                warmLionDotMask(size, density)
            } else null
            val pattern = Path()
            val tile = cell * 16f
            // Cache one repeat's centers. Rotate each individual square around its own
            // center; reuse one path per frame instead of transforming thousands of cells.
            val centers = (0 until 16).flatMap { row ->
                (0 until 16).filter { column -> homePatternColoredCell(column, row) }
                    .map { column -> (column + .5f) * cell to (row + .5f) * cell }
            }
            onDrawBehind {
                drawRect(Color.Black)
                val angle = phase?.value ?: 0f
                when (illustration.kind) {
                    HomeIllustration.MAYFLY -> {
                        drawHomeMayfly(accent, angle)
                        return@onDrawBehind
                    }
                    HomeIllustration.GRADIENT -> {
                        drawHomeGradient(accent, angle)
                        return@onDrawBehind
                    }
                    HomeIllustration.WARM_LION -> {
                        warmLionMask?.let { drawHomeWarmLion(accent, angle, it) }
                        return@onDrawBehind
                    }
                    HomeIllustration.CHECKERBOARD -> Unit
                }
                val offset = cell * homePatternOffsetCells(angle)
                val radians = homePatternRotation(angle).toDouble() * Math.PI / 180.0
                val half = cell / 2f
                val u = half * cos(radians).toFloat()
                val v = half * sin(radians).toFloat()
                pattern.reset()
                for ((x, y) in centers) {
                    pattern.moveTo(x - u + v, y - v - u)
                    pattern.lineTo(x + u + v, y + v - u)
                    pattern.lineTo(x + u - v, y + v + u)
                    pattern.lineTo(x - u - v, y - v + u)
                    pattern.close()
                }
                clipRect {
                    translate(left = -offset, top = -offset) {
                        // Include the adjacent repeats so rotating corners never leave
                        // a seam at a tile boundary or at the viewport edges.
                        for (row in -1..(rows / 16)) {
                            for (column in -1..(columns / 16)) {
                                translate(left = column * tile, top = row * tile) {
                                    drawPath(pattern, homePatternTint(accent, angle))
                                }
                            }
                        }
                    }
                }
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
    val lifecycle = LocalView.current.findViewTreeLifecycleOwner()?.lifecycle
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

/** One system-motion observer per app composition; standalone previews and tests can fall back. */
internal val LocalKlyroMotionEnabled = compositionLocalOf<Boolean?> { null }

/** React immediately when accessibility settings change. */
@Composable
internal fun rememberHomeMotionEnabled(): Boolean {
    LocalKlyroMotionEnabled.current?.let { return it }
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
