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
    /**
     * The floor everywhere outside a session. ADDENDUM-03 Part 17 raises it from 44.
     */
    val TapTarget = 48.dp

    /**
     * The floor inside a session, where somebody is moving and looking at the phone
     * from two feet away.
     */
    val SessionTapTarget = 56.dp

    /**
     * The primary action, which spans the content width and sits in the bottom third.
     * ADDENDUM-03 Part 17 asks for at least 64dp.
     */
    val PrimaryHeight = 64.dp

    /** The 2 px inset outline on a secondary button, a pill, and the back button. */
    val Outline = 2.dp

    val GlyphInCard = 52.dp
    val GlyphInTile = 28.dp
    val GlyphTile = 44.dp
    val GlyphInStrip = 32.dp
    val GlyphInAbilityTile = 40.dp
}
