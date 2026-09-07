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
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
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

        SectionTitle(stringResource(R.string.talk_question))
        ThreeUpChoice(
            options = listOf(
                stringResource(R.string.talk_yes),
                stringResource(R.string.talk_just),
                stringResource(R.string.talk_no),
            ),
            selectedIndex = state.talkTest?.ordinal,
            onSelect = { onTalkTest(TalkTest.entries[it]) },
            selectedFill = SteadyPalette.GreenL,
            selectedOutline = SteadyPalette.Green,
        )
    }
}

private const val MAX_HALF_HOURS = 20
