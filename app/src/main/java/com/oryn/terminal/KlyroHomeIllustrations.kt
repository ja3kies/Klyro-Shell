package com.oryn.terminal

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal enum class HomeIllustration(val label: String, val description: String) {
    CHECKERBOARD("Checkerboard", "Rotating pixel tiles"),
    MAYFLY("Concentric rings", "Pulsing circular pattern"),
    GRADIENT("Animated gradient", "Flowing ambient light"),
    WARM_LION("Warm Lion", "Flowing dotted light streams");

    val periodMs: Int
        get() = if (this == WARM_LION) WARM_LION_PERIOD_MS else HOME_PATTERN_PERIOD_MS

    companion object {
        fun fromStored(value: String?) = entries.firstOrNull { it.name == value } ?: CHECKERBOARD
    }
}

internal data class HomeIllustrationSettings(
    val kind: HomeIllustration = HomeIllustration.CHECKERBOARD,
    val advanced: Boolean = false,
    val speed: Float = 1f
) {
    val effectiveSpeed: Float
        get() = if (advanced) normalizeSpeed(speed) else 1f

    companion object {
        fun normalizeSpeed(value: Float): Float =
            if (value.isFinite()) (value.coerceIn(.5f, 2f) * 4).roundToInt() / 4f else 1f
    }
}

internal fun homeIllustrationPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("klyro_home_illustration", Context.MODE_PRIVATE)

@Composable
internal fun rememberHomeIllustrationSettings(): State<HomeIllustrationSettings> {
    val context = LocalContext.current
    val prefs = remember(context) { homeIllustrationPrefs(context) }
    fun read() = HomeIllustrationSettings(
        HomeIllustration.fromStored(prefs.getString("kind", null)),
        prefs.getBoolean("advanced", false),
        HomeIllustrationSettings.normalizeSpeed(prefs.getFloat("speed", 1f))
    )
    val state = remember(prefs) { mutableStateOf(read()) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            state.value = read()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        state.value = read()
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

/**
 * From Uiverse.io by bociKond — https://uiverse.io/bociKond/strong-mayfly-90
 * Original: https://10015.io/tools/css-background-pattern-generator
 * MIT License; see assets/licenses/home-illustrations-MIT.txt.
 * Preserve the original 76px tile with alternating concentric rings at
 * corners and center. Animate ring luminance and radius within the active theme.
 */
internal fun DrawScope.drawHomeMayfly(accent: Color, degrees: Float) {
    val tile = 76.dp.toPx()
    val radians = degrees * PI / 180.0
    val radius = tile * (.475f + .018f * sin(radians).toFloat())
    val bright = homePatternTint(accent, degrees)
    val dark = accent.copy(
        red = accent.red * .12f,
        green = accent.green * .12f,
        blue = accent.blue * .12f
    )
    drawRect(dark)
    // One shared radial shader: ten alternating bands, hard stops at each boundary.
    val stops = (0 until 10).flatMap { band ->
        val color = if (band % 2 == 0) bright else dark
        listOf(band / 10f to color, (band + 1) / 10f to color)
    }.toTypedArray()
    val rings = Brush.radialGradient(*stops, center = Offset.Zero, radius = radius)
    clipRect {
        for (row in -1..ceil(size.height / tile).toInt()) {
            for (col in -1..ceil(size.width / tile).toInt()) {
                val x = (col + .5f) * tile
                val y = (row + .5f) * tile
                translate(x, y) { drawCircle(rings, radius, center = Offset.Zero) }
                translate(col * tile, row * tile) { drawCircle(rings, radius, center = Offset.Zero) }
            }
        }
    }
}

/** Original Klyro ambient gradient. Two orbiting light fields use only the active accent. */
internal fun DrawScope.drawHomeGradient(accent: Color, degrees: Float) {
    val angle = degrees * PI / 180.0
    val w = size.width
    val h = size.height
    val radius = maxOf(w, h) * .8f
    if (radius <= 0f) return
    val first = Offset(w * (.5f + .28f * cos(angle).toFloat()),
        h * (.45f + .25f * sin(angle).toFloat()))
    val second = Offset(w * (.5f - .32f * cos(angle).toFloat()),
        h * (.42f - .28f * sin(angle).toFloat()))
    drawRect(Brush.radialGradient(
        listOf(accent.copy(alpha = .46f), accent.copy(alpha = .13f), Color.Transparent),
        center = first, radius = radius
    ))
    drawRect(Brush.radialGradient(
        listOf(accent.copy(alpha = .24f), Color.Transparent),
        center = second, radius = radius * .7f
    ))
}
