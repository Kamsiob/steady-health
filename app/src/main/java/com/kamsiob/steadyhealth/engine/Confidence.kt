package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.AbilityState

/**
 * One monthly answer about one tracked item: how it went, and how sure it felt.
 *
 * [sureness] is optional and stays optional. ADDENDUM-03 Part 18 calls it "a second
 * optional monthly rating", and somebody who rates the doing and skips the feeling
 * has answered the question that was asked of them.
 */
data class ItemMonth(
    val itemId: Long,
    val epochDay: Long,
    val rating: Int,
    val sureness: Int? = null,
)

/**
 * Which way a rating went between two months.
 *
 * Three values and not a number, because a number here would be a score and Part 18
 * says this is "never scored". Nothing anywhere sums these or averages them.
 */
enum class Way {
    Up,
    Level,
    Down,
    ;

    companion object {
        /**
         * How far a rating out of ten has to move before it means anything.
         *
         * Two points. One is inside the noise of asking somebody the same question a
         * month apart, and calling a one point wobble a change would make the app say
         * something happened on most months when nothing did.
         */
        const val ENOUGH = 2

        fun of(from: Int, to: Int): Way = when {
            to - from >= ENOUGH -> Up
            from - to >= ENOUGH -> Down
            else -> Level
        }
    }
}

/**
 * What the app says about an ability and the confidence beside it.
 *
 * A closed type rather than a string, so that the engine cannot say anything that is
 * not one of these, and so the words themselves stay with the rest of the app's words.
 */
sealed interface SurerLine {

    /** The ability moved and so did how sure they feel. Part 18's first example. */
    data class BothMoved(val itemText: String) : SurerLine

    /**
     * The ability moved and the feeling has not caught up.
     *
     * Part 18's second example, and the reason this feature is worth building. The
     * sentence ends "That often follows later", which is the only thing the app ever
     * says about what happens next, and it is true: doing precedes feeling able.
     */
    data class NotYet(val itemText: String) : SurerLine

    /**
     * Sure about it before the number moved.
     *
     * Not in Part 18's two examples and worth saying anyway. Somebody who feels surer
     * about the stairs has had a month of using the stairs more easily, whatever the
     * chair stand count did, and an app that only spoke when its own measure moved
     * would be telling them their month did not count.
     */
    data class SurerAnyway(val itemText: String) : SurerLine

    /** Nothing worth a sentence this month. */
    data object Quiet : SurerLine
}

/**
 * Confidence, from ADDENDUM-03 Part 18 and LOGIC.md 3b.
 *
 * "Each tracked item gets a second optional monthly rating: How sure do you feel about
 * it, zero to ten. Both lines on the same chart in Progress. Produces the most useful
 * sentence the app can say."
 *
 * Deterministic, never scored, never clinical, and pure, so the sentence a month
 * produces is the same sentence whenever it is asked for.
 *
 * The thing this engine exists to avoid is the app telling somebody how they feel.
 * Every branch below is a comparison between two numbers the person typed themselves,
 * a month apart, and the sentence reports both. There is no branch where the app
 * disagrees with the feeling, tries to talk somebody out of it, or treats a flat
 * confidence as a problem, and the one forward-looking clause it has ("that often
 * follows later") is about the ordinary order of things and not about this person.
 */
object Confidence {

    /**
     * The sentence for one item, from its last two monthly answers.
     *
     * [state] is what the ability the item belongs to did over the same window. The
     * two halves are separate on purpose: the ability comes from measures the app took
     * and the confidence comes from what somebody said, and the whole value of the
     * sentence is that it puts two different kinds of evidence side by side.
     */
    fun line(state: AbilityState, was: ItemMonth?, now: ItemMonth?, itemText: String): SurerLine {
        val surer = wayOf(was?.sureness, now?.sureness)
        return when {
            // No second rating, or only one month of them. There is nothing to
            // compare, and inventing a direction from a single number is the one
            // thing this engine must never do.
            surer == null -> SurerLine.Quiet

            state == AbilityState.Better && surer == Way.Up -> SurerLine.BothMoved(itemText)
            state == AbilityState.Better -> SurerLine.NotYet(itemText)
            surer == Way.Up -> SurerLine.SurerAnyway(itemText)
            else -> SurerLine.Quiet
        }
    }

    /**
     * Which way the confidence went, or null when it cannot be said.
     *
     * Null rather than Level for a missing rating, because "we do not know" and "it
     * did not move" produce different sentences and collapsing them would have the app
     * report a flat month to somebody who simply skipped the question.
     */
    fun wayOf(was: Int?, now: Int?): Way? =
        if (was == null || now == null) null else Way.of(was, now)

    /**
     * The two lines a chart draws for one item. Part 18: "both lines on the same
     * chart in Progress."
     *
     * Months with no sureness leave a gap in that line rather than a zero. A zero is
     * an answer somebody can give and it means the opposite of not answering.
     */
    fun chart(months: List<ItemMonth>): Pair<List<Pair<Long, Int>>, List<Pair<Long, Int>>> {
        val ordered = months.sortedBy { it.epochDay }
        val doing = ordered.map { it.epochDay to it.rating }
        val sure = ordered.mapNotNull { month -> month.sureness?.let { month.epochDay to it } }
        return doing to sure
    }

    /** The rating scale, both questions. LOGIC.md 3b. */
    const val MOST = 10
    const val LEAST = 0
}
