@file:Suppress("MatchingDeclarationName") // The seven screens of one session.

package com.kamsiob.steadyhealth.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.session.Counted
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.help.Place
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** What every session screen needs from the runner. */
data class SessionUiState(
    val movementName: String = "",
    val setup: String = "",
    val stopRule: String = "",
    val target: Int = 0,
    val lastResult: Int? = null,
    val count: Int = 0,
    val progress: Float = 0f,
    val unit: String = "",
    val countInLeft: Int = 0,
    val restLeft: Int = 0,
    val restTotal: Int = 0,
    val nextName: String? = null,
    val justDid: String? = null,
    val paused: Boolean = false,
    val speaking: Boolean = true,
    val counted: Counted = Counted.Reps,
    val stepOf: String = "",
    /** Shown once, after "Make it easier" has been used. */
    val eased: Boolean = false,
)

/** What every session screen can do. Grouped, because there are seven of them. */
data class SessionActions(
    val onReady: () -> Unit,
    val onSkipCountIn: () -> Unit,
    val onTap: () -> Unit,
    val onEndSet: () -> Unit,
    val onSkipRest: () -> Unit,
    val onPause: () -> Unit,
    val onEasier: () -> Unit,
    val onSkip: () -> Unit,
    val onEnough: () -> Unit,
    val onHurts: () -> Unit,
    val onSpeaker: () -> Unit,
)

/**
 * S2 and S6. Ready.
 *
 * The movement's name, its setup line, the target, and the stop rule. One button. The
 * stop rule is on this screen rather than buried, because somebody deciding whether
 * they can do this needs to know how to stop before they start.
 */
@Composable
fun ReadyScreen(
    state: SessionUiState,
    actions: SessionActions,
    modifier: Modifier = Modifier,
) {
    SessionScaffold(
        state = state,
        actions = actions,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.session_ready), onClick = actions.onReady)
        },
    ) {
        SteadyText(
            text = state.stepOf,
            style = SteadyType.Caption,
            color = SteadyPalette.Ink3Text,
        )
        SteadyText(
            text = state.movementName,
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )
        Paragraph(state.setup)

        SectionTitle(stringResource(R.string.session_target, state.target))
        state.lastResult?.let {
            Paragraph(stringResource(R.string.session_last_time, it))
        }

        NoteBlock(state.stopRule)
        if (state.eased) NoteBlock(stringResource(R.string.exit_eased), tint = SteadyPalette.GreenL)
    }
}

/**
 * S3. Three, two, one, go.
 *
 * The number fills the screen because nobody is holding the phone at this point. It
 * has no buttons except the ones every session screen has.
 */
