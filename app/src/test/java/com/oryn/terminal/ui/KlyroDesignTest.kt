package com.oryn.terminal.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** WCAG 2.1 contrast smoke checks for every exported preset. */
class KlyroDesignTest {
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val x = value.toDouble()
            return if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
        }
        return color.red * 0.0 + // keep JVM/Compose color component access explicit
            0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
    }

    @Test fun everyPresetHasReadableChromeAndSelectedLabel() {
        for (dark in listOf(false, true)) {
            for (id in KlyroColors.accents) {
                val colors = KlyroColors.create(dark, id)
                assertTrue("$id/$dark foreground", contrast(colors.foreground, colors.chrome) >= 4.5)
                assertTrue("$id/$dark selected text", contrast(colors.onAccent, colors.accent) >= 4.5)
                assertTrue("$id/$dark keys", contrast(colors.foreground, colors.key) >= 4.5)
            }
        }
    }

    @Test fun unknownPresetFallsBackWithoutChangingLightDarkMode() {
        assertEquals("mint", KlyroColors.normalizedAccent(null))
        assertEquals("mint", KlyroColors.normalizedAccent("nonexistent"))
        assertEquals(false, KlyroColors.create(false, "bogus").dark)
        assertEquals(true, KlyroColors.create(true, "bogus").dark)
        assertEquals(KlyroColors.accents.toSet().size, KlyroColors.accents.size)
    }

    @Test fun darkTerminalRetainsLegibleBaseAcrossAllThemes() {
        for (dark in listOf(false, true)) {
            for (id in KlyroColors.accents) {
                assertEquals(Color(0xFF101619), KlyroColors.create(dark, id).terminal)
            }
        }
    }
}
