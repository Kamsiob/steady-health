package com.kamsiob.steadyhealth.session

import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.domain.GettingAround
import org.junit.Test

/**
 * Coming back after time away. ADDENDUM-03 Part 15.
 *
 * The rules worth holding are the ones about restraint: the question is asked once per
 * gap and never twice, the doctor line is said once in a lifetime, and an answer that
 * says nothing about a person's health is acted on gently rather than ignored.
 */
class ComingBackTest {

    @Test
    fun aShortAbsenceIsNotAnInterruptionAndIsNeverMentioned() {
        val six = ComingBackEngine.of(Gap(lastSessionDay = LAST, today = LAST + 6))
        assertThat(six).isEqualTo(ComingBack.Ordinary)
    }

    @Test
    fun sevenDaysIsWhereTheQuestionStarts() {
        val asked = ComingBackEngine.of(Gap(lastSessionDay = LAST, today = LAST + 7))
        assertThat(asked).isEqualTo(ComingBack.Ask(days = 7, startAgain = false))
    }

    @Test
    fun somebodyWithNoSessionsAtAllIsNotComingBackFromAnything() {
        assertThat(ComingBackEngine.of(Gap(lastSessionDay = null, today = LAST + 400)))
            .isEqualTo(ComingBack.Ordinary)
    }

    @Test
    fun aBusyLifeTakesOneRungForTwoSessionsAndSaysNothing() {
        val easing = ComingBackEngine.answer(WhyAway.Busy, gapOf(10))
        assertThat(easing.stepsSmaller).isEqualTo(1)
        assertThat(easing.forSessions).isEqualTo(ComingBackEngine.BUSY_SESSIONS)
        assertThat(easing.rampUntilDay).isNull()
        assertThat(easing.asides).isEmpty()
        assertThat(easing.startAgain).isFalse()
    }

    @Test
    fun beingUnwellTakesTwoRungsAndAFortnightAndBothSentences() {
        val easing = ComingBackEngine.answer(WhyAway.Unwell, gapOf(10))
        assertThat(easing.stepsSmaller).isEqualTo(ComingBackEngine.UNWELL_STEPS)
        assertThat(easing.rampUntilDay).isEqualTo(LAST + 10 + ComingBackEngine.RAMP_DAYS)
        assertThat(easing.asides).containsExactly(Aside.TakingItSlowly, Aside.WorthAWord).inOrder()
        assertThat(easing.forSessions).isNull()
    }

    @Test
    fun theDoctorLineIsSaidOnceInALifetimeAndTheOtherOneIsNot() {
        val again = ComingBackEngine.answer(
            WhyAway.Unwell,
            gapOf(10).copy(saidWorthAWordBefore = true),
        )
        assertThat(again.asides).containsExactly(Aside.TakingItSlowly)
    }

    @Test
    fun beingAwayIsTheStandardOneStepAndNothingElse() {
        val easing = ComingBackEngine.answer(WhyAway.Away, gapOf(21))
        assertThat(easing.stepsSmaller).isEqualTo(1)
        assertThat(easing.forSessions).isNull()
        assertThat(easing.rampUntilDay).isNull()
        assertThat(easing.asides).isEmpty()
    }

    @Test
    fun anAnswerWithheldIsTreatedExactlyAsABusyLife() {
        val quiet = ComingBackEngine.answer(WhyAway.RatherNotSay, gapOf(10))
        val busy = ComingBackEngine.answer(WhyAway.Busy, gapOf(10))
        assertThat(WhyAway.RatherNotSay.counts).isEqualTo(WhyAway.Busy)
        assertThat(quiet.copy(why = WhyAway.Busy)).isEqualTo(busy)
    }

    @Test
    fun whatWasSaidIsKeptAsItWasTapped() {
        assertThat(ComingBackEngine.answer(WhyAway.RatherNotSay, gapOf(10)).why)
            .isEqualTo(WhyAway.RatherNotSay)
    }

    @Test
    fun aGapIsAskedAboutOnceAndThenLeftAlone() {
        val answered = gapOf(10).copy(answered = Answered(WhyAway.RatherNotSay, LAST, LAST + 10))
        val settled = ComingBackEngine.of(answered)
        assertThat(settled).isInstanceOf(ComingBack.Settled::class.java)
        assertThat((settled as ComingBack.Settled).easing.stepsSmaller).isEqualTo(1)
    }

