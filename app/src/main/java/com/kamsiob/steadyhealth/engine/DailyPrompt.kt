package com.kamsiob.steadyhealth.engine

/** One daily prompt that went out, and whether it was opened. */
data class Prompted(val epochDay: Long, val opened: Boolean)

/** What the daily prompt should do today, and which words to use. */
sealed interface Prompt {
    /** Send it, in the ordinary rotation. */
    data object Ordinary : Prompt

    /** Send it after a week of quiet, with the one sentence that says so. */
    data object StillHere : Prompt

    /** Say nothing today. */
    data object Quiet : Prompt

    /** Stop for good and say so in Settings. */
    data object TurnItOff : Prompt
}

/**
 * The one daily prompt. ADDENDUM-03 Part 13, replacing LOGIC.md 12's model.
 *
 * On by default, at the anchor time, under ten words. It never mentions a day
 * somebody did not do anything, because there is no rule in this app that fires on
 * one and no string that names one.
 *
 * It also knows when to stop. Four dismissals without opening and it goes quiet for a
 * week; it comes back once saying so; four more after that and it turns itself off
 * and tells Settings why. An app that keeps asking after being ignored eight times is
 * not a reminder, it is a nag, and somebody uninstalls it rather than finding the
 * switch.
 *
 * Everything here is a pure function of what already happened, because the thing that
 * calls it runs at seven in the morning with nobody watching.
 */
object DailyPrompt {

    /** Dismissed this many days running and it goes quiet. */
    const val QUIET_AFTER = 4

    /** Dismissed this many in total and it stops for good. */
    const val OFF_AFTER = 8

    /** How long the quiet week is. */
    const val QUIET_DAYS = 7

    fun today(sent: List<Prompted>, today: Long): Prompt {
        if (sent.any { it.epochDay == today }) return Prompt.Quiet

        val run = sent.sortedBy { it.epochDay }.takeLastWhile { !it.opened }.size
        val last = sent.maxOfOrNull { it.epochDay }

        return when {
            run >= OFF_AFTER -> Prompt.TurnItOff
            run < QUIET_AFTER -> Prompt.Ordinary
            // Exactly four: a week of nothing, and then one that says so.
            run > QUIET_AFTER -> Prompt.Ordinary
            last == null -> Prompt.Ordinary
            today - last < QUIET_DAYS -> Prompt.Quiet
            else -> Prompt.StillHere
        }
    }
}
