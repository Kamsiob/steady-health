package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TagPill
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing

/** One of the last seven days, as the screen offers it. */
data class PastDay(val epochDay: Long, val label: String)

/** What the log screen draws. */
data class LogPastUiState(
    val days: List<PastDay> = emptyList(),
    val chosenDay: Long? = null,
    val movements: List<LibraryRow> = emptyList(),
    val chosen: Set<String> = emptySet(),
)

/**
 * A session that already happened. ADDENDUM-03 Part 14.
 *
 * Three taps: the day, the movements, add it. No numbers are asked for, because
 * somebody logging Tuesday's walk on Thursday does not remember how many, and asking
 * would turn three taps into an interrogation. What is recorded is that it happened,
 * which is the part the week and the history care about.
 */
@Composable
fun LogPastScreen(
    state: LogPastUiState,
    onDay: (Long) -> Unit,
    onMovement: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.log_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            if (state.chosenDay != null && state.chosen.isNotEmpty()) {
                PrimaryButton(label = stringResource(R.string.log_save), onClick = onSave)
            }
        },
    ) {
        SectionTitle(stringResource(R.string.log_when))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        ) {
            state.days.forEach { day ->
                TagPill(
                    label = day.label,
                    selected = state.chosenDay == day.epochDay,
                    onClick = { onDay(day.epochDay) },
                )
            }
        }

        SectionTitle(stringResource(R.string.log_what))
        state.movements.forEach { row ->
            ListItem(
                heading = row.name,
                subtitle = row.feeds,
                next = row.id in state.chosen,
                onClick = { onMovement(row.id) },
            )
        }

        if (state.chosen.isEmpty()) NoteBlock(stringResource(R.string.log_none_picked))
    }
}
