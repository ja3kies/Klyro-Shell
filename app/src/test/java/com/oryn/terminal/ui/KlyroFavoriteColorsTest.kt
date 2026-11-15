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
}
