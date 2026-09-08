package com.kamsiob.steadyhealth.session

/**
 * The one line on Today that is the person's own history said back to them.
 *
 * ADDENDUM-03 Part 9. Not a streak, not a badge, not a target, and never an
 * evaluation. Everything here is a fact the person could check themselves by
 * scrolling back, which is the test for whether it belongs.
 *
 * Exactly one line, or none. Two would be a feed, and a screen that always has
 * something to say ends up saying things that are not true.
 */
sealed interface Noticed {

    /** A movement whose numbers have gone up since the first time it was done. */
    data class Climbed(val movementId: String, val from: Int, val to: Int) : Noticed

    /** More days with a session this week than last. Days, never a run of them. */
    data class MoreDaysThanLastWeek(val thisWeek: Int, val lastWeek: Int) : Noticed

    /** How many sessions there have been, once there are enough to be worth saying. */
    data class HowMany(val sessions: Int) : Noticed

    /**
     * What day of this they are on, counted from the day they started.
     *
     * The line for when there is nothing else true to say. ADDENDUM-03 Part 9: never
     * empty, and one true small thing is better than a cheerful invented one.
     */
    data class DayNumber(val day: Int) : Noticed

    companion object {

        /** Under this many sessions the app has nothing honest to say, and says nothing. */
        const val ENOUGH_TO_SAY = 3

        /** A movement has to have been done this many times before its numbers mean anything. */
        const val ENOUGH_OF_ONE = 3

        /** Three is a glance. Four is a list. */
        const val MOST_LINES = 3

        private const val DAYS_IN_WEEK = 7

        /**
         * The first rule that matches, in order of what a person would rather hear.
         *
         * Something they can do more of beats a count of days, and a count of days
         * beats a count of sessions, because the last one is the app talking about
         * itself.
         */
        fun of(history: List<Done>, today: Long): Noticed? =
            climbed(history) ?: moreDays(history, today) ?: howMany(history)

        /**
         * Everything true worth saying today, at most three, never nothing.
         *
         * ADDENDUM-03 Part 9. The order is what a person would rather hear: something
         * they can do more of, then how the week is going, then a count. When none of
         * those is true, which day of this they are on, which is always true and
         * never flattering.
         */
        fun all(history: List<Done>, today: Long, since: Long?): List<Noticed> {
            val lines = listOfNotNull(
                climbed(history),
                moreDays(history, today),
                howMany(history),
            ).take(MOST_LINES)
            if (lines.isNotEmpty()) return lines
            val day = since?.let { (today - it + 1).toInt() }?.takeIf { it >= 1 }
            return listOfNotNull(day?.let { DayNumber(it) })
        }

        private fun climbed(history: List<Done>): Noticed? = history
            .groupBy { it.movementId }
            .filterValues { it.size >= ENOUGH_OF_ONE }
            .mapNotNull { (id, done) ->
                val ordered = done.sortedBy { it.epochDay }
                val from = ordered.first().result
                val to = ordered.last().result
                if (to > from) Climbed(id, from, to) else null
            }
            .maxByOrNull { it.to - it.from }

        private fun moreDays(history: List<Done>, today: Long): Noticed? {
            val days = history.map { it.epochDay }.toSet()
            val thisWeek = days.count { it > today - DAYS_IN_WEEK && it <= today }
            val lastWeek = days.count { it > today - 2 * DAYS_IN_WEEK && it <= today - DAYS_IN_WEEK }
            return if (thisWeek > lastWeek && lastWeek > 0) {
                MoreDaysThanLastWeek(thisWeek, lastWeek)
            } else {
                null
            }
        }

        private fun howMany(history: List<Done>): Noticed? {
            val sessions = history.map { it.epochDay }.distinct().size
            return if (sessions >= ENOUGH_TO_SAY) HowMany(sessions) else null
        }
    }
}
