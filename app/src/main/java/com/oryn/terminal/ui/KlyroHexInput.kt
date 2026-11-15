package com.oryn.terminal.ui

/** Pure ASCII-only HEX rules, also used for accessibility validation.
 * Avoid accepting non-ASCII Unicode digits that can yield unexpected colors. */
internal object KlyroHexInput {
    private val complete = Regex("^#[0-9A-F]{6}$")
    fun sanitize(raw: String): String {
        val digits = raw.removePrefix("#").filter {
            it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
        }.take(6).uppercase()
        return "#$digits"
    }
    fun isComplete(value: String): Boolean = complete.matches(value)
}
