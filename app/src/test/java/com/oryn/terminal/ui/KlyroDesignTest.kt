package com.oryn.terminal.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
    }

    @Test fun monochromeModesSurvivePersistedThemeValidation() {
        assertEquals(6, KlyroColors.themeModes.size)
        for (mode in KlyroColors.themeModes) {
            assertEquals(mode, KlyroColors.normalizedThemeMode(mode))
        }
        assertEquals("system", KlyroColors.normalizedThemeMode(null))
        assertEquals("system", KlyroColors.normalizedThemeMode("malformed"))
        assertEquals(true, KlyroColors.monochrome("mono-black").dark)
        assertEquals(false, KlyroColors.monochrome("mono-paper").dark)
        assertEquals(false, KlyroColors.monochrome("mono-contrast").dark)
    }

    @Test fun runtimeReadinessAndWarningInkRemainReadableOnBothThemes() {
        for (dark in listOf(false, true)) {
            val surface = KlyroColors.create(dark, "lime").chrome
            assertTrue("ready status dark=$dark", contrast(KlyroColors.create(dark, "lime").readyInk, surface) >= 4.5)
            assertTrue("warning status dark=$dark", contrast(KlyroColors.create(dark, "lime").warningInk, surface) >= 4.5)
        }
    }

    @Test fun statusBannersStayReadableOnMatchingSemanticSurfaces() {
        for (dark in listOf(false, true)) {
            for (id in KlyroColors.accents) {
                val c = KlyroColors.create(dark, id)
                assertTrue("warning $id/$dark",
                    contrast(c.warningInk, c.warningContainer) >= 4.5)
                assertTrue("danger $id/$dark",
                    contrast(c.danger, c.dangerContainer) >= 4.5)
            }
        }
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
        assertEquals("lime", KlyroColors.normalizedAccent(null))
        assertEquals("lime", KlyroColors.normalizedAccent("nonexistent"))
        assertEquals("lime", KlyroColors.normalizedAccent("mint"))
        assertEquals("coral", KlyroColors.normalizedAccent("teal"))
        assertEquals("orchid", KlyroColors.normalizedAccent("violet"))
        assertEquals(false, KlyroColors.create(false, "bogus").dark)
        assertEquals(true, KlyroColors.create(true, "bogus").dark)
        assertEquals(KlyroColors.accents.toSet().size, KlyroColors.accents.size)
    }

    @Test fun allFourPrimaryAndExtendedPresetsAreDistinct() {
        assertEquals(4, KlyroColors.primary.size)
        assertEquals(3, KlyroColors.extended.size)
        assertEquals(listOf("lime", "cyan", "coral", "orchid"),
            KlyroColors.primary.map { it.id })
        assertEquals(listOf("#BDCC64", "#00F0FF", "#F57C41", "#CA5DEC"),
            KlyroColors.primary.map { it.darkHex })
        assertEquals(listOf("#8D0826", "#EBEF7E", "#DB0A71"),
            KlyroColors.extended.map { it.sourceHex })
        assertEquals(listOf("#56620C", "#006779", "#A33C11", "#77349B"),
            KlyroColors.primary.map { it.lightHex })
    }

    @Test fun customColorIsPersistentAndAlwaysReadable() {
        val id = KlyroColors.customId(0x8000F0FF.toInt())
        assertEquals("custom:#8000F0FF", KlyroColors.normalizedAccent(id))
        assertEquals("lime", KlyroColors.normalizedAccent("custom:#oops"))
        for (dark in listOf(false, true)) {
            val c = KlyroColors.create(dark, id)
            assertTrue(contrast(c.onAccent, c.accent) >= 4.5)
        }
    }

    @Test fun terminalSurfaceTracksThemeAndKeepsContrast() {
        for (dark in listOf(false, true)) {
            for (id in KlyroColors.accents) {
                val c = KlyroColors.create(dark, id)
                assertTrue("$id/$dark terminal foreground", contrast(c.foreground, c.terminal) >= 7.0)
                assertTrue("$id/$dark muted on chrome", contrast(c.muted, c.chrome) >= 4.5)
                assertTrue("$id/$dark muted on tile", contrast(c.muted, c.tile) >= 4.5)
                assertTrue("$id/$dark terminal luminance",
                    if (dark) luminance(c.terminal) < 0.08 else luminance(c.terminal) > 0.65)
            }
        }
    }

    @Test fun selectedAccentChangesAllSurfaceRoles() {
        for (dark in listOf(false, true)) {
            val a = KlyroColors.create(dark, "mint")
            val b = KlyroColors.create(dark, "violet")
            assertTrue(a.background != b.background)
            assertTrue(a.chrome != b.chrome)
            assertTrue(a.terminal != b.terminal)
            assertTrue(a.key != b.key)
            assertTrue(a.sheet != b.sheet)
            assertTrue(a.tile != b.tile)
        }
    }

    @Test fun everyMonochromeTerminalHasReadablePromptCursorAndAnsi() {
        val expected = mapOf(
            "mono-black" to Triple("#000000", "#FFFFFF", "#FFFFFF"),
            "mono-paper" to Triple("#FFFFFF", "#101010", "#101010"),
            "mono-contrast" to Triple("#000000", "#FFFFFF", "#FFFFFF")
        )
        for ((mode, values) in expected) {
            val properties = KlyroTerminalPalette.properties(KlyroColors.monochrome(mode))
            assertEquals(mode + " canvas", values.first, properties.getProperty("background"))
            assertEquals(mode + " text", values.second, properties.getProperty("foreground"))
            assertEquals(mode + " prompt", values.third, properties.getProperty("color6"))
            assertEquals(mode + " caret", values.third, properties.getProperty("cursor"))
            assertEquals(mode + " ANSI 7", values.third, properties.getProperty("color7"))
            val background = Color(android.graphics.Color.parseColor(values.first))
            val text = Color(android.graphics.Color.parseColor(values.second))
            assertTrue(mode + " text contrast", contrast(text, background) >= 7.0)
        }
    }

    @Test fun nativeTerminalSchemeTracksComposeDefaultsAndLightAnsiContrast() {
        for (dark in listOf(false, true)) {
            for (id in KlyroColors.accents) {
                val colors = KlyroColors.create(dark, id)
                val native = KlyroTerminalPalette.properties(colors)
                val expectedBg = "#%06X".format(
                    colors.terminal.toArgb() and 0xFFFFFF
                )
                assertEquals("native terminal background $id/$dark", expectedBg,
                    native.getProperty("background"))
                assertEquals("ANSI palette completeness", 19, native.size)
                assertEquals("native prompt should follow selected accent",
                    "#%06X".format(colors.accent.toArgb() and 0xFFFFFF),
                    native.getProperty("color6"))
            }
        }
    }

    @Test fun typefaceSelectionHasSafeFallback() {
        assertEquals("system", KlyroTerminalFonts.normalize(null))
        assertEquals("system", KlyroTerminalFonts.normalize("invalid"))
        assertEquals("jetbrains", KlyroTerminalFonts.normalize("jetbrains"))
        assertEquals(2, KlyroTerminalFonts.options.size)
    }
}
