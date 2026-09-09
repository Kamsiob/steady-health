@file:Suppress("MatchingDeclarationName") // The monthly check, start to finish, one subject.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.RatingRow
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** The four things this check will ask for, before it starts. */
data class CheckIntroUiState(val measures: List<String> = emptyList())

/**
 * The offer to do the check.
 *
 * The safety line is on this screen rather than behind a link, because a caution
 * somebody has to tap to see is a caution nobody sees. "Not now" is a real answer
 * and is the same size as the other button.
 */
@Composable
fun CheckIntroScreen(
    state: CheckIntroUiState,
    onStart: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.check_title),
        onBack = onLater,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.check_intro_start), onClick = onStart)
            SecondaryButton(label = stringResource(R.string.check_intro_later), onClick = onLater)
        },
    ) {
        SteadyText(
            text = stringResource(R.string.check_intro_title),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )
        Paragraph(stringResource(R.string.check_intro_lede))

        SectionTitle(stringResource(R.string.check_intro_what))
        state.measures.forEach { ListItem(heading = it) }

        NoteBlock(stringResource(R.string.check_intro_safety))
    }
}

/** What one measure's screen draws. Grid screen 10. */
data class CheckMeasureUiState(
    val name: String = "",
    val how: String = "",
    val safety: String = "",
    val index: Int = 1,
    val total: Int = 1,
    /** The running count, or the elapsed seconds for a held measure. */
    val value: Int = 0,
    val unit: String = "",
    val secondsLeft: Int? = null,
    val countingLine: String = "",
    /** True when the person taps to count rather than the phone counting. */
    val byHand: Boolean = false,
    val running: Boolean = false,
)

/**
 * One measure, being taken. Grid screen 10.
 *
 * The count is the whole screen, because somebody standing up out of a chair with
 * a phone in their pocket is going to glance at it from two feet away. The dots
 * under it are one per repetition, which is what makes it obvious the phone is
 * actually counting rather than showing a number it made up.
 */
@Composable
fun CheckMeasureScreen(
    state: CheckMeasureUiState,
    onTap: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.check_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            if (state.byHand && state.running) {
                PrimaryButton(label = stringResource(R.string.check_count_it), onClick = onTap)
                SecondaryButton(label = stringResource(R.string.check_stop), onClick = onStop)
            } else if (state.running) {
                PrimaryButton(label = stringResource(R.string.check_stop), onClick = onStop)
            } else {
                PrimaryButton(label = stringResource(R.string.check_intro_start), onClick = onTap)
                SecondaryButton(label = stringResource(R.string.check_skip), onClick = onSkip)
            }
        },
    ) {
        SteadyText(
            text = stringResource(R.string.check_step, state.index, state.total),
            style = SteadyType.Caption,
            color = SteadyPalette.Ink3Text,
        )
        SteadyText(
            text = state.name,
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )
        Paragraph(state.how)

        Spacer(Modifier.height(SteadySpacing.ListGap))

        SteadyText(
            text = "${state.value}",
            style = SteadyType.HeroNumber,
            color = SteadyPalette.Navy,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        SteadyText(
            text = countLine(state),
            style = SteadyType.Caption,
            color = SteadyPalette.Ink2,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        if (state.value > 0) Dots(state.value)

        Paragraph(state.countingLine)
        NoteBlock(state.safety)
    }
}

@Composable
private fun countLine(state: CheckMeasureUiState): String {
    val left = state.secondsLeft ?: return state.unit
    val remaining = androidx.compose.ui.res.pluralStringResource(
        R.plurals.check_seconds_left,
        left,
        left,
    )
    return "${state.unit} · $remaining"
}

/**
 * One dot per repetition, up to a row's worth.
 *
 * Not a progress bar, because there is nothing to make progress towards: the
 * number somebody reaches is the number, and a bar implies a target.
 */
@Composable
private fun Dots(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight, Alignment.CenterHorizontally),
    ) {
        repeat(minOf(count, MOST_DOTS)) {
            Box(
                modifier = Modifier
                    .size(DOT)
                    .clip(SteadyShapes.Round)
                    .background(SteadyPalette.Orange),
            )
        }
    }
}

/**
 * One thing on the person's list, being rated again.
 *
 * [sureness] is the second question, ADDENDUM-03 Part 18, and it is null until it is
 * answered. Null and zero are different answers here and stay different all the way
 * down to the column they are written in.
 */
data class RateAgainItem(
    val id: Long,
    val text: String,
    val rating: Int,
    val before: Int,
    val sureness: Int? = null,
)

/**
 * The person's own list, re-rated. LOGIC.md 3b: "Re-rated monthly with the check."
 *
 * It comes after the measures because the numbers are the part that needs a chair
 * and a wall, and somebody who stops there has still done the useful half. This
 * part takes twenty seconds and is the one CONTENT.md card 12 says is the reason
 * the app asks at all: seven seconds off a chair-stand time is abstract, getting
 * off the floor without your hands is not.
 */
