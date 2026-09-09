@file:Suppress("MatchingDeclarationName") // The screen and the state it draws.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen

/** What the Sunday review draws. */
data class SundayUiState(
    val did: List<String> = emptyList(),
    val moved: String? = null,
    val noticed: String? = null,
    val ahead: String = "",
    /** The line a week card would start from, blank on a week with nothing in it. */
    val cardLine: String = "",
    /**
     * Whether to offer the card here at all. ADDENDUM-03 Part 11.
     *
     * "Offered once at the second Sunday review. Declining hides it until Progress."
     * Declining is not a button: it is scrolling past, which is what most people will
     * do, and the offer does not come back here either way.
     */
    val offerCard: Boolean = false,
)

/**
 * The Sunday review. ADDENDUM-03 Part 10.
 *
 * One screen, opened from Today and never notified, because a notification about
 * reading something is the app asking for attention rather than giving any.
 *
 * It is the only place in the app that looks forward, and it does it with one
 * concrete thing rather than a plan: "Thursday would make four." Everything else here
 * already happened.
 */
@Composable
fun SundayScreen(
    state: SundayUiState,
    onCard: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.sunday_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        SectionTitle(stringResource(R.string.sunday_did))
        state.did.forEach { Paragraph(it) }

        SectionTitle(stringResource(R.string.sunday_moved))
        Paragraph(state.moved ?: stringResource(R.string.sunday_nothing_moved))

        state.noticed?.let {
            SectionTitle(stringResource(R.string.sunday_noticed))
            Paragraph(it)
        }

        SectionTitle(stringResource(R.string.sunday_ahead))
        NoteBlock(state.ahead)

        if (state.offerCard && state.cardLine.isNotBlank()) {
            ListItem(
                heading = stringResource(R.string.card_offer),
                subtitle = stringResource(R.string.card_offer_sub),
                onClick = { onCard(state.cardLine) },
            )
        }
    }
}
