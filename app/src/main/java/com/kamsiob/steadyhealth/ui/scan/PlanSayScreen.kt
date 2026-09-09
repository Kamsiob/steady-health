package com.kamsiob.steadyhealth.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen

/**
 * Saying the plan out loud. ADDENDUM-03 Part 6.
 *
 * One sentence, the way somebody would say it to a friend: "my physio wants me doing
 * ten sit to stands twice a day and heel raises". It goes to the same reader the
 * photographed sheet goes to, and then to the same confirmation screen, because a
 * spoken plan is no more trustworthy than a scanned one and neither is saved unread.
 *
 * WHAT IS HEARD IS SHOWN BEFORE IT IS READ. Speech gets words wrong, and the first
 * place a wrong word does damage is a number: heard as fifteen instead of fifty, it
 * would sit on the confirmation screen looking exactly like something a therapist
 * wrote. So the sentence comes back on this screen first, in the person's own words,
 * with "Say it again" beside it.
 *
 * WHEN THE PHONE CANNOT DO IT, IT SAYS SO AND STOPS. Android's ordinary recogniser
 * sends the audio to a server, and this app runs on the phone. So the only recogniser
 * used is the on-device one, and where a phone has none this screen says one plain
 * sentence and offers typing. The fallback that would work is the one that sends
 * somebody's voice to a server, so there is no fallback. [PlanVoice] holds the rest of
 * the reasoning; this screen is where a person meets it.
 *
 * The microphone is asked for here, on the tap that needs it, and never before. A
 * phone that cannot do this at all is never asked for a microphone at all, because
 * the app already knows it would not use it.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun PlanSayScreen(
    state: PlanWaysUiState,
    onAllow: () -> Unit,
    onListen: () -> Unit,
    onStop: () -> Unit,
    onContinue: () -> Unit,
    onType: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.spoken_title),
        onBack = onBack,
        modifier = modifier,
        footer = { SayFooter(state, onAllow, onListen, onStop, onContinue) },
    ) {
        // Only where it is true. A promise about where a recording stays, on a screen
        // that is about to explain that no recording will be made, is noise.
        if (state.canHear) NoteBlock(stringResource(R.string.spoken_stays_here))

        when {
            !state.canHear -> NotHere(onType)
            !state.microphone -> NeedsMicrophone(onType)
            state.listening -> Paragraph(stringResource(R.string.spoken_listening))
            state.heard.any(Char::isLetter) -> Heard(state.heard)
            else -> Asking()
        }

        // The way that asks for nothing sits under both of the endings that are not a
        // sentence, because "or type it instead" printed with nowhere to type is the
        // app telling somebody to do something it has not offered them.
        if (state.nothingHeard) {
            NoteBlock(stringResource(R.string.spoken_nothing))
            TypeInstead(onType)
        }
    }
}

/** The one sentence, and the way in that asks for nothing. */
@Composable
private fun NotHere(onType: () -> Unit) {
    Paragraph(stringResource(R.string.spoken_not_here))
    TypeInstead(onType)
}

/**
 * The microphone has not been given, and may never be.
 *
 * The way in that asks for nothing sits under the sentence as well as behind the
 * button, because Android stops showing its own question after a second refusal and
 * the app is never told that it has. "Allow the microphone" would then be a button
 * that does nothing at all when it is pressed, which reads as the app being broken
 * rather than as the app having been told no, and asking again is not something this
 * screen is willing to do. So there is always the other way, right there, from the
 * first time this screen is opened.
 */
@Composable
private fun NeedsMicrophone(onType: () -> Unit) {
    Paragraph(stringResource(R.string.spoken_needs_mic))
    TypeInstead(onType)
}

@Composable
private fun TypeInstead(onType: () -> Unit) {
    ListItem(
        heading = stringResource(R.string.ways_type),
        subtitle = stringResource(R.string.ways_type_sub),
        onClick = onType,
    )
}

@Composable
private fun Heard(said: String) {
    SectionTitle(stringResource(R.string.spoken_heard))
    Paragraph(said)
}

@Composable
private fun Asking() {
    Paragraph(stringResource(R.string.spoken_how))
    NoteBlock(stringResource(R.string.spoken_example))
}

/**
 * One action at a time, and nothing offered that cannot be done.
 *
 * A phone with no on-device recogniser gets no button here at all. The way on is the
 * row in the body, because a primary button saying "start talking" under a sentence
 * saying the phone cannot listen is the app arguing with itself.
 */
@Composable
private fun SayFooter(
    state: PlanWaysUiState,
    onAllow: () -> Unit,
    onListen: () -> Unit,
    onStop: () -> Unit,
    onContinue: () -> Unit,
) {
    when {
        !state.canHear -> Unit
        !state.microphone ->
            PrimaryButton(label = stringResource(R.string.spoken_allow), onClick = onAllow)

        state.listening ->
            PrimaryButton(label = stringResource(R.string.spoken_stop), onClick = onStop)

        state.heard.any(Char::isLetter) -> {
            PrimaryButton(
                label = stringResource(R.string.continue_label),
                onClick = onContinue,
            )
            SecondaryButton(label = stringResource(R.string.spoken_again), onClick = onListen)
        }

        else -> PrimaryButton(label = stringResource(R.string.spoken_start), onClick = onListen)
    }
}
