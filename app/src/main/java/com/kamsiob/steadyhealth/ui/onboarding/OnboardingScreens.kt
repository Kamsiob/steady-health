package com.kamsiob.steadyhealth.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.session.Movement
import com.kamsiob.steadyhealth.ui.Labels
import com.kamsiob.steadyhealth.ui.Language
import com.kamsiob.steadyhealth.ui.components.Hero
import com.kamsiob.steadyhealth.ui.components.HeroExplain
import com.kamsiob.steadyhealth.ui.components.HeroHeadline
import com.kamsiob.steadyhealth.ui.components.HeroLabel
import com.kamsiob.steadyhealth.ui.components.HeroSky
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.Pill
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TagPill
import com.kamsiob.steadyhealth.ui.components.TextEntry
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** The hero on O1 is taller than the rest, because it is most of the screen. */
private val WelcomeHero = 420.dp

/**
 * O1. The hook.
 *
 * No sign up, no email, no account, no permission request, nothing to read. One
 * button, and the language pills only when this build speaks more than one language.
 */
@Composable
fun HookScreen(state: OnboardingState, onLanguage: (String) -> Unit, onNext: () -> Unit) {
    SteadyScreen(
        title = null,
        onBack = null,
        footer = { PrimaryButton(label = stringResource(R.string.o1_show_me), onClick = onNext) },
    ) {
        Hero(sky = HeroSky.Morning, minHeight = WelcomeHero) {
            HeroLabel(stringResource(R.string.app_name))
            HeroHeadline(stringResource(R.string.o1_headline))
            HeroExplain(stringResource(R.string.o1_line))
        }

        val languages = Language.available(LocalContext.current)
        if (languages.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
                languages.forEach { tag ->
                    Pill(
                        label = stringResource(Labels.forLanguage(tag)),
                        selected = state.language == tag,
                        onClick = { onLanguage(tag) },
                    )
                }
            }
        }
    }
}

/**
 * O2. One question, four cards, one tap.
 *
 * No Continue button: tapping a card is the answer and the answer is the navigation.
 * Somebody eleven seconds into an app should not have to confirm a choice they can
 * change later from Settings.
 */
@Composable
fun HowYouGetAroundScreen(onChoose: (GettingAround) -> Unit) {
    SteadyScreen(title = null, onBack = null) {
        SteadyText(
            text = stringResource(R.string.o2_question),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )

        GettingAround.entries.forEach { way ->
            ListItem(
                heading = stringResource(Labels.forGettingAround(way)),
                onClick = { onChoose(way) },
            )
        }

        Paragraph(stringResource(R.string.o2_why))
    }
}

/**
 * O3. Their words.
 *
 * The sentence appears exactly as they typed it. The six chips are for anybody who
 * would rather point than type, and two of the six are about being stronger than
 * today rather than holding on to it.
 *
 * Which six depends on the answer to O2, one screen back. A chip is an example of
 * something worth wanting, and a list of them that assumes two feet tells everybody
 * else that the app was written for somebody who is not them.
 */
@Composable
fun TheirWordsScreen(
    state: OnboardingState,
    onTyped: (String) -> Unit,
    onKeep: (String, AbilityDomain) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    SteadyScreen(
        title = null,
        onBack = null,
        footer = {
            if (state.wanted != null) {
                PrimaryButton(label = stringResource(R.string.continue_label), onClick = onNext)
            } else {
                SecondaryButton(label = stringResource(R.string.o3_skip), onClick = onSkip)
            }
        },
    ) {
        SteadyText(
            text = stringResource(R.string.o3_question),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )

        state.wanted?.let { NoteBlock(heading = stringResource(R.string.o3_keeping), text = it) }

        TextEntry(
            value = state.typed,
            onValue = onTyped,
            hint = stringResource(R.string.o3_hint),
            imeAction = ImeAction.Done,
            onSubmit = { onKeep(state.typed, AbilityDomain.Go) },
        )
        if (state.typed.isNotBlank()) {
            PrimaryButton(
                label = stringResource(R.string.o3_add),
                onClick = { onKeep(state.typed, AbilityDomain.Go) },
            )
        }

        SectionTitle(stringResource(R.string.o3_or))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        ) {
            Starters.forWay(state.gettingAround).forEach { starter ->
                val label = stringResource(starterLabel(starter.id))
                TagPill(
                    label = label,
                    selected = state.wanted == label,
                    onClick = { onKeep(label, starter.domain) },
                )
            }
        }
    }
}

