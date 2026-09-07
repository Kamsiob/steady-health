package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Gap decay, LOGIC.md section 6, from the day the app is opened. */
class ReturningEngineTest {

    @Test
    fun aWeekAwayIsNotAGap() {
        assertThat(ReturningEngine.decide(lastSessionDay = 100, today = 107))
            .isEqualTo(Returning.Ordinary)
    }

    @Test
    fun eightDaysIsOneStepBack() {
        val gap = ReturningEngine.decide(lastSessionDay = 100, today = 108)
        assertThat(gap).isInstanceOf(Returning.AfterAGap::class.java)
        assertThat((gap as Returning.AfterAGap).stepsBack).isEqualTo(1)
        assertThat(gap.easingUntilDay).isNull()
    }

    @Test
    fun aMonthIsTwoStepsBackAndAFortnightOfNoOffers() {
        val gap = ReturningEngine.decide(lastSessionDay = 100, today = 129) as Returning.AfterAGap
        assertThat(gap.stepsBack).isEqualTo(2)
        assertThat(gap.easingUntilDay).isEqualTo(129 + ReturningEngine.EASING_DAYS)
    }

    @Test
    fun ninetyDaysAsksTheQuestionsAgain() {
        val short = ReturningEngine.decide(lastSessionDay = 100, today = 189) as Returning.AfterAGap
        assertThat(short.askAgain).isFalse()
        val long = ReturningEngine.decide(lastSessionDay = 100, today = 190) as Returning.AfterAGap
        assertThat(long.askAgain).isTrue()
    }

    @Test
    fun somebodyWhoHasNeverDoneASessionHasNotBeenAway() {
        assertThat(ReturningEngine.decide(lastSessionDay = null, today = 500))
            .isEqualTo(Returning.Ordinary)
    }

    @Test
    fun theStepsBackNeverGrowWithTheGap() {
        // Two is the most it ever takes, however long somebody has been away, so
        // that coming back after a year is not worse than coming back after a
        // month. LOGIC.md section 6 caps it and this holds it there.
        val years = ReturningEngine.decide(lastSessionDay = 0, today = 1000) as Returning.AfterAGap
        assertThat(years.stepsBack).isEqualTo(2)
    }
}
