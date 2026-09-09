package com.kamsiob.steadyhealth.engine

/** One day's passive count, from the phone. */
data class DayUp(val epochDay: Long, val steps: Int)

/**
 * What, if anything, the app says about a day between sessions.
 *
 * ADDENDUM-03 Part 8 item 1: "Steps and time spent up, from the phone, one line on
 * Today with no goal attached." A closed type rather than a string, because the whole
 * risk in this feature is a fifth sentence appearing that quietly sets a target.
 */
sealed interface UpAndAbout {

    /** More than this person's own ordinary day. The only thing worth a line. */
    data object MoreThanUsual : UpAndAbout

    /** Nothing to say, which is most days. */
    data object Quiet : UpAndAbout
}

/**
 * The day between sessions. ADDENDUM-03 Part 8 item 1.
 *
 * "One line on Today with no goal attached." Everything below exists to keep that
 * promise against the obvious temptation, which is that a step count is one number
 * away from being a target and every other app has taken that step.
 *
 * SO: there is no number in the output, no comparison to anybody else, no run of days,
 * no line for a quiet day, and no line for a day that was ordinary. The app has one
 * thing to say here and it says it only when it is true.
 *
 * "This person's own ordinary day" is the median of the days before it rather than a
 * figure from anywhere else. A median rather than a mean because one long day out
 * moves a mean and does not move a median, and the question is what an ordinary day
 * looks like for somebody, not what their average is.
 *
 * The line is never said two days running. Said every day it is true, it stops being
 * an observation and becomes a scoreboard, and somebody who has three good days would
 * be told about the third and hear nothing on the fourth, which reads as a rebuke.
 */
object DayBetween {

    /** How many days of history are needed before anything is said at all. */
    const val ENOUGH_DAYS = 7

    /** How far past ordinary counts as more. A quarter, which is a walk to the shop. */
    const val NOTICEABLY_MORE = 1.25

    /** A day with almost nothing on it is a day the phone was on a table. */
    const val TOO_FEW_TO_MEAN_ANYTHING = 200

    /**
     * The line for today, or nothing.
     *
     * [history] is every day the phone counted, in any order, today included or not.
     * [saidOn] is the days this line has already been said, which is what keeps it
     * from being said twice running.
     */
    fun today(history: List<DayUp>, today: Long, saidOn: Set<Long> = emptySet()): UpAndAbout {
        if (today - 1 in saidOn) return UpAndAbout.Quiet

        val now = history.firstOrNull { it.epochDay == today } ?: return UpAndAbout.Quiet
        if (now.steps < TOO_FEW_TO_MEAN_ANYTHING) return UpAndAbout.Quiet

        val before = history.filter { it.epochDay < today }.map { it.steps }.sorted()
        if (before.size < ENOUGH_DAYS) return UpAndAbout.Quiet

        val ordinary = median(before)
        // An ordinary day of nothing gives no ratio worth taking. A phone that has
        // been in a drawer all week does not get to make today remarkable.
        if (ordinary < TOO_FEW_TO_MEAN_ANYTHING) return UpAndAbout.Quiet

        return if (now.steps >= ordinary * NOTICEABLY_MORE) {
            UpAndAbout.MoreThanUsual
        } else {
            UpAndAbout.Quiet
        }
    }

    /** The middle of a sorted list, and the mean of the middle two when it is even. */
    fun median(sorted: List<Int>): Double = when {
        sorted.isEmpty() -> 0.0
        sorted.size % 2 == 1 -> sorted[sorted.size / 2].toDouble()
        else -> (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
    }
}
