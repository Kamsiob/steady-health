@file:Suppress("MatchingDeclarationName") // The end of a session, and what it asks.

package com.kamsiob.steadyhealth.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.session.Area
import com.kamsiob.steadyhealth.session.Felt
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.Stepper
import com.kamsiob.steadyhealth.ui.components.ThreeUpChoice
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** One line of what was done, and the number behind it, which is editable. */
data class DoneRow(
    val movementId: String,
    val name: String,
    val value: String,
    val count: Int,
    val skipped: Boolean,
    val selfReported: Boolean = false,
)

/** What the done screen draws. */
data class DoneUiState(
    val rows: List<DoneRow> = emptyList(),
    val felt: Felt? = null,
    /** The next session, already adjusted by the answer above. */
    val nextTime: String = "",
    /** True only after the very first session. DESIGN.md 6, warmth in four places. */
    val first: Boolean = false,
    /** True when an hour went by mid session and it was saved where it stood. */
    val awayTooLong: Boolean = false,
)

/**
 * S7. Done.
 *
 * What was done, each number against its last, then one question. Then the next
 * session on the same screen, already adjusted by the answer, because the whole point
 * of asking is that the answer changes something and the person should see it.
 *
 * Nothing here evaluates the session. "Done. That counts." is the same sentence
 * whether somebody did everything or stopped after one movement.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun SessionDoneScreen(
    state: DoneUiState,
    onFelt: (Felt) -> Unit,
    onCorrect: (String, Int) -> Unit,
    onSave: () -> Unit,
    onChangeNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = null,
        onBack = null,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.done_save), onClick = onSave)
            if (state.felt != null) {
                SecondaryButton(
                    label = stringResource(R.string.done_change_next),
                    onClick = onChangeNext,
                )
            }
        },
    ) {
        SteadyText(
            text = stringResource(
                if (state.first) R.string.done_first else R.string.done_title,
            ),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )

        if (state.awayTooLong) NoteBlock(stringResource(R.string.session_hour_away))

        // ADDENDUM-03 Part 14: any counted number is editable here, with plus and
        // minus, and the app says nothing at all about having been corrected.
        state.rows.forEach { row ->
            if (row.skipped) {
                ListItem(heading = row.name, value = stringResource(R.string.done_skipped))
            } else {
                Stepper(
                    label = row.name,
                    value = row.value,
                    supporting = null,
                    onDown = { onCorrect(row.movementId, row.count - 1) },
                    onUp = { onCorrect(row.movementId, row.count + 1) },
                )
            }
        }

        if (state.rows.any { it.selfReported && !it.skipped }) {
            SteadyText(
                text = stringResource(
                    R.string.done_your_own,
                    state.rows.filter { it.selfReported && !it.skipped }
                        .joinToString(", ") { it.name.lowercase() },
                ),
                style = SteadyType.Caption,
                color = SteadyPalette.Ink3Text,
            )
        }

        SectionTitle(stringResource(R.string.done_how))
        ThreeUpChoice(
            options = listOf(
                stringResource(R.string.felt_easy),
                stringResource(R.string.felt_about_right),
                stringResource(R.string.felt_hard),
            ),
            selectedIndex = state.felt?.ordinal,
            onSelect = { onFelt(Felt.entries[it]) },
            selectedFill = SteadyPalette.GreenL,
            selectedOutline = SteadyPalette.Green,
        )

        if (state.nextTime.isNotBlank()) {
            SectionTitle(stringResource(R.string.done_next_time))
            Paragraph(state.nextTime)
        }
    }
}

/**
 * Where it hurts, asked after the session has already stopped.
 *
 * One press stopped everything; this is the second screen, not a confirmation. There
 * is an "I'd rather not say", because somebody in pain does not owe the app a
 * location.
 */
@Composable
fun HurtScreen(
    onArea: (Area?) -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(title = null, onBack = null, modifier = modifier) {
        SteadyText(
            text = stringResource(R.string.hurt_where),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )
        NoteBlock(stringResource(R.string.hurt_why))

        listOf(
            Area.Shoulder to R.string.hurt_shoulder,
            Area.Arm to R.string.hurt_arm,
            Area.Back to R.string.hurt_back,
            Area.Hip to R.string.hurt_hip,
            Area.Knee to R.string.hurt_knee,
            Area.Ankle to R.string.hurt_ankle,
            Area.Neck to R.string.hurt_neck,
        ).forEach { (area, label) ->
            ListItem(heading = stringResource(label), onClick = { onArea(area) })
        }

        ListItem(
            heading = stringResource(R.string.hurt_rather_not),
            onClick = { onArea(null) },
        )
    }
}
