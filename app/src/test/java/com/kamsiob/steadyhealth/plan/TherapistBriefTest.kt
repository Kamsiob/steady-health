package com.kamsiob.steadyhealth.plan

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.session.Area
import com.kamsiob.steadyhealth.session.Felt
import com.kamsiob.steadyhealth.session.Movements
import org.junit.Test

/**
 * The page for the next appointment. ADDENDUM-03 Part 6, LOGIC.md 17.
 *
 * The rules worth holding are the ones a therapist would notice were broken. Every
 * line of their plan is on the page, in their order, whether or not it was done, and
 * a line nobody did says so rather than reading as a nought. The numbers are counts
 * and dates and never a share of anything. What the person said hurt is kept in their
 * own words, on the day they said it, beside the line they were doing at the time.
 * And nothing from outside the stretch of days printed at the top is folded in.
 */
class TherapistBriefTest {

    @Test
    fun anEmptyPlanStillMakesAPage() {
        val brief = TherapistBriefs.of(BriefInputs(plan = plan(lines = emptyList()), today = TODAY))

        assertThat(brief.lines).isEmpty()
        assertThat(brief.heading.planLabel).isEqualTo("physio")
        assertThat(brief.heading.daysWithPlan).isEqualTo(0)
        assertThat(brief.hurt).isEmpty()
        assertThat(brief.ratings).isEmpty()
        assertThat(brief.moreSaid).isEqualTo(0)
    }

    @Test
    fun aPlanNothingWasDoneFromKeepsEveryLineAndClaimsNothing() {
        val brief = TherapistBriefs.of(BriefInputs(plan = plan(), today = TODAY))

        assertWithMessage("a line the therapist set is never dropped")
            .that(brief.lines.map { it.text })
            .containsExactly(SIT_TO_STAND, HEEL_RAISES, WALL_PUSH_UPS)
            .inOrder()
        brief.lines.forEach { line ->
            assertWithMessage(line.text).that(line.happened.ever).isFalse()
            assertWithMessage("no numbers stand in for a line nobody did")
                .that(line.happened.counts)
                .isNull()
            assertThat(line.happened.onDays).isEmpty()
        }
        assertThat(brief.heading.daysWithPlan).isEqualTo(0)
    }

    @Test
    fun aPlanEverythingWasDoneFromCarriesNumbersForEveryLine() {
        val done = listOf(SIT_TO_STAND_ID, HEEL_RAISES_ID, WALL_PUSH_UPS_ID).flatMap { id ->
            (0..3).map { back -> PlanDone(id, TODAY - back, result = 10 + back, target = 10) }
        }

        val brief = TherapistBriefs.of(BriefInputs(plan = plan(), today = TODAY, done = done))

        assertThat(brief.lines.map { it.happened.ever }).containsExactly(true, true, true)
        brief.lines.forEach { line ->
            assertThat(line.happened.counts).isEqualTo(Counts(fewest = 10, usual = 11, most = 13))
            assertThat(line.happened.days).isEqualTo(4)
            assertThat(line.happened.firstDay).isEqualTo(TODAY - 3)
            assertThat(line.happened.lastDay).isEqualTo(TODAY)
        }
        assertThat(brief.heading.daysWithPlan).isEqualTo(4)
    }

    @Test
    fun theUsualNumberIsTheMiddleOneAndNotTheAverage() {
        val done = listOf(8, 8, 9, 30).mapIndexed { index, result ->
            PlanDone(SIT_TO_STAND_ID, TODAY - index, result = result, target = 10)
        }

        val brief = TherapistBriefs.of(BriefInputs(plan = plan(), today = TODAY, done = done))

        assertWithMessage("one good Tuesday does not become the usual day")
            .that(brief.lines.first().happened.counts)
            .isEqualTo(Counts(fewest = 8, usual = 8, most = 30))
    }

    @Test
    fun timesDoneAndDaysDoneAreBothKeptBecauseAPlanCanAskTwiceADay() {
        val done = listOf(
            PlanDone(SIT_TO_STAND_ID, TODAY, result = 10, target = 10),
            PlanDone(SIT_TO_STAND_ID, TODAY, result = 9, target = 10),
            PlanDone(SIT_TO_STAND_ID, TODAY - 1, result = 10, target = 10),
        )

        val brief = TherapistBriefs.of(BriefInputs(plan = plan(), today = TODAY, done = done))

        val happened = brief.lines.first().happened
        assertThat(happened.times).isEqualTo(3)
        assertThat(happened.days).isEqualTo(2)
        assertWithMessage("two on one day is one day at the top of the page")
            .that(brief.heading.daysWithPlan)
            .isEqualTo(2)
    }

