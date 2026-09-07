package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.Ladder

/**
 * What each of the four abilities is called, and what fills it, for one way of
 * getting around.
 *
 * The four domain ids never change, because everything stored carries one of them
 * and a row written last year has to keep meaning what it meant. What changes is
 * the word on the tile and the steps behind it. For somebody in a wheelchair
 * "Get up" is Transfer; for somebody in bed the four are Sit up, Breathe, Grip
 * and Ankles.
 *
 * Nothing here ranks one way above another. There is no "reduced" set and no
 * progression between the four, because a wheelchair is not a worse version of
 * walking, and an app that implies otherwise is the thing this one exists not to
 * be.
 */
data class WayOfGettingAround(
    val way: GettingAround,
    /**
     * Which of the four abilities is called something else here, and nothing
     * about what. The words themselves are resources, because they are shown to
     * a person and this app ships in four languages; what belongs in the engine
     * is only that Get up is Transfer in a wheelchair and Sit up in bed.
     */
    val renamed: Set<AbilityDomain>,
    /** The ladders on offer, in the order Move shows them. */
    val ladders: List<Ladder>,
    /**
     * False where a daily weight is not part of the picture. In bed the daily
     * three become two, and the screen says weighing is off and can be turned
     * back on, rather than showing a card nobody can do.
     */
    val weighsIn: Boolean,
)

/**
 * The four versions of the app.
 *
 * Taken from screens 5, 6, 7, 17 and 18 of the approved grid, which show the same
 * screen for someone on their feet, in a wheelchair and mostly in bed, with the
 * same dignity and the same layout.
 */
object WaysOfGettingAround {

    val onFeet = WayOfGettingAround(
        way = GettingAround.OnFeet,
        renamed = emptySet(),
        ladders = listOf(
            Ladder.Walking,
            Ladder.ChairAndStanding,
            Ladder.Floor,
            Ladder.Pushing,
            Ladder.Balance,
        ),
        weighsIn = true,
    )

    val walker = WayOfGettingAround(
        way = GettingAround.Walker,
        renamed = emptySet(),
        ladders = listOf(
            Ladder.Walking,
            Ladder.ChairAndStanding,
            Ladder.Pushing,
            Ladder.Balance,
        ),
        weighsIn = true,
    )

    val wheelchair = WayOfGettingAround(
        way = GettingAround.Wheelchair,
        renamed = setOf(AbilityDomain.GetUp),
        ladders = listOf(Ladder.Wheeling, Ladder.Seated),
        weighsIn = true,
    )

    val inBed = WayOfGettingAround(
        way = GettingAround.InBed,
        renamed = AbilityDomain.entries.toSet(),
        ladders = listOf(Ladder.InBed),
        weighsIn = false,
    )

    fun forWay(way: GettingAround): WayOfGettingAround = when (way) {
        GettingAround.OnFeet -> onFeet
        GettingAround.Walker -> walker
        GettingAround.Wheelchair -> wheelchair
        GettingAround.InBed -> inBed
    }

    val all: List<WayOfGettingAround> = listOf(onFeet, walker, wheelchair, inBed)
}
