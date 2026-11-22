package com.oryn.terminal.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroAccentGlyphTest {
    private fun contrast(a: Color, b: Color): Float {
        val first = a.luminance()
        val second = b.luminance()
        return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
    }

    @Test fun selectionGlyphIsSofterButStillDistinguishable() {
        for (dark in listOf(false, true)) {
            for (preset in KlyroColors.primary) {
                val swatch = KlyroColors.create(dark, preset.id)
                val soft = lerp(swatch.onAccent, swatch.accent, .14f)
                val ratio = contrast(soft, swatch.accent)
                assertTrue("${preset.id}, dark=$dark, contrast=$ratio", ratio >= 3f)
                assertTrue(ratio <= contrast(swatch.onAccent, swatch.accent))
            }
        }
    }
}
