package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.ExperimentRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.engine.Arm
import com.kamsiob.steadyhealth.engine.Experiment
import com.kamsiob.steadyhealth.engine.ExperimentEngine
import com.kamsiob.steadyhealth.engine.Measures
import com.kamsiob.steadyhealth.engine.Pattern
import com.kamsiob.steadyhealth.engine.PatternMeasure
import com.kamsiob.steadyhealth.engine.Variable
import com.kamsiob.steadyhealth.ui.screens.TryArm
import com.kamsiob.steadyhealth.ui.screens.TryOfferUiState
import com.kamsiob.steadyhealth.ui.screens.TryResultUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Try it and see. Grid screens 14 and 15.
 *
 * The engine finds the pattern, splits the two fortnights and decides whether
 * there was a difference. This turns those into words, and every word it chooses
 * is a hand-written string picked by a condition, so there is nothing here that
 * can claim a result the arithmetic did not produce.
 *
 * Version 1 offers one variable: when the strength set happens. It is the one
 * the grid draws, it is plainly permitted by LOGIC.md 9b, and a feature that
 * offers one honest test is better than one that offers six half-thought ones.
 */
class TryViewModel(application: Application) : AndroidViewModel(application) {

    private val db get() = SteadyDatabase.get(getApplication())
    private val experiments get() = ExperimentRepository(db)

    private val _offer = MutableStateFlow<TryOfferUiState?>(null)
    val offer: StateFlow<TryOfferUiState?> = _offer.asStateFlow()

    private val _result = MutableStateFlow<TryResultUiState?>(null)
    val result: StateFlow<TryResultUiState?> = _result.asStateFlow()

    /** True when there is something to show, so Abilities can offer a way in. */
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private var pattern: Pattern? = null

    fun refresh() = viewModelScope.launch {
        val running = experiments.running()
        if (running != null) {
            readResult(running)
            return@launch
        }
        _result.value = null
        pattern = experiments.offerablePattern(today())
        _offer.value = pattern?.let { offerFor(it) }
        _available.value = _offer.value != null
    }

    private fun offerFor(pattern: Pattern): TryOfferUiState {
        val context = getApplication<Application>()
        val tag = TagLabels.labelFor(pattern.tag).let(context::getString).lowercase()
        return TryOfferUiState(
            observation = context.getString(sentenceFor(pattern), tag, pattern.split),
            evidence = context.resources.getQuantityString(
                R.plurals.try_from_your_words,
                pattern.weeksWith + pattern.weeksWithout,
                pattern.weeksWith + pattern.weeksWithout,
            ),
            question = context.getString(R.string.try_question),
        )
    }

    /**
     * The sentence for one pattern.
     *
     * Weight direction is deliberately not here. A pattern involving weight is
     * still computed and still goes in the visit summary, but it is never the
     * thing an experiment is offered about, because nothing this feature can
     * change is a fair test of somebody's weight.
     */
    @StringRes
    private fun sentenceFor(pattern: Pattern): Int = when (pattern.measure) {
        PatternMeasure.Sleep ->
            if (pattern.higherWithTag) R.string.pattern_sleep_better else R.string.pattern_sleep_worse

        else ->
            if (pattern.higherWithTag) R.string.pattern_moved_more else R.string.pattern_moved_less
    }

    fun start() = viewModelScope.launch {
        val context = getApplication<Application>()
        experiments.start(
            Experiment(
                variable = Variable.TimeOfDay,
                conditionA = context.getString(R.string.condition_mornings),
                conditionB = context.getString(R.string.condition_evenings),
                measureId = Measures.chairStand.id,
                startedOnDay = today(),
            ),
        )
        _offer.value = null
        _available.value = false
    }

    /** Not now. Nothing is recorded as a refusal, because it is not one. */
    fun notNow() = viewModelScope.launch {
        experiments.declineUntil(today() + NOT_NOW_DAYS)
        _offer.value = null
        _available.value = false
    }

    private suspend fun readResult(experiment: Experiment) {
        val context = getApplication<Application>()
        if (!experiment.finished(today())) {
            _available.value = false
            return
        }

        val outcome = ExperimentEngine.split(experiment, experiments.results())
        val measure = Measures.byId(experiment.measureId)
        val unit = measure?.let { context.getString(nameOf(it)) }.orEmpty()

        if (!outcome.enoughToRead) {
            _result.value = TryResultUiState(
                headline = context.getString(R.string.try_result_none),
                reading = context.getString(R.string.try_unfinished),
            )
            _available.value = true
            return
        }

        val winnerName = when (outcome.winner) {
            Arm.A -> experiment.conditionA
            Arm.B -> experiment.conditionB
            null -> null
        }

        _result.value = TryResultUiState(
            headline = winnerName
                ?.let { context.getString(R.string.try_result_title_a, it) }
                ?: context.getString(R.string.try_result_none),
            a = arm(experiment.conditionA, outcome.aAverage, unit),
            b = arm(experiment.conditionB, outcome.bAverage, unit),
            // The engine decides whether there was a difference; this only says
            // which of two hand-written sentences that makes true.
            reading = winnerName
                ?.let { context.getString(R.string.try_reading_winner, it, it.lowercase()) }
                ?: context.getString(R.string.try_reading_none),
            suggestLabel = winnerName?.let { context.getString(R.string.try_suggest, it) },
        )
        _available.value = true
    }

    fun finish(keep: Boolean) = viewModelScope.launch {
        experiments.finish(keep)
        _result.value = null
        _available.value = false
        refresh()
    }

    private fun arm(label: String, value: Double, unit: String) = TryArm(
        label = getApplication<Application>().resources.getQuantityString(
            R.plurals.try_arm_label,
            Experiment.WEEKS_PER_ARM,
            label,
            Experiment.WEEKS_PER_ARM,
        ),
        value = value.toInt().toString(),
        unit = unit,
    )

    private fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private companion object {
        /** "Not now" is not asked again for this long. The same fortnight as an offer. */
        const val NOT_NOW_DAYS = 14L
    }
}
