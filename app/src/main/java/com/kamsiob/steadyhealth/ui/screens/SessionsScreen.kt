package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SessionCard
import com.kamsiob.steadyhealth.ui.components.SessionCardState
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.help.Place
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** One movement, as the library lists it. */
data class LibraryRow(
    val id: String,
    val name: String,
    val feeds: String,
    val needs: String,
    val leftOut: Boolean,
)

/** One session already done, as the history lists it. */
data class HistoryRow(
    val runId: Long,
    val whenIt: String,
    val what: String,
    val how: String?,
)

/** What the Sessions tab draws. */
data class SessionsUiState(
    val session: SessionCardState? = null,
    val library: List<LibraryRow> = emptyList(),
    val history: List<HistoryRow> = emptyList(),
)

/**
 * Sessions. MASTER_SPEC 5, from ADDENDUM-03 Part 20.
 *
 * Everything you can do and everything you have done, with today's session at the top
 * so the one thing somebody came here to start is not below a list of sixty things.
 *
 * The library shows what a movement needs before it shows anything else, because the
 * first question anybody has about a movement is whether they can do it in the room
 * they are standing in.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun SessionsScreen(
    state: SessionsUiState,
    onGo: () -> Unit,
    onSomethingSmall: () -> Unit,
    onWithoutThePhone: () -> Unit,
    onMovement: (String) -> Unit,
    onRepeat: (Long) -> Unit,
    onLogPast: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(title = null, onBack = null, modifier = modifier, help = Place.Sessions) {
        SteadyText(
            text = stringResource(R.string.tab_sessions),
            style = SteadyType.Greeting,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )

        state.session?.let {
            SessionCard(
                state = it,
                onGo = onGo,
                onSomethingSmall = onSomethingSmall,
                onWithoutThePhone = onWithoutThePhone,
            )
        }

        SectionTitle(stringResource(R.string.sessions_history))
        if (state.history.isEmpty()) {
            NoteBlock(stringResource(R.string.sessions_history_empty))
        } else {
            state.history.forEach { row ->
                ListItem(
                    heading = row.whenIt,
                    subtitle = listOfNotNull(row.what, row.how).joinToString(" · "),
                    value = stringResource(R.string.sessions_again),
                    onClick = { onRepeat(row.runId) },
                )
            }
        }

        // ADDENDUM-03 Part 5: one button for every piece of paper, in Sessions and in
        // You. Somebody standing in a hallway holding a sheet should not have to
        // decide what kind of sheet it is before the app will look at it.
        ListItem(
            heading = stringResource(R.string.scan_something),
            subtitle = stringResource(R.string.scan_sub),
            onClick = onScan,
        )

        ListItem(
            heading = stringResource(R.string.sessions_log_past),
            subtitle = stringResource(R.string.sessions_log_sub),
            onClick = onLogPast,
        )

        SectionTitle(stringResource(R.string.sessions_library))
        Paragraph(stringResource(R.string.sessions_library_sub))
        state.library.forEach { row ->
            ListItem(
                heading = row.name,
                subtitle = "${row.feeds} · ${row.needs}",
                value = if (row.leftOut) stringResource(R.string.library_left_out) else null,
                onClick = { onMovement(row.id) },
            )
        }
    }
}

/** "One movement", "three movements". Used by the view model, kept beside the row. */
@Composable
fun movementsSaid(count: Int): String =
    pluralStringResource(R.plurals.history_movements, count, count)
