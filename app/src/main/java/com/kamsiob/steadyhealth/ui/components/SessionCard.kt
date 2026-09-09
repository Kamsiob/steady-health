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
 * One therapist's plan on the card: their label and their own lines.
 *
 * ADDENDUM-03 Part 6: a physio plan and an OT plan coexist, each labelled, each
 * separate. A list of these rather than one list of movements is what makes that
 * true on the screen as well as in the database. With one plan the label is already
 * the eyebrow above the card and is not drawn again here.
 */
data class PlanOnCard(val label: String, val movements: List<String>)

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

    /**
     * The therapists' plans, each with its own lines. Part 6: never merged.
     *
     * Empty when nobody has given this person a plan, and then [movements] is the
     * app's own session instead.
     */
    val theirPlans: List<PlanOnCard> = emptyList(),

    /** The app's own suggestions, when a plan is what the card is showing. */
    val alsoMovements: List<String> = emptyList(),

    /**
     * Whether the app's own suggestions are on, or null when there is no plan.
     *
     * Part 6 gives the person one tap to turn the extras off entirely, so the tap
     * lives on the card beside the thing it turns off. Null means there is nothing
     * to be extra to and the row is not drawn at all.
     */
    val extrasOn: Boolean? = null,

    /**
     * "This one's on your physio's list too." Part 6: done once, counts for both.
     *
     * One sentence however many movements overlap, which is what Part 6's "the app
     * says so once" is read as here. Not once ever: the line explains why a movement
     * the person expected is missing from the suggestions below, and an explanation
     * of what is on the screen today has to be on the screen today. The sentence
     * that is said once ever is the other one, about whose plan this is, and that
     * one is a fact about the app rather than about this morning.
     */
    val onBothLists: String? = null,

    /** A plan movement that clashes with something the person avoids. Flagged, not dropped. */
    val toAskAbout: List<String> = emptyList(),
)

@Composable
@Suppress("LongParameterList") // One card, one callback for each thing on it.
fun SessionCard(
    state: SessionCardState,
    onGo: () -> Unit,
    onSomethingSmall: () -> Unit,
    onWithoutThePhone: () -> Unit,
    onExtras: () -> Unit,
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

        PlansOnCard(state.theirPlans)
        Notes(state)

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

        AlsoIfYouWantMore(state, onExtras)

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

/**
 * The therapists' plans, each under its own name once there is more than one.
 *
 * ADDENDUM-03 Part 6: a physio plan and an OT plan coexist, each labelled, each
 * separate. With a single plan the eyebrow at the top of the card has already said
 * whose it is, and a heading here as well would be the app labelling a list of one.
 */
@Composable
private fun PlansOnCard(plans: List<PlanOnCard>) {
    plans.forEach { plan ->
        if (plans.size > 1) {
            SteadyText(
                text = plan.label,
                style = SteadyType.CardTitle,
                color = SteadyPalette.Sand,
                modifier = Modifier.padding(top = SteadySpacing.Tight),
            )
        }
        plan.movements.forEach { name ->
            SteadyText(text = name, style = SteadyType.Body, color = SteadyPalette.White)
        }
    }
}

/**
 * Everything the card says about today's session under the movements themselves.
 *
 * Its own function because the card had grown past what detekt will accept in one
 * piece, and this is the part of it that is a list of optional sentences rather than
 * a layout. Order is the point: what the session has, then what changed about it,
 * then what is not in it and why.
 */
@Composable
private fun Notes(state: SessionCardState) {
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
}

/**
 * The app's own suggestions, and the one tap that ends them.
 *
 * ADDENDUM-03 Part 6: they sit below the plan, clearly separate, never merged and
 * never counted as part of it, so they are drawn as a block of their own and "theirs"
 * and "ours" is visible at a glance rather than something somebody has to read
 * carefully to work out.
 *
 * The tap is here, on the card, beside the thing it turns off. Part 6 says the person
 * can turn the extras off entirely in one tap, and the same switch sitting in the You
 * tab is a tab, a scroll and a switch. The row stays when they are off, saying so and
 * offering them back, because a tap that removes the only way to undo it is a trap.
 * It is not drawn at all when there is no plan, since there is nothing to be extra to.
 */
@Composable
private fun AlsoIfYouWantMore(state: SessionCardState, onExtras: () -> Unit) {
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

    state.extrasOn?.let { on ->
        TextLink(
            label = stringResource(if (on) R.string.plan_also_off else R.string.plan_also_on),
            onClick = onExtras,
            modifier = Modifier.padding(top = SteadySpacing.Tight),
        )
    }
}
