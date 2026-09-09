@file:Suppress("MatchingDeclarationName") // One screen and the state it draws.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.session.ChairHeight
import com.kamsiob.steadyhealth.session.Kit
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.SwitchRow

/** What is in the room, and which chair. */
data class KitUiState(
    val kit: Set<Kit> = emptySet(),
    val chairHasArms: Boolean = false,
    val chairHeight: ChairHeight? = null,
)

/**
 * Equipment and the chair, changed after setup. ADDENDUM-03 Part 20.
 *
 * Both answers already existed and neither had a way back to it. O5 asks about the
 * chair once, in passing, on the screen after the first session, and the height is
 * asked inside a session at the moment it matters. Neither is a thing that stays
 * true: a band gets bought, a step gets moved, and the armchair somebody was using
 * is replaced by a dining chair. This is the screen where that gets said.
 *
 * The equipment list is longer than the one question at setup asks. Setup asks about
 * a chair because a chair is what the first fortnight needs; the step, the band and
 * the floor decide whether a whole part of the library is ever offered, and until
 * this screen there was no way to turn any of them on.
 */
@Composable
fun KitScreen(
    state: KitUiState,
    onKit: (Kit) -> Unit,
    onArms: (Boolean) -> Unit,
    onHeight: (ChairHeight) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.kit_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        SectionTitle(stringResource(R.string.kit_what))
        Paragraph(stringResource(R.string.kit_why))

        KIT_ROWS.forEach { (piece, label) ->
            ListItem(
                heading = stringResource(label),
                done = piece in state.kit,
                onClick = { onKit(piece) },
            )
        }

        // Only once there is a chair to have arms. A chair with arms is still a
        // chair: it changes which variant is offered, not whether anything is.
        if (Kit.Chair in state.kit) {
            SwitchRow(
                label = stringResource(R.string.kit_arms),
                subtitle = stringResource(R.string.kit_arms_sub),
                checked = state.chairHasArms,
                onChange = onArms,
            )

            SectionTitle(stringResource(R.string.kit_chair_height))
            CHAIR_ROWS.forEach { (height, label) ->
                ListItem(
                    heading = stringResource(label),
                    next = state.chairHeight == height,
                    onClick = { onHeight(height) },
                )
            }
            Paragraph(stringResource(R.string.kit_chair_height_why))
        }
    }
}

/**
 * The equipment, in the order somebody is likely to have it.
 *
 * A table rather than a `when`, because there is nothing here to decide. [Kit.None]
 * is not on it: it means a movement needs nothing, which is not something anybody
 * owns or can be without.
 */
private val KIT_ROWS = listOf(
    Kit.Chair to R.string.kit_chair,
    Kit.Wall to R.string.kit_wall,
    Kit.Step to R.string.kit_step,
    Kit.Band to R.string.kit_band,
    Kit.Weights to R.string.kit_weights,
    Kit.Floor to R.string.kit_floor,
)

/** The three heights, low to high, in the words the session already uses for them. */
private val CHAIR_ROWS = listOf(
    ChairHeight.Low to R.string.chair_low,
    ChairHeight.Level to R.string.chair_usual,
    ChairHeight.High to R.string.chair_high,
)
