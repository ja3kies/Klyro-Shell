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
import androidx.compose.ui.graphics.lerp
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
    MAYFLY("Concentric rings", "Expanding radial ripples"),
    GRADIENT("Animated gradient", "Flowing ambient light"),
    WARM_LION("Warm Lion", "Flowing dotted light streams");

    val periodMs: Int
        get() = when (this) {
            CHECKERBOARD -> HOME_PATTERN_PERIOD_MS
            MAYFLY -> 6_000
            GRADIENT -> 12_000
            WARM_LION -> WARM_LION_PERIOD_MS
        }

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
 * corners and center. Animate expanding ripples within the active theme.
 */
/** A crest moves outward by one ring spacing per cycle. Corner and center tiles alternate. */
internal fun mayflyRingPhase(distance: Float, degrees: Float, corner: Boolean): Float {
    val turns = distance * 5f - degrees / 360f + if (corner) .5f else 0f
    return (.5 + .5 * cos(turns * 2.0 * PI)).toFloat()
}

internal fun DrawScope.drawHomeMayfly(accent: Color, degrees: Float) {
    val tile = 76.dp.toPx()
    val radius = tile * .475f
    val dark = lerp(Color.Black, accent, .12f)
    val bright = lerp(Color.Black, accent, .86f)
    drawRect(dark)
    // Antialiased concentric bands propagate from each center instead of
    // merely scaling a static texture. Two shared shaders cover every tile.
    fun ringBrush(corner: Boolean): Brush {
        val stops = (0..80).map { index ->
            val distance = index / 80f
            val wave = mayflyRingPhase(distance, degrees, corner)
            val edge = ((1f - distance) * 20f).coerceIn(0f, 1f)
            distance to lerp(dark, bright, wave * edge)
        }.toTypedArray()
        return Brush.radialGradient(*stops, center = Offset.Zero, radius = radius)
    }
    val centers = ringBrush(false)
    val corners = ringBrush(true)
    clipRect {
        for (row in -1..ceil(size.height / tile).toInt()) {
            for (col in -1..ceil(size.width / tile).toInt()) {
                translate((col + .5f) * tile, (row + .5f) * tile) {
                    drawCircle(centers, radius, center = Offset.Zero)
                }
                translate(col * tile, row * tile) {
                    drawCircle(corners, radius, center = Offset.Zero)
                }
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
