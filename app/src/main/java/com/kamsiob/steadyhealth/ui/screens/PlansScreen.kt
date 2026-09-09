@file:Suppress("MatchingDeclarationName") // One screen and the state it draws.

package com.kamsiob.steadyhealth.ui.screens

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One plan somebody has, with whose it is, how big it is, and when they next go in. */
data class PlanRow(
    val id: Long,
    val label: String,
    val howMany: String,
    val reviewDay: Long?,
)

/** Every live plan, and the export that only means anything once there is one. */
data class PlansUiState(val plans: List<PlanRow> = emptyList())

/**
 * Your therapist, and when you next see them. ADDENDUM-03 Parts 6 and 20.
 *
 * Every live plan, each under its own name, because Part 6 says a physio's plan and
 * an OT's plan coexist and stay separate. A screen that showed the first one and
 * called it "your plan" would be the app deciding which therapist counts.
 *
 * Each plan carries its own date. Two plans reviewed at the same appointment happen
 * to have the same date, and that is a coincidence about the diary rather than
 * something the app should join up.
 *
 * Nothing here changes what is inside a plan. The lines are the therapist's and the
 * app does not offer to edit them, only to say when the next appointment is and to
 * put a plan away once it is over.
 */
@Composable
fun PlansScreen(
    state: PlansUiState,
    onReviewDay: (Long, Long?) -> Unit,
    onArchive: (Long) -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.plans_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        if (state.plans.isEmpty()) {
            NoteBlock(stringResource(R.string.plans_none))
            return@SteadyScreen
        }

        Paragraph(stringResource(R.string.plans_why))

        state.plans.forEach { plan ->
            SectionTitle(plan.label, aside = plan.howMany)
            Appointment(
                day = plan.reviewDay,
                onDay = { onReviewDay(plan.id, it) },
            )
            ListItem(
                heading = stringResource(R.string.plans_away),
                subtitle = stringResource(R.string.plans_away_why),
                onClick = { onArchive(plan.id) },
            )
        }

        // Under every plan rather than beside one of them: the page is about all of
        // them at once, which is what somebody carries into one appointment.
        ListItem(heading = stringResource(R.string.plan_export), onClick = onExport)
    }
}

/**
 * The appointment, and the one row that sets it.
 *
 * The same row and the same platform date picker as the screen that confirms a new
 * plan, for the reason that screen gives: it is the calendar every other app on the
 * phone shows, already translated, already right under TalkBack and at any font
 * scale. Asked here in the same words so that changing a date afterwards is
 * recognisably the same act as setting it.
 */
@Composable
private fun Appointment(day: Long?, onDay: (Long?) -> Unit) {
    val context = LocalContext.current
    val date = day?.let(LocalDate::ofEpochDay)
    val shown = date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    val action = if (day == null) R.string.plan_review_set else R.string.plan_review_change

    ListItem(
        heading = shown ?: stringResource(R.string.plan_review_none),
        subtitle = stringResource(R.string.plan_review_sub),
        value = stringResource(action),
        onClick = {
            val start = date ?: LocalDate.now()
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDay(LocalDate.of(year, month + 1, dayOfMonth).toEpochDay())
                },
                start.year,
                start.monthValue - 1,
                start.dayOfMonth,
            ).show()
        },
    )

    if (day != null) {
        ListItem(
            heading = stringResource(R.string.plan_review_clear),
            onClick = { onDay(null) },
        )
    }
}
