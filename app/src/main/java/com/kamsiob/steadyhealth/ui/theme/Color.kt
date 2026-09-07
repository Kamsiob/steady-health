package com.kamsiob.steadyhealth.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette, from DESIGN.md section 2, and nothing else.
 *
 * This is a closed set. There is no red in it, and no colour anywhere in the app
 * comes from outside it, because the one thing the palette has to guarantee is
 * that nothing turns a different colour because a number went the wrong way.
 *
 * The `Text` variants exist because DESIGN.md asks for two things that cannot
 * both be true of one hex: the approved colours in its table, and 4.5:1 for every
 * word on ground and on white. So the table gives fills, lines and illustration,
 * and a word painted in one of those hues takes the slightly darker member of the
 * same hue. `ContrastTest` holds every pair to it.
 */
object SteadyPalette {

    val White = Color(0xFFFFFFFF)
    val Ground = Color(0xFFFBF8F3)

    val Ink = Color(0xFF1F2340)
    val Ink2 = Color(0xFF5C6180)

    /** Captions and disabled, as a shape. For a word, use [Ink3Text]. */
    val Ink3 = Color(0xFF9A9DB3)

    /** 4.83 on white, 4.55 on ground. */
    val Ink3Text = Color(0xFF6C708F)

    /** The walk, the weight line, the morning sky, the dial arc. Not a text fill. */
    val Orange = Color(0xFFF5843E)

    /** Morning hills, pressed state, and the primary button under white 16/800. */
    val OrangeD = Color(0xFFE86A22)

    /** 4.83 on white, 4.56 on ground. Orange words, including the Summary action. */
    val OrangeText = Color(0xFFBD5114)

    /** The morning hero's hills. White body text sits on the back one. */
    val HillFront = Color(0xFFC45414)
    val HillBack = Color(0xFFA54511)

    val OrangeL = Color(0xFFFFB27A)
    val Peach = Color(0xFFFFD9BF)

    val Navy = Color(0xFF1E2A5A)
    val NavyL = Color(0xFF2E3D7D)

    val Sky = Color(0xFF7FA6F2)
    val SkyL = Color(0xFFDCE7FC)

    val Sand = Color(0xFFF6E7D3)
    val Butter = Color(0xFFFFD766)

    /** The Steady ability, and nothing else. */
    val Plum = Color(0xFFB48BC8)
    val PlumT = Color(0xFFF4E9F7)

    /** Done, and nothing else. As a shape. For the word, use [GreenText]. */
    val Green = Color(0xFF2E8B62)

    /** 5.37 on white, 5.07 on ground, 4.52 on green-l. */
    val GreenText = Color(0xFF287855)

    val GreenL = Color(0xFFDDF0E4)

    /** The empty day on the week row and the months path. */
    val Empty = Color(0xFFEDE7DE)
    val EmptyDeep = Color(0xFFE5DED2)

    /** The hairline above the tab bar and between rows. */
    val Hairline = Color(0xFFEFEAE2)
}

/**
 * The four abilities and their fixed tints, from DESIGN.md section 2.
 *
 * They never swap. A person learns that Carry is the blue one long before they
 * read the word, and a tint that moves takes that away.
 */
enum class Ability(val id: String) {
    GetUp("get_up"),
    Go("go"),
    Carry("carry"),
    Steady("steady"),
    ;

    val tint: Color
        get() = when (this) {
            GetUp -> SteadyPalette.Sand
            Go -> SteadyPalette.GreenL
            Carry -> SteadyPalette.SkyL
            Steady -> SteadyPalette.PlumT
        }
}
