package com.oryn.terminal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Klyro-only visual tokens. One source of truth for the status/navigation bars,
 * session pills, shortcuts, settings, search and rename dialogs.
 *
 * Light mode follows the Soft Mint vision; the dark palette is independent.
 * Accents are selected for contrast on each surface, never reused unchanged
 * across light and dark backgrounds.
 */
data class KlyroColors(
    val dark: Boolean,
    val accentId: String,
    val background: Color,
    val chrome: Color,
    val terminal: Color,
    val key: Color,
    val sheet: Color,
    val tile: Color,
    val foreground: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color
) {
    // Semantic statuses used by every screen, regardless of current accent.
    val readyInk: Color get() = if (dark) Color(0xFFACF3BE) else Color(0xFF12603E)
    val warningInk: Color get() = if (dark) Color(0xFFF2CD8F) else Color(0xFF885017)
    val warningContainer: Color get() = lerp(sheet, warningInk, .12f)
    val dangerContainer: Color get() = lerp(sheet, danger, .12f)

    companion object {
        /** Distinct primary hues and intentionally darker light-mode counterparts. */
        data class AccentOption(
            val id: String,
            val label: String,
            val darkHex: String,
            val lightHex: String,
            val seedHex: String,
            val sourceHex: String = darkHex
        )

        val primary = listOf(
            AccentOption("lime", "Lime", "#BDCC64", "#56620C", "#919B25"),
            AccentOption("cyan", "Cyan", "#00F0FF", "#006779", "#007C98"),
            AccentOption("coral", "Coral", "#F57C41", "#A33C11", "#B75A30"),
            AccentOption("orchid", "Orchid", "#CA5DEC", "#77349B", "#8745AD")
        )
        val extended = listOf(
            AccentOption("wine", "Wine", "#FF7894", "#8D0826", "#8D0826", "#8D0826"),
            AccentOption("citron", "Citron", "#EBEF7E", "#626900", "#939B31"),
            AccentOption("berry", "Berry", "#FF73B1", "#A40052", "#B50B65", "#DB0A71")
        )
        val accents: List<String> = primary.map { it.id } + extended.map { it.id }
        private val all = primary + extended
        private val customPattern = Regex("^custom:#[0-9A-Fa-f]{8}$")

        /** Preserve older installed accent selections without exposing redundant hues. */
        fun normalizedAccent(id: String?): String = when (id) {
            "mint" -> "lime"
            "teal" -> "coral"
            "violet" -> "orchid"
            else -> when {
                id in accents -> id!!
                id != null && customPattern.matches(id) -> id.uppercase().replace("CUSTOM:", "custom:")
                else -> "lime"
            }
        }

        fun customId(argb: Int): String = "custom:#%08X".format(argb)
        fun selectedOption(id: String?): AccentOption? =
            all.firstOrNull { it.id == normalizedAccent(id) }

        private fun hex(input: String): Color =
            Color((0xFF000000L or input.removePrefix("#").toLong(16)).toInt())

        private fun lum(color: Color): Double {
            fun linear(v: Float): Double {
                val channel = v.toDouble()
                return if (channel <= .04045) channel / 12.92
                else Math.pow((channel + .055) / 1.055, 2.4)
            }
            return .2126 * linear(color.red) + .7152 * linear(color.green) +
                .0722 * linear(color.blue)
        }
        private fun contrast(a: Color, b: Color): Double {
            val lighter = maxOf(lum(a), lum(b))
            val darker = minOf(lum(a), lum(b))
            return (lighter + .05) / (darker + .05)
        }
        /** Retains hue as much as possible while ensuring AA label contrast. */
        private fun accessibleAccent(input: Color, dark: Boolean): Color {
            val ink = if (dark) Color(0xFF101719) else Color.White
            var result = input.copy(alpha = 1f)
            val target = if (dark) Color.White else Color.Black
            var steps = 0
            while (contrast(result, ink) < 4.5 && steps++ < 50) {
                result = lerp(result, target, .055f)
            }
            return result
        }

        fun create(dark: Boolean, accentId: String?): KlyroColors {
            val id = normalizedAccent(accentId)
            val option = all.firstOrNull { it.id == id }
            val canvas = if (dark) Color(0xFF11191D) else Color(0xFFF2F7F5)
            val raw = if (option != null) hex(if (dark) option.darkHex else option.lightHex)
                else Color(id.removePrefix("custom:#").toLong(16).toInt())
            // Alpha is supported in custom picker values. Flatten it against
            // the current theme to avoid invisible ink in native TerminalView.
            val opaque = lerp(canvas, raw.copy(alpha = 1f), raw.alpha)
            val accent = accessibleAccent(opaque, dark)
            val seed = if (option != null) hex(option.seedHex) else opaque
            return if (dark) KlyroColors(
                dark = true, accentId = id,
                background = lerp(Color(0xFF11191D), seed, .15f),
                chrome = lerp(Color(0xFF192327), seed, .12f),
                terminal = lerp(Color(0xFF121A1E), seed, .12f),
                key = lerp(Color(0xFF29383C), seed, .18f),
                sheet = lerp(Color(0xFF202A2F), seed, .12f),
                tile = lerp(Color(0xFF2B393D), seed, .18f),
                foreground = Color(0xFFE9F1F2),
                muted = Color(0xFFB4C4C7),
                accent = accent, onAccent = Color(0xFF101719),
                danger = Color(0xFFFFB5BA)
            ) else KlyroColors(
                dark = false, accentId = id,
                background = lerp(Color(0xFFF2F7F5), seed, .12f),
                chrome = lerp(Color(0xFFFDFEFC), seed, .055f),
                terminal = lerp(Color(0xFFF1F7F4), seed, .115f),
                key = lerp(Color(0xFFE6EFEB), seed, .19f),
                sheet = lerp(Color(0xFFFDFEFC), seed, .065f),
                tile = lerp(Color(0xFFECF3EF), seed, .18f),
                foreground = Color(0xFF1B2B31),
                muted = Color(0xFF455962),
                accent = accent, onAccent = Color.White,
                danger = Color(0xFF90343B)
            )
        }
    }
}

/** Cross-screen geometry: one place to adjust every Klyro modal and action. */
object KlyroUiMetrics {
    val modalCorner = 28.dp
    val sheetCorner = 28.dp
    val cardCorner = 22.dp
    val controlCorner = 14.dp
    val minTouch = 48.dp
    val sheetGutter = 16.dp
    val cardPadding = 14.dp
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 24.dp
    val compactRow = 52.dp
    val optionRow = 48.dp
    val sectionGap = 20.dp
    val selectedCorner = 16.dp
    val selectedBorder = 1.5.dp
    val glyphSmall = 20.dp
    val glyphMedium = 24.dp
}

val LocalKlyroColors = compositionLocalOf { KlyroColors.create(true, "lime") }

@Composable
fun KlyroModalTheme(content: @Composable () -> Unit) {
    val colors = LocalKlyroColors.current
    val scheme = if (colors.dark) darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        background = colors.sheet,
        onBackground = colors.foreground,
        surface = colors.sheet,
        onSurface = colors.foreground,
        surfaceVariant = colors.tile,
        onSurfaceVariant = colors.muted,
        error = colors.danger,
        outline = colors.muted
    ) else lightColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        background = colors.sheet,
        onBackground = colors.foreground,
        surface = colors.sheet,
        onSurface = colors.foreground,
        surfaceVariant = colors.tile,
        onSurfaceVariant = colors.muted,
        error = colors.danger,
        outline = colors.muted
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
