@file:Suppress("MatchingDeclarationName") // One ability in depth, and the weight behind it.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.ui.components.Block
import com.kamsiob.steadyhealth.ui.components.Hero
import com.kamsiob.steadyhealth.ui.components.HeroExplain
import com.kamsiob.steadyhealth.ui.components.HeroLabel
import com.kamsiob.steadyhealth.ui.components.HeroNumber
import com.kamsiob.steadyhealth.ui.components.HeroSky
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TwoUp
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** One measure behind an ability, with where it is and where it was. */
data class MeasureRow(
    val name: String,
    val value: String,
    val before: String?,
    val state: AbilityState,
)

/** What one ability's page draws. Grid screen 9. */
data class AbilityDetailUiState(
    val name: String = "",
    val lifeSentence: String = "",
    val before: String = "",
    val measures: List<MeasureRow> = emptyList(),
    /** The deterministic "what fed this" sentences. Never the model's. */
    val whatFedThis: List<String> = emptyList(),
    /** The exercises behind this ability, shown from the first day. */
    val feeds: List<String> = emptyList(),
    val items: List<TrackedItemState> = emptyList(),
    /** Set when this ability has gone quieter and the note has not been shown. */
    val quieter: QuieterUiState? = null,
)

/**
 * One ability in depth. Grid screen 9.
 *
 * The order is the argument: what you can do, then what it was, then the numbers
 * behind it, then what fed it. Somebody who reads only the first line has read
 * the important part, and the numbers are there for somebody who wants them
 * rather than in front of somebody who does not.
 *
 * "What fed this" is written by the engine from sessions and weight, never by the
 * model. AI.md is explicit that the model may not write a life sentence, and this
 * is the same claim with more words.
 */
@Composable
fun AbilityDetailScreen(
    state: AbilityDetailUiState,
    onWeight: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(title = state.name, onBack = onBack, modifier = modifier) {
        if (state.lifeSentence.isNotBlank()) {
            SteadyText(
                text = state.lifeSentence,
                style = SteadyType.ScreenTitleBig,
                color = SteadyPalette.Navy,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (state.before.isNotBlank()) Paragraph(state.before)

        state.quieter?.let { quiet ->
            NoteBlock(
                heading = quiet.sentence,
                text = quiet.numbers,
                tint = SteadyPalette.SkyL,
            )
        }

        if (state.measures.isNotEmpty()) {
            SectionTitle(stringResource(R.string.ability_behind_it))
            state.measures.forEach { row ->
                ListItem(
                    heading = row.name,
                    subtitle = row.before,
                    value = row.value,
                )
            }
        }

        if (state.whatFedThis.isNotEmpty()) {
            SectionTitle(stringResource(R.string.ability_what_fed))
            state.whatFedThis.forEach { Paragraph(it) }
        }

        if (state.feeds.isNotEmpty()) {
            SectionTitle(stringResource(R.string.ability_feeds))
            state.feeds.forEach { ListItem(heading = it) }
        }

        if (state.items.isNotEmpty()) {
            SectionTitle(stringResource(R.string.abilities_your_list))
            state.items.forEach { item ->
                ListItem(
                    heading = item.text,
                    subtitle = stringResource(R.string.rating_now, item.rating),
                )
            }
        }

        Spacer(Modifier.height(SteadySpacing.ListGap))
        ListItem(
            heading = stringResource(R.string.weight_label),
            subtitle = stringResource(R.string.ability_weight_lever),
            onClick = onWeight,
        )
    }
}

/** What the weight page draws. Grid screen 19. */
data class WeightPageUiState(
    val value: String = "",
    val unit: String = "",
    val explain: String = "",
    val morning: Boolean = true,
    val sinceLabel: String = "",
    val since: String = "",
    val daysLabel: String = "",
    val days: String = "",
    /** The one sentence that connects the number to an ability. */
    val asALever: String = "",
)

/**
 * Weight, as a lever. Grid screen 19.
 *
 * The mechanics are unchanged from the weight-first version of this app and the
 * words are not. The number is here, smoothed, with the raw morning reading under
 * it, and then one sentence saying where it shows up in a life: fewer pounds to
 * carry up the stairs. That sentence is the reason weight is still in the app at
 * all.
 */
@Composable
fun WeightPageScreen(
    state: WeightPageUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.weight_page_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        Hero(sky = if (state.morning) HeroSky.Morning else HeroSky.Evening) {
            HeroLabel(stringResource(R.string.weight_label))
            HeroNumber(value = state.value, unit = state.unit)
            if (state.explain.isNotBlank()) HeroExplain(state.explain)
        }

        // A block with nothing in it is worse than no block, so each of these
        // appears only when there is something to put in it.
        if (state.since.isNotBlank() && state.days.isNotBlank()) {
            TwoUp {
                Block(
                    label = state.sinceLabel,
                    value = state.since,
                    modifier = Modifier.weight(1f),
                )
                Block(
                    label = state.daysLabel,
                    value = state.days,
                    modifier = Modifier.weight(1f),
                )
            }
        } else if (state.days.isNotBlank()) {
            Block(
                label = state.daysLabel,
                value = state.days,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.asALever.isNotBlank()) {
            NoteBlock(state.asALever, modifier = Modifier.fillMaxWidth())
        }
    }
}
