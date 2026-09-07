package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.domain.NextDayFeel
import com.kamsiob.steadyhealth.domain.TalkTest
import org.junit.Test

/** Pacing mode, LOGIC.md section 7. Nothing in here ever goes up. */
class PacingEngineTest {

    @Test
    fun aWorseDayTakesAFifthOff() {
        assertThat(PacingEngine.reduce(Envelope(10, 3)).minutes).isEqualTo(8)
        assertThat(PacingEngine.reduce(Envelope(5, 3)).minutes).isEqualTo(4)
    }

    @Test
    fun reducingNeverReachesNothing() {
        var envelope = Envelope(5, 3)
        repeat(TWENTY_WORSE_DAYS) { envelope = PacingEngine.reduce(envelope) }
        assertThat(envelope.minutes).isEqualTo(PacingEngine.FLOOR_MINUTES)
    }

    @Test
    fun reducingNeverChangesTheDays() {
        assertThat(PacingEngine.reduce(Envelope(10, 3)).daysPerWeek).isEqualTo(3)
    }

    @Test
    fun theSuggestionIsNeverAboveTheEnvelope() {
        val envelope = Envelope(10, 3)
        (0..envelope.daysPerWeek).forEach { done ->
            assertThat(PacingEngine.suggestedMinutes(envelope, done))
                .isAtMost(envelope.minutes)
        }
    }

    @Test
    fun onceTheDaysAreUsedTheSuggestionIsNothing() {
        val envelope = Envelope(10, 3)
        assertThat(PacingEngine.suggestedMinutes(envelope, 2)).isEqualTo(10)
        assertThat(PacingEngine.suggestedMinutes(envelope, 3)).isEqualTo(0)
        assertThat(PacingEngine.suggestedMinutes(envelope, 4)).isEqualTo(0)
    }

    @Test
    fun twoWorseDaysInAMonthTurnsPacingOn() {
        val sessions = listOf(worseOn(1), worseOn(20))
        assertThat(PacingEngine.shouldTurnOn(sessions, today = 30)).isTrue()
    }

    @Test
    fun oneWorseDayDoesNot() {
        assertThat(PacingEngine.shouldTurnOn(listOf(worseOn(1)), today = 30)).isFalse()
    }

    @Test
    fun twoWorseDaysMoreThanAMonthApartDoNot() {
        val sessions = listOf(worseOn(1), worseOn(80))
        assertThat(PacingEngine.shouldTurnOn(sessions, today = 100)).isFalse()
    }

    @Test
    fun aGoodDayIsNotAWorseDay() {
        val sessions = listOf(
            worseOn(1).copy(nextDayFeel = NextDayFeel.Better),
            worseOn(2).copy(nextDayFeel = NextDayFeel.Same),
        )
        assertThat(PacingEngine.shouldTurnOn(sessions, today = 10)).isFalse()
    }

    private fun worseOn(day: Long) = DoneSession(
        epochDay = day,
        stepIndex = 0,
        durationSeconds = 300,
        talkTest = TalkTest.YesEasily,
        nextDayFeel = NextDayFeel.Worse,
    )

    private companion object {
        const val TWENTY_WORSE_DAYS = 20
    }
}
