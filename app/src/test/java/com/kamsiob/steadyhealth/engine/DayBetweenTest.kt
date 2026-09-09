package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The day between sessions, and every day it says nothing.
 *
 * Part 8 item 1 gives this one sentence and no target. Most of what follows is a test
 * that it stays quiet, because a step count is one number away from being a target and
 * the quiet days are what keep it from becoming one.
 */
class DayBetweenTest {

    @Test
    fun aDayWellPastOrdinaryIsWorthTheLine() {
        val history = week(3000) + DayUp(TODAY, 5000)

        assertThat(DayBetween.today(history, TODAY)).isEqualTo(UpAndAbout.MoreThanUsual)
    }

    @Test
    fun anOrdinaryDaySaysNothing() {
        val history = week(3000) + DayUp(TODAY, 3200)

        assertThat(DayBetween.today(history, TODAY)).isEqualTo(UpAndAbout.Quiet)
    }

    @Test
    fun aQuietDaySaysNothingAtAll() {
        // There is no line for a quiet day and there is never going to be one.
        val history = week(3000) + DayUp(TODAY, 400)

        assertThat(DayBetween.today(history, TODAY)).isEqualTo(UpAndAbout.Quiet)
    }

    @Test
    fun itIsNeverSaidTwoDaysRunning() {
        val history = week(3000) + DayUp(TODAY, 9000)

        assertThat(DayBetween.today(history, TODAY, saidOn = setOf(TODAY - 1)))
            .isEqualTo(UpAndAbout.Quiet)
        assertThat(DayBetween.today(history, TODAY, saidOn = setOf(TODAY - 2)))
            .isEqualTo(UpAndAbout.MoreThanUsual)
    }

    @Test
    fun nothingIsSaidBeforeThereIsAWeekToCompareWith() {
        val short = (1..DayBetween.ENOUGH_DAYS - 1).map { DayUp(TODAY - it, 3000) }

        assertThat(DayBetween.today(short + DayUp(TODAY, 9000), TODAY))
            .isEqualTo(UpAndAbout.Quiet)
    }

    @Test
    fun aPhoneInADrawerAllWeekDoesNotMakeTodayRemarkable() {
        // Ordinary of almost nothing gives no ratio worth taking, and somebody who
        // left their phone at home for a week has not suddenly changed.
        val history = week(20) + DayUp(TODAY, 900)

        assertThat(DayBetween.today(history, TODAY)).isEqualTo(UpAndAbout.Quiet)
    }

    @Test
    fun aDayThePhoneWasNotCarriedSaysNothing() {
        val history = week(3000) + DayUp(TODAY, 50)

        assertThat(DayBetween.today(history, TODAY)).isEqualTo(UpAndAbout.Quiet)
    }

    @Test
    fun todayMissingFromTheHistoryIsNotAnError() {
        assertThat(DayBetween.today(week(3000), TODAY)).isEqualTo(UpAndAbout.Quiet)
        assertThat(DayBetween.today(emptyList(), TODAY)).isEqualTo(UpAndAbout.Quiet)
    }

    @Test
    fun oneLongDayOutDoesNotMoveWhatOrdinaryMeans() {
        // A median rather than a mean, so a marathon last Tuesday does not raise the
        // bar for the rest of the month.
        val history = listOf(
            DayUp(TODAY - 7, 30_000),
            DayUp(TODAY - 6, 3000),
            DayUp(TODAY - 5, 3000),
            DayUp(TODAY - 4, 3000),
            DayUp(TODAY - 3, 3000),
            DayUp(TODAY - 2, 3000),
            DayUp(TODAY - 1, 3000),
            DayUp(TODAY, 5000),
        )

        assertThat(DayBetween.today(history, TODAY)).isEqualTo(UpAndAbout.MoreThanUsual)
    }

    @Test
    fun theOrderOfTheHistoryDoesNotMatter() {
        val history = (week(3000) + DayUp(TODAY, 5000)).reversed()

        assertThat(DayBetween.today(history, TODAY)).isEqualTo(UpAndAbout.MoreThanUsual)
    }

    @Test
    fun theMiddleOfAnEvenListIsTheMeanOfItsTwoMiddleValues() {
        assertThat(DayBetween.median(listOf(1, 3))).isEqualTo(2.0)
        assertThat(DayBetween.median(listOf(1, 2, 3))).isEqualTo(2.0)
        assertThat(DayBetween.median(emptyList())).isEqualTo(0.0)
    }

    private fun week(steps: Int) =
        (1..DayBetween.ENOUGH_DAYS).map { DayUp(TODAY - it, steps) }

    private companion object {
        const val TODAY = 20_000L
    }
}
