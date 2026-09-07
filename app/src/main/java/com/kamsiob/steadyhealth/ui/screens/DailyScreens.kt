package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.DayRating
import com.kamsiob.steadyhealth.domain.TalkTest
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.ui.components.Dial
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.Stepper
import com.kamsiob.steadyhealth.ui.components.TextEntry
import com.kamsiob.steadyhealth.ui.components.ThreeUpChoice
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType
import com.kamsiob.steadyhealth.util.Convert

/** What the weigh-in screen draws. */
data class WeighInUiState(
    val kg: Double = 80.0,
    val units: Units = Units.Imperial,
    val note: String = "",
)

/** Screen 5 of the grid. A dial you turn with your thumb, and two buttons. */
@Composable
fun WeighInScreen(
    state: WeighInUiState,
    onWeight: (Double) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.weigh_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.weigh_save), onClick = onSave)
            SteadyText(
                text = stringResource(R.string.weigh_whenever),
                style = SteadyType.Caption,
                color = SteadyPalette.Ink3Text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
    ) {
        Spacer(Modifier.height(SteadySpacing.Inside))
        Dial(
            value = state.kg,
            displayValue = Convert.weightLabel(state.kg, state.units),
            unit = stringResource(if (state.units == Units.Imperial) R.string.unit_lb else R.string.unit_kg),
            onValue = onWeight,
            spoken = Convert.weightLabel(state.kg, state.units),
        )
        SteadyText(
            text = stringResource(R.string.weigh_this_morning),
            style = SteadyType.Body,
            color = SteadyPalette.Ink2,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        if (state.note.isNotBlank()) Paragraph(state.note)
    }
}

/** What say-how-today-went draws. */
data class SayHowUiState(
    val sentence: String = "",
    val sleepHalfHours: Int = 14,
    val dayRating: DayRating? = null,
)

/**
 * Screen 6 of the grid. A sentence, sleep, and how the day felt.
 *
 * The microphone is absent rather than disabled: the reader is Phase 3 and an app
 * that shows a control it cannot honour is worse than one that shows a keyboard,
 * which is the path the whole thing is designed around anyway.
 */
@Composable
fun SayHowScreen(
    state: SayHowUiState,
    onSentence: (String) -> Unit,
    onSleep: (Int) -> Unit,
    onRating: (DayRating) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.say_title),
        onBack = onBack,
        modifier = modifier,
        footer = { PrimaryButton(label = stringResource(R.string.say_save), onClick = onSave) },
    ) {
        TextEntry(
            value = state.sentence,
            onValue = onSentence,
            hint = stringResource(R.string.say_hint),
            singleLine = false,
            imeAction = ImeAction.Default,
        )

        SectionTitle(stringResource(R.string.say_sleep))
        Stepper(
            label = stringResource(R.string.say_sleep),
            value = stringResource(R.string.say_hours, hoursLabel(state.sleepHalfHours)),
            supporting = null,
            onDown = { onSleep((state.sleepHalfHours - 1).coerceAtLeast(0)) },
            onUp = { onSleep((state.sleepHalfHours + 1).coerceAtMost(MAX_HALF_HOURS)) },
        )

        SectionTitle(stringResource(R.string.say_feel))
        ThreeUpChoice(
            options = listOf(
                stringResource(R.string.say_rough),
                stringResource(R.string.say_okay),
                stringResource(R.string.say_good),
            ),
            selectedIndex = state.dayRating?.ordinal,
            onSelect = { onRating(DayRating.entries[it]) },
        )
    }
}

private fun hoursLabel(halfHours: Int): String {
    val whole = halfHours / 2
    return if (halfHours % 2 == 0) "$whole" else "$whole.5"
}

/** What the walking screen draws. */
data class WalkingUiState(
    val walkName: String = "",
    val elapsed: String = "0:00",
    val instruction: String = "",

    /**
     * The parts of the set, listed above the timer. Only the bed session has
     * them; a walk is one thing and the list would be a list of one.
     */
    val parts: List<MoveItem> = emptyList(),
)

/** Screen 10 of the grid. Minutes big, the instruction under them. */
@Composable
fun WalkingScreen(
    state: WalkingUiState,
    onStop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = state.walkName,
        onBack = onBack,
        modifier = modifier,
        footer = { PrimaryButton(label = stringResource(R.string.walking_stop), onClick = onStop) },
    ) {
        state.parts.forEach { part ->
            ListItem(
                heading = part.name,
                subtitle = part.instruction,
                value = part.amount,
            )
        }

        Spacer(Modifier.height(SteadySpacing.Inside))
        SteadyText(
            text = state.elapsed,
            style = SteadyType.HeroNumber,
            color = SteadyPalette.Navy,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Paragraph(
            text = state.instruction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** What the done screen draws. */
data class WalkDoneUiState(
    val summary: String = "",
    val elapsed: String = "",
    val talkTest: TalkTest? = null,

    /**
     * True in bed, where the talk test is the wrong question.
     *
     * The grid's own note on screen 18: the talk test becomes "how do you feel",
     * and done is done. The three answers still mean what they meant to the
     * progression rules, so nothing behind the screen changes.
     */
    val askHowYouFeel: Boolean = false,
)

/** Screen 11. Today's time, then the talk test in plain words. */
@Composable
fun WalkDoneScreen(
    state: WalkDoneUiState,
    onTalkTest: (TalkTest) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.walk_done_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            PrimaryButton(
                label = stringResource(R.string.walk_done_save),
                onClick = onSave,
                enabled = state.talkTest != null,
            )
        },
    ) {
        SteadyText(
            text = stringResource(R.string.walk_done_line),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
        )
        Paragraph("${state.summary} ${state.elapsed}".trim())

        SectionTitle(
            stringResource(
                if (state.askHowYouFeel) R.string.feel_question else R.string.talk_question,
            ),
        )
        ThreeUpChoice(
            options = if (state.askHowYouFeel) {
                listOf(
                    stringResource(R.string.feel_good),
                    stringResource(R.string.feel_same),
                    stringResource(R.string.feel_tired),
                )
            } else {
                listOf(
                    stringResource(R.string.talk_yes),
                    stringResource(R.string.talk_just),
                    stringResource(R.string.talk_no),
                )
            },
            selectedIndex = state.talkTest?.ordinal,
            onSelect = { onTalkTest(TalkTest.entries[it]) },
            selectedFill = SteadyPalette.GreenL,
            selectedOutline = SteadyPalette.Green,
        )
    }
}

/** What the offer screen draws. Grid screen 12. */
data class OfferUiState(val walkName: String = "", val instruction: String = "")

/**
 * A longer walk is ready.
 *
 * Offered, never assigned, which is why both answers are buttons of equal weight
 * and why the second one is "Not yet" rather than a dismissal. Saying not yet
 * costs nothing and suppresses the offer for a fortnight.
 */
@Composable
fun OfferScreen(
    state: OfferUiState,
    onAccept: () -> Unit,
    onNotYet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.offer_title),
        onBack = onNotYet,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.offer_yes), onClick = onAccept)
            SecondaryButton(label = stringResource(R.string.offer_no), onClick = onNotYet)
        },
    ) {
        SectionTitle(stringResource(R.string.offer_line, state.walkName))
        Paragraph(state.instruction)
    }
}

private const val MAX_HALF_HOURS = 20
