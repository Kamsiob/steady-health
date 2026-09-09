package com.kamsiob.steadyhealth.plan

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.session.Adaptation
import com.kamsiob.steadyhealth.session.Movements
import com.kamsiob.steadyhealth.session.SessionPlan
import com.kamsiob.steadyhealth.session.Step
import org.junit.Test

/**
 * Reading a therapist's plan. ADDENDUM-03 Part 6.
 *
 * The rules worth holding are the ones about refusing. A line the app cannot read
 * comes back visibly unread, a line naming two movements comes back naming neither, a
 * line saying to avoid something is never matched to the thing it names, and a number
 * the app cannot believe comes back as no number at all. All four cost
 * the person one tap on a screen they were going to look at anyway, which is the
 * trade, because a confident wrong match on a sheet somebody was handed at a hospital
 * is not something they would ever think to check.
 *
 * The other rule worth holding is that nothing here moves a therapist's numbers.
 */
class PlanMatchingTest {

    // --- The lines Part 6 names ----------------------------------------------

    @Test
    fun readsTenSitToStandsTwiceADay() {
        val item = PlanMatching.readOne("10 sit to stands twice a day")

        assertThat(item.movement?.id).isEqualTo("sit_to_stand")
        assertThat(item.howMany).isEqualTo(HowMany.Reps(10))
        assertThat(item.howOften).isEqualTo(HowOften.ADay(2))
        assertThat(item.sureness).isEqualTo(Sureness.Named)
    }

    @Test
    fun readsHeelRaisesWrittenWithAnX() {
        val item = PlanMatching.readOne("heel raises x15")

        assertThat(item.movement?.id).isEqualTo("heel_raises")
        assertThat(item.howMany).isEqualTo(HowMany.Reps(15))
        assertThat(item.howOften).isEqualTo(HowOften.Unsaid)
    }

    @Test
    fun readsThreeSetsOfEightAsSetsAndNotAsFifteen() {
        val item = PlanMatching.readOne("3 sets of 8 wall push ups")

        assertThat(item.movement?.id).isEqualTo("wall_push_up")
        assertThat(item.howMany).isEqualTo(HowMany.Reps(reps = 8, sets = 3))
    }

    @Test
    fun readsAHoldInSecondsAndNoticesEachSide() {
        val item = PlanMatching.readOne("hold 30 seconds each side")

        assertThat(item.howMany).isEqualTo(HowMany.Hold(30))
        assertThat(item.eachSide).isTrue()
        assertWithMessage("no movement is named on that line").that(item.movement).isNull()
        assertThat(item.sureness).isEqualTo(Sureness.Unmatched)
    }

    @Test
    fun readsAWalkInMinutes() {
        val item = PlanMatching.readOne("walk 10 minutes daily")

        assertThat(item.movement?.id).isEqualTo("walk")
        assertThat(item.howMany).isEqualTo(HowMany.Minutes(10))
        assertThat(item.howOften).isEqualTo(HowOften.ADay(1))
    }

    // --- The words people actually write --------------------------------------

    @Test
    fun allTheWaysOfWritingSitToStandMeanTheSameMovement() {
        val ways = listOf(
            "sit to stand",
            "sit-to-stands",
            "STS",
            "Chair Stands",
            "sit to stands, 10",
            "chair rises",
        )

        ways.forEach { line ->
            assertWithMessage(line).that(PlanMatching.readOne(line).movement?.id)
                .isEqualTo("sit_to_stand")
        }
    }

    @Test
    fun theLongerSpellingWinsOverTheShorterOne() {
        assertThat(PlanMatching.readOne("wall push ups x10").movement?.id).isEqualTo("wall_push_up")
        assertThat(PlanMatching.readOne("semi tandem 20 seconds").movement?.id).isEqualTo("semi_tandem")
        assertThat(PlanMatching.readOne("heel to toe walking").movement?.id).isEqualTo("heel_toe_walk")
        assertThat(PlanMatching.readOne("brisk walk 20 minutes").movement?.id).isEqualTo("walk_brisk")
    }

    @Test
    fun aFamilyOfMovementsComesBackAsAGuessAndNotAsSomethingItRead() {
        val item = PlanMatching.readOne("push ups x 10")

        assertThat(item.movement?.id).isEqualTo("wall_push_up")
        assertThat(item.sureness).isEqualTo(Sureness.Likely)
        assertWithMessage("the screen shows this one as a guess").that(item.guessed).isTrue()
    }

    @Test
    fun aSpokenNumberIsANumber() {
        val item = PlanMatching.readOne("ten chair stands")

        assertThat(item.howMany).isEqualTo(HowMany.Reps(10))
    }

