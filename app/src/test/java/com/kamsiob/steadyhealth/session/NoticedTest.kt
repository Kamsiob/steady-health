package com.kamsiob.steadyhealth.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The one noticed line. ADDENDUM-03 Part 9.
 *
 * The rules worth holding are the ones about silence: a new person is told nothing,
 * and a quiet week is never mentioned at all.
 */
class NoticedTest {

    @Test
    fun saysNothingAtAllUntilThereIsSomethingToSay() {
        assertThat(Noticed.of(emptyList(), TODAY)).isNull()
        assertThat(Noticed.of(listOf(done("heel_raises", TODAY, 10)), TODAY)).isNull()
    }

    @Test
    fun aMovementThatWentUpIsTheFirstThingItSays() {
        val history = listOf(
            done("heel_raises", TODAY - 8, 10),
            done("heel_raises", TODAY - 4, 12),
            done("heel_raises", TODAY, 14),
        )
        assertThat(Noticed.of(history, TODAY)).isEqualTo(Noticed.Climbed("heel_raises", 10, 14))
    }

    @Test
    fun aMovementThatWentDownIsNeverMentioned() {
        val history = listOf(
            done("heel_raises", TODAY - 8, 14),
            done("heel_raises", TODAY - 4, 12),
            done("heel_raises", TODAY, 10),
        )
        val noticed = Noticed.of(history, TODAY)
        assertThat(noticed).isNotInstanceOf(Noticed.Climbed::class.java)
    }

    @Test
    fun aQuieterWeekIsNotAThingItSays() {
        val history = listOf(
            done("a", TODAY - 13, 5),
            done("b", TODAY - 12, 5),
            done("c", TODAY - 11, 5),
            done("d", TODAY - 2, 5),
        )
        val noticed = Noticed.of(history, TODAY)
        assertThat(noticed).isNotInstanceOf(Noticed.MoreDaysThanLastWeek::class.java)
    }

    @Test
    fun moreDaysThanLastWeekIsSaidWhenItIsTrue() {
        val history = listOf(
            done("a", TODAY - 9, 5),
            done("b", TODAY - 5, 5),
            done("c", TODAY - 3, 5),
            done("d", TODAY - 1, 5),
        )
        assertThat(Noticed.of(history, TODAY)).isEqualTo(Noticed.MoreDaysThanLastWeek(3, 1))
    }

    @Test
    fun theCountOfSessionsIsTheLastResort() {
        val history = listOf(
            done("a", TODAY - 2, 5),
            done("b", TODAY - 1, 5),
            done("c", TODAY, 5),
        )
        assertThat(Noticed.of(history, TODAY)).isEqualTo(Noticed.HowMany(3))
    }

    @Test
    fun thereIsAlwaysSomethingTrueToSay() {
        val nothing = Noticed.all(emptyList(), TODAY, since = TODAY - 8)
        assertThat(nothing).containsExactly(Noticed.DayNumber(9))
    }

    @Test
    fun itSaysAtMostThreeThings() {
        val history = (1..12).map { done("heel_raises", TODAY - it, 20 - it) }
        assertThat(Noticed.all(history, TODAY, since = TODAY - 40).size)
            .isAtMost(Noticed.MOST_LINES)
    }

    @Test
    fun theDayNumberIsOnlyTheLastResort() {
        val history = listOf(
            done("a", TODAY - 2, 5),
            done("b", TODAY - 1, 5),
            done("c", TODAY, 5),
        )
        assertThat(Noticed.all(history, TODAY, since = TODAY - 8))
            .doesNotContain(Noticed.DayNumber(9))
    }

    private fun done(id: String, day: Long, result: Int) =
        Done(movementId = id, epochDay = day, result = result, target = result)

    private companion object {
        const val TODAY = 20_000L
    }
}
