package com.oryn.terminal.ui

import android.content.Context
import android.graphics.Typeface
import kotlin.math.roundToInt

/**
 * Bundled OFL terminal typefaces use immutable, verified font assets.
 * Android monospace remains the fallback.
 */
internal object KlyroTerminalFonts {
    const val DEFAULT_SIZE = 14f
    const val MIN_SIZE = 9
    const val MAX_SIZE = 24

    fun normalizeSize(size: Float): Float = if (size.isFinite()) {
        size.roundToInt().coerceIn(MIN_SIZE, MAX_SIZE).toFloat()
    } else DEFAULT_SIZE

    data class Choice(val id: String, val label: String)

    val options = listOf(
        Choice("system", "System Mono"),
        Choice("jetbrains", "JetBrains Mono"),
        Choice("source-code-pro", "Source Code Pro")
    )

    fun normalize(id: String?): String =
        id?.takeIf { candidate -> options.any { it.id == candidate } } ?: "system"

    fun load(context: Context, id: String): Typeface {
        val asset = when (normalize(id)) {
            "jetbrains" -> "fonts/JetBrainsMono-Regular.ttf"
            "source-code-pro" -> "fonts/SourceCodePro-Regular.ttf"
            else -> return Typeface.MONOSPACE
        }
        return runCatching {
            Typeface.createFromAsset(context.assets, asset)
        }.getOrElse { Typeface.MONOSPACE }
    }
}
