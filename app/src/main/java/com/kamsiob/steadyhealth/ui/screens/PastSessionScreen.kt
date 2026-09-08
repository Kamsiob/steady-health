package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.Stepper

/** One movement of a past session, as it can be corrected. */
data class PastMovement(val movementId: String, val name: String, val count: Int)

/** What the past session screen draws. */
data class PastSessionUiState(
    val runId: Long = 0,
    val whenIt: String = "",
    val how: String? = null,
    val movements: List<PastMovement> = emptyList(),
)

/**
 * One session already done. ADDENDUM-03 Part 14.
 *
 * The numbers are editable and the session is removable, and the app says nothing
 * about either. Somebody who counted wrong on Tuesday should be able to fix it on
 * Thursday without being asked why.
 *
 * "Do this again" is here as well, because the most likely reason to open a past
 * session is wanting today to be like it.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun PastSessionScreen(
    state: PastSessionUiState,
    onCount: (String, Int) -> Unit,
    onRepeat: () -> Unit,
    onRemove: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.past_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.sessions_again), onClick = onRepeat)
            SecondaryButton(label = stringResource(R.string.past_remove), onClick = onRemove)
        },
    ) {
        SectionTitle(state.whenIt)
        state.how?.let { Paragraph(it) }

        state.movements.forEach { movement ->
            Stepper(
                label = movement.name,
                value = movement.count.toString(),
                supporting = null,
                onDown = { onCount(movement.movementId, movement.count - 1) },
                onUp = { onCount(movement.movementId, movement.count + 1) },
            )
        }
    }
}