@Composable
fun RateAgainScreen(
    items: List<RateAgainItem>,
    onRate: (Long, Int) -> Unit,
    onSureness: (Long, Int) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.check_title),
        onBack = onBack,
        modifier = modifier,
        footer = { PrimaryButton(label = stringResource(R.string.check_next), onClick = onDone) },
    ) {
        SteadyText(
            text = stringResource(R.string.rate_title),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )
        Paragraph(stringResource(R.string.rate_scale))

        items.forEach { item ->
            SectionTitle(item.text)
            RatingRow(
                rating = item.rating,
                onRate = { onRate(item.id, it) },
                label = item.text,
            )

            // The second question, under the first rather than on a screen of its
            // own. Part 18 calls it optional and it looks optional: it has no
            // Skip, because leaving a row alone is already skipping it, and a
            // Skip button would make passing over it feel like a decision.
            Paragraph(stringResource(R.string.rate_sure))
            RatingRow(
                rating = item.sureness ?: 0,
                onRate = { onSureness(item.id, it) },
                label = stringResource(R.string.rate_sure_of, item.text),
            )
        }
    }
}

/** One measure's result, as the done screen shows it. */
data class CheckResultRow(
    val name: String,
    val value: String,
    val state: AbilityState,
    val stateLabel: String,
)

/** What the done screen draws. Grid screen 12. */
data class CheckDoneUiState(
    val lifeSentence: String = "",
    val wanted: String = "",
    val rows: List<CheckResultRow> = emptyList(),
    val anySame: Boolean = false,
    /**
     * What an ability did and how sure they feel, in one sentence. Part 18.
     *
     * At most one, and blank most months. It is the strongest sentence the app has
     * and saying it every month about every item would spend it.
     */
    val surer: String = "",
    /**
     * The fourth warm place. Part 16, said once ever, per crossing.
     *
     * "You said you wanted to get off the floor without your hands. You just did
     * it." Part 16 writes it with an exclamation mark and it does not have one,
     * because DESIGN.md section 6 bans them everywhere and the sentence does not
     * need one. It names what was done. It does not evaluate it.
     */
    val justDidIt: String = "",
    /**
     * The line a milestone card would start from, in the person's own voice.
     *
     * Separate from [justDidIt] because that one is the app talking to them and this
     * one is them talking to somebody else, and the two are not the same sentence.
     */
    val cardLine: String = "",
)

/**
 * What that means. Grid screen 12.
 *
 * Life first and numbers second, which is the whole reframe in one screen order.
 * The note about Same is shown whenever anything held, and it is not a
 * consolation: strength falls between one and a half and five percent a year from
 * midlife if nothing is done, so a number that has not moved is a year of that
 * not happening.
 */
@Composable
fun CheckDoneScreen(
    state: CheckDoneUiState,
    onSave: () -> Unit,
    onCard: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.check_title),
        onBack = onBack,
        modifier = modifier,
        footer = { PrimaryButton(label = stringResource(R.string.check_save), onClick = onSave) },
    ) {
        SteadyText(
            text = stringResource(R.string.check_done),
            style = SteadyType.Caption,
            color = SteadyPalette.Ink3Text,
        )

        if (state.lifeSentence.isNotBlank()) {
            SteadyText(
                text = state.lifeSentence,
                style = SteadyType.ScreenTitleBig,
                color = SteadyPalette.Navy,
                modifier = Modifier.semantics { heading() },
            )
            if (state.wanted.isNotBlank()) Paragraph(state.wanted)
        }

        // Above the numbers, because it is the only thing on this screen that is
        // about something they said they wanted rather than about a measurement.
        if (state.justDidIt.isNotBlank()) {
            NoteBlock(state.justDidIt, tint = SteadyPalette.Butter)
            // Part 11 calls the milestone card "the one people will actually send",
            // and this is the moment it exists. Offered as a row and never as a
            // prompt: somebody who does not want to tell anybody scrolls past it.
            if (state.cardLine.isNotBlank()) {
                ListItem(
                    heading = stringResource(R.string.card_offer),
                    subtitle = stringResource(R.string.card_offer_sub),
                    onClick = { onCard(state.cardLine) },
                )
            }
        }

        state.rows.forEach { row ->
            ListItem(heading = row.name, subtitle = row.value, value = row.stateLabel)
        }

        // Under the numbers rather than over them. It is a sentence about what the
        // numbers meant, and it reads as one only once they have been seen.
        if (state.surer.isNotBlank()) Paragraph(state.surer)

        if (state.anySame) {
            NoteBlock(
                heading = stringResource(R.string.check_same_is_a_result),
                text = stringResource(R.string.check_same_why),
            )
        }
    }
}

/** What the Quieter screen draws. Grid screen 13. */
data class QuieterUiState(
    val ability: String = "",
    val sentence: String = "",
    val numbers: String = "",
    val held: String = "",
)

/**
 * When something slips. Grid screen 13.
 *
 * The hard screen, handled once and quietly. No colour changes, nothing turns
 * red, and nothing here is a diagnosis: a description of what the numbers did,
 * one suggestion, and everything that held listed beside it so that one measure
 * is not read as the whole person.
 *
 * Shown once per ability per six months and never repeated, which is enforced by
 * the engine rather than by this screen.
 */
@Composable
fun QuieterScreen(
    state: QuieterUiState,
    onSummary: () -> Unit,
    onOkay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = state.ability,
        onBack = onOkay,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.quieter_open_summary), onClick = onSummary)
            SecondaryButton(label = stringResource(R.string.check_okay), onClick = onOkay)
        },
    ) {
        SteadyText(
            text = state.sentence,
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )
        Paragraph(state.numbers)

        NoteBlock(stringResource(R.string.quieter_note))

        NoteBlock(
            heading = stringResource(R.string.quieter_held),
            text = "${state.held} ${stringResource(R.string.quieter_one_thing)}".trim(),
            tint = SteadyPalette.GreenL,
        )
    }
}

private val DOT = 10.dp
private const val MOST_DOTS = 20
