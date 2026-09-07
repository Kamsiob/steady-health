@file:Suppress("MatchingDeclarationName") // The question and the answer, one subject.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TextEntry
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** One card, as the list shows it. */
data class CardRow(val id: String, val title: String, val summary: String)

/** What Ask a question draws. */
data class AskUiState(
    val question: String = "",
    val matches: List<CardRow> = emptyList(),
    /** Every card, shown when nothing has been asked yet. */
    val all: List<CardRow> = emptyList(),
    /** True when a question was asked and nothing fitted. */
    val nothingFits: Boolean = false,
)

/**
 * Ask a question.
 *
 * Not in the grid, so built from DESIGN.md section 8: a form, so no hero, and
 * the same list rows as everywhere else. The list is visible before anything is
 * typed, because these cards are worth reading whether or not somebody arrived
 * with a question, and because a search box in front of hidden content is a
 * guessing game.
 *
 * Nothing here is generated. Every card is hand-written and every one names its
 * source, and when nothing fits the app says so rather than showing the closest
 * thing and hoping.
 */
@Composable
fun AskScreen(
    state: AskUiState,
    onQuestion: (String) -> Unit,
    onCard: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.ask_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        TextEntry(
            value = state.question,
            onValue = onQuestion,
            hint = stringResource(R.string.ask_placeholder),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.nothingFits) {
            NoteBlock(stringResource(R.string.ask_nothing_fits))
        }

        val rows = state.matches.ifEmpty { state.all }
        SectionTitle(
            stringResource(
                when {
                    state.matches.isNotEmpty() -> R.string.ask_closest
                    state.nothingFits -> R.string.ask_closest
                    else -> R.string.ask_everything
                },
            ),
        )

        rows.forEach { row ->
            ListItem(
                heading = row.title,
                subtitle = row.summary,
                onClick = { onCard(row.id) },
            )
        }
    }
}

/** What one card draws. */
data class CardUiState(
    val title: String = "",
    val body: String = "",
    val source: String = "",
)

/**
 * One card.
 *
 * The body first, then the source in smaller words at the bottom. CONTENT.md puts
 * a source on every card so the person can go and check, and a source that is
 * hidden behind a link is a source nobody checks.
 */
@Composable
fun CardScreen(
    state: CardUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(title = state.title, onBack = onBack, modifier = modifier) {
        state.body.split("\n\n").filter { it.isNotBlank() }.forEach { paragraph ->
            Paragraph(paragraph)
        }

        Spacer(Modifier.height(SteadySpacing.Inside))

        SteadyText(
            text = stringResource(R.string.card_source, state.source),
            style = SteadyType.Caption,
            color = SteadyPalette.Ink3Text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
