package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.ui.components.AbilityTile
import com.kamsiob.steadyhealth.ui.components.CarryGlyph
import com.kamsiob.steadyhealth.ui.components.DailyCard
import com.kamsiob.steadyhealth.ui.components.Hero
import com.kamsiob.steadyhealth.ui.components.HeroExplain
import com.kamsiob.steadyhealth.ui.components.HeroLabel
import com.kamsiob.steadyhealth.ui.components.HeroNumber
import com.kamsiob.steadyhealth.ui.components.HeroSky
import com.kamsiob.steadyhealth.ui.components.MoveGlyph
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.RiseGlyph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyGlyph
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TalkGlyph
import com.kamsiob.steadyhealth.ui.components.TravelGlyph
import com.kamsiob.steadyhealth.ui.components.WeekRow
import com.kamsiob.steadyhealth.ui.components.WeighGlyph
import com.kamsiob.steadyhealth.ui.theme.Ability
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** One ability, as Today shows it. */
data class AbilityTileState(
    val domain: AbilityDomain,
    val name: String,
    val lifeSentence: String,
    val isNew: Boolean = false,
)

/** What Today draws. */
data class TodayUiState(
    val date: String = "",
    val greeting: String = "",
    val abilities: List<AbilityTileState> = emptyList(),
    val weightValue: String? = null,
    val weightUnit: String? = null,
    val weightExplain: String = "",
    val morning: Boolean = true,
    val weighedIn: Boolean = false,
    val saidHowItWent: Boolean = false,
    val moved: Boolean = false,
    val nextWalkName: String = "",
    val dayLetters: List<String> = emptyList(),
    val walkedThisWeek: List<Boolean> = emptyList(),
    val todayIndex: Int = 0,
    val daysWalked: Int = 0,

    /**
     * False for somebody mostly in bed, where a daily weight is not part of the
     * picture. The screen then says so once, in a block, rather than showing a
     * card nobody can do. Grid screen 7.
     */
    val weighsIn: Boolean = true,

    /** What the Move card says, which is not the same for all four ways. */
    val moveTitle: String = "",
    val moveSubtitle: String = "",

    /** The label above the next thing: "Your next walk", "Today, for Go". */
    val nextLabel: String = "",

    /** Shown once when somebody comes back after time away. Never a loss. */
    val welcomeBack: String? = null,

    /** One sentence the app owes the person, until they say they have read it. */
    val notice: String? = null,
) {
    val doneCount: Int
        get() = listOfNotNull(weighedIn.takeIf { weighsIn }, saidHowItWent, moved).count { it }

    /** Three, or two for somebody who is not weighing in. */
    val dailyCount: Int get() = if (weighsIn) ALL_THREE else ALL_THREE - 1

    val allDone: Boolean get() = doneCount == dailyCount
}

/**
 * Today, from the grid, screens 5 to 7.
 *
 * The abilities lead and the weight follows, which is the whole reframe in one
 * layout. The four tiles sit at the top with a sentence about life in each; the
 * weight is a block below them, not a hero, because it is a lever rather than the
 * score.
 *
 * The three daily things keep their fixed tints and their fixed order. Somebody
 * learns where the blue one is long before they read the words on it.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    onAbility: (AbilityDomain) -> Unit,
    onWeighIn: () -> Unit,
    onSayHow: () -> Unit,
    onMove: () -> Unit,
    onAsk: () -> Unit,
    onSettings: () -> Unit,
    onNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(title = null, onBack = null, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = SteadySpacing.ListGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SteadyText(text = state.date, style = SteadyType.Caption, color = SteadyPalette.Ink3Text)
                SteadyText(
                    text = state.greeting,
                    style = SteadyType.Greeting,
                    color = SteadyPalette.Navy,
                    modifier = Modifier.semantics { heading() },
                )
            }
            RoundAction(
                icon = R.drawable.ic_question,
                spoken = stringResource(R.string.action_ask),
                onClick = onAsk,
            )
            RoundAction(
                icon = R.drawable.ic_settings,
                spoken = stringResource(R.string.action_settings),
                onClick = onSettings,
            )
        }

        AbilityGrid(state.abilities, onAbility)

        if (state.weighsIn && state.weightValue != null) {
            Hero(sky = if (state.morning) HeroSky.Morning else HeroSky.Evening) {
                HeroLabel(stringResource(R.string.weight_label))
                HeroNumber(value = state.weightValue, unit = state.weightUnit)
                if (state.weightExplain.isNotBlank()) HeroExplain(state.weightExplain)
            }
        }

        state.welcomeBack?.let { NoteBlock(it) }

        state.notice?.let {
            NoteBlock(
                text = it,
                tint = SteadyPalette.SkyL,
                onDismiss = onNotice,
                dismissLabel = stringResource(R.string.notice_ok),
            )
        }

        SectionTitle(text = stringResource(R.string.today_section), aside = doneAside(state))

        DailyThree(state = state, onWeighIn = onWeighIn, onSayHow = onSayHow, onMove = onMove)

        if (!state.weighsIn) NoteBlock(stringResource(R.string.today_weighing_off))

        if (state.nextWalkName.isNotBlank()) NextThing(state = state, onMove = onMove)

        if (state.dayLetters.isNotEmpty()) {
            WeekRow(
                dayLetters = state.dayLetters,
                walked = state.walkedThisWeek,
                todayIndex = state.todayIndex,
                spoken = pluralStringResource(R.plurals.week_row_spoken, state.daysWalked, state.daysWalked),
            )
        }
    }
}

/**
 * The four abilities, two by two, leading the screen.
 *
 * Two by two and never one by four, because the four are equals and a list would
 * put one of them first.
 */
