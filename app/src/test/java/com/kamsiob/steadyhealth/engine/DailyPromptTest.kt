package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The daily prompt. ADDENDUM-03 Part 13.
 *
 * The rules worth holding are the ones about stopping: an app that keeps asking after
 * being ignored eight times gets uninstalled rather than turned off.
 */
class DailyPromptTest {

    @Test
    fun itGoesOutEveryDayWhileItIsBeingOpened() {
        val opened = (1..10).map { Prompted(TODAY - it, opened = true) }
        assertThat(DailyPrompt.today(opened, TODAY)).isEqualTo(Prompt.Ordinary)
    }

    @Test
    fun onlyOneGoesOutInADay() {
        val already = listOf(Prompted(TODAY, opened = false))
        assertThat(DailyPrompt.today(already, TODAY)).isEqualTo(Prompt.Quiet)
    }

    @Test
    fun fourDismissalsRunningBuyAWeekOfQuiet() {
        val ignored = (1..4).map { Prompted(TODAY - it, opened = false) }
        assertThat(DailyPrompt.today(ignored, TODAY)).isEqualTo(Prompt.Quiet)
    }

    @Test
    fun afterTheQuietWeekItComesBackOnceSayingSo() {
        val ignored = (1..4).map { Prompted(TODAY - DailyPrompt.QUIET_DAYS - it, opened = false) }
        assertThat(DailyPrompt.today(ignored, TODAY)).isEqualTo(Prompt.StillHere)
    }

    @Test
    fun oneThatWasOpenedClearsTheRun() {
        val history = (1..4).map { Prompted(TODAY - it - 1, opened = false) } +
            Prompted(TODAY - 1, opened = true)
        assertThat(DailyPrompt.today(history, TODAY)).isEqualTo(Prompt.Ordinary)
    }

    @Test
    fun eightDismissalsTurnItOffForGood() {
        val ignored = (1..DailyPrompt.OFF_AFTER).map { Prompted(TODAY - it, opened = false) }
        assertWithMessage("it kept asking after being ignored eight times")
            .that(DailyPrompt.today(ignored, TODAY)).isEqualTo(Prompt.TurnItOff)
    }

    @Test
    fun aBrandNewPersonGetsOne() {
        assertThat(DailyPrompt.today(emptyList(), TODAY)).isEqualTo(Prompt.Ordinary)
    }

    private companion object {
        const val TODAY = 20_000L
    }
}
