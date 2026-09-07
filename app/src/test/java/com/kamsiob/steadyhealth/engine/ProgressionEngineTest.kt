package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.domain.NextDayFeel
import com.kamsiob.steadyhealth.domain.TalkTest
import org.junit.Test

/** LOGIC.md section 6. Going up is hard and coming down is easy, on purpose. */
class ProgressionEngineTest {

    private val amounts = Ladders.walking.map { it.amount }

    private fun session(
        day: Long,
        step: Int,
        minutes: Int,
        talk: TalkTest? = TalkTest.YesEasily,
        nextDay: NextDayFeel? = null,
    ) = DoneSession(day, step, minutes * 60, talk, nextDay)

    @Test
    fun twoEasySessionsOfferTheNextStep() {
        val decision = ProgressionEngine.decide(
            currentStepIndex = 0,
            sessions = listOf(session(1, 0, 2), session(2, 0, 2)),
            stepAmounts = amounts,
            today = 3,
        )
        assertThat(decision).isEqualTo(Progression.Offer(1))
    }

    @Test
    fun oneEasySessionIsNotEnough() {
        val decision = ProgressionEngine.decide(0, listOf(session(1, 0, 2)), amounts, today = 2)

        assertThat(decision).isEqualTo(Progression.Stay)
    }

    @Test
    fun sayingNotYetSuppressesTheOfferForAFortnight() {
        val sessions = listOf(session(1, 0, 2), session(2, 0, 2))

        assertThat(ProgressionEngine.decide(0, sessions, amounts, today = 5, offerDeclinedUntilDay = 16))
            .isEqualTo(Progression.Stay)
        assertThat(ProgressionEngine.decide(0, sessions, amounts, today = 16, offerDeclinedUntilDay = 16))
            .isEqualTo(Progression.Offer(1))
    }

    @Test
    fun worseTheNextDayDropsAStepAndBeatsAnOffer() {
        val sessions = listOf(
            session(1, 1, 4),
            session(2, 1, 4, nextDay = NextDayFeel.Worse),
        )
        val decision = ProgressionEngine.decide(1, sessions, amounts, today = 3)

        assertThat(decision).isEqualTo(Progression.StepBack(0, Progression.Because.WorseTheNextDay))
    }

    @Test
    fun twoHardTalkTestsDropAStep() {
        val sessions = listOf(
            session(1, 2, 6, talk = TalkTest.No),
            session(2, 2, 6, talk = TalkTest.No),
        )
        assertThat(ProgressionEngine.decide(2, sessions, amounts, today = 3))
            .isEqualTo(Progression.StepBack(1, Progression.Because.TalkTestTwice))
    }

    @Test
    fun theFirstStepNeverDropsBelowItself() {
        val sessions = listOf(session(1, 0, 2, nextDay = NextDayFeel.Worse))

        assertThat(ProgressionEngine.decide(0, sessions, amounts, today = 2))
            .isEqualTo(Progression.Stay)
    }

    @Test
    fun theSpikeCapStopsAJumpMuchLongerThanAnythingRecent() {
        // Two easy sessions at step 6 (16 minutes) but nothing longer than 8
        // minutes in the last month. Step 7 is 20 minutes, well over 110%.
        val sessions = listOf(session(1, 6, 8), session(2, 6, 8))

        assertThat(ProgressionEngine.decide(6, sessions, amounts, today = 3))
            .isEqualTo(Progression.Stay)
    }

    @Test
    fun theSpikeCapAllowsAnOrdinaryStepUp() {
        // 11 minutes recently, and step 5 is 14. Under 110% of nothing would be
        // wrong; 14 is over 11 * 1.1, so this must stay. Step 4 at 11 is fine.
        val sessions = listOf(session(1, 3, 11), session(2, 3, 11))

        assertThat(ProgressionEngine.decide(3, sessions, amounts, today = 3))
            .isEqualTo(Progression.Offer(4))
    }

    @Test
    fun withNothingInTheWindowThereIsNoSpikeToCap() {
        assertThat(ProgressionEngine.withinSpikeCap(1, emptyList(), amounts, today = 100)).isTrue()
    }

    @Test
    fun eightDaysAwayDropsOneStep() {
        val sessions = listOf(session(1, 4, 11))

        assertThat(ProgressionEngine.decide(4, sessions, amounts, today = 9))
            .isEqualTo(Progression.StepBack(3, Progression.Because.TimeAway))
    }

    @Test
    fun aMonthAwayDropsTwo() {
        val sessions = listOf(session(1, 4, 11))

        assertThat(ProgressionEngine.decide(4, sessions, amounts, today = 30))
            .isEqualTo(Progression.StepBack(2, Progression.Because.TimeAway))
    }

    @Test
    fun aWeekOffChangesNothing() {
        val sessions = listOf(session(1, 4, 11), session(2, 4, 11))

        assertThat(ProgressionEngine.decide(4, sessions, amounts, today = 8))
            .isNotEqualTo(Progression.StepBack(3, Progression.Because.TimeAway))
    }

    @Test
    fun everyRungOfTheLadderCanActuallyBeClimbed() {
        // The regression that matters. Read as bare arithmetic against the next
        // rung, the 110% cap freezes everybody at two minutes forever, because
        // every step on the specified ladder is more than 110% of the one before
        // it. Somebody doing the step they are on can always be offered the next.
        (0 until amounts.lastIndex).forEach { step ->
            val done = listOf(
                session(1, step, amounts[step]),
                session(2, step, amounts[step]),
            )
            assertThat(ProgressionEngine.decide(step, done, amounts, today = 3))
                .isEqualTo(Progression.Offer(step + 1))
        }
    }

    @Test
    fun somebodyWhoseWalksAreBelowTheirStepIsNotOfferedTheNextOne() {
        // The case the cap exists for: sitting at step seven, actually walking
        // eight minutes, being offered twenty.
        val sessions = listOf(session(1, 7, 8), session(2, 7, 8))

        assertThat(ProgressionEngine.decide(7, sessions, amounts, today = 3))
            .isEqualTo(Progression.Stay)
    }

    @Test
    fun theTopOfTheLadderStaysAtTheTop() {
        val last = amounts.lastIndex
        val sessions = listOf(
            session(1, last, amounts[last]),
            session(2, last, amounts[last]),
        )
        assertThat(ProgressionEngine.decide(last, sessions, amounts, today = 3))
            .isEqualTo(Progression.Stay)
    }
}