@Composable
private fun AbilityGrid(abilities: List<AbilityTileState>, onAbility: (AbilityDomain) -> Unit) {
    abilities.chunked(2).forEach { pair ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        ) {
            pair.forEach { ability ->
                AbilityTile(
                    name = ability.name,
                    lifeSentence = ability.lifeSentence,
                    tint = tintFor(ability.domain),
                    modifier = Modifier.weight(1f),
                    onClick = { onAbility(ability.domain) },
                    glyph = { AbilityGlyph(ability.domain) },
                )
            }
            if (pair.size == 1) Box(Modifier.weight(1f))
        }
    }
}

/** "2 of 3 done", "All three done", or "Both done" for somebody not weighing in. */
@Composable
private fun doneAside(state: TodayUiState): String = when {
    !state.allDone -> stringResource(R.string.today_progress, state.doneCount, state.dailyCount)
    state.weighsIn -> stringResource(R.string.today_all_done)
    else -> stringResource(R.string.today_all_done_two)
}

/**
 * The dark row at the bottom of Today: what the next one is, and a way in.
 *
 * Its own composable because the label above the name is not always "Your next
 * walk". It is "Today, for Go" in a wheelchair and "Two minutes in bed" at the
 * other end, and the state carries the words rather than the screen choosing.
 */
@Composable
private fun NextThing(state: TodayUiState, onMove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SteadyShapes.LifeCard)
            .background(SteadyPalette.Navy)
            .clickable(role = Role.Button, onClick = onMove)
            .padding(SteadySpacing.Inside),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SteadyText(
                text = state.nextLabel,
                style = SteadyType.CardTitle,
                color = SteadyPalette.White.copy(alpha = LABEL_ALPHA),
            )
            SteadyText(
                text = state.nextWalkName,
                style = SteadyType.SectionTitle,
                color = SteadyPalette.White,
            )
        }
        Box(
            modifier = Modifier
                .clip(SteadyShapes.Round)
                .background(SteadyPalette.OrangeD)
                .padding(horizontal = GO_SIDE, vertical = GO_TOP),
        ) {
            SteadyText(
                text = stringResource(R.string.today_go),
                style = SteadyType.Button,
                color = SteadyPalette.White,
            )
        }
    }
}

/**
 * The three daily things, in their fixed order with their fixed tints.
 *
 * A section of its own because it is one idea, not because a counter asked for
 * it: the order and the tints never change, and keeping them in one place is
 * what makes that easy to see.
 */
@Composable
private fun DailyThree(
    state: TodayUiState,
    onWeighIn: () -> Unit,
    onSayHow: () -> Unit,
    onMove: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
        if (state.weighsIn) {
            DailyCard(
                title = stringResource(
                    if (state.weighedIn) R.string.today_weighed_in else R.string.today_weigh_in,
                ),
                subtitle = stringResource(R.string.today_weigh_sub),
                tint = SteadyPalette.Sand,
                done = state.weighedIn,
                modifier = Modifier.weight(1f),
                onClick = onWeighIn,
            ) { WeighGlyph() }
        }

        DailyCard(
            title = stringResource(
                if (state.saidHowItWent) R.string.today_said else R.string.today_say,
            ),
            subtitle = stringResource(R.string.today_say_sub),
            tint = SteadyPalette.SkyL,
            done = state.saidHowItWent,
            modifier = Modifier.weight(1f),
            onClick = onSayHow,
        ) { TalkGlyph() }

        DailyCard(
            title = if (state.moved) stringResource(R.string.today_moved) else state.moveTitle,
            subtitle = state.moveSubtitle,
            tint = SteadyPalette.GreenL,
            done = state.moved,
            modifier = Modifier.weight(1f),
            onClick = onMove,
        ) { MoveGlyph() }
    }
}

@Composable
private fun RoundAction(icon: Int, spoken: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SteadySpacing.TapTarget)
            .clickable(role = Role.Button, onClickLabel = spoken, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = spoken,
            tint = SteadyPalette.Navy,
            modifier = Modifier.size(ACTION_ICON),
        )
    }
}

/** The tint for an ability, from DESIGN.md section 2. They never swap. */
fun tintFor(domain: AbilityDomain) = when (domain) {
    AbilityDomain.GetUp -> Ability.GetUp.tint
    AbilityDomain.Go -> Ability.Go.tint
    AbilityDomain.Carry -> Ability.Carry.tint
    AbilityDomain.Steady -> Ability.Steady.tint
}

/**
 * The glyph for an ability. Four different shapes, not one shape in four colours,
 * because nothing in this app is carried by colour alone.
 */
@Composable
fun AbilityGlyph(domain: AbilityDomain, modifier: Modifier = Modifier) {
    when (domain) {
        AbilityDomain.GetUp -> RiseGlyph(modifier)
        AbilityDomain.Go -> TravelGlyph(modifier)
        AbilityDomain.Carry -> CarryGlyph(modifier)
        AbilityDomain.Steady -> SteadyGlyph(modifier)
    }
}

private const val ALL_THREE = 3
private const val LABEL_ALPHA = 0.92f
private val ACTION_ICON = 24.dp
private val GO_SIDE = 18.dp
private val GO_TOP = 10.dp
