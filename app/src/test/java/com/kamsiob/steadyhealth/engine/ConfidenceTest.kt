package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.domain.AbilityState
import org.junit.Test

/**
 * Confidence, and mostly what it refuses to say.
 *
 * ADDENDUM-03 Part 18 asks for one sentence and it is easy to write. The tests worth
 * having are the ones that hold the engine to saying nothing: on a skipped question, on
 * a first month, on a wobble of one point, and on a month where the feeling went down.
 */
class ConfidenceTest {

    @Test
    fun bothMovedIsTheSentencePartEighteenAsksFor() {
        val line = Confidence.line(
            state = AbilityState.Better,
            was = month(day = 0, rating = 4, sureness = 4),
            now = month(day = 30, rating = 7, sureness = 7),
            itemText = STAIRS,
        )

        assertThat(line).isEqualTo(SurerLine.BothMoved(STAIRS))
    }

    @Test
    fun theNumberMovingWithoutTheFeelingIsItsOwnSentence() {
        val line = Confidence.line(
            state = AbilityState.Better,
            was = month(day = 0, rating = 4, sureness = 5),
            now = month(day = 30, rating = 7, sureness = 5),
            itemText = STAIRS,
        )

        assertThat(line).isEqualTo(SurerLine.NotYet(STAIRS))
    }

    @Test
    fun feelingSurerCountsEvenWhenTheMeasureHeld() {
        // A month of using the stairs more easily is a month, whatever the chair
        // stand count did. An app that only speaks when its own number moves is
        // telling somebody their month did not count.
        val line = Confidence.line(
            state = AbilityState.Same,
            was = month(day = 0, rating = 5, sureness = 3),
            now = month(day = 30, rating = 5, sureness = 8),
            itemText = STAIRS,
        )

        assertThat(line).isEqualTo(SurerLine.SurerAnyway(STAIRS))
    }

    @Test
    fun aSkippedSecondQuestionSaysNothingAtAll() {
        val line = Confidence.line(
            state = AbilityState.Better,
            was = month(day = 0, rating = 4, sureness = null),
            now = month(day = 30, rating = 8, sureness = 9),
            itemText = STAIRS,
        )

        assertThat(line).isEqualTo(SurerLine.Quiet)
    }

    @Test
    fun theFirstMonthHasNothingToCompareItselfWith() {
        val line = Confidence.line(
            state = AbilityState.Better,
            was = null,
            now = month(day = 30, rating = 8, sureness = 9),
            itemText = STAIRS,
        )

        assertThat(line).isEqualTo(SurerLine.Quiet)
    }

    @Test
    fun aWobbleOfOnePointIsNotAChange() {
        assertThat(Confidence.wayOf(5, 6)).isEqualTo(Way.Level)
        assertThat(Confidence.wayOf(6, 5)).isEqualTo(Way.Level)
        assertThat(Confidence.wayOf(5, 7)).isEqualTo(Way.Up)
        assertThat(Confidence.wayOf(7, 5)).isEqualTo(Way.Down)
    }

    @Test
    fun aFeelingThatWentDownIsNeverRemarkedOn() {
        // The app has nothing useful to say about it and every available sentence
        // would be the app disagreeing with somebody about their own month.
        listOf(AbilityState.Better, AbilityState.Same, AbilityState.Quieter).forEach { state ->
            val line = Confidence.line(
                state = state,
                was = month(day = 0, rating = 5, sureness = 8),
                now = month(day = 30, rating = 5, sureness = 3),
                itemText = STAIRS,
            )

            if (state == AbilityState.Better) {
                assertThat(line).isEqualTo(SurerLine.NotYet(STAIRS))
            } else {
                assertThat(line).isEqualTo(SurerLine.Quiet)
            }
        }
    }

    @Test
    fun aQuietMonthOnBothSidesSaysNothing() {
        val line = Confidence.line(
            state = AbilityState.Same,
            was = month(day = 0, rating = 5, sureness = 5),
            now = month(day = 30, rating = 5, sureness = 5),
            itemText = STAIRS,
        )

        assertThat(line).isEqualTo(SurerLine.Quiet)
    }

    @Test
    fun aZeroIsAnAnswerAndAMissingOneIsNot() {
        assertThat(Confidence.wayOf(0, 4)).isEqualTo(Way.Up)
        assertThat(Confidence.wayOf(4, 0)).isEqualTo(Way.Down)
        assertThat(Confidence.wayOf(null, 4)).isNull()
        assertThat(Confidence.wayOf(4, null)).isNull()
    }

    @Test
    fun theChartLeavesAGapForASkippedMonthRatherThanAZero() {
        val months = listOf(
            month(day = 60, rating = 7, sureness = 6),
            month(day = 0, rating = 4, sureness = 3),
            month(day = 30, rating = 5, sureness = null),
        )

        val (doing, sure) = Confidence.chart(months)

        assertThat(doing).containsExactly(0L to 4, 30L to 5, 60L to 7).inOrder()
        assertThat(sure).containsExactly(0L to 3, 60L to 6).inOrder()
    }

    @Test
    fun bothQuestionsUseTheSameScale() {
        assertThat(Confidence.LEAST).isEqualTo(0)
        assertThat(Confidence.MOST).isEqualTo(10)
    }

    private fun month(day: Long, rating: Int, sureness: Int?) =
        ItemMonth(itemId = 1L, epochDay = day, rating = rating, sureness = sureness)

    private companion object {
        const val STAIRS = "Stairs without stopping"
    }
}
