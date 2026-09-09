package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.ui.components.AbilityRow
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TextLink
import com.kamsiob.steadyhealth.ui.help.Place
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
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
data class TrackedItemState(
    val text: String,
    val domain: AbilityDomain,
    val rating: Int,
    /** The rating as it is shown: a number, or a word when numbers are off. */
    val said: String = "",
)

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

    /** The last four weeks, oldest first, each against the number chosen. Part 10. */
    val weeks: List<WeekBar> = emptyList(),

    /** One sentence about those four weeks, and never about a run of them. */
    val weeksSaid: String = "",

    /** The same thing then and now, in their own history. Part 9. */
    val lookBack: String? = null,

    /**
     * The first month, then and now, for one thing they named. Part 16.
     *
     * One of the four places this app is warm, and it is here for a fortnight after
     * the first month and never again.
     */
    val firstMonth: FirstMonthCard? = null,

    /**
     * What the monthly check needs, in the words of this version of the app.
     *
     * Worded by the view model, which knows how the person gets around. The row used
     * to promise everybody a chair and a wall, which are two things the wheelchair
     * check and the bed check never ask for.
     */
    val checkLede: String = "",
)

/** The first month card, already worded. Part 16, the second warm place. */
data class FirstMonthCard(val heading: String, val line: String)

/**
 * One week, as a bar. Nothing here joins it to the week beside it.
 *
 * [label] is what is printed under the bar, which is the count with numbers on and
 * nothing at all with them off. The bar keeps its real height either way, because
 * LOGIC.md says charts keep their shape and lose their axes, and a shape carries no
 * digits.
 */
data class WeekBar(val done: Int, val wanted: Int, val spoken: String, val label: String = "")

/**
 * Abilities, from the grid, screen 8. The tab that replaced History.
 *
 * Four rows, each with its life sentence and its state, then the person's own
 * list underneath. Same is drawn exactly as Better is drawn: same type, same
 * size, same weight, and only the pill colour differs, because holding a number
 * for a year is the work rather than the absence of it.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun AbilitiesScreen(
    state: AbilitiesUiState,
    onAbility: (AbilityDomain) -> Unit,
    onCheck: () -> Unit,
    onSummary: () -> Unit,
    onTry: () -> Unit,
    onCard: () -> Unit,
    onPlaces: () -> Unit,
    modifier: Modifier = Modifier,
    tryOffer: Boolean = false,
    tryResult: Boolean = false,
) {
    SteadyScreen(title = null, onBack = null, modifier = modifier, help = Place.Progress) {
        SteadyText(
            text = stringResource(R.string.tab_progress),
            style = SteadyType.Greeting,
            color = SteadyPalette.Navy,
        )

        // ADDENDUM-03 Part 10. Four bars, each its own week, with nothing joining them.
        if (state.weeks.isNotEmpty()) {
            SectionTitle(stringResource(R.string.weeks_title))
            Row(
                modifier = Modifier.fillMaxWidth().height(BAR_AREA),
                horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
                verticalAlignment = Alignment.Bottom,
            ) {
                state.weeks.forEach { week -> WeekColumn(week, Modifier.weight(1f)) }
            }
            Paragraph(state.weeksSaid)
        }

        // Above the abilities and above the look back, because for the fortnight
        // it exists it is the thing worth reading first, and after that it is gone.
        state.firstMonth?.let { card ->
            NoteBlock(heading = card.heading, text = card.line)
        }

        state.lookBack?.let {
            SectionTitle(stringResource(R.string.look_back_title))
            NoteBlock(it)
        }

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

        // Only when there is something to say. Never announced, never badged: an
        // app that nags somebody about an optional experiment has misunderstood
        // what the experiment is for.
        if (tryOffer || tryResult) {
            ListItem(
                heading = stringResource(
                    if (tryResult) R.string.try_result else R.string.try_action,
                ),
                subtitle = stringResource(R.string.settings_try_sub),
                next = true,
                onClick = onTry,
            )
        }

        ListItem(
            heading = stringResource(R.string.check_title),
            subtitle = state.checkLede,
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
                    // The rating is what the person said, reported back to them,
                    // so it is one of the figures that becomes a word. NumbersOff
                    // says why at length.
                    subtitle = item.said,
                    tileTint = tintFor(item.domain),
                    glyph = { AbilityGlyph(item.domain) },
                    value = null,
                )
            }
        }

        if (state.waiting) {
            Paragraph(stringResource(R.string.abilities_waiting))
        }

        // Both of these live at the bottom, unbadged and unannounced. Part 11 says
        // the card is offered once at the second Sunday review and lives in Progress
        // afterwards, and Part 8 says the walkthrough is optional and off by default.
        // Neither is a thing the app should ask anybody about twice.
        ListItem(
            heading = stringResource(R.string.card_title),
            subtitle = stringResource(R.string.card_offer_sub),
            onClick = onCard,
        )
        ListItem(
            heading = stringResource(R.string.places_offer),
            subtitle = stringResource(R.string.places_offer_sub),
            onClick = onPlaces,
        )
    }
}

/**
 * One week, drawn against the number the person chose.
 *
 * The bar is how much of that week's own number was met and nothing else. There is no
 * line joining it to the week beside it and no total across the four, because there
 * is no such thing here as carrying anything over.
 */
@Composable
private fun WeekColumn(week: WeekBar, modifier: Modifier = Modifier) {
    val filled = if (week.wanted <= 0) 0f else (week.done.toFloat() / week.wanted).coerceIn(0f, 1f)
    Column(
        modifier = modifier.semantics { contentDescription = week.spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_AREA - BAR_LABEL)
                .clip(RoundedCornerShape(SteadySpacing.Tight))
                .background(SteadyPalette.Sand),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(filled.coerceAtLeast(EMPTY_SLIVER))
                    .clip(RoundedCornerShape(SteadySpacing.Tight))
                    .background(SteadyPalette.Green),
            )
        }
        SteadyText(
            text = week.label,
            style = SteadyType.Caption,
            color = SteadyPalette.Ink3Text,
            modifier = Modifier.height(BAR_LABEL),
        )
    }
}

/** Tall enough to read from across a room, short enough not to be the screen. */
private val BAR_AREA = 120.dp
private val BAR_LABEL = 24.dp

/** A week with nothing in it still draws something, so four bars are always four. */
private const val EMPTY_SLIVER = 0.04f

private fun labelFor(state: AbilityState) = when (state) {
    AbilityState.Better -> R.string.state_better
    AbilityState.Same -> R.string.state_same
    AbilityState.Quieter -> R.string.state_quieter
}
