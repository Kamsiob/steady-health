package com.kamsiob.steadyhealth.places

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.GettingAround
import org.junit.Test

/**
 * The six questions, and the rules Part 8 makes about them.
 *
 * Most of what matters here is what the feature does NOT do, so most of these are
 * tests that nothing counts anything.
 */
class PlacesTest {

    @Test
    fun thereAreSixOfThemAndEveryIdIsDistinct() {
        assertThat(Places.all).hasSize(SIX)
        assertThat(Places.all.map { it.id }).containsNoDuplicates()
        assertThat(Places.count).isEqualTo(SIX)
    }

    @Test
    fun everyOneOfPartEightsSixTopicsIsHere() {
        // ADDENDUM-03 Part 8 names them: lighting on the stairs and the night route
        // to the bathroom, loose rugs and cables, where a rail would help, what is on
        // the floor near the bed, something to hold in the bathroom, indoor shoes.
        assertThat(Places.all.map { it.id }).containsExactly(
            "stairs_light",
            "night_route",
            "rugs_cables",
            "something_to_hold",
            "near_the_bed",
            "indoor_shoes",
        )
    }

    @Test
    fun everyQuestionHasSomethingToSayUnderIt() {
        Places.all.forEach { question ->
            assertThat(question.where).isNotEqualTo(0)
            assertThat(question.question).isNotEqualTo(0)
            assertThat(question.fix).isNotEqualTo(0)
        }
        assertThat(Places.all.map { it.fix }).containsNoDuplicates()
    }

    @Test
    fun theFixIsOfferedForNotYetAndForNotSureButNotForSorted() {
        assertThat(Said.Sorted.wantsTheFix).isFalse()
        assertThat(Said.NotYet.wantsTheFix).isTrue()
        assertThat(Said.NotSure.wantsTheFix).isTrue()
    }

    @Test
    fun itIsFinishedOnlyWhenEveryQuestionHasAnAnswer() {
        val all = Places.all.associate { it.id to Said.Sorted }
        assertThat(Places.finished(all)).isTrue()
        assertThat(Places.finished(all - "indoor_shoes")).isFalse()
        assertThat(Places.finished(emptyMap())).isFalse()
    }

    @Test
    fun anAnswerOfNotSureStillCountsAsAnswered() {
        // Four of the six are about a room somebody is not standing in. Forcing a
        // yes or a no there gets guesses, so not sure is an answer and not a skip.
        val all = Places.all.associate { it.id to Said.NotSure }
        assertThat(Places.finished(all)).isTrue()
    }

    @Test
    fun whatIsWorthALookKeepsTheOrderTheyWereAskedIn() {
        val said = mapOf(
            "indoor_shoes" to Said.NotYet,
            "stairs_light" to Said.NotSure,
            "rugs_cables" to Said.Sorted,
        )

        assertThat(Places.worthALook(said).map { it.id })
            .containsExactly("stairs_light", "indoor_shoes")
            .inOrder()
    }

    @Test
    fun somebodyWhoSortedEverythingHasNothingToLookAt() {
        val all = Places.all.associate { it.id to Said.Sorted }

        assertThat(Places.worthALook(all)).isEmpty()
    }

    @Test
    fun everyWayIsAskedSixQuestionsAndTheSameSix() {
        // Not five, and not the same six with two of them left to somebody else. The
        // ids are the storage, so they hold across the four; the words are what
        // change, because a flight of stairs and a pair of indoor shoes are not
        // places somebody in a wheelchair moves through.
        GettingAround.entries.forEach { way ->
            val six = Places.forWay(way)
            assertWithMessage(way.id).that(six).hasSize(SIX)
            assertWithMessage(way.id).that(six.map { it.id })
                .containsExactlyElementsIn(Places.all.map { it.id })
            six.forEach {
                assertWithMessage("${way.id} ${it.id}").that(it.where).isNotEqualTo(0)
                assertWithMessage("${way.id} ${it.id}").that(it.question).isNotEqualTo(0)
                assertWithMessage("${way.id} ${it.id}").that(it.fix).isNotEqualTo(0)
            }
        }
    }

    @Test
    fun nobodyIsAskedAboutStairsOrIndoorShoesFromASeat() {
        val onFeet = Places.forWay(GettingAround.OnFeet)

        listOf(GettingAround.Wheelchair, GettingAround.InBed).forEach { way ->
            val theirs = Places.forWay(way).associateBy { it.id }
            listOf("stairs_light", "indoor_shoes").forEach { id ->
                assertWithMessage("$id is still the on-feet question for ${way.id}")
                    .that(theirs.getValue(id).question)
                    .isNotEqualTo(onFeet.first { it.id == id }.question)
            }
        }
    }

    @Test
    fun aWalkerIsWalking() {
        assertThat(Places.forWay(GettingAround.Walker))
            .isEqualTo(Places.forWay(GettingAround.OnFeet))
    }

    @Test
    fun everyAnswerSurvivesBeingWrittenDownAndReadBack() {
        Said.entries.forEach { assertThat(Said.fromId(it.id)).isEqualTo(it) }
        assertThat(Said.fromId("")).isNull()
        assertThat(Said.fromId("sorted_out")).isNull()
    }

    private companion object {
        const val SIX = 6
    }
}
