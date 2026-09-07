package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.Hero
import com.kamsiob.steadyhealth.ui.components.HeroExplain
import com.kamsiob.steadyhealth.ui.components.HeroLabel
import com.kamsiob.steadyhealth.ui.components.HeroSky
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** One thing today's session is made of. */
data class MoveItem(val name: String, val instruction: String, val amount: String)

/** What Move draws. */
data class MoveUiState(
    val whatTheyWant: String = "",
    val walkName: String = "",
    val walkInstruction: String = "",
    val strength: MoveItem? = null,
)

/**
 * Move, from the grid, screens 16 to 18.
 *
 * What the person said they want sits at the top, in their own words, because it
 * is the reason any of this is happening and the app should not be the only one
 * who remembers it. Then today's two things: one strength set that feeds an
 * ability, and the walk.
 */
@Composable
fun MoveScreen(
    state: MoveUiState,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = null,
        onBack = null,
        modifier = modifier,
        footer = { PrimaryButton(label = stringResource(R.string.move_go), onClick = onGo) },
    ) {
        SteadyText(
            text = stringResource(R.string.tab_move),
            style = SteadyType.Greeting,
            color = SteadyPalette.Navy,
        )

        if (state.whatTheyWant.isNotBlank()) {
            SectionTitle(stringResource(R.string.move_what_you_want))
            Paragraph(text = state.whatTheyWant, color = SteadyPalette.Navy)
        }

        Hero(sky = HeroSky.Plain) {
            HeroLabel(stringResource(R.string.move_next))
            SteadyText(
                text = state.walkName,
                style = SteadyType.ScreenTitleBig,
                color = SteadyPalette.White,
            )
            if (state.walkInstruction.isNotBlank()) HeroExplain(state.walkInstruction)
        }

        state.strength?.let { set ->
            SectionTitle(stringResource(R.string.move_strength))
            ListItem(heading = set.name, subtitle = set.instruction, value = set.amount)
        }
    }
}
