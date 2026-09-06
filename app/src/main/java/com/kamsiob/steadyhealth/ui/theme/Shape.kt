package com.kamsiob.steadyhealth.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The radius set from DESIGN.md section 2. "No other values exist" is the rule,
 * so a screen reaching for 18 is a screen that has picked the wrong component.
 */
object SteadyShapes {
    val Hero = RoundedCornerShape(30.dp)
    val Card = RoundedCornerShape(24.dp)
    val LifeCard = RoundedCornerShape(26.dp)
    val ListItem = RoundedCornerShape(22.dp)
    val GlyphTile = RoundedCornerShape(16.dp)
    val Doc = RoundedCornerShape(20.dp)

    /** Buttons, pills and tags. Fully round means half the height, whatever it is. */
    val Round = RoundedCornerShape(percent = 50)
}

/**
 * The spacing rules from DESIGN.md section 2, as named values rather than numbers
 * scattered through the screens.
 */
object SteadySpacing {
    val Screen = 18.dp
    val BetweenBlocks = 12.dp
    val Inside = 18.dp
    val InsideTight = 16.dp
    val AboveSectionTitle = 6.dp
    val ListGap = 8.dp
    val Tight = 10.dp

    /** The accessibility floor: nothing a finger has to hit is smaller. */
    val TapTarget = 44.dp

    /** The 2 px inset outline on a secondary button, a pill, and the back button. */
    val Outline = 2.dp

    val GlyphInCard = 52.dp
    val GlyphInTile = 28.dp
    val GlyphTile = 44.dp
    val GlyphInStrip = 32.dp
    val GlyphInAbilityTile = 40.dp
}
