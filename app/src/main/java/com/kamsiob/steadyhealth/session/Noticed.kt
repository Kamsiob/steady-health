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

    companion object {

        /** Under this many sessions the app has nothing honest to say, and says nothing. */
        const val ENOUGH_TO_SAY = 3

        /** A movement has to have been done this many times before its numbers mean anything. */
        const val ENOUGH_OF_ONE = 3

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
