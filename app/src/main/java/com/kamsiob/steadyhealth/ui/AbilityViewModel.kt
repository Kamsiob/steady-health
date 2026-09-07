package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.AbilityRepository
import com.kamsiob.steadyhealth.data.CheckRepository
import com.kamsiob.steadyhealth.data.MovementRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.WeightRepository
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.Ladder
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.engine.AbilityEngine
import com.kamsiob.steadyhealth.engine.Ladders
import com.kamsiob.steadyhealth.engine.LifeSentences
import com.kamsiob.steadyhealth.engine.Measure
import com.kamsiob.steadyhealth.engine.MeasureChange
import com.kamsiob.steadyhealth.engine.MeasureUnit
import com.kamsiob.steadyhealth.engine.Measures
import com.kamsiob.steadyhealth.engine.WeightEngine
import com.kamsiob.steadyhealth.ui.screens.AbilityDetailUiState
import com.kamsiob.steadyhealth.ui.screens.MeasureRow
import com.kamsiob.steadyhealth.ui.screens.TrackedItemState
import com.kamsiob.steadyhealth.ui.screens.WeightPageUiState
import com.kamsiob.steadyhealth.util.Convert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * One ability in depth, and the weight behind it. Grid screens 9 and 19.
 *
 * Two read-only pages, in their own view model because they read and never write.
 * Everything on them comes from rows: the sentence from the hand-written table,
 * the measures from the checks, and what fed them from the sessions and the
 * weight. Nothing here is written by anything that could be wrong about it.
 */
class AbilityViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * The database, resolved on every use rather than held.
     *
     * Deleting everything closes the database and destroys its key, and anything
     * holding the old instance then throws "Database is closed" on its next
     * write. That happened on the phone, on the first screen of setup, right
     * after somebody had deleted everything, which is the worst possible moment
     * for this app to crash.
     *
     * The repositories are stateless wrappers, so resolving them per call costs
     * an object allocation and removes the whole class of bug.
     */
    private val db get() = SteadyDatabase.get(getApplication())
    private val profile get() = ProfileRepository(db)
    private val checks get() = CheckRepository(db)
    private val abilities get() = AbilityRepository(db)
    private val movement get() = MovementRepository(db)
    private val weight get() = WeightRepository(db)

    private val _detail = MutableStateFlow(AbilityDetailUiState())
    val detail: StateFlow<AbilityDetailUiState> = _detail.asStateFlow()

    private val _weightPage = MutableStateFlow(WeightPageUiState())
    val weightPage: StateFlow<WeightPageUiState> = _weightPage.asStateFlow()

    /**
     * One ability in depth. Grid screen 9.
     *
     * Everything on it comes from rows: the sentence from the table, the measures
     * from the checks, and what fed it from the sessions and the weight. Nothing
     * here is written by anything that could be wrong about it.
     */
    fun open(domain: AbilityDomain) = viewModelScope.launch {
        val context = getApplication<Application>()
        val results = checks.results()
        val measured = checks.latestValues()
        val items = abilities.items().filter { it.domain == domain.id }
        val changes = AbilityEngine.lastChanges(domain, results)

        _detail.value = AbilityDetailUiState(
            name = context.getString(nameFor(domain)),
            lifeSentence = sentenceFor(domain, measured, items.firstOrNull()?.text),
            before = beforeSentence(
                domain = domain,
                measured = measured,
                changes = changes,
                takenAtAll = results.any { Measures.byId(it.measureId)?.domain == domain },
            ),
            measures = changes.map { change ->
                MeasureRow(
                    name = context.getString(nameOf(change.measure)),
                    value = measureValue(change.measure, change.to.value),
                    before = context.getString(
                        R.string.ability_before,
                        measureValue(change.measure, change.from.value),
                    ),
                    state = when {
                        change.improved -> AbilityState.Better
                        change.quieter -> AbilityState.Quieter
                        else -> AbilityState.Same
                    },
                )
            },
            whatFedThis = whatFedThis(domain),
            feeds = feeds(domain),
            items = items.mapNotNull { item ->
                abilities.latestRating(item.id)?.let {
                    TrackedItemState(item.text, domain, it.rating)
                }
            },
        )
    }

    /**
     * What this ability looked like at the previous check.
     *
     * Only when there is a previous check and a sentence that was true then and
     * is not the one true now. Repeating today's sentence in the past tense would
     * be the screen saying nothing twice.
     */
    private fun beforeSentence(
        domain: AbilityDomain,
        measured: Map<String, Double>,
        changes: List<MeasureChange>,
        takenAtAll: Boolean,
    ): String {
        val context = getApplication<Application>()
        if (changes.isEmpty()) {
            return context.getString(
                if (takenAtAll) R.string.ability_measured_once else R.string.ability_no_check_yet,
            )
        }
        val then = changes.associate { it.measure.id to it.from.value }
        val was = LifeSentences.forDomain(domain, then) ?: return ""
        val now = LifeSentences.forDomain(domain, measured)
        return if (was.id == now?.id) "" else context.getString(lifeStringOf(was.id))
    }

    /**
     * The two deterministic sentences: what you did, and what you are carrying.
     *
     * Written by the engine from counts it has, never by the model. AI.md
     * forbids the model a life sentence, and this is the same claim with more
     * words in it.
     */
    /**
     * The exercises that feed this ability, from the ladders this person can see.
     *
     * Grid screen 9 calls them "the things that feed it". They are here from the
     * first day, before anything has been measured, because an ability page with
     * nothing on it teaches nobody what the ability is.
     */
    private suspend fun feeds(domain: AbilityDomain): List<String> =
        Ladders.visibleLadders(profile.gettingAround(), profile.exclusions())
            .ladders
            .flatMap { it.steps }
            .filter { it.domain == domain }
            .map { it.name }
            .distinct()
            .take(MOST_FEEDS)

    private suspend fun whatFedThis(domain: AbilityDomain): List<String> {
        val context = getApplication<Application>()
        val plan = Ladders.visibleLadders(profile.gettingAround(), profile.exclusions())
        val sessions = Ladder.entries
            .filter { ladder -> plan.steps(ladder).any { it.domain == domain } }
            .sumOf { movement.doneSessions(it).size }

        val series = weight.series()
        val lost = if (series.size >= 2) series.first().smoothedKg - series.last().smoothedKg else 0.0

        return buildList {
            if (sessions > 0) {
                val count = context.resources.getQuantityString(
                    R.plurals.fed_session_count,
                    sessions,
                    sessions,
                )
                add(context.getString(R.string.fed_sessions, count))
            }
            if (lost > 0 && profile.showNumbers()) {
                val amount = "${Convert.weightLabel(lost, profile.units())} ${unitLabel(profile.units())}"
                add(context.getString(R.string.fed_weight, amount))
            }
        }
    }

    private fun measureValue(measure: com.kamsiob.steadyhealth.engine.Measure, value: Double): String {
        val context = getApplication<Application>()
        val whole = value.toInt()
        return if (measure.unit == MeasureUnit.Seconds) {
            context.resources.getQuantityString(R.plurals.check_secs, whole, whole)
        } else {
            "$whole"
        }
    }

    /** Weight, as a lever. Grid screen 19. */
    fun openWeight() = viewModelScope.launch {
        val context = getApplication<Application>()
        val units = profile.units()
        val series = weight.series()
        val latest = series.lastOrNull()
        val lost = if (series.size >= 2) series.first().smoothedKg - series.last().smoothedKg else 0.0
        val moved = movement.doneSessions(primaryLadder(profile.gettingAround()))
            .filter { it.durationSeconds >= Ladders.COUNTS_AS_A_SESSION_SECONDS }
            .map { it.epochDay }
            .distinct()
            .size

        val showNumbers = profile.showNumbers()
        // Worked out before the state is built rather than inside it, so that
        // nothing here depends on the order named arguments happen to evaluate in.
        val direction = directionWord(series)

        _weightPage.value = WeightPageUiState(
            value = if (showNumbers) {
                latest?.let { Convert.weightLabel(it.smoothedKg, units) }.orEmpty()
            } else {
                direction
            },
            unit = if (showNumbers) unitLabel(units) else "",
            explain = weightExplain(weight.forDay(today())?.rawKg, units, showNumbers, direction),
            morning = LocalTime.now().hour < EVENING_HOUR,
            sinceLabel = context.getString(R.string.weight_since),
            since = if (lost > 0 && showNumbers) {
                "${Convert.weightLabel(lost, units)} ${unitLabel(units)}"
            } else {
                ""
            },
            daysLabel = context.getString(R.string.weight_days),
            days = "$moved",
            asALever = if (lost > 0 && showNumbers) {
                context.getString(
                    R.string.weight_as_lever,
                    "${Convert.weightLabel(lost, units)} ${unitLabel(units)}",
                )
            } else {
                ""
            },
        )
    }

    private fun sentenceFor(
        domain: AbilityDomain,
        measured: Map<String, Double>,
        theirWords: String?,
    ): String {
        val sentence = LifeSentences.forDomain(domain, measured)
        return if (sentence != null) {
            getApplication<Application>().getString(lifeStringOf(sentence.id))
        } else {
            theirWords.orEmpty()
        }
    }

    private fun nameFor(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_get_up
        AbilityDomain.Go -> R.string.ability_go
        AbilityDomain.Carry -> R.string.ability_carry
        AbilityDomain.Steady -> R.string.ability_steady
    }

    private fun unitLabel(units: Units): String = getApplication<Application>().getString(
        if (units == Units.Imperial) R.string.unit_lb else R.string.unit_kg,
    )

    /** The direction, for somebody who asked not to see the figure. */
    private fun directionWord(series: List<com.kamsiob.steadyhealth.engine.Smoothed>): String {
        val context = getApplication<Application>()
        val first = context.getString(R.string.direction_first)
        val now = series.lastOrNull() ?: return first
        val then = series.lastOrNull { it.epochDay <= now.epochDay - A_MONTH } ?: return first
        return context.getString(
            when {
                now.smoothedKg < then.smoothedKg - WeightEngine.SAME_BAND_KG -> R.string.direction_lower
                now.smoothedKg > then.smoothedKg + WeightEngine.SAME_BAND_KG -> R.string.direction_higher
                else -> R.string.direction_same
            },
        )
    }

    private fun weightExplain(
        rawKg: Double?,
        units: Units,
        showNumbers: Boolean,
        comparedTo: String = "",
    ): String {
        val context = getApplication<Application>()
        // "Nothing to compare yet" followed by "than a month ago" is two halves of
        // a sentence that does not exist. The line under the word is only there
        // when the word is a comparison.
        if (!showNumbers) {
            return if (comparedTo == context.getString(R.string.direction_first)) {
                ""
            } else {
                context.getString(R.string.direction_since)
            }
        }
        if (rawKg == null) return context.getString(R.string.weight_first_week)
        return context.getString(
            R.string.weight_daily_line,
            "${Convert.weightLabel(rawKg, units)} ${unitLabel(units)}",
        )
    }

    /** The ladder a session is recorded against, for this way of getting around. */
    private fun primaryLadder(way: GettingAround) = when (way) {
        GettingAround.OnFeet, GettingAround.Walker -> Ladder.Walking
        GettingAround.Wheelchair -> Ladder.Wheeling
        GettingAround.InBed -> Ladder.InBed
    }

    private fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private companion object {
        const val EVENING_HOUR = 18
        const val A_MONTH = 30L

        /** Enough to show what the ability is, not the whole ladder. */
        const val MOST_FEEDS = 5
    }
}
