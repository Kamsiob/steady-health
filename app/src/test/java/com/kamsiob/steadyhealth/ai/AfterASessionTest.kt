package com.kamsiob.steadyhealth.ai

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The optional line at the end of a session. AI.md job 7.
 *
 * The rules worth testing are the ones that keep it small: a fixed vocabulary, at
 * most three, and nothing about a day where the question is about four minutes.
 */
class AfterASessionTest {

    @Test
    fun everyTagOfferedIsOneOfTheTwentyFour() {
        // The whole safety of pointing a model at somebody's words is that it can
        // return nothing this list does not contain. A tag invented here would be
        // outside that promise even without a model in the room.
        AfterASession.offered.forEach {
            assertWithMessage(it.id).that(Tags.all).contains(it)
        }
    }

    @Test
    fun nothingAboutFoodOrSleepIsAsked() {
        // Those are questions about a day. This one is about the last four minutes.
        val groups = AfterASession.offered.map { it.group }.toSet()
        assertThat(groups).doesNotContain(TagGroup.Food)
        assertThat(groups).doesNotContain(TagGroup.Sleep)
        assertThat(groups).doesNotContain(TagGroup.Life)
    }

    @Test
    fun theBodyTagsAreThereBecauseTheyChangeWhatHappensNext() {
        assertThat(AfterASession.offered.map { it.id }).containsAtLeast("sore", "pain")
    }

    @Test
    fun atMostThreeAreKept() {
        val many = AfterASession.offered.map { it.id }.toSet()

        assertThat(AfterASession.chosen(many)).hasSize(AfterASession.MOST_CHOSEN)
    }

    @Test
    fun theOrderIsTheLibrarysAndNotTheOrderTheyWereTapped() {
        val picked = setOf("calm", "sore")

        assertThat(AfterASession.chosen(picked).map { it.id })
            .containsExactly("sore", "calm")
            .inOrder()
    }

    @Test
    fun aTagThatIsNotOfferedIsNeverKept() {
        assertThat(AfterASession.chosen(setOf("ate_out", "slept_well"))).isEmpty()
    }

    @Test
    fun theGridStopsAtThreeRatherThanWarning() {
        assertThat(AfterASession.roomForMore(emptySet())).isTrue()
        assertThat(AfterASession.roomForMore(setOf("sore", "pain"))).isTrue()
        assertThat(AfterASession.roomForMore(setOf("sore", "pain", "calm"))).isFalse()
    }

    @Test
    fun choosingNothingIsAnOrdinaryOutcome() {
        // AI.md: "most will skip it entirely, which is fine."
        assertThat(AfterASession.chosen(emptySet())).isEmpty()
    }
}
