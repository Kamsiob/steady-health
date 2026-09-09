@file:Suppress("MatchingDeclarationName") // The screen and the state it draws.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.places.PlaceQuestion
import com.kamsiob.steadyhealth.places.Places
import com.kamsiob.steadyhealth.places.Said
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.ThreeUpChoice

/** What the walkthrough has been told so far. */
data class PlacesUiState(
    val answers: Map<String, Said> = emptyMap(),
    /** True once every question has an answer and the last one has been passed. */
    val finished: Boolean = false,
)

/**
 * The places you move through. ADDENDUM-03 Part 8 item 2.
 *
 * Every question on one screen rather than one at a time. A walkthrough of six
 * questions on six screens is six chances to put the phone down, and the questions
 * are about six different rooms, so there is no order in which they have to be read.
 * Somebody who only wants to answer the one about the stairs can answer it and leave.
 *
 * The fix appears under the question the moment it is wanted, rather than being saved
 * up for the end. It is the only thing this feature is for, and a screen that holds it
 * back until all six are answered is a screen that asks for six answers before it
 * gives anything.
 *
 * Nothing counts. There is no progress line, no total, and no summary sentence about
 * how many were sorted. Part 8 is explicit that this is not a score and not a safety
 * assessment, and the surest way to keep it from becoming one is to have nowhere for
 * a number to go.
 */
@Composable
fun PlacesScreen(
    state: PlacesUiState,
    onSaid: (String, Said) -> Unit,
    onStartAgain: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.places_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            if (Places.finished(state.answers)) {
                PrimaryButton(label = stringResource(R.string.places_done_action), onClick = onBack)
            }
        },
    ) {
        Paragraph(stringResource(R.string.places_intro))

        Places.all.forEach { question ->
            Question(
                question = question,
                said = state.answers[question.id],
                onSaid = { onSaid(question.id, it) },
            )
        }

        if (Places.finished(state.answers)) {
            NoteBlock(stringResource(R.string.places_repeat))
            SecondaryButton(
                label = stringResource(R.string.places_again),
                onClick = onStartAgain,
            )
        }
    }
}

/**
 * One question, its three answers, and the fix under two of them.
 *
 * "Not sure" shows the fix as well as "not yet" does. Somebody who cannot picture
 * their own landing at night is exactly the person the sentence is for, and reading it
 * costs nothing.
 */
@Composable
private fun Question(question: PlaceQuestion, said: Said?, onSaid: (Said) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(question.where))
        Paragraph(stringResource(question.question))
        ThreeUpChoice(
            options = CHOICES.map { stringResource(it.second) },
            selectedIndex = CHOICES.indexOfFirst { it.first == said }.takeIf { it >= 0 },
            onSelect = { onSaid(CHOICES[it].first) },
        )
        if (said?.wantsTheFix == true) {
            NoteBlock(stringResource(question.fix))
        }
    }
}

/** The three answers, in the order they are offered. */
private val CHOICES = listOf(
    Said.Sorted to R.string.places_sorted,
    Said.NotYet to R.string.places_not_yet,
    Said.NotSure to R.string.places_not_sure,
)
