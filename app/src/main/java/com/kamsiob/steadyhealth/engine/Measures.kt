package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround

/** What a measure is counted in. Decides the words, never the arithmetic. */
enum class MeasureUnit { Repetitions, Seconds, MetresPerSecond, Stage }

/** How the phone gets the number. */
enum class HowCounted {
    /** The person taps once per repetition. Always available, always the fallback. */
    ByHand,

    /** The phone times each stand from the motion sensor, in a pocket. Grid 10. */
    ByMotion,

    /** The camera counts full repetitions, on the phone, nothing recorded. Grid 11. */
    ByCamera,

    /** The person reads a stopwatch the app runs. */
    ByClock,
}

/**
 * One thing the monthly check measures.
 *
 * Everything here is data, including the thresholds, so that LOGIC.md section 8
 * and this file can be read side by side. Nothing computes a grade and nothing
 * combines two of these, because MASTER_SPEC forbids an aggregate and there is
 * nowhere in the type to put one.
 */
data class Measure(
    val id: String,
    val domain: AbilityDomain,
    val unit: MeasureUnit,
    val counted: HowCounted,
    /** True when more is better. False for the timed ones, where less is. */
    val moreIsBetter: Boolean,
    /**
     * The smallest change that is a change rather than measurement noise.
     *
     * Null where no source gives one. A measure with no detectable change is
     * still recorded and still shown; it just never decides whether an ability is
     * Better, Same or Quieter, because deciding that on noise is how an app tells
     * somebody they are getting worse when they are not.
     */
    val detectableChange: Double?,
    /**
     * Past this, the app says once that it is worth mentioning to a clinician.
     * Never a diagnosis, never repeated, and it never blocks anything.
     */
    val worthMentioning: Double? = null,
    /** How long the person is doing it for, in seconds, where that is fixed. */
    val seconds: Int? = null,
)

/** One number, on one day. */
data class MeasureResult(val measureId: String, val epochDay: Long, val value: Double)

/**
 * The measures, from LOGIC.md section 8 and research/03 Part 5.
 *
 * The monthly check is four of them, one per ability, and MASTER_SPEC section 6.5
 * holds it to ten minutes with a chair and a wall. That constraint is the reason
 * the six-minute walk, the Cooper and the timed up and go are not in it: they
 * need a corridor, a track, or three clear metres, and a check somebody cannot do
 * in their own front room is a check that does not happen.
 */
object Measures {

    val chairStand = Measure(
        id = "chair_stand_30",
        domain = AbilityDomain.GetUp,
        unit = MeasureUnit.Repetitions,
        counted = HowCounted.ByMotion,
        moreIsBetter = true,
        // Rikli and Jones: MDC 2 to 3 repetitions, 4 or more meaningful. Three is
        // the cautious end of a range, which is the end this app takes.
        detectableChange = 3.0,
        seconds = 30,
    )

    val wallPushUps = Measure(
        id = "wall_push_ups",
        domain = AbilityDomain.Carry,
        unit = MeasureUnit.Repetitions,
        counted = HowCounted.ByCamera,
        moreIsBetter = true,
        // No published detectable change for a wall push-up count, so it is
        // recorded and shown and never used to decide an ability's state.
        detectableChange = null,
        seconds = 30,
    )

    val bandRows = Measure(
        id = "band_rows",
        domain = AbilityDomain.Carry,
        unit = MeasureUnit.Repetitions,
        counted = HowCounted.ByHand,
        moreIsBetter = true,
        detectableChange = null,
        seconds = 30,
    )

    val singleLegStance = Measure(
        id = "single_leg_stance",
        domain = AbilityDomain.Steady,
        unit = MeasureUnit.Seconds,
        counted = HowCounted.ByClock,
        moreIsBetter = true,
        // Springer's per-cell norms are unverified, so no norm is shown. The
        // ten second line is the one LOGIC.md gives and is a note, not a grade.
        detectableChange = null,
        worthMentioning = 10.0,
        seconds = 45,
    )

