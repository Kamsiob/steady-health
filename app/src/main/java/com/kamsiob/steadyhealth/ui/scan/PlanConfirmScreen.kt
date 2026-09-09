@file:Suppress("MatchingDeclarationName") // The screen and the state it draws.

package com.kamsiob.steadyhealth.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
