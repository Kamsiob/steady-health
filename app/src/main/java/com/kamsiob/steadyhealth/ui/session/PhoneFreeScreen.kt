package com.kamsiob.steadyhealth.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.session.Counted
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.Stepper
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** "Asked for 8", "Asked for 30 seconds", "Asked for 2 minutes". */
@Composable
private fun asked(row: PhoneFreeRow): String = when (row.counted) {
    Counted.Hold -> pluralStringResource(R.plurals.no_phone_asked_seconds, row.asked, row.asked)
    Counted.Minutes -> pluralStringResource(R.plurals.no_phone_asked_minutes, row.asked, row.asked)
    else -> stringResource(R.string.no_phone_asked, row.asked)
}

/** One movement of a session somebody is doing away from the phone. */
data class PhoneFreeRow(
    val movementId: String,
    val name: String,
    val setup: String,
    val stopRule: String,
    val asked: Int,
    val managed: Int,
    /** Repetitions, seconds or minutes. The screen has to say which. */
    val counted: Counted = Counted.Reps,
)

/** What the phone-free screen draws. */
data class PhoneFreeUiState(
    val rows: List<PhoneFreeRow> = emptyList(),
    val reading: Boolean = false,
    val logging: Boolean = false,
)

/**
 * A session done away from the phone. ADDENDUM-03 Part 14.
 *
 * The whole session written out and, if wanted, read out, then the phone goes down.
 * Afterwards one screen of numbers with plus and minus, and what comes back is
 * recorded as a session like any other.
 *
 * There is no timer and no counting here on purpose. Somebody who wanted the app to
 * lead them would have pressed Start; this is for the person who does not.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun PhoneFreeScreen(
    state: PhoneFreeUiState,
    onRead: () -> Unit,
    onDidIt: () -> Unit,
    onManaged: (String, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.no_phone_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            if (state.logging) {
                PrimaryButton(label = stringResource(R.string.no_phone_save), onClick = onSave)
            } else {
                PrimaryButton(label = stringResource(R.string.no_phone_did_it), onClick = onDidIt)
                SecondaryButton(
                    label = stringResource(
                        if (state.reading) R.string.no_phone_stop else R.string.no_phone_read,
                    ),
                    onClick = onRead,
                )
            }
        },
    ) {
        if (state.logging) {
            val timed = state.rows.any { it.counted != Counted.Reps && it.counted != Counted.Taps }
            SteadyText(
                text = stringResource(
                    if (timed) R.string.no_phone_how_long else R.string.no_phone_how_many,
                ),
                style = SteadyType.ScreenTitleBig,
                color = SteadyPalette.Navy,
                modifier = Modifier.semantics { heading() },
            )
            state.rows.forEach { row ->
                Stepper(
                    label = row.name,
                    value = row.managed.toString(),
                    supporting = asked(row),
                    onDown = { onManaged(row.movementId, row.managed - 1) },
                    onUp = { onManaged(row.movementId, row.managed + 1) },
                )
            }
            return@SteadyScreen
        }

        Paragraph(stringResource(R.string.no_phone_intro))
        state.rows.forEach { row ->
            SectionTitle(row.name)
            Paragraph(row.setup)
            Paragraph(asked(row))
            NoteBlock(row.stopRule)
        }
    }
}
