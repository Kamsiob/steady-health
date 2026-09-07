package com.kamsiob.steadyhealth.export

import android.content.Context
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ai.SummaryValidator
import com.kamsiob.steadyhealth.ai.SummaryWriter
import com.kamsiob.steadyhealth.ai.VisitWindow
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.VisitRepository
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.engine.AbilityEngine
import com.kamsiob.steadyhealth.engine.Measures
import com.kamsiob.steadyhealth.engine.VisitSummaryEngine
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The visit summary, built once and used in two places.
 *
 * The page is shown on screen and put in the export, and both have to say the
 * same thing. Building it twice from two call sites is how they stop saying the
 * same thing, so it is built here.
 *
 * The pipeline is the one ADDENDUM-01 asks for and does not vary: the engine
 * assembles a brief, a writer turns it into paragraphs, and the validator checks
 * every one against the brief before any of it leaves this function.
 */
object SummaryPages {

    private const val SIX_MONTHS = 182L

    suspend fun build(
        context: Context,
        db: SteadyDatabase,
        fromDay: Long? = null,
    ): SummaryPage {
        val visits = VisitRepository(db)
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val from = fromDay ?: maxOf(today - SIX_MONTHS, visits.firstDay() ?: today)
        val window = VisitWindow(from, today, "${date(from)} to ${date(today)}")

        val inputs = visits.inputs(
            window = window,
            abilityNames = AbilityDomain.entries.associateWith { context.getString(nameFor(it)) },
            measureNames = Measures.all.associate { it.id to context.getString(measureName(it.id)) },
            stateNames = AbilityState.entries.associateWith { context.getString(stateFor(it)) },
        )

        val provenance = context.getString(
            R.string.summary_provenance,
            date(window.fromDay),
            date(window.toDay),
        )

        val paragraphs: List<String>
        val questions: List<String>
        if (VisitSummaryEngine.enough(inputs)) {
            val brief = VisitSummaryEngine.brief(inputs)
            val verdict = SummaryValidator.check(SummaryWriter.write(brief), brief)
            paragraphs = verdict.kept.map { it.text }
                .ifEmpty { listOf(context.getString(R.string.summary_fallback)) }
            questions = verdict.questions.map { it.text }
        } else {
            paragraphs = listOf(context.getString(R.string.summary_not_yet))
            questions = emptyList()
        }

        return SummaryPage(
            title = context.getString(R.string.summary_title),
            window = provenance,
            paragraphs = paragraphs,
            questionsHeading = context.getString(R.string.summary_questions),
            questions = questions,
            numbersHeading = context.getString(R.string.summary_numbers),
            numbers = numbers(context, inputs),
            notTracked = context.getString(R.string.summary_not_tracked),
            provenance = provenance,
        )
    }

    /** The measures table. LOGIC.md section 13: function first, then the levers. */
    private fun numbers(
        context: Context,
        inputs: com.kamsiob.steadyhealth.engine.VisitInputs,
    ): List<Pair<String, String>> = buildList {
        AbilityDomain.entries.forEach { domain ->
            add(
                inputs.abilityNames[domain].orEmpty() to
                    inputs.stateNames[AbilityEngine.stateOf(domain, inputs.results)].orEmpty(),
            )
        }
        inputs.results.groupBy { it.measureId }.forEach { (id, taken) ->
            val sorted = taken.sortedBy { it.epochDay }
            val name = inputs.measureNames[id] ?: id
            add(
                name to if (sorted.size < 2) {
                    "${sorted.last().value.toInt()}"
                } else {
                    context.getString(
                        R.string.summary_from_to,
                        "${sorted.first().value.toInt()}",
                        "${sorted.last().value.toInt()}",
                    )
                },
            )
        }
        inputs.items.forEach { item ->
            add(
                item.text to context.getString(
                    R.string.summary_from_to,
                    "${item.firstRating}",
                    "${item.lastRating}",
                ),
            )
        }
    }

    private fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))

    private fun nameFor(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_get_up
        AbilityDomain.Go -> R.string.ability_go
        AbilityDomain.Carry -> R.string.ability_carry
        AbilityDomain.Steady -> R.string.ability_steady
    }

    private fun stateFor(state: AbilityState) = when (state) {
        AbilityState.Better -> R.string.state_better
        AbilityState.Same -> R.string.state_same
        AbilityState.Quieter -> R.string.state_quieter
    }

    @Suppress("CyclomaticComplexMethod") // A table of constants, not a decision.
    private fun measureName(id: String) = when (id) {
        Measures.chairStand.id -> R.string.measure_chair_stand_30
        Measures.twoMinuteStep.id -> R.string.measure_two_minute_step
        Measures.wallPushUps.id -> R.string.measure_wall_push_ups
        Measures.bandRows.id -> R.string.measure_band_rows
        Measures.singleLegStance.id -> R.string.measure_single_leg_stance
        Measures.fourStageBalance.id -> R.string.measure_four_stage_balance
        Measures.seatedReach.id -> R.string.measure_seated_reach
        Measures.seatedBalance.id -> R.string.measure_seated_balance
        Measures.wheelingMinutes.id -> R.string.measure_wheeling_two_minute
        Measures.sitToEdge.id -> R.string.measure_sit_to_edge
        Measures.breathHold.id -> R.string.measure_slow_breaths
        Measures.gripHold.id -> R.string.measure_grip_hold
        else -> R.string.measure_ankle_pumps
    }
}
