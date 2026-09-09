@file:Suppress("MatchingDeclarationName") // The screen and the state it draws.

package com.kamsiob.steadyhealth.ui.scan

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.plan.HowMany
import com.kamsiob.steadyhealth.plan.HowOften
import com.kamsiob.steadyhealth.plan.PlanItem
import com.kamsiob.steadyhealth.plan.Sureness
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TextEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Confirming a plan. ADDENDUM-03 Part 6.
 *
 * All four ways in end here and nothing is saved unconfirmed, so this screen is the
 * only door. Every line is shown as it was given, with the app's guess under it,
 * because the person is the one who knows what their therapist meant and the app is
 * the one that might have read "STS" as something else.
 *
 * A guess is labelled as a guess. That is not a warning: Part 6 expects the app to
 * propose and the person to confirm, and a screen that hid its uncertainty would be
 * asking them to confirm something they cannot see.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun PlanConfirmScreen(
    state: PlanDraftUiState,
    onLabel: (String) -> Unit,
    onDrop: (Int) -> Unit,
    onReviewDay: (Long?) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.plan_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            if (state.items.isNotEmpty()) {
                PrimaryButton(label = stringResource(R.string.plan_save), onClick = onSave)
            }
        },
    ) {
        Paragraph(stringResource(R.string.plan_intro))
        NoteBlock(stringResource(R.string.plan_theirs))

        SectionTitle(stringResource(R.string.plan_label))
        TextEntry(
            value = state.label,
            onValue = onLabel,
            hint = stringResource(R.string.plan_label_hint),
            imeAction = ImeAction.Done,
        )

        state.items.forEachIndexed { at, item ->
            // Tapping a line takes it out. There is no confirmation, because the
            // whole screen is the confirmation and a line removed here was never
            // saved in the first place.
            ListItem(
                heading = item.line,
                subtitle = said(item),
                value = stringResource(R.string.plan_drop),
                onClick = { onDrop(at) },
            )
        }

        SectionTitle(stringResource(R.string.plan_review))
        Appointment(day = state.reviewDay, onDay = onReviewDay)
    }
}

/**
 * The appointment, and the one row that sets it.
 *
 * It is optional and it stays optional. Nothing on this screen waits for it, the
 * plan saves without it, and a plan with no appointment behaves exactly as a plan
 * with one does apart from the single prompt two days before. Somebody who does not
 * know when they are next going in should be able to walk past this row.
 *
 * The platform date picker rather than one drawn here. It is the calendar every
 * other app on the phone shows, it is already translated into every language this
 * app will ship in, it already reads correctly under TalkBack, and it already
 * handles the font scale. A prettier one written here would be four of those things
 * done again and one of them done worse.
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

    // Only once there is one to clear. A row offering to unset nothing is a row
    // somebody has to read past.
    if (day != null) {
        ListItem(
            heading = stringResource(R.string.plan_review_clear),
            onClick = { onDay(null) },
        )
    }
}

/** What the app made of one line, said in one sentence under it. */
@Composable
private fun said(item: PlanItem): String {
    val name = item.movement?.name ?: stringResource(R.string.plan_unmatched)
    val many = manySaid(item.howMany)
    val often = oftenSaid(item.howOften)
    val note = when (item.sureness) {
        Sureness.Named -> null
        Sureness.Likely -> stringResource(R.string.plan_guessed)
        Sureness.MoreThanOne -> stringResource(R.string.plan_more_than_one)
        Sureness.Unmatched -> stringResource(R.string.plan_unmatched)
    }
    val side = stringResource(R.string.plan_each_side).takeIf { item.eachSide }
    return listOfNotNull(name, many, often, side, note).joinToString(" · ")
}

@Composable
private fun manySaid(many: HowMany): String? = when (many) {
    HowMany.Unsaid -> stringResource(R.string.plan_no_number)
    is HowMany.Reps ->
        if (many.sets > 1) {
            stringResource(R.string.plan_sets, many.sets, many.reps)
        } else {
            stringResource(R.string.plan_reps, many.reps)
        }

    is HowMany.Hold -> stringResource(R.string.plan_hold, many.seconds)
    is HowMany.Minutes -> stringResource(R.string.plan_minutes, many.minutes)
}

@Composable
private fun oftenSaid(often: HowOften): String? = when (often) {
    HowOften.Unsaid -> null
    is HowOften.ADay -> stringResource(R.string.plan_a_day, often.times)
    is HowOften.AWeek -> stringResource(R.string.plan_a_week, often.times)
    HowOften.EveryOtherDay -> stringResource(R.string.plan_other_day)
}
