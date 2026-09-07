package com.kamsiob.steadyhealth.ai

import com.kamsiob.steadyhealth.domain.DayRating
import com.kamsiob.steadyhealth.domain.TalkTest

/**
 * The Sunday write-up without the model.
 *
 * AI.md: "without the model, the Sunday write-up is a fixed template filled from
 * the engine's numbers." This is that, and it is not a stopgap: the model is an
 * optional download that most people will never install, so for most people this
 * is the Sunday note, and it has to be worth reading on its own.
 *
 * The sentences are hand-written and chosen by conditions. Nothing here
 * generates language, which is why it can be held to the same rules as the model
 * and checked by the same filter.
 *
 * The strings live in code rather than resources for now, alongside every step
 * name in Ladders.kt, and move together in Phase 6 when the four languages land.
 */
object WeekWriter {

    /** At most this many words in the whole note. AI.md job 3. */
    const val MOST_WORDS = 80

    fun write(brief: WeekBrief): WeekNote {
        if (brief.nothingHappened) return WeekNote(listOf(QUIET_WEEK), fromModel = false)
        val paragraphs = listOfNotNull(whatYouDid(brief), whatYouSaid(brief))
        return WeekNote(paragraphs, fromModel = false)
    }

    /** The first paragraph: what happened, in the order somebody would say it. */
    private fun whatYouDid(brief: WeekBrief): String {
        val parts = buildList {
            add(daysLine(brief))
            talkLine(brief)?.let(::add)
            if (brief.stepOffered) add("A longer one is ready when you are.")
            weightLine(brief)?.let(::add)
        }
        return parts.joinToString(" ")
    }

    private fun daysLine(brief: WeekBrief): String = when {
        brief.daysMoved == 0 -> "You did not get out this week."
        brief.minutes == null -> "You moved on ${days(brief.daysMoved)}."
        else -> "You moved on ${days(brief.daysMoved)}, ${brief.minutes} minutes in all."
    }

    /**
     * The talk test, said as what it means rather than as a count.
     *
     * Only when every session agreed, because "three easy and one hard" is a
     * table, not a sentence, and the table is on the other tab.
     */
    private fun talkLine(brief: WeekBrief): String? {
        if (brief.talkResults.isEmpty()) return null
        return when {
            brief.talkResults.all { it == TalkTest.YesEasily } -> "Every one felt easy."
            brief.talkResults.all { it == TalkTest.No } -> "They were hard going this week."
            else -> null
        }
    }

    /** Never a number, and never in the same sentence as anything about food. */
    private fun weightLine(brief: WeekBrief): String? =
        brief.weightDirection?.let { "Your weight is ${it.id} than last week." }

    /** The second paragraph: their own week, from their own words. */
    private fun whatYouSaid(brief: WeekBrief): String? {
        val parts = buildList {
            sleepLine(brief)?.let(::add)
            moodLine(brief)?.let(::add)
            bodyLine(brief)?.let(::add)
            ratingLine(brief)?.let(::add)
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun sleepLine(brief: WeekBrief): String? {
        val badly = brief.speakableTags.firstOrNull { it.tag == "slept_badly" || it.tag == "short_sleep" }
        val well = brief.speakableTags.firstOrNull { it.tag == "slept_well" }
        return when {
            badly != null && badly.days >= MANY_DAYS -> "Sleep came up on ${days(badly.days)}."
            well != null && well.days >= MANY_DAYS -> "You slept well most of the week."
            else -> null
        }
    }

    private fun moodLine(brief: WeekBrief): String? {
        val stressed = brief.speakableTags.firstOrNull { it.tag == "stressed" || it.tag == "busy" }
        return stressed?.takeIf { it.days >= MANY_DAYS }?.let { "It was a full week." }
    }

    private fun bodyLine(brief: WeekBrief): String? {
        val sore = brief.speakableTags.firstOrNull { it.tag == "sore" || it.tag == "pain" }
        return sore?.takeIf { it.days >= MANY_DAYS }?.let {
            "You mentioned soreness on ${days(it.days)}."
        }
    }

    /**
     * How the days felt, only when they agreed with each other.
     *
     * A week of three good days and three rough ones is a week, not a finding,
     * and the app has nothing useful to say about it.
     */
    private fun ratingLine(brief: WeekBrief): String? {
        val ratings = brief.dayRatings
        if (ratings.size < MANY_DAYS) return null
        return when {
            ratings.all { it == DayRating.Good } -> "You said the days went well."
            ratings.all { it == DayRating.Rough } -> "It sounds like a hard week."
            else -> null
        }
    }

    private fun days(count: Int) = if (count == 1) "one day" else "$count days"

    /** Three days out of seven is where a thing stops being one day. */
    private const val MANY_DAYS = 3

    private const val QUIET_WEEK =
        "A quiet week here. Nothing to read back yet, and nothing wrong with that."
}
