package com.kamsiob.steadyhealth.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow

/**
 * The accessibility floor in DESIGN.md section 7, as arithmetic.
 *
 * DESIGN.md asks for two things that one hex cannot satisfy at once: the approved
 * colours in its table, and 4.5:1 for every word on ground and on white. The
 * resolution is that the table gives fills and the `Text` variants give words.
 * This file is what stops that resolution from quietly eroding, because the
 * failure it prevents is invisible on the machine of anybody with good eyes and a
 * bright screen.
 *
 * Thresholds are WCAG AA as Android applies it: 4.5:1 for text, and 3.0:1 for
 * text at 18 sp or at 14 sp bold and above.
 */
class ContrastTest {

    @Test
    fun everyWordOnGroundAndOnWhitePassesFourPointFive() {
        val words = mapOf(
            "ink" to SteadyPalette.Ink,
            "ink2" to SteadyPalette.Ink2,
            "ink3-text" to SteadyPalette.Ink3Text,
            "navy" to SteadyPalette.Navy,
            "orange-text" to SteadyPalette.OrangeText,
            "green-text" to SteadyPalette.GreenText,
        )
        val surfaces = mapOf(
            "ground" to SteadyPalette.Ground,
            "white" to SteadyPalette.White,
        )

        val failures = words.flatMap { (wordName, word) ->
            surfaces.mapNotNull { (surfaceName, surface) ->
                val r = ratio(word, surface)
                "$wordName on $surfaceName is ${round(r)}".takeIf { r < NORMAL }
            }
        }
        assertThat(failures).isEmpty()
    }

    @Test
    fun theWordsOnATintedBlockPass() {
        val pairs = listOf(
            Triple("navy on sand", SteadyPalette.Navy, SteadyPalette.Sand),
            Triple("navy on sky-l", SteadyPalette.Navy, SteadyPalette.SkyL),
            Triple("navy on green-l", SteadyPalette.Navy, SteadyPalette.GreenL),
            Triple("navy on plum-t", SteadyPalette.Navy, SteadyPalette.PlumT),
            Triple("navy on butter", SteadyPalette.Navy, SteadyPalette.Butter),
            Triple("navy on peach", SteadyPalette.Navy, SteadyPalette.Peach),
            Triple("ink2 on sand", SteadyPalette.Ink2, SteadyPalette.Sand),
            Triple("green-text on green-l", SteadyPalette.GreenText, SteadyPalette.GreenL),
            Triple("white on navy", SteadyPalette.White, SteadyPalette.Navy),
            Triple("white on navy-l", SteadyPalette.White, SteadyPalette.NavyL),
            Triple("white on hill-front", SteadyPalette.White, SteadyPalette.HillFront),
            Triple("white on hill-back", SteadyPalette.White, SteadyPalette.HillBack),
        )
        val failures = pairs.mapNotNull { (name, word, surface) ->
            val r = ratio(word, surface)
            "$name is ${round(r)}".takeIf { r < NORMAL }
        }
        assertThat(failures).isEmpty()
    }

    @Test
    fun thePrimaryButtonPassesAtItsOwnSizeAndWeight() {
        // White at 16 sp weight 800, which Android counts as large text, so the
        // threshold is 3.0. The approved orange gives 2.54 and is why the button
        // is filled with orange-d instead.
        assertThat(ratio(SteadyPalette.White, SteadyPalette.OrangeD)).isAtLeast(LARGE)
        assertThat(ratio(SteadyPalette.White, SteadyPalette.Orange)).isLessThan(LARGE)
    }

    @Test
    fun theTabBarPassesInBothStates() {
        assertThat(ratio(SteadyPalette.Navy, SteadyPalette.White)).isAtLeast(NORMAL)
        assertThat(ratio(SteadyPalette.Ink3Text, SteadyPalette.White)).isAtLeast(NORMAL)
    }

    @Test
    fun nothingInThePaletteIsRed() {
        // DESIGN.md section 2: there is no red anywhere in the app, and nothing
        // turns a different colour because a number went the wrong way. A hue
        // check is the only way to keep that true as the palette grows.
        val palette = mapOf(
            "orange" to SteadyPalette.Orange,
            "orange-d" to SteadyPalette.OrangeD,
            "orange-text" to SteadyPalette.OrangeText,
            "orange-l" to SteadyPalette.OrangeL,
            "peach" to SteadyPalette.Peach,
            "butter" to SteadyPalette.Butter,
            "plum" to SteadyPalette.Plum,
            "green" to SteadyPalette.Green,
        )
        val reds = palette.filter { (_, colour) -> isRed(colour) }.keys
        assertThat(reds).isEmpty()
    }

    /**
     * Red as a person sees it: a hue within 20 degrees of pure red that is also
     * saturated enough to read as a colour rather than as a warm grey.
     */
    private fun isRed(colour: Color): Boolean {
        val r = colour.red
        val g = colour.green
        val b = colour.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        if (max - min < 0.15f) return false
        val hue = when (max) {
            r -> 60f * (((g - b) / (max - min)) % 6f)
            g -> 60f * (((b - r) / (max - min)) + 2f)
            else -> 60f * (((r - g) / (max - min)) + 4f)
        }
        val normalised = (hue + 360f) % 360f
        return normalised < RED_ARC || normalised > 360f - RED_ARC
    }

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun luminance(colour: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(colour.red) +
            0.7152 * channel(colour.green) +
            0.0722 * channel(colour.blue)
    }

    private fun round(value: Double) = (value * 100).toInt() / 100.0

    private companion object {
        const val NORMAL = 4.5
        const val LARGE = 3.0
        const val RED_ARC = 20f
    }
}
