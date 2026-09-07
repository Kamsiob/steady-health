package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.ui.components.AbilityRow
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TextLink
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** One of the four, as the Abilities tab shows it. */
data class AbilityRowState(
    val domain: AbilityDomain,
    val name: String,
    val lifeSentence: String,
    val state: AbilityState,
)

/** One thing the person said they want, with where they rated it. */
data class TrackedItemState(val text: String, val domain: AbilityDomain, val rating: Int)

/** What the Abilities tab draws. */
data class AbilitiesUiState(
    val abilities: List<AbilityRowState> = emptyList(),
    val items: List<TrackedItemState> = emptyList(),
    val waiting: Boolean = true,

    /**
     * The Sunday write-up, when there is a week worth reading back.
     *
     * It lives on this tab rather than on Today because it is read-back, and
     * read-back is what this tab is for. Today is for today.
     */
    val week: List<String> = emptyList(),
)

/**
 * Abilities, from the grid, screen 8. The tab that replaced History.
 *
 * Four rows, each with its life sentence and its state, then the person's own
 * list underneath. Same is drawn exactly as Better is drawn: same type, same
 * size, same weight, and only the pill colour differs, because holding a number
 * for a year is the work rather than the absence of it.
 */
@Composable
fun AbilitiesScreen(
    state: AbilitiesUiState,
    onAbility: (AbilityDomain) -> Unit,
    onCheck: () -> Unit,
    onSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(title = null, onBack = null, modifier = modifier) {
        SteadyText(
            text = stringResource(R.string.abilities_title),
            style = SteadyType.Greeting,
            color = SteadyPalette.Navy,
        )

        state.abilities.forEach { ability ->
            AbilityRow(
                name = ability.name,
                lifeSentence = ability.lifeSentence,
                tint = tintFor(ability.domain),
                state = ability.state,
                stateLabel = stringResource(labelFor(ability.state)),
                onClick = { onAbility(ability.domain) },
                glyph = { AbilityGlyph(ability.domain) },
            )
        }

        ListItem(
            heading = stringResource(R.string.check_title),
            subtitle = stringResource(R.string.check_intro_lede),
            onClick = onCheck,
        )

        // DESIGN.md puts this here as a text action rather than a card: it is a
        // door, not a thing to look at, and a badge on it would make a page about
        // somebody's body into something that nags them.
        TextLink(
            label = stringResource(R.string.summary_action),
            onClick = onSummary,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.week.isNotEmpty()) {
            SectionTitle(stringResource(R.string.abilities_your_week))
            state.week.forEach { paragraph -> Paragraph(paragraph) }
        }

        if (state.items.isNotEmpty()) {
            SectionTitle(stringResource(R.string.abilities_your_list))
            state.items.forEach { item ->
                ListItem(
                    heading = item.text,
                    subtitle = stringResource(R.string.rating_now, item.rating),
                    tileTint = tintFor(item.domain),
                    glyph = { AbilityGlyph(item.domain) },
                )
            }
        }

        if (state.waiting) {
            Paragraph(stringResource(R.string.abilities_waiting))
        }
    }
}

private fun labelFor(state: AbilityState) = when (state) {
    AbilityState.Better -> R.string.state_better
    AbilityState.Same -> R.string.state_same
    AbilityState.Quieter -> R.string.state_quieter
}
