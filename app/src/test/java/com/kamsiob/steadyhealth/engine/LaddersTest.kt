package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.Ladder
import org.junit.Test

/** LOGIC.md section 5, and the floor it promises. */
class LaddersTest {

    @Test
    fun thereIsAlwaysSomethingToDo() {
        // The promise in LOGIC.md section 5: when nearly everything is excluded
        // there is still the seated ladder, still short walks, still band work.
        val plan = Ladders.visibleLadders(Exclusion.entries.toSet())

        assertThat(plan.hasSomethingToDo).isTrue()
        assertThat(plan.steps(Ladder.ChairAndStanding)).isNotEmpty()
        assertThat(plan.steps(Ladder.Walking)).isNotEmpty()
    }

    @Test
    fun pushingHidesThePushingLadder() {
        val plan = Ladders.visibleLadders(setOf(Exclusion.Pushing))

        assertThat(plan.steps(Ladder.Pushing)).isEmpty()
    }

    @Test
    fun theFloorExclusionHidesFloorWork() {
        val plan = Ladders.visibleLadders(setOf(Exclusion.GettingOnTheFloor))

        assertThat(plan.steps(Ladder.Floor)).isEmpty()
    }

    @Test
    fun lyingFlatHidesTheOneStepThatNeedsIt() {
        val plan = Ladders.visibleLadders(setOf(Exclusion.LyingFlat))

        assertThat(plan.steps(Ladder.Floor).map { it.name }).doesNotContain("Glute bridge")
        assertThat(plan.steps(Ladder.Floor)).isNotEmpty()
    }

    @Test
    fun everyStepBelongsToExactlyOneAbility() {
        // MASTER_SPEC section 4. There is nowhere to put a step that belongs to
        // none, and nothing that belongs to two.
        val all = Ladders.visibleLadders(emptySet()).ladders.flatMap { it.steps }

        assertThat(all).isNotEmpty()
        all.forEach { assertThat(AbilityDomain.entries).contains(it.domain) }
    }

    @Test
    fun theWalkingLadderStartsAtTwoMinutes() {
        // ONBOARDING.md names it: "What's a two-minute walk from your door?"
        assertThat(Ladders.walking.first().amount).isEqualTo(2)
    }

    @Test
    fun everyLadderGoesUpwards() {
        listOf(Ladders.walking, Ladders.chairAndStanding, Ladders.floor, Ladders.pushing, Ladders.balance)
            .forEach { ladder ->
                assertThat(ladder.map { it.index }).isInOrder()
            }
    }

    @Test
    fun everyStepSaysHowToDoIt() {
        // VISUALS.md: version 1 ships with written instructions and no animation,
        // and those words have to stand alone well enough to do the movement from.
        val all = listOf(
            Ladders.walking,
            Ladders.chairAndStanding,
            Ladders.floor,
            Ladders.pushing,
            Ladders.balance,
        ).flatten()

        all.forEach {
            assertThat(it.instruction).isNotEmpty()
            assertThat(it.name).isNotEmpty()
        }
    }

    @Test
    fun everyBalanceStepSaysToStandNearSomething() {
        // LOGIC.md section 5: always "near a wall or counter" in the instruction.
        Ladders.balance.forEach {
            assertThat(it.instruction.lowercase()).contains("near a wall or counter")
        }
    }
}