    @Test
    fun oneInAMovementNameIsNeverANumber() {
        val item = PlanMatching.readOne("one leg stand")

        assertThat(item.movement?.id).isEqualTo("one_leg")
        assertWithMessage("nobody was ever asked for one repetition")
            .that(item.howMany).isEqualTo(HowMany.Unsaid)
    }

    @Test
    fun aBareNumberIsReadInTheUnitTheMovementIsCountedIn() {
        assertThat(PlanMatching.readOne("tandem stand 30").howMany).isEqualTo(HowMany.Hold(30))
        assertThat(PlanMatching.readOne("walk 15").howMany).isEqualTo(HowMany.Minutes(15))
        assertThat(PlanMatching.readOne("heel raises 12").howMany).isEqualTo(HowMany.Reps(12))
    }

    // --- How often -------------------------------------------------------------

    @Test
    fun readsTheWaysOfSayingHowOften() {
        assertThat(PlanMatching.readOne("heel raises daily").howOften).isEqualTo(HowOften.ADay(1))
        assertThat(PlanMatching.readOne("heel raises twice daily").howOften).isEqualTo(HowOften.ADay(2))
        assertThat(PlanMatching.readOne("heel raises 3 times a day").howOften).isEqualTo(HowOften.ADay(3))
        assertThat(PlanMatching.readOne("heel raises 3 times a week").howOften).isEqualTo(HowOften.AWeek(3))
        assertThat(PlanMatching.readOne("heel raises weekly").howOften).isEqualTo(HowOften.AWeek(1))
        assertThat(PlanMatching.readOne("heel raises every other day").howOften)
            .isEqualTo(HowOften.EveryOtherDay)
        assertThat(PlanMatching.readOne("heel raises morning and evening").howOften)
            .isEqualTo(HowOften.ADay(2))
    }

    @Test
    fun theNumberInAFrequencyIsNotACountOfRepetitions() {
        val item = PlanMatching.readOne("chair stands 3 times a week")

        assertThat(item.howOften).isEqualTo(HowOften.AWeek(3))
        assertWithMessage("three is how often, not how many").that(item.howMany)
            .isEqualTo(HowMany.Unsaid)
    }

    // --- What it refuses to guess at --------------------------------------------

    @Test
    fun aLineNamingNothingInTheLibraryComesBackUnmatched() {
        val lines = listOf(
            "clamshells x 20",
            "SLR 3 sets of 10",
            "keep up the good work",
            "review in 6 weeks",
        )

        lines.forEach { line ->
            val item = PlanMatching.readOne(line)
            assertWithMessage(line).that(item.movement).isNull()
            assertWithMessage(line).that(item.sureness).isEqualTo(Sureness.Unmatched)
            assertWithMessage(line).that(item.ready).isFalse()
        }
    }

    @Test
    fun aLineNamingTwoMovementsIsGivenNeitherOfThem() {
        val item = PlanMatching.readOne("sit to stands and heel raises")

        assertThat(item.movement).isNull()
        assertThat(item.sureness).isEqualTo(Sureness.MoreThanOne)
        assertThat(item.couldBe.map { it.id }).containsExactly("sit_to_stand", "heel_raises")
    }

    @Test
    fun aNumberTooLargeToBelieveIsNoNumberAtAll() {
        val item = PlanMatching.readOne("heel raises x 4000")

        assertThat(item.movement?.id).isEqualTo("heel_raises")
        assertThat(item.howMany).isEqualTo(HowMany.Unsaid)
        assertWithMessage("the line itself survives whatever the parser makes of it")
            .that(item.line).isEqualTo("heel raises x 4000")
    }

    @Test
    fun theLineIsKeptExactlyAsItWasGiven() {
        val given = "Sit-to-stands (from the dining chair) x10, twice a day"

        assertThat(PlanMatching.readOne(given).line).isEqualTo(given)
    }

    @Test
    fun aLineSayingNotToDoSomethingIsNeverMatchedToIt() {
        val lines = listOf(
            "avoid stairs",
            "no push ups for now",
            "do not do stairs",
            "chair stands, never on the deep sofa",
        )

        lines.forEach { line ->
            val item = PlanMatching.readOne(line)
            assertWithMessage(line).that(item.movement).isNull()
            assertWithMessage(line).that(item.sureness).isEqualTo(Sureness.Unmatched)
        }
    }

    @Test
    fun aLineSayingToAvoidTwoThingsIsNotCutIntoTwoThingsToDo() {
        val items = PlanMatching.readAll("Avoid deep squats and stairs")

        assertThat(items).hasSize(1)
        assertWithMessage("cut in half, the second half loses the word that made it a warning")
            .that(items.single().movement).isNull()
    }

