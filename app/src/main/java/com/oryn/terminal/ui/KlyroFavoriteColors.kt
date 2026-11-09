package com.oryn.terminal.ui

/**
 * Bounded, user-owned custom accent favourites.
 *
 * No JSON dependency is needed: one sanitized entry per line, and only
 * literal eight-digit ARGB values are accepted when decoding preferences.
 */
internal object KlyroFavoriteColors {
    data class Favorite(val hex: String, val label: String)
    private val hexPattern = Regex("^#[0-9A-F]{8}$")
    private val invalidNameChars = Regex("[\\r\\n|\\p{Cntrl}]")
    const val MAX_FAVORITES = 6

    fun normalizeHex(input: String): String? =
        input.uppercase().takeIf { hexPattern.matches(it) }

    fun normalizeLabel(input: String): String =
        input.trim().replace(invalidNameChars, " ").take(22)
            .ifBlank { "Custom" }

    fun decode(raw: String?): List<Favorite> = raw.orEmpty().lineSequence()
        .take(40)
        .mapNotNull { line ->
            val items = line.split("|", limit = 2)
            val hex = normalizeHex(items.firstOrNull().orEmpty()) ?: return@mapNotNull null
            Favorite(hex, normalizeLabel(items.getOrElse(1) { "Custom" }))
        }
        .distinctBy { it.hex }
        .take(MAX_FAVORITES)
        .toList()

    fun encode(favorites: List<Favorite>): String =
        favorites.take(MAX_FAVORITES).mapNotNull { favorite ->
            val hex = normalizeHex(favorite.hex) ?: return@mapNotNull null
            hex + "|" + normalizeLabel(favorite.label)
        }.joinToString("\n")

    /** Do not silently delete a saved color when the six-slot collection is full. */
    fun canSaveWithoutEviction(favorites: List<Favorite>, hex: String): Boolean {
        val value = normalizeHex(hex) ?: return false
        return favorites.size < MAX_FAVORITES || favorites.any { it.hex == value }
    }

    fun save(favorites: List<Favorite>, hex: String, name: String): List<Favorite> {
        val value = normalizeHex(hex) ?: return favorites
        if (!canSaveWithoutEviction(favorites, value)) return favorites
        val updated = Favorite(value, normalizeLabel(name))
        return (listOf(updated) + favorites.filterNot { it.hex == value })
            .take(MAX_FAVORITES)
    }
}
