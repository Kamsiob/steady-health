package com.kamsiob.steadyhealth.ui.onboarding

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.GettingAround
import org.junit.Test

/**
 * The six chips on O3, for each way of getting around.
 *
 * ADDENDUM-03 Part 3 asks for six starter chips and says "not all maintenance". The
 * original six were four sentences about two feet, which is the thing this test
 * exists to stop coming back: a chip is an example of something worth wanting, and a
 * list of them that assumes a body tells everybody with a different one that the app
 * was written for somebody else.
 */
class StartersTest {

    @Test
    fun everyWayGetsSixOfThemAndTheyAreDistinct() {
        GettingAround.entries.forEach { way ->
            val six = Starters.forWay(way)
            assertWithMessage(way.id).that(six).hasSize(SIX)
            assertWithMessage(way.id).that(six.map { it.id }).containsNoDuplicates()
        }
    }

    @Test
    fun noSetIsAllMaintenance() {
        // Two of every six are about being stronger or going further than today,
        // which is what Part 3 means by "not all maintenance" and what the opening
        // of ADDENDUM-03 means by the app having been written for one direction.
        val building = setOf("further", "further_wheel", "stronger", "breath", "edge")

        GettingAround.entries.forEach { way ->
            val count = Starters.forWay(way).count { it.id in building }
            assertWithMessage(way.id).that(count).isAtLeast(TWO)
        }
    }

    @Test
    fun everySetReachesAtLeastThreeOfTheFourAbilities() {
        GettingAround.entries.forEach { way ->
            val domains = Starters.forWay(way).map { it.domain }.toSet()
            assertWithMessage(way.id).that(domains.size).isAtLeast(THREE)
        }
    }

    @Test
    fun nobodySittingDownIsOfferedTheStairsOrTheFloorOrAWalk() {
        val onFeetOnly = setOf("floor", "stairs", "further", "shopping")

        listOf(GettingAround.Wheelchair, GettingAround.InBed).forEach { way ->
            Starters.forWay(way).forEach {
                assertWithMessage("${way.id} was offered ${it.id}")
                    .that(it.id in onFeetOnly)
                    .isFalse()
            }
        }
    }

    @Test
    fun aWalkerIsWalking() {
        assertThat(Starters.forWay(GettingAround.Walker))
            .isEqualTo(Starters.forWay(GettingAround.OnFeet))
    }

    @Test
    fun theSameAmbitionIsOfferedInAllFour() {
        // "Get stronger than I am" is in every set. Leaving it out of one of them
        // would be the app deciding whose ceiling is worth naming.
        GettingAround.entries.forEach { way ->
            assertWithMessage(way.id).that(Starters.forWay(way).map { it.id })
                .contains("stronger")
        }
    }

    @Test
    fun theWholeListIsEveryChipFromEverySetAndEachOfThemOnce() {
        val everyChip = GettingAround.entries.flatMap { Starters.forWay(it) }.map { it.id }.toSet()

        assertThat(Starters.all.map { it.id }).containsNoDuplicates()
        assertThat(Starters.all.map { it.id }).containsExactlyElementsIn(everyChip)
        assertThat(Starters.all.map { it.domain }).containsAnyIn(AbilityDomain.entries)
    }

    private companion object {
        const val SIX = 6
        const val THREE = 3
        const val TWO = 2
    }
}
