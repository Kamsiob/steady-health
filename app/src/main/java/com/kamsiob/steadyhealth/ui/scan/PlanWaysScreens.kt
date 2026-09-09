package com.kamsiob.steadyhealth.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.ui.Labels
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TextEntry
import com.kamsiob.steadyhealth.ui.screens.movementsSaid

/**
 * The four ways in, offered. ADDENDUM-03 Part 6.
 *
 * Part 6 names four ways to get a therapist's plan into the app and this is where a
 * person chooses between them. It sits in front of the camera rather than beside it
 * because the camera used to be the only door, and a person who cannot photograph
 * their sheet, or never had one on paper, had no way in at all.
 *
 * The camera is first for two reasons. It is the one most people will reach for, and
 * it is also the only one of the four that is not about a plan: ADDENDUM-03 Part 5
 * puts a letter, an appointment card and anything else printed down the same path,
 * and this screen is the one entry point to it. The line under the title says so,
 * because a screen offering a therapist's plan three ways and any piece of paper once
 * would otherwise be quietly asking somebody with a letter to guess.
 */
@Composable
fun PlanWaysScreen(
    onCamera: () -> Unit,
    onSay: () -> Unit,
    onPick: () -> Unit,
    onType: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.ways_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        Paragraph(stringResource(R.string.ways_intro))

        ListItem(
            heading = stringResource(R.string.ways_camera),
            subtitle = stringResource(R.string.ways_camera_sub),
            onClick = onCamera,
        )
        ListItem(
            heading = stringResource(R.string.ways_say),
            subtitle = stringResource(R.string.ways_say_sub),
            onClick = onSay,
        )
        ListItem(
            heading = stringResource(R.string.ways_pick),
            subtitle = stringResource(R.string.ways_pick_sub),
            onClick = onPick,
        )
        ListItem(
            heading = stringResource(R.string.ways_type),
            subtitle = stringResource(R.string.ways_type_sub),
            onClick = onType,
        )
    }
}

/**
 * Typing the plan out. ADDENDUM-03 Part 6.
 *
 * The plainest of the four and the one the other two fall back to, so it asks for
 * nothing: no camera, no microphone, no permission of any kind. One movement to a
 * line is what the guidance asks for because it is what a sheet looks like, and the
 * reader behind this takes a run on sentence as well, so somebody who ignores that
 * and types the lot in one go still gets sensible items back.
 *
 * The box is several lines deep before anything is typed. A field one line tall asks
 * for one line whatever the sentence above it says.
 */
@Composable
fun PlanTypeScreen(
    state: PlanWaysUiState,
    onTyped: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.type_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            // A letter and not just a character. Punctuation on its own reads as
            // nothing at all further down, and a Continue that leads to an empty
            // confirmation screen looks like the app breaking.
            if (state.typed.any(Char::isLetter)) {
                PrimaryButton(
                    label = stringResource(R.string.continue_label),
                    onClick = onContinue,
                )
            }
        },
    ) {
        Paragraph(stringResource(R.string.type_how))
        TextEntry(
            value = state.typed,
            onValue = onTyped,
            hint = stringResource(R.string.type_hint),
            singleLine = false,
            // The return key has to make a line, not finish the screen, because a line
            // is the unit this box is asking for.
            imeAction = ImeAction.Default,
            minHeight = TYPING_BOX,
        )
    }
}

/**
 * Picking the plan off the library. ADDENDUM-03 Part 6.
 *
 * The same rows the Sessions tab lists, under the same four ability headings the
 * library is written in, so a movement is called one thing everywhere in the app.
 * Warm ups and cool downs are here as well, which the Sessions tab leaves out: a
 * sheet can ask for shoulder rolls or calf stretches, and a list that could not offer
 * them would send that person back to typing.
 *
 * A movement left out for now is shown and can still be picked. Part 6 is firm that a
 * plan movement clashing with something the person avoids is flagged rather than
 * dropped, and hiding it here would be dropping it before it was ever a plan.
 */
@Composable
fun PlanPickScreen(
    state: PlanWaysUiState,
    onMovement: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.pick_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            if (state.picked.isNotEmpty()) {
                PrimaryButton(
                    label = stringResource(R.string.continue_label),
                    onClick = onContinue,
                )
            }
        },
    ) {
        Paragraph(stringResource(R.string.pick_how))
        NoteBlock(
            if (state.picked.isEmpty()) {
                stringResource(R.string.log_none_picked)
            } else {
                movementsSaid(state.picked.size)
            },
        )

        AbilityDomain.entries.forEach { domain ->
            val rows = state.library.filter { it.domain == domain }
            if (rows.isNotEmpty()) {
                SectionTitle(stringResource(Labels.forAbility(domain)))
                rows.forEach { row ->
                    ListItem(
                        heading = row.name,
                        // What it needs, not what it feeds: the heading above already
                        // said what it feeds, and the room somebody is standing in is
                        // the question they actually have.
                        subtitle = row.needs,
                        value = if (row.leftOut) {
                            stringResource(R.string.library_left_out)
                        } else {
                            null
                        },
                        next = row.id in state.picked,
                        onClick = { onMovement(row.id) },
                    )
                }
            }
        }
    }
}

/** Deep enough to show that more than one line is wanted, before anything is typed. */
private val TYPING_BOX = 140.dp
