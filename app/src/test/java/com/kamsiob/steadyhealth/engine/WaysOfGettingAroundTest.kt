package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.Ladder
import org.junit.Test

/**
 * The four versions of the app.
 *
 * These tests are mostly about what is never true rather than what is: that no
 * way of getting around is left with nothing to do, that all four abilities exist
 * in all of them, and that the wheelchair and bed versions are not the on-feet
 * version with things taken away.
 */
class WaysOfGettingAroundTest {

    @Test
    fun everyWayOfGettingAroundHasSomethingToDo() {
        WaysOfGettingAround.all.forEach { way ->
            assertWithMessage("${way.way.id} has something to do")
                .that(Ladders.visibleLadders(way.way).hasSomethingToDo)
                .isTrue()
        }
    }

    @Test
    fun everyWayOfGettingAroundFeedsAllFourAbilities() {
        WaysOfGettingAround.all.forEach { way ->
            val fed = Ladders.visibleLadders(way.way)
                .ladders
                .flatMap { it.steps }
                .map { it.domain }
                .toSet()
            assertWithMessage("${way.way.id} feeds every ability")
                .that(fed)
                .containsExactlyElementsIn(AbilityDomain.entries)
        }
    }

    @Test
    fun theWheelchairVersionIsNotTheWalkingVersionWithWalkingRemoved() {
        val chair = Ladders.visibleLadders(GettingAround.Wheelchair).ladders.map { it.ladder }
        assertThat(chair).containsAtLeast(Ladder.Wheeling, Ladder.Seated)
        assertThat(chair).containsNoneOf(Ladder.Walking, Ladder.Floor, Ladder.Balance)
    }

    @Test
    fun wheelingHasTheSameStepsAsWalkingBecauseAMinuteIsAMinute() {
        assertThat(Ladders.wheeling.map { it.amount })
            .isEqualTo(Ladders.walking.map { it.amount })
    }

    @Test
    fun weighingInIsOffOnlyInBed() {
        assertThat(WaysOfGettingAround.all.filterNot { it.weighsIn }.map { it.way })
            .containsExactly(GettingAround.InBed)
    }

    @Test
    fun aBedSessionStartsAtThreePartsAndGrowsByOne() {
        assertThat(Ladders.bedSet(0)).hasSize(Ladders.BED_SET_PARTS)
        assertThat(Ladders.bedSet(1)).hasSize(Ladders.BED_SET_PARTS + 1)
        assertThat(Ladders.bedSet(0).map { it.name })
            .containsAtLeast("Ankle pumps", "Towel squeeze")
            .inOrder()
    }

    @Test
    fun theFirstBedSetIsAboutTwoMinutes() {
        val seconds = Ladders.bedSet(0).sumOf { it.amount }
        assertThat(seconds).isAtLeast(TWO_MINUTES)
        assertThat(seconds).isAtMost(TWO_MINUTES + HALF_A_MINUTE)
    }

    @Test
    fun leavingPushingOutReplacesItRatherThanRemovingIt() {
        val plan = Ladders.visibleLadders(GettingAround.OnFeet, setOf(Exclusion.Pushing))
        assertThat(plan.ladders.map { it.ladder }).doesNotContain(Ladder.Pushing)
        assertThat(plan.steps(Ladder.Seated)).isNotEmpty()
        assertThat(plan.steps(Ladder.Seated).all { it.name.contains("press") }).isTrue()
    }

    @Test
    fun everythingLeftOutStillLeavesSomethingToDo() {
        WaysOfGettingAround.all.forEach { way ->
            assertWithMessage("${way.way.id} with everything left out")
                .that(Ladders.visibleLadders(way.way, Exclusion.entries.toSet()).hasSomethingToDo)
                .isTrue()
        }
    }

    @Test
    fun everyLadderAWayOffersActuallyHasStepsInIt() {
        // A way listing a ladder that renders empty would be a heading with
        // nothing under it, which is worse than not offering it at all.
        WaysOfGettingAround.all.forEach { way ->
            val plan = Ladders.visibleLadders(way.way)
            way.ladders.forEach { ladder ->
                assertWithMessage("${way.way.id} offers ${ladder.id}")
                    .that(plan.steps(ladder))
                    .isNotEmpty()
            }
        }
    }

    @Test
    fun theFourAbilityIdsAreTheSameInAllFourVersions() {
        // The words on the tiles change; the ids behind them never do, because a
        // row written last year has to keep meaning what it meant.
        WaysOfGettingAround.all.forEach { way ->
            assertWithMessage("${way.way.id} renames only known abilities")
                .that(AbilityDomain.entries)
                .containsAtLeastElementsIn(way.renamed)
        }
    }

    private companion object {
        const val TWO_MINUTES = 120
        const val HALF_A_MINUTE = 30
    }
}
