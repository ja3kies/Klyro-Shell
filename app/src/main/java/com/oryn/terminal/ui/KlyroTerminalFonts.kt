package com.oryn.terminal.ui

import android.content.Context
import android.graphics.Typeface

/**
 * DSH Go uses JetBrains Mono (SIL OFL 1.1) with system monospace fallback.
 * CI pins its font asset to an immutable source commit and verifies its blob.
 */
internal object KlyroTerminalFonts {
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
