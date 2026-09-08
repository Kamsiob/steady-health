package com.kamsiob.steadyhealth.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kamsiob.steadyhealth.R

/**
 * Figtree, bundled, from one variable font file.
 *
 * One file rather than six static weights: Figtree publishes a variable font with
 * a weight axis, so 62 KB covers 400 through 900 instead of 1.6 MB covering six
 * points on the same axis. Variable fonts are read from API 26 and this app is
 * API 29 up, so there is no fallback path to keep working.
 */
private fun figtree(weight: Int) = Font(
    resId = R.font.figtree,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Figtree = FontFamily(
    figtree(WEIGHT_REGULAR),
    figtree(WEIGHT_MEDIUM),
    figtree(WEIGHT_SEMIBOLD),
    figtree(WEIGHT_BOLD),
    figtree(WEIGHT_EXTRABOLD),
    figtree(WEIGHT_BLACK),
)

/**
 * The type table from DESIGN.md section 2, and nothing between the rows.
 *
 * "Nothing in between" is the rule, so these are the only styles that exist and a
 * screen that wants 19sp is a screen that has misread the table. Sentence case
 * everywhere, no uppercase labels, no italics.
 *
 * Sizes are sp so they follow the system font size, which the accessibility floor
 * requires. ADDENDUM-03 Part 17 raises that floor: body text never below 16sp, so
 * Body and BodyStrong moved up from 14 and Caption from 12.
 *
 * Line heights are set with trim turned off so a large font size grows
 * the line box rather than clipping the letters.
 */
object SteadyType {

    /**
     * The count on the live session screen, and the largest type in the app.
     *
     * Readable from two feet away by somebody standing up out of a chair with the
     * phone on a table. Nothing else is this size, and nothing else needs to be.
     */
    val SessionCount = style(size = 96, weight = WEIGHT_BLACK, tracking = -0.05f)

    val HeroNumber = style(size = 64, weight = WEIGHT_BLACK, tracking = -0.05f)
    val HeroHeadline = style(size = 38, weight = WEIGHT_BLACK, tracking = -0.04f)
    val ScreenTitleBig = style(size = 26, weight = WEIGHT_BLACK, tracking = -0.04f)
    val Greeting = style(size = 28, weight = WEIGHT_EXTRABOLD, tracking = -0.035f)
    val SectionTitle = style(size = 17, weight = WEIGHT_EXTRABOLD, tracking = -0.02f)
    val BlockValue = style(size = 24, weight = WEIGHT_BLACK, tracking = -0.04f)
    val CardTitle = style(size = 15, weight = WEIGHT_EXTRABOLD, tracking = -0.02f)
    val ListItemHeading = style(size = 16, weight = WEIGHT_EXTRABOLD, tracking = -0.02f)
    val Body = style(size = 16, weight = WEIGHT_REGULAR, tracking = 0f)
    val BodyStrong = style(size = 16, weight = WEIGHT_SEMIBOLD, tracking = 0f)
    val Caption = style(size = 14, weight = WEIGHT_SEMIBOLD, tracking = 0f)
    val Button = style(size = 16, weight = WEIGHT_EXTRABOLD, tracking = -0.01f)

    /** The tab bar label, DESIGN.md section 3. Small, and it scales with the system. */
    val TabLabel = style(size = 12, weight = WEIGHT_BOLD, tracking = 0f)

    /** The screen title in the 44 px top row, DESIGN.md section 4. */
    val ScreenTitle = style(size = 20, weight = WEIGHT_EXTRABOLD, tracking = -0.02f)

    private fun style(size: Int, weight: Int, tracking: Float) = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight(weight),
        // The variable font carries every weight, so a synthesised bold would be
        // a second, worse version of a weight the file already has.
        fontSynthesis = FontSynthesis.None,
        fontSize = size.sp,
        lineHeight = (size * LINE_HEIGHT).sp,
        letterSpacing = tracking.em,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )
}

private const val LINE_HEIGHT = 1.35f

internal const val WEIGHT_REGULAR = 400
internal const val WEIGHT_MEDIUM = 500
internal const val WEIGHT_SEMIBOLD = 600
internal const val WEIGHT_BOLD = 700
internal const val WEIGHT_EXTRABOLD = 800
internal const val WEIGHT_BLACK = 900
