package com.kamsiob.steadyhealth.engine

/** What the app does about a person coming back after time away. */
sealed interface Returning {

    /** They were here recently enough that nothing needs saying. */
    data object Ordinary : Returning

    /**
     * They have been away. [stepsBack] is what the ladder does about it, and
     * [easingUntilDay] is the fortnight after a long gap during which nothing is
     * offered.
     *
     * [askAgain] is the ninety day rule: the capability questions are re-run
     * before any step is suggested, because a year-old answer about stairs is not
     * an answer about today.
     *
     * [askTheWayAgain] is the other half of that, and a separate question with a
     * separate answer: how somebody gets around is the one setting that decides
     * which library, which measures and which sentences exist, and half a year is
     * long enough for it to have changed in either direction. LOGIC.md 3b re-asks
     * it after a gap over ninety days; ADDENDUM-03 Part 15 re-runs O2 after sixty,
     * which is the same rule said stricter, so sixty is what this uses. Asking is
     * all it is: the previous answer is filled in and one tap keeps it.
     */
    data class AfterAGap(
        val days: Int,
        val stepsBack: Int,
        val easingUntilDay: Long?,
        val askAgain: Boolean,
        val askTheWayAgain: Boolean,
    ) : Returning
}

/**
 * Gap decay, from LOGIC.md section 6, run when the app opens rather than after a
 * session.
 *
 * It has to be here and not in [ProgressionEngine.decide], because somebody who
 * has been away for a month meets the decision before they do anything, not
 * after. The engine returns what happened; the screen decides the words, and
 * DESIGN.md is explicit that none of them frame it as loss.
 */
object ReturningEngine {

    /** No sessions for this long: one step back. */
    const val SHORT_GAP_DAYS = ProgressionEngine.SHORT_GAP_DAYS

    /** This long: two steps back, and a fortnight where nothing is offered. */
    const val LONG_GAP_DAYS = ProgressionEngine.LONG_GAP_DAYS

    const val EASING_DAYS = ProgressionEngine.EASING_DAYS

    /** After this long the capability questions are asked again. */
    const val ASK_AGAIN_DAYS = 90

    /**
     * After this long, how somebody gets around is asked again as well.
     *
     * ADDENDUM-03 Part 15 and LOGIC.md 15b: a gap of sixty days or more re-runs O2
     * and O3 with the previous answers filled in. Nothing is changed for them and
     * nothing is lost by keeping the same answer; the app simply stops assuming.
     */
    const val ASK_THE_WAY_AGAIN_DAYS = 60

    fun decide(lastSessionDay: Long?, today: Long): Returning {
        val gap = (today - (lastSessionDay ?: return Returning.Ordinary)).toInt()
        if (gap < SHORT_GAP_DAYS) return Returning.Ordinary
        val long = gap >= LONG_GAP_DAYS
        return Returning.AfterAGap(
            days = gap,
            stepsBack = if (long) 2 else 1,
            easingUntilDay = if (long) today + EASING_DAYS else null,
            askAgain = gap >= ASK_AGAIN_DAYS,
            askTheWayAgain = gap >= ASK_THE_WAY_AGAIN_DAYS,
        )
    }
}