    @Test
    fun aMonthOfMixedHistoryReadsOneLineAtATime() {
        val brief = TherapistBriefs.of(mixedMonth())

        val sitToStand = brief.lines.first { it.lineId == SIT_TO_STAND_ID }
        val heelRaises = brief.lines.first { it.lineId == HEEL_RAISES_ID }
        val wallPushUps = brief.lines.first { it.lineId == WALL_PUSH_UPS_ID }

        assertThat(sitToStand.happened.days).isEqualTo(15)
        assertThat(sitToStand.happened.counts).isEqualTo(Counts(fewest = 8, usual = 10, most = 12))
        assertThat(heelRaises.happened.days).isEqualTo(4)
        assertWithMessage("a line they stopped doing keeps the days it was done on")
            .that(heelRaises.happened.lastDay)
            .isEqualTo(TODAY - 21)
        assertThat(wallPushUps.happened.ever).isFalse()
        assertWithMessage("a line that conflicts is flagged and left in")
            .that(wallPushUps.given.conflictsWith)
            .isEqualTo(Exclusion.Pushing)
    }

    @Test
    fun aMonthOfMixedHistoryCountsTheRatingsThePersonGave() {
        val brief = TherapistBriefs.of(mixedMonth())

        assertThat(brief.ratings).containsExactly(
            RatingCount(Felt.AboutRight, 9),
            RatingCount(Felt.Hard, 4),
        ).inOrder()
        assertWithMessage("a rating nobody gave is left off rather than shown as a nought")
            .that(brief.ratings.map { it.felt })
            .doesNotContain(Felt.Easy)
        assertThat(brief.hardDays).hasSize(4)
        assertWithMessage("most recent first").that(brief.hardDays.first()).isEqualTo(TODAY - 1)
    }

    @Test
    fun whatHurtIsGroupedByAreaAndKeepsTheirOwnWords() {
        val brief = TherapistBriefs.of(mixedMonth())

        assertThat(brief.hurt.map { it.area }).containsExactly(Area.Knee, Area.Back).inOrder()
        val knee = brief.hurt.first { it.area == Area.Knee }
        assertThat(knee.days).isEqualTo(3)
        assertThat(knee.lastDay).isEqualTo(TODAY - 2)
        assertWithMessage("verbatim, and the most recent first")
            .that(knee.said)
            .containsExactly(
                Said(TODAY - 2, "sore on the stairs the next morning"),
                Said(TODAY - 9, "knee was grumbling by the eighth one"),
            ).inOrder()
    }

    @Test
    fun whatHurtNamesTheLineTheyWereDoingAtTheTime() {
        val brief = TherapistBriefs.of(mixedMonth())

        assertWithMessage("the therapist's own wording, not a movement id")
            .that(brief.hurt.first { it.area == Area.Knee }.during)
            .containsExactly(SIT_TO_STAND)
    }

    @Test
    fun sentencesThatDidNotFitAreCountedRatherThanQuietlyDropped() {
        val brief = TherapistBriefs.of(mixedMonth())

        assertThat(brief.hurt.sumOf { it.said.size }).isEqualTo(3)
        assertThat(brief.moreSaid).isEqualTo(1)
    }

    @Test
    fun theWindowStartsAtTheLastAppointmentWhenThereWasOne() {
        val brief = TherapistBriefs.of(mixedMonth().copy(sinceDay = TODAY - 14))

        assertThat(brief.heading.fromDay).isEqualTo(TODAY - 14)
        assertWithMessage("nothing from before the last appointment is folded in")
            .that(brief.lines.first { it.lineId == HEEL_RAISES_ID }.happened.ever)
            .isFalse()
        assertThat(brief.hurt.map { it.area }).containsExactly(Area.Knee)
    }

    @Test
    fun theWindowStartsAtTheDayThePlanWasGivenWhenThereWasNoAppointment() {
        val brief = TherapistBriefs.of(mixedMonth())

        assertThat(brief.heading.fromDay).isEqualTo(TODAY - GIVEN_DAYS_AGO)
        assertThat(brief.heading.toDay).isEqualTo(TODAY)
    }

    @Test
    fun theWindowNeverReachesFurtherBackThanTwelveWeeks() {
        val old = plan(givenOnDay = TODAY - 400)
        val done = listOf(PlanDone(SIT_TO_STAND_ID, TODAY - 200, result = 10, target = 10))

        val brief = TherapistBriefs.of(BriefInputs(plan = old, today = TODAY, done = done))

        assertThat(brief.heading.fromDay).isEqualTo(TODAY - TherapistBriefs.LONGEST_WINDOW + 1)
        assertThat(brief.lines.first().happened.ever).isFalse()
    }

    @Test
    fun aPlanSetUpForTomorrowStillMakesAPage() {
        val brief = TherapistBriefs.of(BriefInputs(plan = plan(givenOnDay = TODAY + 1), today = TODAY))

        assertThat(brief.heading.fromDay).isEqualTo(TODAY)
        assertThat(brief.heading.toDay).isEqualTo(TODAY)
        assertThat(brief.lines).hasSize(3)
    }

    @Test
    fun theAppsOwnSessionsSitBesideThePlanAndNeverInsideIt() {
        val brief = TherapistBriefs.of(mixedMonth().copy(otherSessionDays = 6))

        assertThat(brief.heading.otherSessionDays).isEqualTo(6)
        assertThat(brief.heading.daysWithPlan).isEqualTo(17)
    }

