package com.oryn.terminal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

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
            return if (dark) KlyroColors(
                dark = true, accentId = id,
                background = Color(0xFF101719),
                chrome = Color(0xFF182528),
                terminal = Color(0xFF101619),
                key = Color(0xFF28383A),
                sheet = Color(0xFF1D2B2E),
                tile = Color(0xFF293A3D),
                foreground = Color(0xFFE3F1EA),
                muted = Color(0xFFABC2BB),
                accent = accent,
                onAccent = Color(0xFF101719),
                danger = Color(0xFFFFB5BA)
            ) else KlyroColors(
                dark = false, accentId = id,
                background = Color(0xFFDDEFEA),
                chrome = Color(0xFFF8FBFA),
                terminal = Color(0xFF101619),
                key = Color(0xFFDDEBE6),
                sheet = Color(0xFFF8FBFA),
                tile = Color(0xFFE8F2EE),
                foreground = Color(0xFF18282B),
                muted = Color(0xFF526D64),
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
