package com.kamsiob.steadyhealth.engine

/** One morning, as the engine sees it. */
data class Reading(val epochDay: Long, val kg: Double)

/** A reading with the smoothed value that followed it. */
data class Smoothed(val epochDay: Long, val rawKg: Double, val smoothedKg: Double)

/** Which way the line has gone, in the only three words the app has for it. */
enum class WeightDirection(val id: String) {
    ALittleLower("a little lower"),
    AboutTheSame("about the same"),
    ALittleHigher("a little higher"),
}

/**
 * Weight, from LOGIC.md section 1.
 *
 * Everything here is a pure function of the readings and the day it is asked
 * about. Nothing reads a clock, so the same inputs always give the same answer
 * and every rule can be checked without a device.
 *
 * The smoothing is the whole reason the app can ask somebody to weigh themselves
 * daily without it being punishing: a single heavy morning moves the number by a
 * tenth of the difference, so water and salt mostly cancel and what is left is
 * the direction.
 */
object WeightEngine {

    /**
     * Alpha 0.10 on the new reading, which is roughly a twenty day average.
     * From the Hacker's Diet. Tunable, and the one number here that changes how
     * the whole app feels.
     */
    const val ALPHA = 0.10

    /** Below this, "about the same". A third of a kilo is inside daily noise. */
    const val SAME_BAND_KG = 0.35

    /** The window a change chip compares across. */
    const val CHANGE_DAYS = 30

    /**
     * The smoothed series. The first reading seeds the average with itself, and
     * missing days are skipped rather than interpolated: a day nobody stood on
     * the scale is not a day with a weight in it.
     */
    fun smooth(readings: List<Reading>): List<Smoothed> {
        var average: Double? = null
        return readings.sortedBy { it.epochDay }.map { reading ->
            val next = average?.let { it + ALPHA * (reading.kg - it) } ?: reading.kg
            average = next
            Smoothed(reading.epochDay, reading.kg, next)
        }
    }

    /** The smoothed value one new reading produces, given the one before it. */
    fun next(previousSmoothed: Double?, reading: Double): Double =
        previousSmoothed?.let { it + ALPHA * (reading - it) } ?: reading

    /**
     * Which way the line has gone since [days] ago, or null when there is not
     * enough to say. Null is a real answer and the screen says nothing rather
     * than saying "about the same" about a week nobody measured.
     */
    fun direction(series: List<Smoothed>, asOfDay: Long, days: Int = CHANGE_DAYS): WeightDirection? {
        val latest = series.lastOrNull { it.epochDay <= asOfDay } ?: return null
        val then = series.lastOrNull { it.epochDay <= asOfDay - days } ?: return null
        val change = latest.smoothedKg - then.smoothedKg
        return when {
            change < -SAME_BAND_KG -> WeightDirection.ALittleLower
            change > SAME_BAND_KG -> WeightDirection.ALittleHigher
            else -> WeightDirection.AboutTheSame
        }
    }

    /** How much the smoothed value moved, for the screens that show a figure. */
    fun changeKg(series: List<Smoothed>, asOfDay: Long, days: Int = CHANGE_DAYS): Double? {
        val latest = series.lastOrNull { it.epochDay <= asOfDay } ?: return null
        val then = series.lastOrNull { it.epochDay <= asOfDay - days } ?: return null
        return latest.smoothedKg - then.smoothedKg
    }
}
