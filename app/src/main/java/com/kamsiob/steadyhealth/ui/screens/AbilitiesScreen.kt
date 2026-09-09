package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.ui.components.AbilityRow
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TextLink
import com.kamsiob.steadyhealth.ui.help.Place
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** One of the four, as the Abilities tab shows it. */
data class AbilityRowState(
    val domain: AbilityDomain,
    val name: String,
    val lifeSentence: String,
    val state: AbilityState,
)

/** One thing the person said they want, with where they rated it. */
data class TrackedItemState(
    val text: String,
    val domain: AbilityDomain,
    val rating: Int,
    /** The rating as it is shown: a number, or a word when numbers are off. */
    val said: String = "",
)

/** What the Abilities tab draws. */
data class AbilitiesUiState(
    val abilities: List<AbilityRowState> = emptyList(),
    val items: List<TrackedItemState> = emptyList(),

    /**
     * True until the first monthly check has happened, and false ever after.
     *
     * It draws the one sentence that says why the list has no numbers beside it yet.
     * Once there has been a check the sentence is not true any more, so it goes.
     */
    val waiting: Boolean = true,

    /**
     * The Sunday write-up, when there is a week worth reading back.
     *
     * It lives on this tab rather than on Today because it is read-back, and
     * read-back is what this tab is for. Today is for today.
     */
    val week: List<String> = emptyList(),

    /** The last four weeks, oldest first, each against the number chosen. Part 10. */
    val weeks: List<WeekBar> = emptyList(),

    /** One sentence about those four weeks, and never about a run of them. */
    val weeksSaid: String = "",

    /** The same thing then and now, in their own history. Part 9. */
    val lookBack: String? = null,

    /**
     * The first month, then and now, for one thing they named. Part 16.
     *
     * One of the four places this app is warm, and it is here for a fortnight after
     * the first month and never again.
     */
    val firstMonth: FirstMonthCard? = null,

    /**
     * What the monthly check needs, in the words of this version of the app.
     *
     * Worded by the view model, which knows how the person gets around. The row used
     * to promise everybody a chair and a wall, which are two things the wheelchair
     * check and the bed check never ask for.
     */
    val checkLede: String = "",

    /**
     * Whether weighing in is switched on. MASTER_SPEC 6.1, ADDENDUM-03 Part 18.
     *
     * Weight is never on Today and never has a tab. Part 20 gives it a way in here
     * and here only, and it is the last thing on the screen because it is one of the
     * levers rather than the point.
     */
    val weighsIn: Boolean = false,

    /** True once a therapist's plan exists, which is the only time the page means anything. */
    val hasPlan: Boolean = false,

    /** True once there is a month with something in it to look back at. */
    val hasMonths: Boolean = false,
)

/** The first month card, already worded. Part 16, the second warm place. */
data class FirstMonthCard(val heading: String, val line: String)

/**
 * One week, as a bar. Nothing here joins it to the week beside it.
 *
 * [label] is what is printed under the bar, which is the count with numbers on and
 * nothing at all with them off. The bar keeps its real height either way, because
 * LOGIC.md says charts keep their shape and lose their axes, and a shape carries no
 * digits.
 */
data class WeekBar(val done: Int, val wanted: Int, val spoken: String, val label: String = "")

/**
 * Progress, from the grid's screen 8, carrying what ADDENDUM-03 Part 20 says it does.
 *
 * The order is Part 20's order and the order is the argument. What you said you want
 * comes first, because it is the reason any of this is happening. Then what changed,
 * then the weeks, then the look back, then a month, then the numbers, then weight if
 * it is on, then the things somebody might have come here to do.
 *
 * Same is drawn exactly as Better is drawn: same type, same size, same weight, and
 * only the pill colour differs, because holding a number for a year is the work
 * rather than the absence of it.
 *
 * Everything at the bottom is unbadged and unannounced. A card, a month, a page for
 * an appointment and one more thing you would like to be able to do are all things
 * somebody comes looking for rather than things the app should ask them about.
 */
