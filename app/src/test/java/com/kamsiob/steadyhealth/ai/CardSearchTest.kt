package com.kamsiob.steadyhealth.ai

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Finding a card without the model, which is the path most people are on.
 *
 * The titles are passed in rather than read from resources, so this runs without
 * a device and without Robolectric. They are the real titles, copied from
 * CONTENT.md, and a test below holds them to the real card ids.
 */
class CardSearchTest {

    @Test
    fun thereAreTwentyFiveCardsAndEveryIdIsDistinct() {
        assertThat(Cards.all.map { it.id }).containsNoDuplicates()
        assertThat(Cards.all).hasSize(titles.size)
    }

    @Test
    fun everyCardIsReachableByAtLeastOneQuestionSomebodyWouldAsk() {
        questions.forEach { (question, expected) ->
            assertWithMessage(question)
                .that(CardSearch.best(question, titles)?.id)
                .isEqualTo(expected)
        }
    }

    @Test
    fun everyCardIsTheAnswerToSomethingInThisTest() {
        // A card nobody can find is a card nobody reads, so every one of them has
        // to be the best answer to at least one question here.
        val reachable = questions.values.toSet()
        Cards.all.forEach { card ->
            assertWithMessage("${card.id} is reachable").that(reachable).contains(card.id)
        }
    }

    @Test
    fun aQuestionWithNothingBehindItReturnsNothing() {
        assertThat(CardSearch.best("", titles)).isNull()
        assertThat(CardSearch.best("qqqq zzzz", titles)).isNull()
    }

    @Test
    fun theSearchNeverInventsACard() {
        val ids = Cards.all.map { it.id }.toSet()
        questions.keys.forEach { question ->
            CardSearch.search(question, titles).forEach {
                assertThat(ids).contains(it.card.id)
            }
        }
    }

    @Test
    fun aVagueQuestionDoesNotPickAFavourite() {
        // "Why" is in most of these titles. If the common words counted, the
        // ordering would be noise dressed up as an answer.
        val matches = CardSearch.search("why", titles)
        assertThat(matches).isEmpty()
    }

    private val titles = mapOf(
        "how_it_works" to "How Steady Health works",
        "scale_jumps" to "Why the scale jumps overnight",
        "weight_flat" to "Why weight goes flat",
        "two_minutes" to "Does two minutes really count?",
        "fasting" to "How intermittent fasting works",
        "muscle_meds" to "Muscle and weight-loss medications",
        "smoothed" to "Why the app shows a smoothed number",
        "no_calories" to "Why there are no calories here",
        "talk_test" to "What the talk test is",
        "slowly" to "Why walks get longer slowly",
        "after_a_break" to "What to do after a break",
        "same_counts" to "Why staying the same counts",
        "what_you_want" to "Why the app asks what you want to be able to do",
        "strength_first" to "Why strength comes before cardio after fifty",
        "balance_skill" to "Balance is a skill, not a gift",
        "after_operation" to "What usually happens after an operation",
        "what_a_physio_does" to "What a physiotherapist actually does",
        "talk_to_a_doctor" to "How to raise mobility with a doctor",
        "footwear" to "Shoes and staying steady",
        "protein" to "Protein and muscle, plainly",
        "morning_stiffness" to "What morning stiffness usually is",
        "off_the_floor" to "Getting up off the floor, in stages",
        "grip" to "Why the app measures grip",
        "sore_day" to "Sore the day after, and what it means",
        "sleep_energy" to "Sleep, energy, and why the app asks",
    )

    private val questions = mapOf(
        "why did I gain four pounds overnight" to "scale_jumps",
        "the scale jumped this morning" to "scale_jumps",
        "my weight is stuck" to "weight_flat",
        "nothing is moving any more" to "weight_flat",
        "does two minutes really count" to "two_minutes",
        "is a short walk enough" to "two_minutes",
        "should I try fasting" to "fasting",
        "what about an eating window" to "fasting",
        "will I lose muscle on semaglutide" to "muscle_meds",
        "will the medication cost me muscle" to "muscle_meds",
        "why are there two numbers" to "smoothed",
        "what is the smoothed number" to "smoothed",
        "can I log my meals" to "no_calories",
        "why no food diary" to "no_calories",
        "what is the talk test" to "talk_test",
        "how hard should the pace be" to "talk_test",
        "why are the walks so slow to get longer" to "slowly",
        "when do I get offered more" to "slowly",
        "I have been away for a month" to "after_a_break",
        "I missed two weeks" to "after_a_break",
        "nothing changed this month" to "same_counts",
        "what if I am just holding" to "same_counts",
        "why does it ask what i want" to "what_you_want",
        "what is my list for" to "what_you_want",
        "how does this app work" to "how_it_works",
        "what is this app" to "how_it_works",
        "is cardio or strength more use after fifty" to "strength_first",
        "why so much strength and not cardio" to "strength_first",
        "how do i get my balance back" to "balance_skill",
        "is balance a skill" to "balance_skill",
        "what happens after a hip replacement" to "after_operation",
        "what happens after an operation" to "after_operation",
        "what does a physiotherapist do" to "what_a_physio_does",
        "what does a physiotherapist actually do" to "what_a_physio_does",
        "how do i raise this with my doctor" to "talk_to_a_doctor",
        "raise mobility with a doctor" to "talk_to_a_doctor",
        "what shoes are best" to "footwear",
        "what shoes keep you steady" to "footwear",
        "how much protein do i need" to "protein",
        "is protein worth bothering with" to "protein",
        "why am i so stiff in the morning" to "morning_stiffness",
        "is morning stiffness usual" to "morning_stiffness",
        "how do i get up off the floor" to "off_the_floor",
        "what if i end up down on the floor" to "off_the_floor",
        "why does grip matter" to "grip",
        "what does grip tell you" to "grip",
        "i am sore the day after" to "sore_day",
        "sore after a session" to "sore_day",
        "why does it ask about sleep" to "sleep_energy",
        "no energy today" to "sleep_energy",
    )
}
