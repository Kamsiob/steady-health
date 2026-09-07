package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.ai.Tags

/** What a pattern is compared against. LOGIC.md section 11. */
enum class PatternMeasure(val id: String) {
    Sleep("sleep"),
    DaysMoved("days_moved"),
    WeightDirection("weight_direction"),
}

/** One week, reduced to the three things a pattern can turn on. */
data class WeekOfDays(
    val weekStartDay: Long,
    /** Which tags appeared on three or more days that week. */
    val tagsOnThreeDays: Set<String>,
    val sleepAverageHours: Double?,
    val daysMoved: Int,
    /** Negative when the smoothed weight came down that week. */
    val weightChangeKg: Double?,
)

/** A pattern the engine found, with the split that earns it. */
data class Pattern(
    val tag: String,
    val measure: PatternMeasure,
    /** True when the weeks with the tag were the higher ones. */
    val higherWithTag: Boolean,
    val weeksWith: Int,
    val weeksWithout: Int,
    /** "5 of 7", exactly as it must appear in anything written about it. */
    val split: String,
)

/**
 * Patterns, from LOGIC.md section 11.
 *
 * Computed after six weeks, per tag, comparing weeks where the tag appeared on
 * three or more days against weeks where it did not, and reported only when the
 * split is at least five of seven weeks in one direction.
 *
 * Restriction tags are excluded from this computation entirely. Not filtered
 * afterwards, not handled carefully: they never enter it. The pattern this app
 * must never find is "you ate light and your weight went down", and the way to
 * never find it is to never look.
 */
object Patterns {

    /** A tag has to be on this many days for the week to count as a week with it. */
    const val DAYS_IN_A_WEEK_WITH = 3

    /** Six weeks before anything is computed. */
    const val WEEKS_BEFORE = 6

    /** At least this many of seven in one direction. */
    const val LEAST_SPLIT = 5
    const val OF_WEEKS = 7

    fun find(weeks: List<WeekOfDays>): List<Pattern> {
        if (weeks.size < WEEKS_BEFORE) return emptyList()
        val tags = weeks.flatMap { it.tagsOnThreeDays }.toSet() - Tags.restriction
        return tags.flatMap { tag -> PatternMeasure.entries.mapNotNull { pattern(tag, it, weeks) } }
    }

    private fun pattern(tag: String, measure: PatternMeasure, weeks: List<WeekOfDays>): Pattern? {
        val with = weeks.filter { tag in it.tagsOnThreeDays }
        val without = weeks.filterNot { tag in it.tagsOnThreeDays }
        if (with.isEmpty() || without.isEmpty()) return null

        val withValues = with.mapNotNull { value(it, measure) }
        val withoutValues = without.mapNotNull { value(it, measure) }
        if (withValues.isEmpty() || withoutValues.isEmpty()) return null

        val higher = withValues.average() > withoutValues.average()

        // The split: how many of the weeks with the tag fell on the same side of
        // the weeks without it. Five of seven, or it is not a pattern.
        val other = withoutValues.average()
        val agreeing = withValues.count { (it > other) == higher }
        if (agreeing < LEAST_SPLIT || withValues.size < LEAST_SPLIT) return null

        return Pattern(
            tag = tag,
            measure = measure,
            higherWithTag = higher,
            weeksWith = with.size,
            weeksWithout = without.size,
            split = "$agreeing of ${withValues.size}",
        )
    }

    private fun value(week: WeekOfDays, measure: PatternMeasure): Double? = when (measure) {
        PatternMeasure.Sleep -> week.sleepAverageHours
        PatternMeasure.DaysMoved -> week.daysMoved.toDouble()
        PatternMeasure.WeightDirection -> week.weightChangeKg
    }
}
