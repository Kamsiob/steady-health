@file:Suppress("MatchingDeclarationName") // The card and the state it draws.

package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * Today's session, as one card.
 *
 * DESIGN.md 4b: it always says what to do, how long it takes, and has one obvious
 * button. Everything in it comes from the engine's plan, so the card and the session
 * cannot disagree about what today is.
 */
data class SessionCardState(
    val length: String = "",
    val movements: List<String> = emptyList(),
    /** "With a warm up and a cool down", rather than two more lines in the list. */
    val bookends: String? = null,
    val feeds: String? = null,
    val adaptation: String? = null,
    val doneToday: Boolean = false,
    /** "Left the knee movements out this week", when an area is suppressed. */
    val leftOut: String? = null,
)

@Composable
fun SessionCard(
    state: SessionCardState,
    onGo: () -> Unit,
    onSomethingSmall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SteadyShapes.LifeCard)
            .background(SteadyPalette.Navy)
            .padding(SteadySpacing.Inside),
        verticalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
    ) {
        SteadyText(
            text = stringResource(R.string.card_label),
            style = SteadyType.CardTitle,
            color = SteadyPalette.Sand,
        )
        Explained(
            text = state.length,
            explanation = stringResource(R.string.card_minutes_explain),
            style = SteadyType.ScreenTitleBig,
            colour = SteadyPalette.White,
            tint = SteadyPalette.NavyL,
            explanationColour = SteadyPalette.Sand,
            modifier = Modifier.semantics { heading() },
        )

        state.movements.forEach { name ->
            SteadyText(text = name, style = SteadyType.Body, color = SteadyPalette.White)
        }

        state.bookends?.let {
            SteadyText(text = it, style = SteadyType.Body, color = SteadyPalette.Sand)
        }

        state.feeds?.let {
            SteadyText(text = it, style = SteadyType.Caption, color = SteadyPalette.Sand)
        }
        state.adaptation?.let {
            SteadyText(text = it, style = SteadyType.Body, color = SteadyPalette.Sand)
        }
        state.leftOut?.let {
            SteadyText(text = it, style = SteadyType.Body, color = SteadyPalette.Sand)
        }

        if (state.doneToday) {
            SteadyText(
                text = stringResource(R.string.card_done),
                style = SteadyType.BodyStrong,
                color = SteadyPalette.White,
                modifier = Modifier.padding(top = SteadySpacing.Tight),
            )
        }

        PrimaryButton(
            label = stringResource(if (state.doneToday) R.string.card_again else R.string.card_go),
            onClick = onGo,
            modifier = Modifier.padding(top = SteadySpacing.Tight),
        )

        // ADDENDUM-03 Part 2, tiredness. Ninety seconds, and it counts as a session.
        // A quiet second line rather than a second button, because it is an out and
        // not a choice between two equal things.
        TextLink(
            label = stringResource(R.string.offer_got_a_minute),
            onClick = onSomethingSmall,
            modifier = Modifier.padding(top = SteadySpacing.Tight),
        )
    }
}
