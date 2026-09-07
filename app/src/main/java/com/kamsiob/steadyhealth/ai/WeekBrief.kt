package com.kamsiob.steadyhealth.ai

import com.kamsiob.steadyhealth.domain.DayRating
import com.kamsiob.steadyhealth.domain.TalkTest

/** Which way the smoothed weight went. Never a number, at any point. */
enum class WeightDirection(val id: String) {
    ALittleLower("a little lower"),
    AboutTheSame("about the same"),
    ALittleHigher("a little higher"),
}

/** One tag and how many days it appeared on. */
data class TagCount(val tag: String, val days: Int)

/**
 * Everything the Sunday write-up is allowed to know, from LOGIC.md section 10.
 *
 * The engine assembles this and the model receives exactly it: no database, no
 * second turn, and no weight as a number. The direction word is computed here,
 * so the model cannot be the thing that decides a person's weight went up.
 *
 * When numbers are off, [minutes] is null and nothing downstream may invent one.
 */
data class WeekBrief(
    val daysMoved: Int,
    val minutes: Int?,
    val tags: List<TagCount>,
    val dayRatings: List<DayRating>,
    val sleepAverageHours: Double?,
    val walkName: String,
    val stepOffered: Boolean,
    val talkResults: List<TalkTest>,
    val weightDirection: WeightDirection?,
    val whatTheyWant: String,
) {
    /** The tags that may be spoken about at all. LOGIC.md section 10 and 11. */
    val speakableTags: List<TagCount>
        get() = tags.filterNot { it.tag in Tags.restriction }

    val nothingHappened: Boolean
        get() = daysMoved == 0 && tags.isEmpty() && dayRatings.isEmpty()
}

/** The write-up, however it was produced. */
data class WeekNote(val paragraphs: List<String>, val fromModel: Boolean) {
    val words: Int get() = paragraphs.sumOf { it.trim().split(Regex("\\s+")).size }
}