@Composable
fun CountInScreen(
    state: SessionUiState,
    actions: SessionActions,
    modifier: Modifier = Modifier,
) {
    SessionScaffold(
        state = state,
        actions = actions,
        modifier = modifier,
        footer = {
            SecondaryButton(
                label = stringResource(R.string.session_skip_count_in),
                onClick = actions.onSkipCountIn,
                inSession = true,
            )
        },
    ) {
        Spacer(Modifier.height(SteadySpacing.Inside))
        SteadyText(
            text = state.movementName,
            style = SteadyType.SectionTitle,
            color = SteadyPalette.Ink2,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        BigCount(
            value = state.countInLeft,
            spoken = "$" + "{state.countInLeft}",
            animate = motionOn(),
        )
    }
}

/**
 * S4. The set, live. The dominant screen of the app.
 *
 * An enormous count inside a ring that fills toward the target, the last result under
 * it, and the exits always visible rather than behind anything. Everything about this
 * screen assumes the phone is on a table two feet away and the person is moving.
 */
@Composable
fun LiveScreen(
    state: SessionUiState,
    actions: SessionActions,
    modifier: Modifier = Modifier,
) {
    SessionScaffold(
        state = state,
        actions = actions,
        modifier = modifier,
        footer = {
            if (state.counted == Counted.Taps || state.counted == Counted.Reps) {
                PrimaryButton(
                    label = stringResource(R.string.session_done_set),
                    onClick = actions.onEndSet,
                )
            } else {
                PrimaryButton(
                    label = stringResource(R.string.session_done_set),
                    onClick = actions.onEndSet,
                )
            }
        },
    ) {
        SteadyText(
            text = state.movementName,
            style = SteadyType.SectionTitle,
            color = SteadyPalette.Ink2,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
            textAlign = TextAlign.Center,
        )

        ProgressRing(
            progress = state.progress,
            modifier = Modifier.fillMaxWidth().padding(horizontal = RING_INSET),
            animate = motionOn(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BigCount(
                    value = state.count,
                    spoken = spokenCount(state),
                    animate = motionOn(),
                )
                SteadyText(
                    text = stringResource(R.string.session_of, state.target),
                    style = SteadyType.SectionTitle,
                    color = SteadyPalette.Ink3Text,
                )
            }
        }

        state.lastResult?.let {
            SteadyText(
                text = stringResource(R.string.session_last_time, it),
                style = SteadyType.Body,
                color = SteadyPalette.Ink2,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * S5. Rest.
 *
 * A shrinking arc rather than a ticking number, with what was just done and what is
 * next. Rest is the only timed thing in this app, and it is skippable, which is what
 * keeps it inside the accessibility rule about time limits.
 */
@Composable
fun RestScreen(
    state: SessionUiState,
    actions: SessionActions,
    modifier: Modifier = Modifier,
) {
    SessionScaffold(
        state = state,
        actions = actions,
        modifier = modifier,
        footer = {
            PrimaryButton(
                label = stringResource(R.string.session_skip_rest),
                onClick = actions.onSkipRest,
            )
        },
    ) {
        SteadyText(
            text = stringResource(R.string.session_rest),
            style = SteadyType.SectionTitle,
            color = SteadyPalette.Ink2,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
            textAlign = TextAlign.Center,
        )

        ShrinkingArc(
            remaining = state.restLeft,
            total = state.restTotal,
            modifier = Modifier.fillMaxWidth().padding(horizontal = RING_INSET),
            animate = motionOn(),
        ) {
            state.justDid?.let {
                SteadyText(
                    text = it,
                    style = SteadyType.BlockValue,
                    color = SteadyPalette.Navy,
                    textAlign = TextAlign.Center,
                )
            }
        }

        state.nextName?.let {
            SteadyText(
                text = stringResource(R.string.session_next_up, it),
                style = SteadyType.SectionTitle,
                color = SteadyPalette.Navy,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The frame every session screen shares.
 *
 * The speaker toggle and pause sit at the top, the three exits and the pain button sit
 * above the footer, and the primary action sits in the bottom third. Putting them here
 * rather than on each screen is what makes "always visible, never buried" true rather
 * than intended.
 */
@Composable
private fun SessionScaffold(
    state: SessionUiState,
    actions: SessionActions,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    SteadyScreen(
        title = null,
        onBack = null,
        modifier = modifier,
        help = Place.Session,
        footer = {
            ExitRow(actions)
            footer()
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(
                label = stringResource(
                    if (state.speaking) R.string.session_speaker_on else R.string.session_speaker_off,
                ),
                onClick = actions.onSpeaker,
                modifier = Modifier.weight(1f),
                inSession = true,
            )
            SecondaryButton(
                label = stringResource(
                    if (state.paused) R.string.session_resume else R.string.session_pause,
                ),
                onClick = actions.onPause,
                modifier = Modifier.weight(1f),
                inSession = true,
            )
        }
        content()
    }
}

/**
 * The three exits and the pain button, on every screen of every session.
 *
 * Equal weight and never behind a menu. ADDENDUM-03 Part 2 is explicit that these are
 * not a settings screen: they are what a bad day looks like, and a bad day is common.
 */
@Composable
private fun ExitRow(actions: SessionActions) {
    Column(verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
            SecondaryButton(
                label = stringResource(R.string.exit_easier),
                onClick = actions.onEasier,
                modifier = Modifier.weight(1f),
                inSession = true,
            )
            SecondaryButton(
                label = stringResource(R.string.exit_skip),
                onClick = actions.onSkip,
                modifier = Modifier.weight(1f),
                inSession = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
            SecondaryButton(
                label = stringResource(R.string.exit_enough),
                onClick = actions.onEnough,
                modifier = Modifier.weight(1f),
                inSession = true,
            )
            SecondaryButton(
                label = stringResource(R.string.exit_hurts),
                onClick = actions.onHurts,
                modifier = Modifier.weight(1f),
                inSession = true,
            )
        }
    }
}

@Composable
private fun spokenCount(state: SessionUiState): String =
    "${state.count} ${stringResource(R.string.session_of, state.target)}"

private val RING_INSET = 40.dp
