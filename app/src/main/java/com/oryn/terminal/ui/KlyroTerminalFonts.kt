package com.oryn.terminal.ui

import android.content.Context
import android.graphics.Typeface
import kotlin.math.roundToInt

/**
 * DSH Go uses JetBrains Mono (SIL OFL 1.1) with system monospace fallback.
 * CI pins its font asset to an immutable source commit and verifies its blob.
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
        Choice("jetbrains", "JetBrains Mono")
    )

    fun normalize(id: String?): String =
        id?.takeIf { candidate -> options.any { it.id == candidate } } ?: "system"

    fun load(context: Context, id: String): Typeface {
        if (normalize(id) != "jetbrains") return Typeface.MONOSPACE
        return runCatching {
            Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono-Regular.ttf")
        }.getOrElse { Typeface.MONOSPACE }
    }
}
