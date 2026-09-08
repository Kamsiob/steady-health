package com.kamsiob.steadyhealth.session

/**
 * The same thing, then and now. ADDENDUM-03 Part 9.
 *
 * Entirely the person's own history. Nothing here compares them to anybody, to a
 * norm, or to a number the app chose, because the only comparison that has ever meant
 * anything to somebody doing this is the one with themselves in March.
 *
 * It rotates weekly so the same card is not there every day, and it says nothing at
 * all until there is a real then to look back at.
 */
data class LookBack(val movementId: String, val then: Int, val now: Int, val daysApart: Int)

object LookBacks {

    /** Under this many days apart, "then" is just last week and not worth saying. */
    const val FAR_ENOUGH = 21

    /**
     * One look back, or none, chosen by the week so it changes on its own.
     *
     * Only movements that have a first and a last far enough apart are candidates,
     * and only ones that went up: this card exists to show somebody something they
     * did, and a card that says they got worse is not that card. What went down is
     * still in the history, still on Progress, and still theirs to read.
     */
    fun of(history: List<Done>, today: Long): LookBack? {
        val candidates = history
            .groupBy { it.movementId }
            .mapNotNull { (id, done) ->
                val ordered = done.sortedBy { it.epochDay }
                val first = ordered.first()
                val last = ordered.last()
                val apart = (last.epochDay - first.epochDay).toInt()
                if (apart >= FAR_ENOUGH && last.result > first.result) {
                    LookBack(id, first.result, last.result, apart)
                } else {
                    null
                }
            }
            .sortedBy { it.movementId }
        if (candidates.isEmpty()) return null
        val week = (today / Week.DAYS).toInt()
        return candidates[Math.floorMod(week, candidates.size)]
    }
}
