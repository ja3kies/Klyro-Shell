package com.oryn.terminal

import android.provider.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.oryn.terminal.ui.LocalKlyroColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// A deliberately small, authored pixel palette. No generated or scaled bitmap.
private val night = Color(0xFF081F2A)
private val distantRidge = Color(0xFF1B4C57)
private val middleRidge = Color(0xFF123D48)
private val foreground = Color(0xFF0D303A)
private val outline = Color(0xFF09222D)
private val fur = Color(0xFF66878A)
private val muzzle = Color(0xFFDFE6DE)
private val windowLight = Color(0xFFE2B66A)

@Composable
internal fun KlyroHomePixelCover(height: Dp, screenChrome: Color) {
    val accent = LocalKlyroColors.current.accent
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) > 0f
        }.getOrDefault(false)
    }

    Box(Modifier.fillMaxWidth().height(height).clearAndSetSemantics { }) {
        // Draw in whole device pixels; the scene is independent of bitmap resolution.
        Canvas(Modifier.fillMaxSize()) { drawLandscape(accent) }

        if (motionEnabled) {
            val transition = rememberInfiniteTransition(label = "Klyro cover")
            val shimmer by transition.animateFloat(
                initialValue = .16f, targetValue = .72f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3600), repeatMode = RepeatMode.Reverse
                ),
                label = "Pixel stars"
            )
            Canvas(Modifier.fillMaxSize()) {
                val unit = size.width / 160f
                val points = arrayOf(21f to 15f, 48f to 23f, 93f to 13f, 142f to 19f)
                for ((x, y) in points) pixel(unit, x, y, 1f, 1f,
                    accent.copy(alpha = shimmer))
            }
        }

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

private fun DrawScope.pixel(
    unit: Float, x: Float, y: Float, width: Float, height: Float, color: Color
) {
    val left = (x * unit).roundToInt().toFloat()
    val top = (y * unit).roundToInt().toFloat()
    val right = ((x + width) * unit).roundToInt().toFloat()
    val bottom = ((y + height) * unit).roundToInt().toFloat()
    drawRect(color, Offset(left, top),
        Size(max(1f, right - left), max(1f, bottom - top)))
}

private fun DrawScope.ridge(
    unit: Float, horizon: Float, peaks: FloatArray, slope: Float, color: Color
) {
    val bottom = size.height / unit
    for (x in 0 until 160 step 2) {
        var height = 0f
        for (index in peaks.indices step 2) {
            height = max(height, peaks[index + 1] - abs(x - peaks[index]) * slope)
        }
        val y = horizon - height.coerceAtLeast(0f)
        pixel(unit, x.toFloat(), y, 2f, bottom - y, color)
    }
}

private fun DrawScope.drawLandscape(accent: Color) {
    val unit = size.width / 160f
    val h = size.height / unit
    drawRect(night)

    // Few, intentional stars; no random glitter or bloom effects.
    val stars = arrayOf(7f to 17f, 31f to 11f, 59f to 21f, 82f to 9f,
        103f to 27f, 137f to 11f, 151f to 31f)
    for ((x, y) in stars) pixel(unit, x, y, 1f, 1f, muzzle.copy(alpha = .75f))
    drawCrescent(unit, 105f, h * .16f, accent)

    ridge(unit, h * .69f, floatArrayOf(14f, 15f, 51f, 27f, 85f, 34f,
        128f, 23f, 156f, 16f), .65f, distantRidge)
    ridge(unit, h * .75f, floatArrayOf(0f, 19f, 36f, 27f, 72f, 17f,
        106f, 28f, 152f, 18f), .85f, middleRidge)

    // The horizon, shore and cottage are intentionally low-detail.
    pixel(unit, 0f, h * .73f, 160f, h * .27f, foreground)
    for (x in 0 until 160 step 10) {
        val top = h * .71f - if (x % 20 == 0) 6f else 3f
        pixel(unit, x.toFloat(), top, 2f, h * .73f - top, outline)
    }
    for ((x, w, shift) in arrayOf(
        Triple(6f, 30f, 1f), Triple(45f, 23f, 4f),
        Triple(80f, 36f, 8f), Triple(115f, 21f, 12f)
    )) {
        pixel(unit, x, h * .8f + shift, w, 1f, distantRidge)
    }
    drawCabin(unit, 23f, h * .73f)

    // Klyro's signature: a geometric horned fox, not a generic animal render.
    drawKlyroMascot(unit, 117f, h * .74f, accent)
}

private fun DrawScope.drawCrescent(unit: Float, x: Float, y: Float, accent: Color) {
    val rows = arrayOf(
        "...XXXX...", "..XXXXXX..", ".XXXXXXXX.", ".XXXXXXXX.",
        ".XXXXXXX..", ".XXXXXX...", "..XXXXX...", "...XXXX..."
    )
    for ((dy, row) in rows.withIndex()) for ((dx, ch) in row.withIndex()) {
        if (ch == 'X') pixel(unit, x + dx, y + dy, 1f, 1f,
            accent.copy(alpha = .8f))
    }
    // Carve a crescent with the sky color, retaining a hard pixel edge.
    for (dy in 0..5) {
        pixel(unit, x + 5f + if (dy < 2) 1f else 0f,
            y + dy, 5f, 1f, night)
    }
}

private fun DrawScope.drawCabin(unit: Float, x: Float, ground: Float) {
    pixel(unit, x + 2f, ground - 11f, 12f, 11f, outline)
    pixel(unit, x, ground - 12f, 16f, 2f, distantRidge)
    pixel(unit, x + 3f, ground - 14f, 10f, 2f, middleRidge)
    pixel(unit, x + 5f, ground - 7f, 3f, 4f, windowLight)
    pixel(unit, x + 10f, ground - 7f, 2f, 4f, windowLight)
}

private fun DrawScope.drawKlyroMascot(
    unit: Float, x: Float, ground: Float, accent: Color
) {
    // Tail, boots and coat form a single readable silhouette at 1x scale.
    pixel(unit, x - 9f, ground - 12f, 6f, 4f, muzzle)
    pixel(unit, x - 12f, ground - 15f, 5f, 4f, fur)
    pixel(unit, x - 6f, ground - 9f, 7f, 4f, fur)
    pixel(unit, x + 3f, ground - 4f, 4f, 5f, outline)
    pixel(unit, x + 12f, ground - 4f, 4f, 5f, outline)
    pixel(unit, x + 1f, ground - 18f, 17f, 15f, fur)
    pixel(unit, x + 6f, ground - 15f, 12f, 10f, middleRidge)

    // Two swept horns and a pointed ear distinguish the Klyro mascot.
    pixel(unit, x + 7f, ground - 28f, 3f, 9f, muzzle)
    pixel(unit, x + 5f, ground - 30f, 3f, 3f, muzzle)
    pixel(unit, x + 17f, ground - 29f, 3f, 8f, muzzle)
    pixel(unit, x + 19f, ground - 31f, 3f, 3f, muzzle)
    pixel(unit, x + 22f, ground - 27f, 4f, 9f, fur)

    // Angular head, cheek and muzzle. One cyan eye is the only bright detail.
    pixel(unit, x + 12f, ground - 23f, 14f, 12f, fur)
    pixel(unit, x + 18f, ground - 12f, 10f, 4f, muzzle)
    pixel(unit, x + 25f, ground - 16f, 6f, 4f, muzzle)
    pixel(unit, x + 30f, ground - 14f, 2f, 1f, outline)
    pixel(unit, x + 24f, ground - 20f, 2f, 1f, accent)
    pixel(unit, x + 10f, ground - 13f, 3f, 3f, accent)
}
