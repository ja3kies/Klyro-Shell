package com.oryn.terminal

/** Inspect the actual STDERR section, never infer that every warning is mandoc. */
internal object KlyroVerificationWarnings {
    fun stderrLines(diagnostics: String): List<String> {
        val section = diagnostics.substringAfter("\nSTDERR:\n", "")
        if (section.isBlank() || section.trim() == "(empty)") return emptyList()
        return section.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isKnownMandoc(line: String): Boolean =
        line.startsWith("update-alternatives: warning:") &&
            line.contains("makewhatis") && line.contains("mandoc")

    fun unexpectedLines(diagnostics: String): List<String> =
        stderrLines(diagnostics).filterNot(::isKnownMandoc)

    fun suppressHarmlessWarning(diagnostics: String, hasWarnings: Boolean, runtimeReady: Boolean): Boolean {
        if (!hasWarnings || !runtimeReady) return false
        val lines = stderrLines(diagnostics)
        return lines.isNotEmpty() && lines.all(::isKnownMandoc)
    }
}
