package com.kamsiob.steadyhealth.session

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The week. ADDENDUM-03 Part 10.
 *
 * The rules worth holding are the ones that make this not a streak: a quiet week
 * does not touch the week before it, and three sessions in one day is one day.
 */
class WeekTest {

    @Test
    fun aWeekIsCountedInDaysAndNotInSessions() {
        val threeOnSunday = listOf(
            done("a", TODAY),
            done("b", TODAY),
            done("c", TODAY),
        )
        assertThat(Week.of(threeOnSunday, TODAY, wanted = 3).done).isEqualTo(1)
    }

    @Test
    fun onlyTheLastSevenDaysCount() {
        val history = listOf(done("a", TODAY - 6), done("b", TODAY - 7), done("c", TODAY - 20))
        assertThat(Week.of(history, TODAY, wanted = 3).done).isEqualTo(1)
    }

    @Test
    fun aQuietWeekTakesNothingFromTheWeekBeforeIt() {
        val busy = (1..3).map { done("a", TODAY - DAYS - it) }
        val quiet = Week.of(busy, TODAY, wanted = 3)

        assertWithMessage("this week is its own week").that(quiet.done).isEqualTo(0)
        assertWithMessage("nothing is negative, lost, or reset").that(quiet.toGo).isEqualTo(3)
    }

    @Test
    fun pastTheNumberIsExtraRatherThanMore() {
        val week = Week.of((0..4).map { done("a", TODAY - it) }, TODAY, wanted = 3)
        assertThat(week.met).isTrue()
        assertThat(week.toGo).isEqualTo(0)
    }

    @Test
    fun aStoredNumberOutsideTheThreeChoicesIsBroughtBackIn() {
        assertThat(Week.of(emptyList(), TODAY, wanted = 99).wanted).isEqualTo(Week.CHOICES.last())
        assertThat(Week.of(emptyList(), TODAY, wanted = 0).wanted).isEqualTo(Week.CHOICES.first())
    }

    private fun done(id: String, day: Long) =
        Done(movementId = id, epochDay = day, result = 5, target = 5)

    private companion object {
        const val TODAY = 20_000L
        const val DAYS = 7
    }
}