    @Test
    fun aQualifierTheTableKnowsInAnotherOrderIsAskedAboutRatherThanReadPast() {
        val item = PlanMatching.readOne("step ups on a higher step x10")

        assertThat(item.movement).isNull()
        assertThat(item.sureness).isEqualTo(Sureness.MoreThanOne)
        assertThat(item.couldBe.map { it.id }).containsExactly("step_up_low", "step_up_high")
        assertWithMessage("the number on the line is still the number")
            .that(item.howMany).isEqualTo(HowMany.Reps(10))
    }

    @Test
    fun aNumberAnXAndAStretchOfTimeIsLeftForThePersonToSay() {
        val item = PlanMatching.readOne("heel raises 2 x daily")

        assertThat(item.movement?.id).isEqualTo("heel_raises")
        assertWithMessage("two is either the repetitions or the times, so it is neither")
            .that(item.howMany).isEqualTo(HowMany.Unsaid)
        assertThat(item.howOften).isEqualTo(HowOften.Unsaid)
    }

    @Test
    fun anArticleSettlesTheSameNotation() {
        assertThat(PlanMatching.readOne("chair stands 2 x a day").howOften)
            .isEqualTo(HowOften.ADay(2))
        assertThat(PlanMatching.readOne("chair stands 3 x a week").howOften)
            .isEqualTo(HowOften.AWeek(3))
    }

    @Test
    fun aRateTheAppHasNoWordForIsNotReadAsRepetitions() {
        val item = PlanMatching.readOne("chair stands 5 times an hour")

        assertThat(item.movement?.id).isEqualTo("sit_to_stand")
        assertWithMessage("five is how often, and there is nowhere to put an hour")
            .that(item.howMany).isEqualTo(HowMany.Unsaid)
        assertThat(item.howOften).isEqualTo(HowOften.Unsaid)
    }

    @Test
    fun setsWrittenAfterTheNumberCountForAsMuchAsSetsWrittenBeforeIt() {
        assertThat(PlanMatching.readOne("chair stands 8 reps 3 sets").howMany)
            .isEqualTo(HowMany.Reps(reps = 8, sets = 3))
        assertThat(PlanMatching.readOne("3 sets of 8 chair stands").howMany)
            .isEqualTo(HowMany.Reps(reps = 8, sets = 3))
    }

    @Test
    fun setsByRepsWithADayOnTheEndIsStillSetsByReps() {
        val item = PlanMatching.readOne("3 x 10 chair stands daily")

        assertThat(item.howMany).isEqualTo(HowMany.Reps(reps = 10, sets = 3))
        assertThat(item.howOften).isEqualTo(HowOften.ADay(1))
    }

    // --- Cutting text into lines -------------------------------------------------

    @Test
    fun aSpokenSentenceWithTwoMovementsBecomesTwoItems() {
        val said = "ten sit to stands twice a day and heel raises"

        val items = PlanMatching.readAll(said)

        assertThat(items.map { it.movement?.id }).containsExactly("sit_to_stand", "heel_raises")
            .inOrder()
        assertThat(items.first().howMany).isEqualTo(HowMany.Reps(10))
        assertThat(items.first().howOften).isEqualTo(HowOften.ADay(2))
    }

    @Test
    fun aSpokenSentenceWithThePersonTalkingRoundItStillBecomesTwoItems() {
        val said = "my physio wants me doing ten sit to stands twice a day and heel raises"

        val items = PlanMatching.readAll(said)

        assertThat(items.map { it.movement?.id }).containsExactly("sit_to_stand", "heel_raises")
            .inOrder()
        assertThat(items.first().howMany).isEqualTo(HowMany.Reps(10))
        assertThat(items.first().howOften).isEqualTo(HowOften.ADay(2))
    }

    @Test
    fun aSpokenListWithCommasInItBecomesOneItemPerMovement() {
        val said = "chair stands, heel raises and a walk"

        val items = PlanMatching.readAll(said)

        assertThat(items.map { it.movement?.id })
            .containsExactly("sit_to_stand", "heel_raises", "walk").inOrder()
    }

    @Test
    fun aCommaBetweenAMovementAndItsNumbersIsNotACut() {
        // The half after the comma names no movement, so the line stays whole and the
        // numbers stay attached to the movement they were written for.
        val items = PlanMatching.readAll("heel raises, 3 sets of 10, twice a day")

        assertThat(items).hasSize(1)
        assertThat(items.single().movement?.id).isEqualTo("heel_raises")
        assertThat(items.single().howMany).isEqualTo(HowMany.Reps(reps = 10, sets = 3))
        assertThat(items.single().howOften).isEqualTo(HowOften.ADay(2))
    }

