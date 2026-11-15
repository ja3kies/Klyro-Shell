package com.oryn.terminal

/** Escape-sequence titles come from untrusted terminal programs, not UI input. */
internal object TerminalTitlePolicy {
    private val unwanted = Regex("[\\p{Cntrl}\\p{Cf}]")
    fun safeTitle(raw: String?): String =
        raw?.replace(unwanted, "")?.trim()?.take(64)?.trim()?.takeIf { it.isNotEmpty() } ?: "Bash"
}
