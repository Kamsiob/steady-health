@file:Suppress("MatchingDeclarationName") // One screen, its state, and the month it draws.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.Block
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TwoUp
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * One month, already worded.
 *
 * [moved] is one entry per day of that month, in order, and nothing else. It is the
 * whole picture: which days had something in them and which did not.
 *
 * [daysMoved] and [minutes] are blank with numbers off, and the picture is then the
 * only thing on the screen. A direction word in their place would have to be a
 * direction against the month before, which is the one thing this screen is not
 * allowed to say.
 */
data class MonthPicture(
    val name: String,
    val moved: List<Boolean>,
    val daysMoved: String = "",
    val minutes: String = "",
    val spoken: String = "",
)

/** Every month with something in it, oldest first, and which one is showing. */
data class MonthsUiState(
    val months: List<MonthPicture> = emptyList(),
    val at: Int = 0,
)

/**
 * The months. ADDENDUM-03 Part 20, drawn from the v1 grid's screen 15.
 *
 * One month at a time, as a picture: a dot for every day of it, filled on the days
 * something was done. It answers the one question nothing else in the app answers,
 * which is what a month that is not the last four weeks looked like. The four week
 * view is a fixed window on the present and the look back card is one sentence.
 *
 * Nothing joins one month to the next. There is no line across them, no total over
 * them, and no sentence comparing this one to the one before. Moving between them is
 * navigation and not a comparison: two buttons that change which month is on the
 * screen, and nothing that carries anything from one to the other.
 *
 * There is deliberately no empty state dressed up as a month. A month with nothing
 * in it is not in the list at all, so the buttons never land on a blank grid and
 * nobody is shown a page of empty circles with their own name on it.
 */
@Composable
fun MonthsScreen(
    state: MonthsUiState,
    onEarlier: () -> Unit,
    onLater: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val month = state.months.getOrNull(state.at)
    SteadyScreen(
        title = month?.name ?: stringResource(R.string.months_row),
        onBack = onBack,
        modifier = modifier,
    ) {
        if (month == null) {
            NoteBlock(stringResource(R.string.months_none))
            return@SteadyScreen
        }

        MonthGrid(month)

        if (month.daysMoved.isNotBlank()) {
            TwoUp {
                Block(
                    label = stringResource(R.string.months_days),
                    value = month.daysMoved,
                    modifier = Modifier.weight(1f),
                )
                Block(
                    label = stringResource(R.string.months_minutes),
                    value = month.minutes,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
            if (state.at > 0) {
                SecondaryButton(
                    label = stringResource(R.string.months_earlier),
                    onClick = onEarlier,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.at < state.months.lastIndex) {
                SecondaryButton(
                    label = stringResource(R.string.months_later),
                    onClick = onLater,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Paragraph(stringResource(R.string.months_why))
    }
}

/**
 * The month itself: a dot a day, seven across.
 *
 * Seven across so the columns are the days of the week, which is how somebody reads
 * their own fortnight without being told anything. The first row is short when the
 * month does not begin on the same weekday the rows do; that is honest, and drawing
 * a leading run of blanks to line it up with a calendar would put empty circles on
 * the screen that stand for days in a different month.
 *
 * One description for the whole grid rather than one a dot, because thirty-one
 * announcements is not a picture and is not readable at any speed.
 */
@Composable
private fun MonthGrid(month: MonthPicture, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = month.spoken },
        verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
    ) {
        SteadyText(
            text = month.name,
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
        )
        month.moved.chunked(ACROSS).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
                week.forEach { moved ->
                    Box(
                        modifier = Modifier
                            .size(DOT)
                            .clip(CircleShape)
                            .background(if (moved) SteadyPalette.Green else SteadyPalette.Sand),
                    )
                }
            }
        }
    }
}

/** Seven a row, so a column is a day of the week. */
private const val ACROSS = 7

/** Big enough to see across a room and small enough that five rows fit. */
private val DOT = 28.dp