    @Test
    fun aCommaOnALineSayingNotToDoSomethingIsNotACutEither() {
        val items = PlanMatching.readAll("avoid stairs, heel raises")

        assertThat(items).hasSize(1)
        assertWithMessage("the line said to avoid something and names nothing to do")
            .that(items.single().movement).isNull()
        assertThat(items.single().sureness).isEqualTo(Sureness.Unmatched)
    }

    @Test
    fun aMovementWithAndInItsNameIsNotCutInHalf() {
        val items = PlanMatching.readAll("heel and toe")

        assertThat(items).hasSize(1)
        assertThat(items.single().movement?.id).isEqualTo("warm_heel_toe")
    }

    @Test
    fun aFrequencyWithAndInItIsNotCutInHalf() {
        val items = PlanMatching.readAll("chair stands morning and evening")

        assertThat(items).hasSize(1)
        assertThat(items.single().howOften).isEqualTo(HowOften.ADay(2))
    }

    @Test
    fun listMarkersAndEmptyLinesComeOffAScannedSheet() {
        val sheet = """
            Home programme
            1. 10 chair stands twice a day

            - heel raises x15
            * walk 10 minutes daily
            -----
        """.trimIndent()

        val items = PlanMatching.readAll(sheet)

        assertThat(items.map { it.movement?.id })
            .containsExactly(null, "sit_to_stand", "heel_raises", "walk").inOrder()
        assertWithMessage("the numbered marker is not read as a number")
            .that(items[1].howMany).isEqualTo(HowMany.Reps(10))
    }

    @Test
    fun readingTheSameSheetTwiceGivesTheSameAnswer() {
        val sheet = "3 sets of 8 wall push ups\nheel raises x15"

        assertThat(PlanMatching.readAll(sheet)).isEqualTo(PlanMatching.readAll(sheet))
    }

    // --- The table itself ----------------------------------------------------------

    @Test
    fun everyMovementTheTableNamesIsInTheLibrary() {
        PlanMatching.tableIds.forEach { id ->
            assertWithMessage(id).that(Movements.byId(id)).isNotNull()
        }
    }

    @Test
    fun everyMovementInTheLibraryCanBeAskedForByName() {
        Movements.all.forEach { movement ->
            val item = PlanMatching.readOne(movement.name)
            assertWithMessage(movement.name).that(item.movement?.id).isEqualTo(movement.id)
        }
    }

    // --- The overlap rule ------------------------------------------------------------

    @Test
    fun aMovementOnBothListsIsNamedOnce() {
        val plan = PlanMatching.read(listOf("10 chair stands twice a day", "heel raises x15"))
        val session = sessionOf("sit_to_stand", "wall_push_up")

        assertThat(PlanMatching.onBoth(plan, session).map { it.id }).containsExactly("sit_to_stand")
    }

    @Test
    fun theAppsOwnSuggestionsNeverIncludeWhatIsAlreadyOnThePlan() {
        val plan = PlanMatching.read(listOf("10 chair stands twice a day"))
        val session = sessionOf("sit_to_stand", "wall_push_up", "walk")

        val extras = PlanMatching.alsoIfYouWantMore(plan, session)

        assertThat(extras.map { it.movement.id }).containsExactly("wall_push_up", "walk").inOrder()
        assertWithMessage("the plan is not touched by asking what else there is")
            .that(plan.map { it.movement?.id }).containsExactly("sit_to_stand")
    }

    @Test
    fun anUnmatchedLineCountsForNothingInTheOverlap() {
        val plan = PlanMatching.read(listOf("clamshells x 20"))
        val session = sessionOf("sit_to_stand")

        assertThat(PlanMatching.onBoth(plan, session)).isEmpty()
        assertThat(PlanMatching.alsoIfYouWantMore(plan, session)).hasSize(1)
    }

    // --- Flagging without dropping -------------------------------------------------------

    @Test
    fun somethingThePersonAvoidsIsFlaggedAndLeftIn() {
        val plan = PlanMatching.read(listOf("3 sets of 8 wall push ups", "heel raises x15"))

        val ask = PlanMatching.toAskAbout(plan, setOf(Exclusion.Pushing), GettingAround.OnFeet)

        assertThat(ask.map { it.movement?.id }).containsExactly("wall_push_up")
        assertWithMessage("the plan still has both, exactly as it was given")
            .that(plan).hasSize(2)
    }

    @Test
    fun nothingIsFlaggedWhenNothingClashes() {
        val plan = PlanMatching.read(listOf("heel raises x15"))

        assertThat(PlanMatching.toAskAbout(plan, emptySet(), GettingAround.OnFeet)).isEmpty()
    }

    private fun sessionOf(vararg ids: String) = SessionPlan(
        steps = ids.mapNotNull { Movements.byId(it) }.map { Step(it, it.startTarget) },
        adaptation = Adaptation(Adaptation.Kind.None),
    )
}