@Composable
@Suppress("LongParameterList", "LongMethod") // One screen, one callback for each thing on it.
fun AbilitiesScreen(
    state: AbilitiesUiState,
    onAbility: (AbilityDomain) -> Unit,
    onCheck: () -> Unit,
    onSummary: () -> Unit,
    onTry: () -> Unit,
    onCard: () -> Unit,
    onMonths: () -> Unit,
    onAdd: () -> Unit,
    onWeight: () -> Unit,
    onWeighIn: () -> Unit,
    onTherapistPage: () -> Unit,
    modifier: Modifier = Modifier,
    tryOffer: Boolean = false,
    tryResult: Boolean = false,
) {
    SteadyScreen(title = null, onBack = null, modifier = modifier, help = Place.Progress) {
        SteadyText(
            text = stringResource(R.string.tab_progress),
            style = SteadyType.Greeting,
            color = SteadyPalette.Navy,
        )

        // Above everything, because for the fortnight it exists it is the thing worth
        // reading first, and after that it is gone.
        state.firstMonth?.let { card ->
            NoteBlock(heading = card.heading, text = card.line)
        }

        if (state.items.isNotEmpty()) {
            SectionTitle(stringResource(R.string.abilities_your_list))
            state.items.forEach { item ->
                ListItem(
                    heading = item.text,
                    // The rating is what the person said, reported back to them,
                    // so it is one of the figures that becomes a word. NumbersOff
                    // says why at length. Blank until the first check has asked.
                    subtitle = item.said.takeIf { it.isNotBlank() },
                    tileTint = tintFor(item.domain),
                    glyph = { AbilityGlyph(item.domain) },
                    value = null,
                )
            }

            // Directly under the list it is about. It used to sit nine blocks lower,
            // where it read as a sentence about the weeks above it rather than about
            // the rows with nothing yet under them.
            if (state.waiting) Paragraph(stringResource(R.string.abilities_waiting))
        }

        state.abilities.forEach { ability ->
            AbilityRow(
                name = ability.name,
                lifeSentence = ability.lifeSentence,
                tint = tintFor(ability.domain),
                state = ability.state,
                stateLabel = stringResource(labelFor(ability.state)),
                onClick = { onAbility(ability.domain) },
                glyph = { AbilityGlyph(ability.domain) },
            )
        }

        LookingBack(state = state, onMonths = onMonths)

        // Only when there is something to say. Never announced, never badged: an
        // app that nags somebody about an optional experiment has misunderstood
        // what the experiment is for.
        if (tryOffer || tryResult) {
            ListItem(
                heading = stringResource(
                    if (tryResult) R.string.try_result else R.string.try_action,
                ),
                subtitle = stringResource(R.string.settings_try_sub),
                next = true,
                onClick = onTry,
            )
        }

        ListItem(
            heading = stringResource(R.string.check_title),
            subtitle = state.checkLede.takeIf { it.isNotBlank() },
            onClick = onCheck,
        )

        // DESIGN.md puts this here as a text action rather than a card: it is a
        // door, not a thing to look at, and a badge on it would make a page about
        // somebody's body into something that nags them.
        TextLink(
            label = stringResource(R.string.summary_action),
            onClick = onSummary,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.week.isNotEmpty()) {
            SectionTitle(stringResource(R.string.abilities_your_week))
            state.week.forEach { paragraph -> Paragraph(paragraph) }
        }

        ProgressDoors(
            state = state,
            onWeight = onWeight,
            onWeighIn = onWeighIn,
            onCard = onCard,
            onAdd = onAdd,
            onTherapistPage = onTherapistPage,
        )
    }
}

/**
 * The three ways of looking back, in the order Part 20 lists them.
 *
 * The four weeks, then the look back card, then a way into one month. Its own
 * composable because they belong together and because the screen that holds them
 * carries eleven other things.
 *
 * Nothing joins any of them to any other. The four bars are four weeks with no line
 * between them, the look back card is one comparison the person's own history
 * already contains, and a month is a month.
 */
