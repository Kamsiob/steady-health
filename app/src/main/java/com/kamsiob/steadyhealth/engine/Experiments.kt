package com.kamsiob.steadyhealth.engine

import kotlin.math.abs

/**
 * The one thing a test is allowed to change.
 *
 * LOGIC.md 9b lists these and forbids everything else, and the forbidding is the
 * important half: never eating, never restriction, never medication, never sleep
 * duration. Those are the variables an app that experiments on somebody's body
 * has no business touching, and there is no value in this enum for any of them.
 */
enum class Variable(val id: String) {
    TimeOfDay("time_of_day"),
    OrderOfExercises("order"),
    WhichExercise("which_exercise"),
    WalkBeforeOrAfter("walk_before_after"),
    IndoorOrOutdoor("indoor_outdoor"),
    RestBetweenSets("rest_between_sets"),
}

/** Which half of the test is running. Two weeks each, in the order offered. */
enum class Arm(val id: String) { A("a"), B("b") }

/** One two-week test, as the engine sees it. */
data class Experiment(
    val variable: Variable,
    /** What the person does now. */
    val conditionA: String,
    /** What the test is trying instead. */
    val conditionB: String,
    val measureId: String,
    val startedOnDay: Long,
) {
    fun arm(today: Long): Arm =
        if (today - startedOnDay < WEEKS_PER_ARM * DAYS_IN_WEEK) Arm.A else Arm.B

    fun finished(today: Long): Boolean =
        today - startedOnDay >= 2 * WEEKS_PER_ARM * DAYS_IN_WEEK

    companion object {
        const val WEEKS_PER_ARM = 2
        const val DAYS_IN_WEEK = 7
    }
}

/** What the two weeks each produced, and the only reading the app will give. */
data class Outcome(
    val aValues: List<Double>,
    val bValues: List<Double>,
    val measure: Measure?,
) {
    val aAverage: Double get() = aValues.averageOrZero()
    val bAverage: Double get() = bValues.averageOrZero()

    /** Signed, positive when B is the better one, in the measure's own direction. */
    val difference: Double
        get() = (bAverage - aAverage).let { if (measure?.moreIsBetter != false) it else -it }

    /**
     * True only when the difference is bigger than the measurement noise.
     *
     * A measure with no published detectable change can never produce a winner,
     * for the same reason it can never make an ability Quieter: a difference
     * nobody has established is meaningful is not a difference.
     */
    val realDifference: Boolean
        get() = measure?.detectableChange?.let { abs(bAverage - aAverage) >= it } ?: false

    val winner: Arm?
        get() = when {
            !realDifference -> null
            difference > 0 -> Arm.B
            else -> Arm.A
        }

    val enoughToRead: Boolean get() = aValues.isNotEmpty() && bValues.isNotEmpty()

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
}

/**
 * Try it and see, from LOGIC.md 9b.
 *
 * The engine finds the pattern, runs the arithmetic, and decides whether there
 * was a difference. AI.md job 4 lets the model word the offer and the result and
 * nothing else, and its post-filter is this object's [Outcome.winner] returning
 * null: a headline that implies a winner when there is none is discarded.
 *
 * "No difference" is a normal outcome and is the answer more often than not. An
 * app that always found something would be an app finding things that are not
 * there.
 */
object ExperimentEngine {

    /** Six weeks of check-ins before anything is offered. LOGIC.md 9b. */
    const val WEEKS_BEFORE_OFFERING = 6

    /**
     * Whether a test may be offered at all.
     *
     * Never in pacing mode, and not because it would be unhelpful: for a
     * post-exertional pattern, deliberately varying what you do for a fortnight
     * to see what happens is the thing that causes harm. LOGIC.md section 7 turns
     * this feature off for those people and never offers it, and that is a
     * decision the person does not have to make while unwell.
     */
    fun mayOffer(weeksOfCheckIns: Int, pacing: Boolean, turnedOn: Boolean): Boolean =
        turnedOn && !pacing && weeksOfCheckIns >= WEEKS_BEFORE_OFFERING

    /** Which values belong to which half, from the day each was recorded. */
    fun split(experiment: Experiment, results: List<MeasureResult>): Outcome {
        val mine = results.filter { it.measureId == experiment.measureId }
        return Outcome(
            aValues = mine.filter { experiment.arm(it.epochDay) == Arm.A }.map { it.value },
            bValues = mine.filter { experiment.arm(it.epochDay) == Arm.B }.map { it.value },
            measure = Measures.byId(experiment.measureId),
        )
    }
}
