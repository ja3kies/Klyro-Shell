package com.oryn.terminal.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class KlyroShortcutTonePolicyTest {
    private val modes = listOf(
        KlyroColors.create(false, "lime"),
        KlyroColors.create(true, "lime"),
        KlyroColors.create(false, "orchid"),
        KlyroColors.create(true, "coral"),
        KlyroColors.monochrome("mono-contrast"),
        KlyroColors.monochrome("mono-black"),
        KlyroColors.monochrome("mono-paper")
    )

    @Test fun defaultKeysStayNeutralAndPasteNeverLooksLikeWarning() {
        for (palette in modes) {
            val neutral = KlyroShortcutTonePolicy.resolve(
                palette, KlyroShortcutTone.NEUTRAL, ShortcutState.DEFAULT)
            val paste = KlyroShortcutTonePolicy.resolve(
                palette, KlyroShortcutTone.PASTE, ShortcutState.DEFAULT)
            assertEquals(palette.key, neutral.background)
            assertEquals(palette.tile, paste.background)
            assertEquals(palette.foreground, paste.ink)
            assertNotEquals(palette.warningContainer, paste.background)
        }
    }

    @Test fun selectIsDistinctAndTracksEachTheme() {
        for (palette in modes) {
            val select = KlyroShortcutTonePolicy.resolve(
                palette, KlyroShortcutTone.SELECT, ShortcutState.DEFAULT)
            val paste = KlyroShortcutTonePolicy.resolve(
                palette, KlyroShortcutTone.PASTE, ShortcutState.DEFAULT)
            assertNotEquals(select.background, paste.background)
            assertEquals(palette.foreground, select.ink)
        }
        assertNotEquals(
            KlyroShortcutTonePolicy.resolve(modes[0], KlyroShortcutTone.SELECT, ShortcutState.DEFAULT).background,
            KlyroShortcutTonePolicy.resolve(modes[1], KlyroShortcutTone.SELECT, ShortcutState.DEFAULT).background)
    }

    @Test fun holdsAreTransientAndInactiveKeysRemainReadable() {
        val palette = modes[0]
        val normal = KlyroShortcutTonePolicy.resolve(
            palette, KlyroShortcutTone.NEUTRAL, ShortcutState.DEFAULT)
        val hold = KlyroShortcutTonePolicy.resolve(
            palette, KlyroShortcutTone.NEUTRAL, ShortcutState.HOLD)
        val inactive = KlyroShortcutTonePolicy.resolve(
            palette, KlyroShortcutTone.PASTE, ShortcutState.INACTIVE)
        assertNotEquals(normal.background, hold.background)
        assertEquals(palette.foreground, hold.ink)
        assertEquals(palette.muted, inactive.ink)
    }
}
