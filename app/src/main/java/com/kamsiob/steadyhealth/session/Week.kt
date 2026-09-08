package com.kamsiob.steadyhealth.session

/**
 * The week, and consistency without streaks. ADDENDUM-03 Part 10, LOGIC.md 16.
 *
 * A number of sessions a week, set once and changeable, and one line on Today saying
 * where the week is against it. Every week stands alone: nothing here can be broken,
 * lost or reset, and a quiet week does not touch the three beside it. That is the
 * whole difference between this and a streak, and it is a property of the type.
 */
data class Week(val done: Int, val wanted: Int) {

    /** True once the week has what it asked for. Anything past this is extra. */
    val met: Boolean get() = done >= wanted

    /** How many more would meet it, or nought. */
    val toGo: Int get() = (wanted - done).coerceAtLeast(0)

    companion object {

        /** Three, four or five. Nothing else is offered and nothing else is stored. */
        val CHOICES = listOf(3, 4, 5)

        const val DEFAULT = 3

        const val DAYS = 7

        /**
         * The week so far, counted in days with a session rather than in sessions.
         *
         * Two sessions in one day is one day. Otherwise somebody who does three on a
         * Sunday has met the week, which is not what the week is for.
         */
        fun of(history: List<Done>, today: Long, wanted: Int): Week {
            val since = today - DAYS + 1
            val days = history.map { it.epochDay }.filter { it in since..today }.distinct()
            return Week(done = days.size, wanted = wanted.coerceIn(CHOICES.first(), CHOICES.last()))
        }
    }
}
