package com.oryn.terminal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

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
    companion object {
        val accents = listOf("mint", "teal", "cyan", "violet")
        fun normalizedAccent(id: String?): String =
            id?.takeIf { it in accents } ?: "mint"

        fun create(dark: Boolean, accentId: String?): KlyroColors {
            val id = normalizedAccent(accentId)
            val accent = when (id) {
                "teal" -> if (dark) Color(0xFF73DDD3) else Color(0xFF006C66)
                "cyan" -> if (dark) Color(0xFF00F0FF) else Color(0xFF006779)
                "violet" -> if (dark) Color(0xFFC7B1FC) else Color(0xFF6543A6)
                else -> if (dark) Color(0xFFB9DAD4) else Color(0xFF24584B)
            }
            // Accent is deliberately adjusted for high-contrast controls.
            // The deeper seed gives the same hue to every tonal surface;
            // mixing pale accents directly into dark chrome was imperceptible.
            val seed = when (id) {
                "teal" -> Color(0xFF008679)
                "cyan" -> Color(0xFF006FA6)
                "violet" -> Color(0xFF7049B1)
                else -> Color(0xFF218D6F)
            }
            return if (dark) KlyroColors(
                dark = true, accentId = id,
                background = lerp(Color(0xFF11191D), seed, 0.15f),
                chrome = lerp(Color(0xFF192327), seed, 0.12f),
                terminal = lerp(Color(0xFF121A1E), seed, 0.12f),
                key = lerp(Color(0xFF29383C), seed, 0.18f),
                sheet = lerp(Color(0xFF202A2F), seed, 0.12f),
                tile = lerp(Color(0xFF2B393D), seed, 0.18f),
                foreground = Color(0xFFE9F1F2),
                muted = Color(0xFFB4C4C7),
                accent = accent,
                onAccent = Color(0xFF101719),
                danger = Color(0xFFFFB5BA)
            ) else KlyroColors(
                dark = false, accentId = id,
                background = lerp(Color(0xFFF2F7F5), seed, 0.12f),
                chrome = lerp(Color(0xFFFDFEFC), seed, 0.055f),
                terminal = lerp(Color(0xFFF1F7F4), seed, 0.115f),
                key = lerp(Color(0xFFE6EFEB), seed, 0.19f),
                sheet = lerp(Color(0xFFFDFEFC), seed, 0.065f),
                tile = lerp(Color(0xFFECF3EF), seed, 0.18f),
                foreground = Color(0xFF1B2B31),
                muted = Color(0xFF455962),
                accent = accent,
                onAccent = Color.White,
                danger = Color(0xFF90343B)
            )
        }
    }
}

val LocalKlyroColors = compositionLocalOf { KlyroColors.create(true, "mint") }

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
