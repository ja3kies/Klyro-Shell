package com.oryn.terminal.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.termux.terminal.TerminalColors
import com.termux.terminal.TerminalSession
import com.termux.terminal.TextStyle
import java.util.Properties

/**
 * Synchronizes the native Termux emulator with Klyro's Compose palette.
 * Custom OSC-set colors are preserved when they differ from prior defaults.
 */
object KlyroTerminalPalette {
    private fun rgb(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

    internal fun properties(colors: KlyroColors): Properties = Properties().apply {
        setProperty("foreground", rgb(colors.foreground))
        setProperty("background", rgb(colors.terminal))
        setProperty("cursor", rgb(colors.accent))
        val ansi = if (colors.accentId.startsWith("mono-")) {
            val dark = colors.dark
            val foreground = if (dark) "#FFFFFF" else "#101010"
            val background = if (dark) "#000000" else "#FFFFFF"
            val secondary = if (dark) "#BFBFBF" else "#555555"
            listOf(
                background, foreground, foreground, secondary,
                secondary, foreground, secondary, foreground,
                secondary, foreground, foreground, secondary,
                secondary, foreground, secondary, foreground
            )
        } else if (colors.dark) listOf(
            "#182528", "#FF6677", "#64DC9B", "#D3BF71",
            "#84B3EE", "#B6A1F2", "#71CDD8", "#DFEDF0",
            "#87999E", "#FF8A97", "#85E6AC", "#E6D38B",
            "#ABC9FF", "#D5BEFF", "#96E7E9", "#FFFFFF"
        ) else listOf(
            "#28363D", "#A13D4A", "#16704D", "#91641D",
            "#2F5DA1", "#7553A0", "#14717D", "#34414B",
            "#66757D", "#B64C59", "#247D56", "#977323",
            "#3566AC", "#815BA9", "#197B85", "#17262D"
        )
        ansi.forEachIndexed { i, value -> setProperty("color$i", value) }
        // Bash uses ANSI 36 (palette color6) for the prompt; selected
        // accent remains readable on both light and dark terminal canvases.
        setProperty("color6", rgb(colors.accent))
    }

    fun apply(colors: KlyroColors, sessions: Iterable<TerminalSession> = emptyList()) {
        val scheme = TerminalColors.COLOR_SCHEME
        val previous = scheme.mDefaultColors.clone()
        scheme.updateWith(properties(colors))
        val indices = (0..15).toList() + listOf(
            TextStyle.COLOR_INDEX_FOREGROUND,
            TextStyle.COLOR_INDEX_BACKGROUND,
            TextStyle.COLOR_INDEX_CURSOR
        )
        sessions.forEach { session ->
            val current = session.emulator?.mColors?.mCurrentColors ?: return@forEach
            for (index in indices) {
                if (current[index] == previous[index]) {
                    current[index] = scheme.mDefaultColors[index]
                }
            }
        }
    }
}
