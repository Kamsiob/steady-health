package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import kotlin.math.abs

/** What one measure did between two checks. */
data class MeasureChange(
    val measure: Measure,
    val from: MeasureResult,
    val to: MeasureResult,
) {
    /** Signed, in the measure's own unit, positive when it moved the good way. */
    val movement: Double
        get() = (to.value - from.value).let { if (measure.moreIsBetter) it else -it }

    /**
     * True when the movement is bigger than measurement noise.
     *
     * False for every measure with no published detectable change, which is most
     * of them. That is deliberate: a measure with no source for what counts as a
     * change cannot be allowed to tell somebody their body changed.
     */
    val beyondNoise: Boolean
        get() = measure.detectableChange?.let { abs(to.value - from.value) >= it } ?: false

    val improved: Boolean get() = beyondNoise && movement > 0
    val quieter: Boolean get() = beyondNoise && movement < 0
    val held: Boolean get() = !beyondNoise
}

/**
 * Better, Same, or Quieter, from LOGIC.md 3b.
 *
 * The three rules, exactly as written: Better when any measure improved beyond
 * its detectable change and none went the other way. Same when all of them are
 * within it. Quieter when one has gone the other way in three consecutive checks.
 *
 * Three consecutive checks is the whole safety of the Quieter rule. One month is
 * a bad morning, two is a coincidence, and the app says nothing about either. It
 * costs three months of silence to avoid telling somebody their body is failing
 * because they were tired on a Tuesday, and that is the right trade.
 */
object AbilityEngine {

    /** Quieter needs this many consecutive checks moving the same way. */
    const val CHECKS_BEFORE_QUIETER = 3

    /** Once shown, not again for this long. LOGIC.md 3b. */
    const val QUIETER_SILENCE_DAYS = 182

    /**
     * The state of one ability, from every result belonging to it.
     *
     * [results] may hold any number of checks in any order; only the last three
     * matter and they are sorted here.
     */
    fun stateOf(domain: AbilityDomain, results: List<MeasureResult>): AbilityState {
        val changes = lastChanges(domain, results)
        if (changes.isEmpty()) return AbilityState.Same
        return when {
            quieterRun(domain, results) -> AbilityState.Quieter
            changes.any { it.improved } && changes.none { it.quieter } -> AbilityState.Better
            else -> AbilityState.Same
        }
    }

    /** What each measure did between the last two checks, for the detail page. */
    fun lastChanges(domain: AbilityDomain, results: List<MeasureResult>): List<MeasureChange> =
        byMeasure(domain, results).mapNotNull { (measure, taken) ->
            if (taken.size < 2) return@mapNotNull null
            MeasureChange(measure, taken[taken.lastIndex - 1], taken.last())
        }

    /**
     * True when some measure has gone the other way three checks running.
     *
     * Every step of that run has to be beyond the measure's own detectable
     * change. A measure with no detectable change can never produce a run, which
     * is the point.
     */
    fun quieterRun(domain: AbilityDomain, results: List<MeasureResult>): Boolean =
        byMeasure(domain, results).any { (measure, taken) ->
            if (taken.size <= CHECKS_BEFORE_QUIETER) return@any false
            val recent = taken.takeLast(CHECKS_BEFORE_QUIETER + 1)
            recent.zipWithNext { from, to -> MeasureChange(measure, from, to) }.all { it.quieter }
        }

    /** Every measure that held, for the sentence that sits beside Quieter. */
    fun whatHeld(domain: AbilityDomain, results: List<MeasureResult>): List<Measure> =
        lastChanges(domain, results).filter { it.held }.map { it.measure }

    private fun byMeasure(
        domain: AbilityDomain,
        results: List<MeasureResult>,
    ): List<Pair<Measure, List<MeasureResult>>> = results
        .groupBy { it.measureId }
        .mapNotNull { (id, taken) ->
            Measures.byId(id)
                ?.takeIf { it.domain == domain }
                ?.let { it to taken.sortedBy { result -> result.epochDay } }
        }
}
