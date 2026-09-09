package com.kamsiob.steadyhealth.ai

import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R

/**
 * One card behind Ask a question.
 *
 * [id] is what the model returns and what is stored; the words are resources,
 * because they are read by a person. [keywords] are what the app matches on
 * without the model, and they are hand-written for the same reason the cards are.
 */
data class Card(
    val id: String,
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
    @param:StringRes val body: Int,
    @param:StringRes val source: Int,
    val keywords: List<String>,
)

/**
 * The cards, from CONTENT.md, which is final copy and ships as written.
 *
 * AI.md job 5 is retrieval only: the model is handed the question and the list of
 * titles and summaries, and returns an id or null. It never writes an answer, and
 * there is nowhere in this file for it to put one.
 *
 * Without the model the person searches the list, which is the whole point of
 * these being hand-written: a card that only a model can find is a card most
 * people will never read.
 *
 * Twenty five of them, which is what ADDENDUM-03 Part 8 asks for. The last twelve
 * are the topics it names, plus the floor guide from item 4 of the same part. That
 * one is a card rather than a session on purpose: it is a sequence to know before
 * it is needed, it cannot be practised safely by following a phone in the moment,
 * and Part 8 says it never mentions falling. It does not.
 */
object Cards {

    val all: List<Card> = listOf(
        Card(
            id = "how_it_works",
            title = R.string.card_how_it_works_title,
            summary = R.string.card_how_it_works_summary,
            body = R.string.card_how_it_works_body,
            source = R.string.card_how_it_works_source,
            keywords = listOf("start", "abilities", "what is this", "how does this work"),
        ),
        Card(
            id = "scale_jumps",
            title = R.string.card_scale_jumps_title,
            summary = R.string.card_scale_jumps_summary,
            body = R.string.card_scale_jumps_body,
            source = R.string.card_scale_jumps_source,
            keywords = listOf("jump", "overnight", "up", "gained", "scale", "water", "morning"),
        ),
        Card(
            id = "weight_flat",
            title = R.string.card_weight_flat_title,
            summary = R.string.card_weight_flat_summary,
            body = R.string.card_weight_flat_body,
            source = R.string.card_weight_flat_source,
            keywords = listOf("flat", "stuck", "plateau", "moving", "nothing is moving"),
        ),
        Card(
            id = "two_minutes",
            title = R.string.card_two_minutes_title,
            summary = R.string.card_two_minutes_summary,
            body = R.string.card_two_minutes_body,
            source = R.string.card_two_minutes_source,
            keywords = listOf("two minutes", "short", "count", "enough", "little"),
        ),
        Card(
            id = "fasting",
            title = R.string.card_fasting_title,
            summary = R.string.card_fasting_summary,
            body = R.string.card_fasting_body,
            source = R.string.card_fasting_source,
            keywords = listOf("fasting", "window", "skip breakfast", "16 8", "eating times"),
        ),
        Card(
            id = "muscle_meds",
            title = R.string.card_muscle_meds_title,
            summary = R.string.card_muscle_meds_summary,
            body = R.string.card_muscle_meds_body,
            source = R.string.card_muscle_meds_source,
            keywords = listOf("muscle", "semaglutide", "tirzepatide", "protein", "injection"),
        ),
        Card(
            id = "smoothed",
            title = R.string.card_smoothed_title,
            summary = R.string.card_smoothed_summary,
            body = R.string.card_smoothed_body,
            source = R.string.card_smoothed_source,
            keywords = listOf("smoothed", "average", "why different", "two numbers"),
        ),
        Card(
            id = "no_calories",
            title = R.string.card_no_calories_title,
            summary = R.string.card_no_calories_summary,
            body = R.string.card_no_calories_body,
            source = R.string.card_no_calories_source,
            keywords = listOf("counting", "food diary", "eating", "log my meals"),
        ),
        Card(
            id = "talk_test",
            title = R.string.card_talk_test_title,
            summary = R.string.card_talk_test_summary,
            body = R.string.card_talk_test_body,
            source = R.string.card_talk_test_source,
            keywords = listOf("talk", "conversation", "pace", "how hard", "effort"),
        ),
        Card(
            id = "slowly",
            title = R.string.card_slowly_title,
            summary = R.string.card_slowly_summary,
            body = R.string.card_slowly_body,
            source = R.string.card_slowly_source,
            keywords = listOf("longer", "slow", "offered", "next step", "why so slow"),
        ),
        Card(
            id = "after_a_break",
            title = R.string.card_after_a_break_title,
            summary = R.string.card_after_a_break_summary,
            body = R.string.card_after_a_break_body,
            source = R.string.card_after_a_break_source,
            keywords = listOf("break", "away", "missed", "stopped", "holiday", "month off"),
        ),
        Card(
            id = "same_counts",
            title = R.string.card_same_counts_title,
            summary = R.string.card_same_counts_summary,
            body = R.string.card_same_counts_body,
            source = R.string.card_same_counts_source,
            keywords = listOf("same", "changed", "nothing changed", "holding", "staying"),
        ),
        Card(
            id = "what_you_want",
            title = R.string.card_what_you_want_title,
            summary = R.string.card_what_you_want_summary,
            body = R.string.card_what_you_want_body,
            source = R.string.card_what_you_want_source,
            keywords = listOf("want", "my list", "rating", "out of ten", "list for"),
        ),
        Card(
            id = "strength_first",
            title = R.string.card_strength_first_title,
            summary = R.string.card_strength_first_summary,
            body = R.string.card_strength_first_body,
            source = R.string.card_strength_first_source,
            keywords = listOf("strength", "cardio", "walking enough", "weights", "after fifty"),
        ),
        Card(
            id = "balance_skill",
            title = R.string.card_balance_skill_title,
            summary = R.string.card_balance_skill_summary,
            body = R.string.card_balance_skill_body,
            source = R.string.card_balance_skill_source,
            keywords = listOf("balance", "wobbly", "one leg", "unsteady", "practice"),
        ),
        Card(
            id = "after_operation",
            title = R.string.card_after_operation_title,
            summary = R.string.card_after_operation_summary,
            body = R.string.card_after_operation_body,
            source = R.string.card_after_operation_source,
            keywords = listOf(
                "operation",
                "surgery",
                "hip replacement",
                "knee replacement",
                "recovery",
            ),
        ),
        Card(
            id = "what_a_physio_does",
            title = R.string.card_what_a_physio_does_title,
            summary = R.string.card_what_a_physio_does_summary,
            body = R.string.card_what_a_physio_does_body,
            source = R.string.card_what_a_physio_does_source,
            keywords = listOf("physio", "physiotherapist", "occupational therapist", "referral"),
        ),
        Card(
            id = "talk_to_a_doctor",
            title = R.string.card_talk_to_a_doctor_title,
            summary = R.string.card_talk_to_a_doctor_summary,
            body = R.string.card_talk_to_a_doctor_body,
            source = R.string.card_talk_to_a_doctor_source,
            keywords = listOf("doctor", "appointment", "tell them", "raise it", "how do i say"),
        ),
        Card(
            id = "footwear",
            title = R.string.card_footwear_title,
            summary = R.string.card_footwear_summary,
            body = R.string.card_footwear_body,
            source = R.string.card_footwear_source,
            keywords = listOf("shoes", "slippers", "footwear", "soles", "indoors"),
        ),
        Card(
            id = "protein",
            title = R.string.card_protein_title,
            summary = R.string.card_protein_summary,
            body = R.string.card_protein_body,
            source = R.string.card_protein_source,
            keywords = listOf("protein", "eat", "meals", "diet", "breakfast"),
        ),
        Card(
            id = "morning_stiffness",
            title = R.string.card_morning_stiffness_title,
            summary = R.string.card_morning_stiffness_summary,
            body = R.string.card_morning_stiffness_body,
            source = R.string.card_morning_stiffness_source,
            keywords = listOf("stiff", "stiffness", "morning", "seized up", "out of bed"),
        ),
        Card(
            id = "off_the_floor",
            title = R.string.card_off_the_floor_title,
            summary = R.string.card_off_the_floor_summary,
            body = R.string.card_off_the_floor_body,
            source = R.string.card_off_the_floor_source,
            keywords = listOf("floor", "get up off the floor", "down on the floor", "stages"),
        ),
        Card(
            id = "grip",
            title = R.string.card_grip_title,
            summary = R.string.card_grip_summary,
            body = R.string.card_grip_body,
            source = R.string.card_grip_source,
            keywords = listOf("grip", "hands", "jars", "hand strength", "bags"),
        ),
        Card(
            id = "sore_day",
            title = R.string.card_sore_day_title,
            summary = R.string.card_sore_day_summary,
            body = R.string.card_sore_day_body,
            source = R.string.card_sore_day_source,
            keywords = listOf("sore", "aching", "day after", "hurts", "next day"),
        ),
        Card(
            id = "sleep_energy",
            title = R.string.card_sleep_energy_title,
            summary = R.string.card_sleep_energy_summary,
            body = R.string.card_sleep_energy_body,
            source = R.string.card_sleep_energy_source,
            keywords = listOf("sleep", "tired", "energy", "bad night", "no energy"),
        ),
    )

    fun byId(id: String): Card? = all.firstOrNull { it.id == id }

    /** How many titles to offer when nothing fits. AI.md job 5. */
    const val CLOSEST = 3
}
