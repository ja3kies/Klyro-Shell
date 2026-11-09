package com.oryn.terminal.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class KlyroThemePreviewPaletteTest {
    @Test fun lightAndDarkPreviewsUseUserAccent() {
        for (accent in KlyroColors.accents) {
            assertEquals(KlyroColors.create(false, accent),
                KlyroThemePreviewPalette.forMode("light", accent))
            assertEquals(KlyroColors.create(true, accent),
                KlyroThemePreviewPalette.forMode("dark", accent))
            val (light, dark) = KlyroThemePreviewPalette.systemPair(accent)
            assertEquals(KlyroColors.create(false, accent), light)
            assertEquals(KlyroColors.create(true, accent), dark)
            assertNotEquals(light.terminal, dark.terminal)
        }
    }

    @Test fun systemPreviewDoesNotInheritMonochromeChrome() {
        val (light, dark) = KlyroThemePreviewPalette.systemPair("coral")
        assertNotEquals(KlyroColors.monochrome("mono-black").terminal, light.terminal)
        assertNotEquals(KlyroColors.monochrome("mono-paper").terminal, dark.terminal)
    }

    @Test fun monochromePreviewsMatchRealTerminalSurfaces() {
        for (mode in listOf("mono-black", "mono-paper", "mono-contrast")) {
            assertEquals(KlyroColors.monochrome(mode),
                KlyroThemePreviewPalette.forMode(mode, "coral"))
        }
    }

    @Test fun distinctAccentsChangePreviews() {
        assertNotEquals(KlyroThemePreviewPalette.forMode("dark", "lime").accent,
            KlyroThemePreviewPalette.forMode("dark", "orchid").accent)
    }
}
