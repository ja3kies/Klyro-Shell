package com.oryn.terminal.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroFavoriteColorsTest {
    @Test fun favoritesRoundTripAndLimit() {
        var saved = emptyList<KlyroFavoriteColors.Favorite>()
        repeat(9) { index ->
            saved = KlyroFavoriteColors.save(
                saved, "#FF%06X".format(index), "Color $index"
            )
        }
        assertEquals(6, saved.size)
        assertEquals(saved, KlyroFavoriteColors.decode(KlyroFavoriteColors.encode(saved)))
        assertEquals("Color 5", saved.first().label)
        assertFalse(KlyroFavoriteColors.canSaveWithoutEviction(saved, "#FF000008"))
        assertTrue(KlyroFavoriteColors.canSaveWithoutEviction(saved, "#FF000005"))
    }

    @Test fun duplicateHexUpdatesNameRatherThanAddingCopy() {
        var saved = KlyroFavoriteColors.save(emptyList(), "#80BDCC64", "Lime")
        saved = KlyroFavoriteColors.save(saved, "#80BDCC64", "My lime")
        assertEquals(1, saved.size)
        assertEquals("My lime", saved.first().label)
    }

    @Test fun invalidPreferencesCannotInjectLinesOrUnboundedHistory() {
        assertEquals(emptyList<KlyroFavoriteColors.Favorite>(),
            KlyroFavoriteColors.decode("javascript:alert(1)|Unsafe"))
        assertEquals("bad name", KlyroFavoriteColors.normalizeLabel("bad|name"))
        assertEquals("Custom", KlyroFavoriteColors.normalizeLabel("  "))
        assertEquals(null, KlyroFavoriteColors.normalizeHex("#123"))
        assertEquals("#FF00AABB", KlyroFavoriteColors.normalizeHex("#ff00aabb"))
    }

    @Test fun savedArgbRoundTripsIntoSelectableAccentWithoutLosingOpacity() {
        val saved = KlyroFavoriteColors.decode("#804488CC|Ocean\\n#FFBDCC64|Lime")
        assertEquals("custom:#804488CC", KlyroFavoriteColors.selectableAccentId(saved[0]))
        assertEquals("custom:#FFBDCC64", KlyroFavoriteColors.selectableAccentId(saved[1]))
        assertEquals("custom:#804488CC",
            KlyroColors.normalizedAccent(KlyroFavoriteColors.selectableAccentId(saved[0])))
        assertEquals(null, KlyroFavoriteColors.selectableAccentId(
            KlyroFavoriteColors.Favorite("#nonsense", "Bad")))
    }

    @Test fun labelLimitDoesNotSplitSupplementaryCharacters() {
        val wanted = "a".repeat(21) + "😀"
        assertEquals(wanted, KlyroFavoriteColors.normalizeLabel(wanted + "x"))
    }
}