@Composable
private fun LookingBack(state: AbilitiesUiState, onMonths: () -> Unit) {
    // ADDENDUM-03 Part 10. Four bars, each its own week, with nothing joining them.
    if (state.weeks.isNotEmpty()) {
        SectionTitle(stringResource(R.string.weeks_title))
        Row(
            modifier = Modifier.fillMaxWidth().height(BAR_AREA),
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            verticalAlignment = Alignment.Bottom,
        ) {
            state.weeks.forEach { week -> WeekColumn(week, Modifier.weight(1f)) }
        }
        Paragraph(state.weeksSaid)
    }

    state.lookBack?.let {
        SectionTitle(stringResource(R.string.look_back_title))
        NoteBlock(it)
    }

    // Only once there is a month with something in it. Part 20 asks for the months,
    // and a month with nothing in it is not one.
    if (state.hasMonths) {
        ListItem(
            heading = stringResource(R.string.months_row),
            subtitle = stringResource(R.string.months_row_sub),
            onClick = onMonths,
        )
    }
}

/**
 * What can be done from Progress, at the bottom, unbadged and unannounced.
 *
 * Its own composable because two of the five only exist sometimes and because the
 * screen above them is already every section Part 20 names. Nothing here is counted
 * and nothing is offered twice: each is something somebody comes looking for rather
 * than something the app should ask them about.
 */
@Composable
private fun ProgressDoors(
    state: AbilitiesUiState,
    onWeight: () -> Unit,
    onWeighIn: () -> Unit,
    onCard: () -> Unit,
    onAdd: () -> Unit,
    onTherapistPage: () -> Unit,
) {
    // Weight, and only when it is switched on. MASTER_SPEC 6.1 keeps it off Today
    // and out of the tabs; Part 20 gives it this one way in. Weighing in is beside
    // the page rather than inside it, because writing a number down and reading the
    // line back are two different errands.
    if (state.weighsIn) {
        ListItem(
            heading = stringResource(R.string.weight_page_title),
            subtitle = stringResource(R.string.settings_weigh_in_sub),
            onClick = onWeight,
        )
        ListItem(
            heading = stringResource(R.string.today_weigh_in),
            subtitle = stringResource(R.string.today_weigh_sub),
            onClick = onWeighIn,
        )
    }

    ListItem(
        heading = stringResource(R.string.card_title),
        subtitle = stringResource(R.string.card_offer_sub),
        onClick = onCard,
    )

    // Part 18 makes the first job reusable: the same question setup asks, asked
    // again whenever somebody wants something else kept track of.
    ListItem(
        heading = stringResource(R.string.progress_add),
        subtitle = stringResource(R.string.progress_add_sub),
        onClick = onAdd,
    )

    // Only once there is a plan. A row offering to make a page about a plan nobody
    // has is a row that does nothing.
    if (state.hasPlan) {
        ListItem(heading = stringResource(R.string.plan_export), onClick = onTherapistPage)
    }
}

/**
 * One week, drawn against the number the person chose.
 *
 * The bar is how much of that week's own number was met and nothing else. There is no
 * line joining it to the week beside it and no total across the four, because there
 * is no such thing here as carrying anything over.
 */
@Composable
private fun WeekColumn(week: WeekBar, modifier: Modifier = Modifier) {
    val filled = if (week.wanted <= 0) 0f else (week.done.toFloat() / week.wanted).coerceIn(0f, 1f)
    Column(
        modifier = modifier.semantics { contentDescription = week.spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_AREA - BAR_LABEL)
                .clip(RoundedCornerShape(SteadySpacing.Tight))
                .background(SteadyPalette.Sand),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(filled.coerceAtLeast(EMPTY_SLIVER))
                    .clip(RoundedCornerShape(SteadySpacing.Tight))
                    .background(SteadyPalette.Green),
            )
        }
        SteadyText(
            text = week.label,
            style = SteadyType.Caption,
            color = SteadyPalette.Ink3Text,
            modifier = Modifier.height(BAR_LABEL),
        )
    }
}

/** Tall enough to read from across a room, short enough not to be the screen. */
private val BAR_AREA = 120.dp
private val BAR_LABEL = 24.dp

/** A week with nothing in it still draws something, so four bars are always four. */
private const val EMPTY_SLIVER = 0.04f

private fun labelFor(state: AbilityState) = when (state) {
    AbilityState.Better -> R.string.state_better
    AbilityState.Same -> R.string.state_same
    AbilityState.Quieter -> R.string.state_quieter
}