    @Test
    fun aLineThatChangedSaysWhoChangedItAndWhatItSaidBefore() {
        val brief = TherapistBriefs.of(mixedMonth())

        assertThat(brief.lines.first { it.lineId == HEEL_RAISES_ID }.given.changed)
            .isEqualTo(Changed(TODAY - 20, ChangedBy.TheirTherapist, "10 heel raises, once a day"))
    }

    @Test
    fun theMarksThePersonPutOnTheirOwnNumbersAreCounted() {
        val done = listOf(
            PlanDone(SIT_TO_STAND_ID, TODAY, result = 10, target = 10, selfCounted = true),
            PlanDone(SIT_TO_STAND_ID, TODAY - 1, result = 8, target = 10, madeEasier = true),
            PlanDone(SIT_TO_STAND_ID, TODAY - 2, result = 10, target = 10),
        )

        val brief = TherapistBriefs.of(BriefInputs(plan = plan(), today = TODAY, done = done))

        assertThat(brief.lines.first().happened.marks)
            .isEqualTo(Marks(madeEasier = 1, selfCounted = 1))
    }

    @Test
    fun theLineOnThePageIsTheOneTheTherapistWroteAndNotTheMovementItMatched() {
        val brief = TherapistBriefs.of(mixedMonth())

        val first = brief.lines.first()
        assertThat(first.text).isEqualTo(SIT_TO_STAND)
        assertThat(first.given.item.movement?.id).isEqualTo("sit_to_stand")
    }

    /**
     * Four weeks that look like four real ones: a line done most days, a line that
     * stopped after the first fortnight, a line never done at all, some days that
     * hurt, and a rating on each day there was a session.
     */
    private fun mixedMonth(): BriefInputs {
        val sitToStand = (0..14).map { back ->
            val result = when (back % 3) {
                0 -> 10
                1 -> 12
                else -> 8
            }
            PlanDone(SIT_TO_STAND_ID, TODAY - back * 2, result = result, target = 10)
        }
        val heelRaises = (21..24).map { back ->
            PlanDone(HEEL_RAISES_ID, TODAY - back, result = 10, target = 10)
        }
        return BriefInputs(
            plan = plan(),
            today = TODAY,
            done = sitToStand + heelRaises,
            hurt = listOf(
                SaidHurt(Area.Knee, TODAY - 2, "sore on the stairs the next morning", SIT_TO_STAND_ID),
                SaidHurt(Area.Knee, TODAY - 9, "knee was grumbling by the eighth one", SIT_TO_STAND_ID),
                SaidHurt(Area.Knee, TODAY - 16, "knee again", SIT_TO_STAND_ID),
                SaidHurt(Area.Back, TODAY - 20, "back was stiff sitting down to it"),
            ),
            ratings = listOf(Felt.Hard, Felt.AboutRight).flatMap { felt ->
                val days = if (felt == Felt.Hard) listOf(1, 6, 13, 19) else (0..8).map { it * 3 + 2 }
                days.map { Rated(TODAY - it, felt) }
            },
        )
    }

    private fun plan(
        givenOnDay: Long = TODAY - GIVEN_DAYS_AGO,
        lines: List<PlannedLine> = defaultLines(),
    ) = TherapistPlan(
        id = "physio_plan",
        label = "physio",
        givenOnDay = givenOnDay,
        reviewDay = TODAY + 2,
        lines = lines,
    )

    private fun defaultLines() = listOf(
        PlannedLine(
            id = SIT_TO_STAND_ID,
            item = PlanItem(
                line = SIT_TO_STAND,
                movement = Movements.byId("sit_to_stand"),
                howMany = HowMany.Reps(reps = 10),
                howOften = HowOften.ADay(times = 2),
                sureness = Sureness.Named,
            ),
        ),
        PlannedLine(
            id = HEEL_RAISES_ID,
            item = PlanItem(
                line = HEEL_RAISES,
                movement = Movements.byId("heel_raises"),
                howMany = HowMany.Reps(reps = 15),
                howOften = HowOften.ADay(times = 1),
                sureness = Sureness.Named,
            ),
            changed = Changed(TODAY - 20, ChangedBy.TheirTherapist, "10 heel raises, once a day"),
        ),
        PlannedLine(
            id = WALL_PUSH_UPS_ID,
            item = PlanItem(
                line = WALL_PUSH_UPS,
                movement = null,
                howMany = HowMany.Reps(reps = 8, sets = 2),
                howOften = HowOften.Unsaid,
                sureness = Sureness.Unmatched,
            ),
            conflictsWith = Exclusion.Pushing,
        ),
    )

    private companion object {
        const val TODAY = 20_000L
        const val GIVEN_DAYS_AGO = 28L

        const val SIT_TO_STAND_ID = "line_1"
        const val HEEL_RAISES_ID = "line_2"
        const val WALL_PUSH_UPS_ID = "line_3"

        const val SIT_TO_STAND = "10 sit to stands, twice a day"
        const val HEEL_RAISES = "15 heel raises, once a day"
        const val WALL_PUSH_UPS = "wall push ups, 8 twice"
    }
}