    val fourStageBalance = Measure(
        id = "four_stage_balance",
        domain = AbilityDomain.Steady,
        unit = MeasureUnit.Stage,
        counted = HowCounted.ByClock,
        moreIsBetter = true,
        detectableChange = 1.0,
        // Not holding tandem for ten seconds is stage three. CDC STEADI.
        worthMentioning = 3.0,
    )

    val twoMinuteStep = Measure(
        id = "two_minute_step",
        domain = AbilityDomain.Go,
        unit = MeasureUnit.Repetitions,
        counted = HowCounted.ByMotion,
        moreIsBetter = true,
        detectableChange = null,
        seconds = 120,
    )

    val seatedReach = Measure(
        id = "seated_reach",
        domain = AbilityDomain.GetUp,
        unit = MeasureUnit.Repetitions,
        counted = HowCounted.ByHand,
        moreIsBetter = true,
        detectableChange = null,
        seconds = 30,
    )

    val wheelingMinutes = Measure(
        id = "wheeling_two_minute",
        domain = AbilityDomain.Go,
        unit = MeasureUnit.Repetitions,
        counted = HowCounted.ByHand,
        moreIsBetter = true,
        detectableChange = null,
        seconds = 120,
    )

    /**
     * Sitting unsupported, hands off. The wheelchair user's Steady.
     *
     * The four-stage balance test is not here for them, because every stage of it
     * is standing. Replacing it with a seated measure is not a smaller version of
     * the same thing; it is the right measure for the ability.
     */
    val seatedBalance = Measure(
        id = "seated_balance",
        domain = AbilityDomain.Steady,
        unit = MeasureUnit.Seconds,
        counted = HowCounted.ByClock,
        moreIsBetter = true,
        detectableChange = null,
        seconds = 60,
    )

    val gripHold = Measure(
        id = "grip_hold",
        domain = AbilityDomain.Carry,
        unit = MeasureUnit.Seconds,
        counted = HowCounted.ByClock,
        moreIsBetter = true,
        detectableChange = null,
        seconds = 60,
    )

    val sitToEdge = Measure(
        id = "sit_to_edge",
        domain = AbilityDomain.GetUp,
        unit = MeasureUnit.Repetitions,
        counted = HowCounted.ByHand,
        moreIsBetter = true,
        detectableChange = null,
        seconds = 60,
    )

    val breathHold = Measure(
        id = "slow_breaths",
        domain = AbilityDomain.Go,
        unit = MeasureUnit.Repetitions,
        counted = HowCounted.ByHand,
        moreIsBetter = true,
        detectableChange = null,
        seconds = 60,
    )

    val ankleRange = Measure(
        id = "ankle_pumps",
        domain = AbilityDomain.Steady,
        unit = MeasureUnit.Repetitions,
        counted = HowCounted.ByHand,
        moreIsBetter = true,
        detectableChange = null,
        seconds = 30,
    )

    val all: List<Measure> = listOf(
        chairStand, wallPushUps, bandRows, singleLegStance, fourStageBalance,
        twoMinuteStep, seatedReach, wheelingMinutes, seatedBalance, gripHold,
        sitToEdge, breathHold, ankleRange,
    )

    fun byId(id: String): Measure? = all.firstOrNull { it.id == id }

    /**
     * The check, for this person: one measure per ability, in a fixed order.
     *
     * One each rather than as many as possible, because MASTER_SPEC gives it ten
     * minutes and because four numbers a month that somebody actually takes beat
     * eight they stop taking. The order is Get up, Go, Carry, Steady, the same
     * order as the tiles, so the check reads like the screen it feeds.
     */
    fun check(way: GettingAround, exclusions: Set<Exclusion> = emptySet()): List<Measure> =
        when (way) {
            GettingAround.OnFeet, GettingAround.Walker -> listOf(
                chairStand,
                twoMinuteStep,
                if (Exclusion.Pushing in exclusions) bandRows else wallPushUps,
                if (Exclusion.Impact in exclusions) fourStageBalance else singleLegStance,
            )

            GettingAround.Wheelchair -> listOf(
                seatedReach,
                wheelingMinutes,
                bandRows,
                seatedBalance,
            )

            GettingAround.InBed -> listOf(sitToEdge, breathHold, gripHold, ankleRange)
        }
}
