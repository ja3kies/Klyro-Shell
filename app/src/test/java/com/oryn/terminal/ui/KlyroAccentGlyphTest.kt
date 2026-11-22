package com.oryn.terminal.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class KlyroAccentGlyphTest {
    private fun contrast(a: Color, b: Color): Float {
        val first = a.luminance()
        val second = b.luminance()
        return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
    }

    @Test fun selectionGlyphIsSofterButStillDistinguishable() {
        for (preset in KlyroColors.primary) {
            val swatch = KlyroColors.create(true, preset.id)
            val soft = accentGlyphTint(swatch.accent)
            val ratio = contrast(soft, swatch.accent)
            assertTrue("${preset.id}, contrast=$ratio", ratio >= 3f)
            assertTrue(ratio <= contrast(swatch.onAccent, swatch.accent))
            val factor = soft.red / swatch.accent.red.coerceAtLeast(.001f)
            if (swatch.accent.red > .025f) {
                assertEquals(swatch.accent.green * factor, soft.green, .012f)
                assertEquals(swatch.accent.blue * factor, soft.blue, .012f)
            }
            assertTrue(soft.luminance() < swatch.accent.luminance())
        }
    }
}
