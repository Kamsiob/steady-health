package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.session.Area
import com.kamsiob.steadyhealth.ui.components.AbilityTile
import com.kamsiob.steadyhealth.ui.components.CarryGlyph
import com.kamsiob.steadyhealth.ui.components.DailyCard
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.RiseGlyph
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SessionCard
import com.kamsiob.steadyhealth.ui.components.SessionCardState
import com.kamsiob.steadyhealth.ui.components.SteadyGlyph
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TalkGlyph
import com.kamsiob.steadyhealth.ui.components.TravelGlyph
import com.kamsiob.steadyhealth.ui.help.Place
import com.kamsiob.steadyhealth.ui.theme.Ability
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * The one question the app asks about an area that hurt, seven days later.
 *
 * Asked once and never repeated, because a suppressed movement that keeps asking to
 * come back is the app arguing with somebody about their own body.
 */
data class BringBack(val question: String, val area: Area)

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

    /** Today's session. Null only while it is still being planned. */
    val session: SessionCardState? = null,

    /** The one line the app noticed, or nothing at all. ADDENDUM-03 Part 9. */
    val noticed: String? = null,

    /** Said once, ever, when the same area has hurt twice in a month. */
    val worthAWord: String? = null,

    /** An area whose week is up, asked about once. */
    val bringBack: BringBack? = null,
) {
    val doneCount: Int
        get() = listOfNotNull(weighedIn.takeIf { weighsIn }, saidHowItWent, moved).count { it }

    /** Three, or two for somebody who is not weighing in. */
    val dailyCount: Int get() = if (weighsIn) ALL_THREE else ALL_THREE - 1

    val allDone: Boolean get() = doneCount == dailyCount
}

/**
 * Today. ADDENDUM-03 Part 2, folded into MASTER_SPEC 6.1.
 *
 * The session card is the screen. It says what today is, how long it takes and what
 * changed about it, and it carries the only button. Under it is one line the app
 * noticed in the person's own history, and then the four abilities, which are what
 * all of this is for.
 *
 * Weight is not here and never has been since the reframe: it appears in a sentence
 * beside an ability that changed, and nowhere else.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun TodayScreen(
    state: TodayUiState,
    onAbility: (AbilityDomain) -> Unit,
    onSayHow: () -> Unit,
    onGo: () -> Unit,
    onSomethingSmall: () -> Unit,
    onBringBack: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(title = null, onBack = null, modifier = modifier, help = Place.Today) {
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
                icon = R.drawable.ic_settings,
                spoken = stringResource(R.string.action_settings),
                onClick = onSettings,
            )
        }

        state.session?.let {
            SessionCard(state = it, onGo = onGo, onSomethingSmall = onSomethingSmall)
        }

        state.bringBack?.let { asking ->
            NoteBlock(asking.question)
            Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
                SecondaryButton(
                    label = stringResource(R.string.hurt_bring_back_yes),
                    onClick = { onBringBack(true) },
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    label = stringResource(R.string.hurt_bring_back_no),
                    onClick = { onBringBack(false) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        state.worthAWord?.let { NoteBlock(it) }

        state.noticed?.let { NoteBlock(it) }

        state.welcomeBack?.let { NoteBlock(it) }

        state.notice?.let {
            NoteBlock(
                text = it,
                tint = SteadyPalette.SkyL,
                onDismiss = onNotice,
                dismissLabel = stringResource(R.string.notice_ok),
            )
        }

        AbilityGrid(state.abilities, onAbility)

        DailyCard(
            title = stringResource(
                if (state.saidHowItWent) R.string.today_said else R.string.today_say,
            ),
            subtitle = stringResource(R.string.today_say_sub),
            tint = SteadyPalette.SkyL,
            done = state.saidHowItWent,
            onClick = onSayHow,
            glyph = { TalkGlyph() },
        )
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
            // The two in a row match each other's height, so a tile whose
            // sentence needs two lines takes its neighbour up with it rather
            // than putting an ellipsis through somebody's own words.
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
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
private val ACTION_ICON = 24.dp
