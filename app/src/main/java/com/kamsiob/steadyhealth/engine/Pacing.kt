package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.NextDayFeel
import kotlin.math.roundToInt

/**
 * The limit somebody sets for themselves: how long, and how many days a week.
 *
 * Not a target. The app only ever suggests staying at or below it, so the number
 * moves in one direction on its own and in either direction when the person says
 * so.
 */
data class Envelope(val minutes: Int, val daysPerWeek: Int)

/**
 * Pacing mode, from LOGIC.md section 7.
 *
 * For a post-exertional pattern (ME/CFS, long COVID, and others) the whole
 * progression model in section 6 is wrong, and running it would make somebody
 * worse. So in pacing mode nothing increases: no offers, no unlock, no spike cap
 * because there is nothing to cap. The person names an envelope and the app stays
 * inside it.
 *
 * Source: NICE NG206 (2021), which withdrew graded exercise therapy, and the CDC.
 * The app says so in its own words on the screen: "This is not graded exercise.
 * Staying inside your energy limit is the method."
 */
object PacingEngine {

    /** A worse day takes a fifth off the envelope. LOGIC.md section 7. */
    const val REDUCTION = 0.20

    /** Two reports of much worse in this many days turns pacing mode on. */
    const val WINDOW_DAYS = 30

    const val WORSE_REPORTS_BEFORE_PACING = 2

    /** Where the envelope starts if the person does not change it. */
    val DEFAULT = Envelope(minutes = 5, daysPerWeek = 3)

    /** Never below this, because an envelope of nothing is not an answer. */
    const val FLOOR_MINUTES = 1

    /**
     * A fifth off, after a day that felt worse.
     *
     * Rounded down rather than to nearest, because the whole point of the rule is
     * that the number errs low.
     */
    fun reduce(envelope: Envelope): Envelope {
        val reduced = (envelope.minutes * (1 - REDUCTION)).toInt()
        return envelope.copy(minutes = reduced.coerceAtLeast(FLOOR_MINUTES))
    }

    /**
     * What to suggest today: the envelope, or less, and never more.
     *
     * There is no ramp and no accumulation. Somebody who has already been at
     * their limit this many days is told they are, rather than being asked for
     * one more.
     */
    fun suggestedMinutes(envelope: Envelope, daysDoneThisWeek: Int): Int =
        if (daysDoneThisWeek >= envelope.daysPerWeek) 0 else envelope.minutes

    /**
     * True when the pattern the person described has now happened twice inside a
     * month, which LOGIC.md makes a trigger.
     *
     * It is a trigger and not a suggestion because pacing mode is the careful
     * state: it removes offers and stops anything increasing. Entering it costs a
     * person nothing they cannot undo from settings, and staying out of it while
     * this pattern is happening is the thing that does harm.
     */
    fun shouldTurnOn(sessions: List<DoneSession>, today: Long): Boolean =
        sessions.count {
            it.nextDayFeel == NextDayFeel.Worse && today - it.epochDay <= WINDOW_DAYS
        } >= WORSE_REPORTS_BEFORE_PACING

    /** The envelope as a percentage of where it started, for the settings screen. */
    fun shareOfStart(envelope: Envelope, start: Envelope): Int =
        if (start.minutes <= 0) 0 else (envelope.minutes * PERCENT / start.minutes.toDouble()).roundToInt()

    private const val PERCENT = 100
}
