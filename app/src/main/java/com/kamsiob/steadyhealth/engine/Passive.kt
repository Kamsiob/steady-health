package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.session.Done

/**
 * Something the app already watched somebody do, offered back to them.
 *
 * [onDay] is when it happened, so the screen can say which day rather than asking
 * somebody to take a number on trust. [counted] is false when the person typed the
 * number rather than the phone counting it, which changes the sentence and is the one
 * thing about a passive measure that has to be admitted out loud.
 */
data class Seen(
    val measureId: String,
    val value: Double,
    val onDay: Long,
    val counted: Boolean,
)

/**
 * Passive measure capture, and the monthly confirm. ADDENDUM-03 Phase 6.
 *
 * The monthly check asks somebody to do a chair stand for thirty seconds and counts
 * it. They have already done chair stands, several times, in ordinary sessions, and
 * the phone counted those too. So the check does not have to start from nothing: it
 * can say what it already saw and ask whether that still sounds right.
 *
 * Why this is worth building. LOGIC.md gives the check ten minutes and MASTER_SPEC
 * says four numbers a month somebody actually takes beat eight they stop taking. Every
 * measure that can be confirmed rather than performed is two minutes back, and the
 * first thing people stop doing is the part that feels like a test.
 *
 * WHAT IT IS NOT. It never records a measure by itself. Nothing here writes anything.
 * A number that reaches Progress has been confirmed by the person on the check screen,
 * because a measure the app decided on its own is a measure somebody cannot argue
 * with, and this app's whole shape is that the person is the one who knows.
 *
 * The best rather than the most recent, within the window. A measure is what somebody
 * can do, and the day they were tired is not the answer to that; the check itself is
 * one attempt on one morning and is read the same way.
 */
object Passive {

    /**
     * How far back to look. Long enough to catch a month of ordinary sessions and
     * short enough that nothing offered is out of date by the time it is offered.
     */
    const val WINDOW_DAYS = 35L

    /**
     * The measures that can be seen at all, and the movement each one watches.
     *
     * Deliberately short. A measure only belongs here when an ordinary session asks
     * for exactly the same thing the check asks for, in the same unit, so that the
     * number means what the check would have meant. Everything not on this list is
     * still performed at the check, which is the honest default.
     *
     * The chair stand is the awkward one and it is here anyway. The check counts full
     * stands in thirty seconds and a session counts stands to a target with no clock
     * on it, so the two are not the same measurement. That is why nothing here is
     * ever recorded without being confirmed, and why the screen says the day and the
     * number rather than presenting it as a result already taken.
     */
    val watching: Map<String, String> = mapOf(
        Measures.chairStand.id to "sit_to_stand",
        Measures.wallPushUps.id to "wall_push_up",
        Measures.bandRows.id to "band_row",
        Measures.singleLegStance.id to "one_leg",
        Measures.seatedReach.id to "seated_forward_lean",
        Measures.gripHold.id to "towel_squeeze",
    )

    /**
     * What the app has already seen for one measure, or null.
     *
     * Null whenever there is nothing to offer, which includes a measure nothing
     * watches, a person who has not done that movement lately, and a result of zero.
     * Zero is not a thing to confirm: it is what an interrupted session leaves behind.
     */
    fun seen(measureId: String, history: List<Done>, today: Long): Seen? {
        val movementId = watching[measureId] ?: return null
        val since = today - WINDOW_DAYS
        val best = history
            .filter { it.movementId == movementId && it.epochDay in since..today && it.result > 0 }
            .maxByOrNull { it.result }
            ?: return null
        return Seen(
            measureId = measureId,
            value = best.result.toDouble(),
            onDay = best.epochDay,
            counted = !best.selfReported,
        )
    }

    /** Everything there is to offer, for the measures this check is going to ask. */
    fun seen(measures: List<Measure>, history: List<Done>, today: Long): Map<String, Seen> =
        measures.mapNotNull { seen(it.id, history, today) }.associateBy { it.measureId }
}
