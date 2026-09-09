package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.AbilityDomain

/**
 * The first month, as a then and now about the person's own words.
 *
 * ADDENDUM-03 Part 16, the second of the four places this app is allowed to be warm.
 * Not a summary of the month and not an assessment of it: one thing they said they
 * wanted, what they said about it at the start, and what they say about it now.
 */
data class ThenAndNow(
    val itemText: String,
    val then: Int,
    val now: Int,
) {
    /**
     * Whether the number moved at all, either way.
     *
     * Both are worth a card and the words differ. What is never done is leaving the
     * card out because the number held: a month of doing the work is a month of doing
     * the work, and a card that only appears on a good month is a card that tells
     * somebody the app was watching for one.
     */
    val moved: Boolean get() = now != then

    val forward: Boolean get() = now > then
}

/**
 * A tracked item crossing the point where it stops being a number.
 *
 * Part 16's fourth place, and the only one of the four with an exclamation in the
 * example, which is not kept: "You said you wanted to get off the floor without your
 * hands. You just did it." The app names what was done. It does not evaluate it and
 * it does not congratulate.
 */
data class JustDidIt(val itemText: String, val sentenceId: String)

/**
 * The four places, and the rules that keep them to four. DESIGN.md 6.
 *
 * "Everywhere else plain. No congratulation for ordinary sessions, no emoji, no
 * exclamation marks, no praise. Affirmations name what was done, never evaluate it."
 *
 * The first and third places live where they happen, in the session's done screen and
 * in the returning flow. The two here are the two that belong to Progress, and they
 * are pure functions for the reason everything else in this package is: a warm
 * sentence that appears at the wrong moment is worse than no warm sentence, and a
 * pure function is a thing that can be tested at every moment there is.
 */
object Warmth {

    /** A month, as this app counts one. Part 16 says the first month. */
    const val A_MONTH = 30L

    /**
     * How long the card stays after the month is up.
     *
     * Somebody who opens Progress on day thirty one should not have missed it, and
     * somebody who opens it on day ninety should not still be reading about their
     * first month. Two weeks is long enough that a fortnight away does not cost it.
     */
    const val STAYS_FOR = 14L

    /**
     * The then and now card, or null.
     *
     * [firstDay] is the day the person started. [months] are the monthly answers for
     * one item, in any order. Null before the first month is up, after the window has
     * passed, and whenever there are not two answers to put side by side, because a
     * card headed "then and now" with one number on it is not a then and now.
     */
    fun firstMonth(
        itemText: String,
        firstDay: Long,
        today: Long,
        months: List<ItemMonth>,
    ): ThenAndNow? {
        val age = today - firstDay
        if (age < A_MONTH || age > A_MONTH + STAYS_FOR) return null
        val ordered = months.sortedBy { it.epochDay }
        val then = ordered.firstOrNull() ?: return null
        val now = ordered.lastOrNull() ?: return null
        if (then.epochDay == now.epochDay) return null
        return ThenAndNow(itemText = itemText, then = then.rating, now = now.rating)
    }

    /**
     * The moment a measure crosses the sentence that names it, said once.
     *
     * [saidAlready] is every sentence id that has been said before, which is what
     * makes "once" true across a month where the number wobbles back and forth over
     * the line. Crossing back and forward again is one crossing, because it is one
     * thing somebody can now do.
     */
    fun justDidIt(
        itemText: String,
        domain: AbilityDomain,
        values: Map<String, Double>,
        saidAlready: Set<String>,
    ): JustDidIt? {
        val reached = LifeSentences.forDomain(domain, values) ?: return null
        if (reached.id in saidAlready) return null
        return JustDidIt(itemText = itemText, sentenceId = reached.id)
    }
}