/**
 * O4. The first session, which is not a question.
 *
 * One movement, its setup, one button. Everything else about this person is still
 * unknown and none of it is needed to do one movement.
 */
@Composable
fun FirstSessionScreen(movement: Movement?, onStart: () -> Unit) {
    SteadyScreen(
        title = null,
        onBack = null,
        footer = {
            if (movement != null) {
                PrimaryButton(label = stringResource(R.string.session_start), onClick = onStart)
            }
        },
    ) {
        SteadyText(
            text = stringResource(R.string.o4_title),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )
        Paragraph(stringResource(R.string.o4_length))

        movement?.let {
            SectionTitle(it.name)
            Paragraph(it.setup)
            NoteBlock(it.stopRule)
        }
    }
}

/**
 * O5. Three things, afterwards, one tap each.
 *
 * They are asked here rather than before the session because none of them is needed
 * to do the session, and asking first is how an app loses somebody in the first
 * minute. Done is always live: every answer on this screen is optional.
 */
@Composable
fun AfterTheSessionScreen(
    state: OnboardingState,
    onExclusion: (Exclusion) -> Unit,
    onNothing: () -> Unit,
    onChair: (ChairAnswer) -> Unit,
    onDone: () -> Unit,
) {
    SteadyScreen(
        title = null,
        onBack = null,
        footer = { PrimaryButton(label = stringResource(R.string.o5_done), onClick = onDone) },
    ) {
        // ADDENDUM-03 Part 3 asks for a visible skip on all three. Saying it once at
        // the top says it about both without putting the word Skip beside every
        // question, which reads as the app expecting to be refused.
        Paragraph(stringResource(R.string.o5_all_optional))

        SectionTitle(stringResource(R.string.o5_avoid))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        ) {
            Exclusion.entries.forEach { exclusion ->
                TagPill(
                    label = stringResource(Labels.forExclusion(exclusion)),
                    selected = exclusion in state.exclusions,
                    onClick = { onExclusion(exclusion) },
                )
            }
            TagPill(
                label = stringResource(R.string.o5_avoid_nothing),
                selected = state.exclusions.isEmpty(),
                onClick = onNothing,
            )
        }

        SectionTitle(stringResource(chairQuestion(state.gettingAround)))
        ChairAnswer.entries.forEach { answer ->
            ListItem(
                heading = stringResource(chairLabel(answer)),
                next = state.chair == answer,
                onClick = { onChair(answer) },
            )
        }
    }
}

/** Which chair is being asked about, which depends on what it is for. */
private fun chairQuestion(way: GettingAround?) = when (way) {
    GettingAround.Wheelchair, GettingAround.InBed -> R.string.o5_chair_transfer
    else -> R.string.o5_chair
}

private fun chairLabel(answer: ChairAnswer) = when (answer) {
    ChairAnswer.Sturdy -> R.string.o5_chair_yes
    ChairAnswer.WithArms -> R.string.o5_chair_arms
    ChairAnswer.None -> R.string.o5_chair_no
}

/**
 * The words on each chip, by id.
 *
 * A map rather than a `when`, because fifteen branches is fifteen branches and this
 * one is a table with nothing to decide.
 */
private val STARTER_LABELS = mapOf(
    "floor" to R.string.o3_chip_floor,
    "stairs" to R.string.o3_chip_stairs,
    "shopping" to R.string.o3_chip_shopping,
    "grandkids" to R.string.o3_chip_grandkids,
    "further" to R.string.o3_chip_further,
    "transfer" to R.string.o3_chip_transfer,
    "block" to R.string.o3_chip_block,
    "lap" to R.string.o3_chip_lap,
    "further_wheel" to R.string.o3_chip_further_wheel,
    "sit_up" to R.string.o3_chip_sit_up,
    "chair_back" to R.string.o3_chip_chair_back,
    "cup" to R.string.o3_chip_cup,
    "breath" to R.string.o3_chip_breath,
    "edge" to R.string.o3_chip_edge,
    "stronger" to R.string.o3_chip_stronger,
)

/**
 * The words on one chip, by id.
 *
 * Internal rather than private because ADDENDUM-03 Part 18 makes this question
 * reusable from Progress, and the second place has to offer the same six chips with
 * the same words. Two lists of starter labels would read as two different questions
 * within a fortnight of one of them being edited.
 */
internal fun starterLabel(id: String) = STARTER_LABELS[id] ?: R.string.o3_chip_stronger