    @Test
    fun everyAnswerIsAskedForOnlyOnceNotJustTheWithheldOne() {
        WhyAway.entries.forEach { why ->
            val answered = gapOf(10).copy(answered = Answered(why, LAST, LAST + 10))
            assertThat(ComingBackEngine.of(answered)).isInstanceOf(ComingBack.Settled::class.java)
        }
    }

    @Test
    fun aFreshGapAfterASessionIsANewQuestion() {
        val laterGap = Gap(
            lastSessionDay = LAST + 30,
            today = LAST + 40,
            answered = Answered(WhyAway.Busy, LAST, LAST + 10),
        )
        assertThat(ComingBackEngine.of(laterGap)).isEqualTo(ComingBack.Ask(days = 10, startAgain = false))
    }

    @Test
    fun sixtyDaysReRunsTheFirstTwoQuestionsAndStartsThreeRungsBack() {
        val asked = ComingBackEngine.of(gapOf(60))
        assertThat(asked).isEqualTo(ComingBack.Ask(days = 60, startAgain = true))
        val easing = ComingBackEngine.answer(WhyAway.Busy, gapOf(60))
        assertThat(easing.stepsSmaller).isEqualTo(ComingBackEngine.START_AGAIN_STEPS)
        assertThat(easing.startAgain).isTrue()
        assertThat(easing.rampUntilDay).isEqualTo(LAST + 60 + ComingBackEngine.RAMP_DAYS)
        assertThat(easing.forSessions).isNull()
    }

    @Test
    fun aLongGapTakesMoreRungsThanBeingUnwellRatherThanFewer() {
        val easing = ComingBackEngine.answer(WhyAway.Unwell, gapOf(120))
        assertThat(easing.stepsSmaller).isEqualTo(ComingBackEngine.START_AGAIN_STEPS)
        assertThat(easing.asides).containsExactly(Aside.TakingItSlowly, Aside.WorthAWord).inOrder()
    }

    @Test
    fun fiftyNineDaysIsStillTheOrdinaryQuestion() {
        assertThat(ComingBackEngine.of(gapOf(59)))
            .isEqualTo(ComingBack.Ask(days = 59, startAgain = false))
    }

    @Test
    fun theRampCoversAFortnightAndThenStops() {
        val easing = ComingBackEngine.answer(WhyAway.Unwell, gapOf(10))
        val cameBack = LAST + 10
        assertThat(easing.rampingOn(cameBack)).isTrue()
        assertThat(easing.rampingOn(cameBack + ComingBackEngine.RAMP_DAYS)).isTrue()
        assertThat(easing.rampingOn(cameBack + ComingBackEngine.RAMP_DAYS + 1)).isFalse()
    }

    @Test
    fun theRampIsAnchoredToTheDayTheyAnsweredAndDoesNotMove() {
        val answered = Answered(WhyAway.Unwell, LAST, LAST + 10)
        val muchLater = Gap(lastSessionDay = LAST, today = LAST + 100, answered = answered)
        val easing = (ComingBackEngine.of(muchLater) as ComingBack.Settled).easing
        assertThat(easing.days).isEqualTo(10)
        assertThat(easing.stepsSmaller).isEqualTo(ComingBackEngine.UNWELL_STEPS)
        assertThat(easing.rampUntilDay).isEqualTo(LAST + 10 + ComingBackEngine.RAMP_DAYS)
    }

    @Test
    fun theRampIsWhatSessionEngineAlreadyMeansByRampingAfterUnwell() {
        val easing = ComingBackEngine.answer(WhyAway.Unwell, gapOf(10))
        val cameBack = LAST + 10
        val plan = SessionEngine.plan(
            SessionInputs(
                way = GettingAround.OnFeet,
                today = cameBack,
                lastSessionDay = LAST,
                rampingAfterUnwell = easing.rampingOn(cameBack),
            ),
        )
        assertThat(plan.adaptation.kind).isEqualTo(Adaptation.Kind.AfterUnwell)
    }

    @Test
    fun anAnswerThatIsNotUnwellLeavesSessionEngineToItsOwnGapRule() {
        val easing = ComingBackEngine.answer(WhyAway.Away, gapOf(10))
        val cameBack = LAST + 10
        val plan = SessionEngine.plan(
            SessionInputs(
                way = GettingAround.OnFeet,
                today = cameBack,
                lastSessionDay = LAST,
                rampingAfterUnwell = easing.rampingOn(cameBack),
            ),
        )
        assertThat(plan.adaptation.kind).isEqualTo(Adaptation.Kind.Shorter)
    }

    private fun gapOf(days: Int) = Gap(lastSessionDay = LAST, today = LAST + days)

    private companion object {
        const val LAST = 20_000L
    }
}
