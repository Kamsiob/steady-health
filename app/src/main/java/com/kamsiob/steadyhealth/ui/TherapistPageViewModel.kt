package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.data.PlanRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.export.TherapistPdf
import com.kamsiob.steadyhealth.plan.BriefInputs
import com.kamsiob.steadyhealth.plan.HowMany
import com.kamsiob.steadyhealth.plan.HowOften
import com.kamsiob.steadyhealth.plan.PlanDone
import com.kamsiob.steadyhealth.plan.PlanItem
import com.kamsiob.steadyhealth.plan.PlannedLine
import com.kamsiob.steadyhealth.plan.SaidHurt
import com.kamsiob.steadyhealth.plan.Sureness
import com.kamsiob.steadyhealth.plan.TherapistBriefs
import com.kamsiob.steadyhealth.plan.TherapistPlan
import com.kamsiob.steadyhealth.session.Movements
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * The one page somebody takes to their next appointment.
 *
 * Its own view model because SteadyViewModel reached detekt's size rule again and
 * this is a whole subject of its own: it reads two halves of the app, joins them, and
 * hands the result to the printer. Nothing else needs any of that.
 */
class TherapistPageViewModel(application: Application) : AndroidViewModel(application) {

    private val db get() = SteadyDatabase.get(getApplication())
    private val plans get() = PlanRepository(db)
    private val runs get() = RunRepository(db)

    /**
     * The one page for the next appointment. ADDENDUM-03 Part 6.
     *
     * Rendered and handed straight to the share sheet, because a file somebody has to
     * go and find is a file that does not reach the appointment.
     *
     * What was done is matched to the plan by movement, which is the only join the two
     * halves have: a session records a movement, and a plan line names one.
     */
    fun share() = viewModelScope.launch {
        val plans = PlanRepository(db)
        val plan = plans.live().firstOrNull() ?: return@launch
        val rows = plans.itemsOf(plan.id)
        val lines = rows.map { row ->
            PlannedLine(
                id = row.id.toString(),
                item = PlanItem(
                    line = row.line,
                    movement = row.movementId?.let(Movements::byId),
                    howMany = HowMany.Unsaid,
                    howOften = HowOften.Unsaid,
                    sureness = Sureness.Named,
                    eachSide = row.eachSide,
                ),
            )
        }
        val byMovement = rows.associate { it.movementId to it.id.toString() }
        val history = runs.history()
        val brief = TherapistBriefs.of(
            BriefInputs(
                plan = TherapistPlan(
                    id = plan.id.toString(),
                    label = plan.label,
                    givenOnDay = plan.createdAt / MILLIS_PER_DAY,
                    reviewDay = plan.reviewDay,
                    lines = lines,
                ),
                today = today(),
                done = history.mapNotNull { row ->
                    byMovement[row.movementId]?.let {
                        PlanDone(
                            lineId = it,
                            epochDay = row.epochDay,
                            result = row.result,
                            target = row.target,
                        )
                    }
                },
                hurt = runs.everSore().map { (area, day) -> SaidHurt(area = area, onDay = day) },
                otherDays = history
                    .filterNot { it.movementId in byMovement.keys }
                    .map { it.epochDay }
                    .distinct(),
            ),
        )
        TherapistPdf.share(getApplication(), brief)
    }

    private fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private companion object {
        /** Milliseconds in a day, for turning a saved timestamp into a day. */
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
