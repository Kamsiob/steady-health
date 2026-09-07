@file:Suppress("MatchingDeclarationName") // The offer and the result, one subject.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.Block
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TwoUp
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** What the offer draws. Grid screen 14. */
data class TryOfferUiState(
    val observation: String = "",
    val evidence: String = "",
    val question: String = "",
)

/**
 * Try it and see. Grid screen 14.
 *
 * What the app noticed, in the person's own words, then one change for two weeks
 * and exactly what will be measured. The sentence about no difference is on this
 * screen rather than on the result, because a test whose honest outcome is only
 * revealed at the end is a test somebody agreed to under a false idea of it.
 *
 * "Not now" is a real answer, the same size as the other one, and nothing keeps
 * a count of how often it is chosen.
 */
@Composable
fun TryOfferScreen(
    state: TryOfferUiState,
    onStart: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.try_action),
        onBack = onNotNow,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.try_start), onClick = onStart)
            SecondaryButton(label = stringResource(R.string.try_not_now), onClick = onNotNow)
        },
    ) {
        SectionTitle(stringResource(R.string.try_noticed))
        SteadyText(
            text = state.observation,
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )
        Paragraph(state.evidence)

        SectionTitle(state.question)
        Paragraph(stringResource(R.string.try_what_happens))

        NoteBlock(stringResource(R.string.try_stop))
    }
}

/** One half of the test, as the result shows it. */
data class TryArm(val label: String, val value: String, val unit: String)

/** What the result draws. Grid screen 15. */
data class TryResultUiState(
    val headline: String = "",
    val a: TryArm = TryArm("", "", ""),
    val b: TryArm = TryArm("", "", ""),
    val reading: String = "",
    val suggestLabel: String? = null,
)

/**
 * What we found. Grid screen 15.
 *
 * Both numbers side by side, at the same size, in the same colour, whichever way
 * it went. The reading underneath is the engine's, and when the two fortnights
 * landed inside the measure's own noise it says so plainly: that is a real answer
 * and the screen treats it as one.
 */
@Composable
fun TryResultScreen(
    state: TryResultUiState,
    onSuggest: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.try_result),
        onBack = onKeep,
        modifier = modifier,
        footer = {
            state.suggestLabel?.let {
                PrimaryButton(label = it, onClick = onSuggest)
            }
            SecondaryButton(label = stringResource(R.string.try_keep), onClick = onKeep)
        },
    ) {
        SteadyText(
            text = state.headline,
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )

        TwoUp {
            listOf(state.a, state.b).forEach { arm ->
                Block(
                    label = arm.label,
                    value = "${arm.value} ${arm.unit}".trim(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Paragraph(state.reading)
    }
}
