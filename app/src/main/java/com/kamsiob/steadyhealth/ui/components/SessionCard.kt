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

    /**
     * "From your physio", when a therapist's plan exists. ADDENDUM-03 Part 6.
     *
     * When this is set, what the card shows IS the plan and Start runs it. The app's
     * own suggestions are a separate list below, never merged into these.
     */
    val theirs: String? = null,

    /** The app's own suggestions, when a plan is what the card is showing. */
    val alsoMovements: List<String> = emptyList(),

    /** Movements on both lists, said once. Part 6: done once, counts for both. */
    val onBothLists: String? = null,

    /** A plan movement that clashes with something the person avoids. Flagged, not dropped. */
    val toAskAbout: List<String> = emptyList(),
)

@Composable
fun SessionCard(
    state: SessionCardState,
    onGo: () -> Unit,
    onSomethingSmall: () -> Unit,
    onWithoutThePhone: () -> Unit,
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
            text = state.theirs ?: stringResource(R.string.card_label),
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
        state.onBothLists?.let {
            SteadyText(text = it, style = SteadyType.Body, color = SteadyPalette.Sand)
        }
        state.toAskAbout.forEach {
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

        // ADDENDUM-03 Part 6: the app's own suggestions sit below the plan, clearly
        // separate, never merged and never counted as part of it. They are drawn in a
        // block of their own so that "theirs" and "ours" is visible at a glance and
        // not something somebody has to read carefully to work out.
        if (state.alsoMovements.isNotEmpty()) {
            SteadyText(
                text = stringResource(R.string.plan_also),
                style = SteadyType.CardTitle,
                color = SteadyPalette.Sand,
                modifier = Modifier.padding(top = SteadySpacing.Inside),
            )
            state.alsoMovements.forEach {
                SteadyText(text = it, style = SteadyType.Body, color = SteadyPalette.Sand)
            }
        }

        // ADDENDUM-03 Part 2, tiredness. Ninety seconds, and it counts as a session.
        // A quiet second line rather than a second button, because it is an out and
        // not a choice between two equal things.
        TextLink(
            label = stringResource(R.string.offer_got_a_minute),
            onClick = onSomethingSmall,
            modifier = Modifier.padding(top = SteadySpacing.Tight),
        )
        TextLink(label = stringResource(R.string.card_no_phone), onClick = onWithoutThePhone)
    }
}
