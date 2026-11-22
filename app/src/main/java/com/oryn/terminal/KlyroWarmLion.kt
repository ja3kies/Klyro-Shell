// From Uiverse.io by SelfMadeSystem - https://uiverse.io/SelfMadeSystem/warm-lion-64
// MIT License, Copyright (c) SelfMadeSystem (SelfMadeSystem), SelfMadeSystem (SelfMadeSystem)
package com.oryn.terminal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor

internal const val WARM_LION_PERIOD_MS = 150_000

// The three original CSS layers in each group coincide modulo the tile:
// two streaks 3px apart and a centered point. Blur spreads the three into
// one glow centered 1.5px from the stream's left edge.
internal data class WarmLionStream(val period: Int, val initial: Int, val end: Int) {
    fun offset(fraction: Float): Float {
        val y = initial + (end - initial) * fraction
        return ((y % period) + period) % period
    }
}

internal val warmLionStreams = listOf(
    WarmLionStream(235, 220, 6800),
    WarmLionStream(252, 24, 13632),
    WarmLionStream(150, 16, 5416),
    WarmLionStream(253, 224, 17175),
    WarmLionStream(204, 19, 5119),
    WarmLionStream(134, 120, 8428),
    WarmLionStream(179, 31, 9876),
    WarmLionStream(299, 235, 13391),
    WarmLionStream(215, 121, 14741),
    WarmLionStream(281, 224, 18770),
    WarmLionStream(158, 26, 5082),
    WarmLionStream(210, 75, 6375)
)

// CSS ::after: opaque 4% black between transparent 2px-radius holes,
// repeated every 8px. Cache this geometry, not a bitmap or a GIF.
internal fun warmLionDotMask(size: Size, density: Float): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    addRect(Rect(0f, 0f, size.width, size.height))
    val pitch = 8f * density
    val radius = 2f * density
    // Include only holes centered in the visible mask; clipping at the
    // rectangle edge maintains the CSS background repeat.
    for (row in 0 until ceil(size.height / pitch).toInt()) {
        for (col in 0 until ceil(size.width / pitch).toInt()) {
            val x = (col + .5f) * pitch
            val y = (row + .5f) * pitch
            addOval(Rect(x - radius, y - radius, x + radius, y + radius))
        }
    }
}

/**
 * Native approximation of the original backdrop blur and brightness filter.
 * Keep the exact stream periods, offsets, linear 150s translation and 8dp
 * perforation grid. The 10s hue cycle becomes a 10s luminance cycle so the
 * pattern never leaves the active theme, including monochrome.
 */
internal fun DrawScope.drawHomeWarmLion(accent: Color, degrees: Float, dotMask: Path) {
    val unit = 1.dp.toPx()
    val fraction = degrees / 360f
    val intensity = (.72 + .25 * cos(fraction * 15.0 * 2.0 * PI)).toFloat()
    val glow = Brush.radialGradient(
        0f to accent.copy(alpha = intensity),
        .35f to accent.copy(alpha = intensity * .76f),
        .7f to accent.copy(alpha = intensity * .24f),
        1f to Color.Transparent,
        center = Offset.Zero, radius = 1f
    )
    val pitch = 25f * unit
    val rx = 24f * unit
    val ry = 116f * unit
    val firstColumn = floor(-rx / pitch).toInt()
    val lastColumn = ceil((size.width + rx) / pitch).toInt()
    for (column in firstColumn..lastColumn) {
        val stream = warmLionStreams[((column % 12) + 12) % 12]
        val x = column * pitch + 1.5f * unit
        val period = stream.period * unit
        val offset = stream.offset(fraction) * unit
        val firstRow = floor((-ry - offset) / period).toInt()
        val lastRow = ceil((size.height + ry - offset) / period).toInt()
        for (row in firstRow..lastRow) {
            translate(x, offset + row * period) {
                scale(rx, ry, pivot = Offset.Zero) {
                    drawCircle(glow, radius = 1f, center = Offset.Zero)
                }
            }
        }
    }
    drawPath(dotMask, Color(0xFF0A0A0A))
}
