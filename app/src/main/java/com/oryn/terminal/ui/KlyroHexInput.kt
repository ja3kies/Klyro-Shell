package com.oryn.terminal.ui

/** Pure ASCII-only HEX rules, also used for accessibility validation.
 * Avoid accepting non-ASCII Unicode digits that can yield unexpected colors. */
internal object KlyroHexInput {
    private val complete = Regex("^#[0-9A-F]{6}$")
    fun sanitize(raw: String): String {
        // Keep an overlong or malformed draft invalid until the user corrects it.
        return "#" + raw.removePrefix("#").take(32).map {
            if (it in 'a'..'f') it.uppercaseChar() else it
        }.joinToString("")
    }
    fun isComplete(value: String): Boolean = complete.matches(value)
}
