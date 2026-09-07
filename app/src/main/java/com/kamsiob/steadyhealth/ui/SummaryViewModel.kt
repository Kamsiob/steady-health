package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ai.SummaryValidator
import com.kamsiob.steadyhealth.ai.SummaryWriter
import com.kamsiob.steadyhealth.ai.VisitWindow
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.VisitRepository
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.engine.Measures
import com.kamsiob.steadyhealth.engine.VisitSummaryEngine
import com.kamsiob.steadyhealth.export.Share
import com.kamsiob.steadyhealth.export.SummaryPage
import com.kamsiob.steadyhealth.export.SummaryPdf
import com.kamsiob.steadyhealth.ui.screens.SummaryNumber
import com.kamsiob.steadyhealth.ui.screens.SummaryUiState
import com.kamsiob.steadyhealth.ui.screens.SummaryWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The visit summary.
 *
 * Its own view model because it is the one place in the app where a model would
 * write something a clinician might act on, and keeping that path in one file
 * makes it possible to read the whole of it in one sitting.
 *
 * The order here is the order that matters: the engine assembles a brief, a
 * writer turns it into paragraphs, and the validator checks every paragraph
 * against the brief before any of it reaches a screen. Today the writer is the
 * template one. When the model is wired it goes in the same slot and through the
 * same check.
 */
class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy { SteadyDatabase.get(application) }
    private val visits by lazy { VisitRepository(db) }

    private val _state = MutableStateFlow(SummaryUiState())
    val state: StateFlow<SummaryUiState> = _state.asStateFlow()

    fun open(window: SummaryWindow = SummaryWindow.SixMonths) = viewModelScope.launch {
        _state.value = SummaryUiState(window = window, loading = true)
        generate(window)
    }

    fun setWindow(window: SummaryWindow) = open(window)

    fun regenerate() = open(_state.value.window)

    /**
     * Write the page and hand it to the share sheet.
     *
     * Nothing is transmitted. The file goes into the app's own cache, the person
     * picks where it goes from there, and the app has no network permission with
     * which to do anything else.
     */
    fun export() = viewModelScope.launch {
        val context = getApplication<Application>()
        val current = _state.value
        val page = SummaryPage(
            title = context.getString(R.string.summary_title),
            window = current.provenance,
            paragraphs = current.paragraphs.ifEmpty {
                listOf(context.getString(R.string.summary_fallback))
            },
            questionsHeading = context.getString(R.string.summary_questions),
            questions = current.questions,
            numbersHeading = context.getString(R.string.summary_numbers),
            numbers = current.numbers.map { it.name to it.value },
            notTracked = context.getString(R.string.summary_not_tracked),
            provenance = current.provenance,
        )
        val file = SummaryPdf.write(context, page, "steady-health-summary.pdf")
        Share.file(
            context = context,
            file = file,
            mimeType = "application/pdf",
            title = context.getString(R.string.summary_export),
        )
    }

    private suspend fun generate(window: SummaryWindow) {
        val context = getApplication<Application>()
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val from = when (window) {
            SummaryWindow.ThreeMonths -> today - THREE_MONTHS
            SummaryWindow.SixMonths -> today - SIX_MONTHS
            SummaryWindow.AllTime -> visits.firstDay() ?: today
        }
        val span = VisitWindow(
            fromDay = maxOf(from, visits.firstDay() ?: from),
            toDay = today,
            label = "${date(from)} to ${date(today)}",
        )

        val inputs = visits.inputs(
            window = span,
            abilityNames = AbilityDomain.entries.associateWith { context.getString(nameFor(it)) },
            measureNames = Measures.all.associate { it.id to context.getString(nameOf(it)) },
            stateNames = AbilityState.entries.associateWith { context.getString(stateFor(it)) },
        )

        val numbers = measuresTable(inputs)
        val provenance = context.getString(
            R.string.summary_provenance,
            date(span.fromDay),
            date(span.toDay),
        )

        if (!VisitSummaryEngine.enough(inputs)) {
            _state.value = SummaryUiState(
                window = window,
                numbers = numbers,
                provenance = provenance,
                notYet = true,
            )
            return
        }

        val brief = VisitSummaryEngine.brief(inputs)
        val summary = SummaryWriter.write(brief)
        val verdict = SummaryValidator.check(summary, brief)

        _state.value = SummaryUiState(
            window = window,
            paragraphs = verdict.kept.map { it.text },
            questions = verdict.questions.map { it.text },
            numbers = numbers,
            provenance = provenance,
            // Never a partial sentence and never an unvalidated one. When nothing
            // survives, the numbers below are still complete and the page says so.
            fellBack = verdict.allFailed,
        )
    }

    /**
     * The measures table. LOGIC.md section 13.
     *
     * Function first, then the levers, and the line about medication at the end
     * because a clinician reading this needs to know what is not in it as much as
     * what is.
     */
    private fun measuresTable(inputs: com.kamsiob.steadyhealth.engine.VisitInputs): List<SummaryNumber> {
        val context = getApplication<Application>()
        val byMeasure = inputs.results.groupBy { it.measureId }
        return buildList {
            AbilityDomain.entries.forEach { domain ->
                add(
                    SummaryNumber(
                        name = inputs.abilityNames[domain].orEmpty(),
                        value = inputs.stateNames[
                            com.kamsiob.steadyhealth.engine.AbilityEngine.stateOf(domain, inputs.results),
                        ].orEmpty(),
                    ),
                )
            }
            byMeasure.forEach { (id, taken) ->
                val sorted = taken.sortedBy { it.epochDay }
                val name = inputs.measureNames[id] ?: id
                add(
                    SummaryNumber(
                        name = name,
                        value = if (sorted.size < 2) {
                            "${sorted.last().value.toInt()}"
                        } else {
                            context.getString(
                                R.string.summary_from_to,
                                "${sorted.first().value.toInt()}",
                                "${sorted.last().value.toInt()}",
                            )
                        },
                    ),
                )
            }
            inputs.items.forEach { item ->
                add(
                    SummaryNumber(
                        name = item.text,
                        value = context.getString(
                            R.string.summary_from_to,
                            "${item.firstRating}",
                            "${item.lastRating}",
                        ),
                    ),
                )
            }
        }
    }

    private fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))

    @StringRes
    private fun nameFor(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_get_up
        AbilityDomain.Go -> R.string.ability_go
        AbilityDomain.Carry -> R.string.ability_carry
        AbilityDomain.Steady -> R.string.ability_steady
    }

    @StringRes
    private fun stateFor(state: AbilityState) = when (state) {
        AbilityState.Better -> R.string.state_better
        AbilityState.Same -> R.string.state_same
        AbilityState.Quieter -> R.string.state_quieter
    }

    private companion object {
        const val THREE_MONTHS = 91L
        const val SIX_MONTHS = 182L
    }
}
