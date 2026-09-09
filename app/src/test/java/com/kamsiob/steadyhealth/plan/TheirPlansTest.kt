package com.kamsiob.steadyhealth.plan

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.session.Movements
import org.junit.Test

/**
 * Two plans stay two. ADDENDUM-03 Part 6.
 *
 * "A physio plan and an OT plan can coexist, each labelled, each separate" is the
 * whole of it, and the way it quietly stops being true is somebody reading every live
 * line into one list to build a session and then reading the label off whichever plan
 * came back first. That is what the app did: the OT's movements appeared under the
 * physio's name. These hold the two halves apart.
 *
 * The label is checked here as well, because somebody will leave the box empty and a
 * card reading "From your" with nothing after it is worse than one that does not say
 * whose the plan is.
 */
class TheirPlansTest {

    @Test
    fun twoPlansKeepTheirOwnLinesAndTheirOwnNames() {
        val plans = listOf(physio, occupational)

        assertThat(plans.map { it.label }).containsExactly("physio", "OT").inOrder()
        assertThat(plans[0].items.map { it.movement?.id }).containsExactly("sit_to_stand")
        assertThat(plans[1].items.map { it.movement?.id })
            .containsExactly("heel_raises", "wall_push_up").inOrder()
    }

    @Test
    fun theSessionRunsEveryLineOfBothInTheOrderTheyWereGiven() {
        val lines = TheirPlans.lines(listOf(physio, occupational))

        assertThat(lines.map { it.movement?.id })
            .containsExactly("sit_to_stand", "heel_raises", "wall_push_up").inOrder()
        assertWithMessage("a second plan adds to the day, it does not replace the first")
            .that(lines).hasSize(3)
    }

    @Test
    fun aMovementOnBothListsIsNamedByThePlanItIsActuallyOn() {
        val movement = requireNotNull(Movements.byId("heel_raises"))

        val whose = TheirPlans.whose(listOf(physio, occupational), movement, "therapist")

        assertWithMessage("heel raises are the OT's, and the card must not say physio")
            .that(whose).isEqualTo("OT")
    }

    @Test
    fun aMovementOnNeitherPlanNamesNobody() {
        val movement = requireNotNull(Movements.byId("walk"))

        assertThat(TheirPlans.whose(listOf(physio, occupational), movement, "therapist")).isNull()
    }

    @Test
    fun aPlanNobodyLabelledFallsBackToThePlainWord() {
        assertThat(TheirPlan(label = "", items = emptyList()).labelOr("therapist"))
            .isEqualTo("therapist")
        assertThat(TheirPlan(label = "   ", items = emptyList()).labelOr("therapist"))
            .isEqualTo("therapist")
    }

    @Test
    fun aLabelTypedWithSpacesRoundItIsStillTheirName() {
        assertThat(TheirPlan(label = "  physio ", items = emptyList()).labelOr("therapist"))
            .isEqualTo("physio")
    }

    @Test
    fun aClashOnTheSecondPlanIsStillFlagged() {
        // The flag is worked out from every line of every plan, not from the first
        // plan, or a second therapist's sheet could ask for the one thing the person
        // said they avoid and nothing would say so.
        val ask = PlanMatching.toAskAbout(
            TheirPlans.lines(listOf(physio, occupational)),
            setOf(Exclusion.Pushing),
            GettingAround.OnFeet,
        )

        assertThat(ask.map { it.movement?.id }).containsExactly("wall_push_up")
    }

    private val physio = TheirPlan(
        label = "physio",
        items = PlanMatching.read(listOf("10 chair stands twice a day")),
    )

    private val occupational = TheirPlan(
        label = "OT",
        items = PlanMatching.read(listOf("heel raises x15", "3 sets of 8 wall push ups")),
    )
}
