package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The two warm places that belong to Progress, and the boundaries around them.
 *
 * DESIGN.md 6 allows four of these in the whole app. The tests that matter are the
 * ones holding the boundary: too early, too late, said twice, and a month where the
 * number did not move, which still gets its card.
 */
class WarmthTest {

    @Test
    fun theCardAppearsWhenTheMonthIsUp() {
        val card = Warmth.firstMonth(STAIRS, firstDay = 0, today = 30, months = twoMonths())

        assertThat(card).isEqualTo(ThenAndNow(STAIRS, then = 3, now = 7))
    }

    @Test
    fun thereIsNoCardBeforeTheMonthIsUp() {
        assertThat(Warmth.firstMonth(STAIRS, 0, today = 29, months = twoMonths())).isNull()
    }

    @Test
    fun theCardGoesAwayAfterItsFortnight() {
        assertThat(Warmth.firstMonth(STAIRS, 0, today = 44, months = twoMonths())).isNotNull()
        assertThat(Warmth.firstMonth(STAIRS, 0, today = 45, months = twoMonths())).isNull()
    }

    @Test
    fun aMonthWhereNothingMovedStillGetsItsCard() {
        // A card that only appears on a good month tells somebody the app was
        // watching for one.
        val flat = listOf(month(0, 5), month(30, 5))

        val card = Warmth.firstMonth(STAIRS, 0, today = 30, months = flat)

        assertThat(card).isEqualTo(ThenAndNow(STAIRS, then = 5, now = 5))
        assertThat(card?.moved).isFalse()
    }

    @Test
    fun aMonthWhereItWentBackStillGetsItsCard() {
        val back = listOf(month(0, 7), month(30, 4))

        val card = Warmth.firstMonth(STAIRS, 0, today = 30, months = back)

        assertThat(card?.moved).isTrue()
        assertThat(card?.forward).isFalse()
    }

    @Test
    fun oneAnswerIsNotAThenAndNow() {
        assertThat(Warmth.firstMonth(STAIRS, 0, today = 30, months = listOf(month(0, 3)))).isNull()
        assertThat(Warmth.firstMonth(STAIRS, 0, today = 30, months = emptyList())).isNull()
    }

    @Test
    fun twoAnswersOnTheSameDayAreOneAnswer() {
        val same = listOf(month(30, 3), month(30, 7))

        assertThat(Warmth.firstMonth(STAIRS, 0, today = 30, months = same)).isNull()
    }

    @Test
    fun theAnswersDoNotHaveToArriveInOrder() {
        val jumbled = listOf(month(30, 7), month(0, 3))

        assertThat(Warmth.firstMonth(STAIRS, 0, today = 30, months = jumbled)?.then).isEqualTo(3)
    }

    @Test
    fun crossingIsSaidOnce() {
        val values = mapOf(Measures.chairStand.id to CROSSED)
        val first = Warmth.justDidIt(FLOOR, DOMAIN, values, saidAlready = emptySet())

        assertThat(first).isNotNull()
        assertThat(Warmth.justDidIt(FLOOR, DOMAIN, values, setOf(first!!.sentenceId))).isNull()
    }

    @Test
    fun aNumberBelowEveryThresholdSaysNothing() {
        val values = mapOf(Measures.chairStand.id to 1.0)

        assertThat(Warmth.justDidIt(FLOOR, DOMAIN, values, emptySet())).isNull()
    }

    @Test
    fun crossingBackAndForwardIsStillOneCrossing() {
        // Once it has been said, the number wobbling over the line again is not a
        // second thing somebody can now do.
        val values = mapOf(Measures.chairStand.id to CROSSED)
        val said = Warmth.justDidIt(FLOOR, DOMAIN, values, emptySet())!!.sentenceId

        assertThat(Warmth.justDidIt(FLOOR, DOMAIN, values, setOf(said))).isNull()
    }

    private fun twoMonths() = listOf(month(0, 3), month(30, 7))

    private fun month(day: Long, rating: Int) =
        ItemMonth(itemId = 1L, epochDay = day, rating = rating)

    private companion object {
        const val STAIRS = "Stairs without stopping"
        const val FLOOR = "Get off the floor"
        const val CROSSED = 14.0
        val DOMAIN = com.kamsiob.steadyhealth.domain.AbilityDomain.GetUp
    }
}
