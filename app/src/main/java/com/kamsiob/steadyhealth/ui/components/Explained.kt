package com.kamsiob.steadyhealth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * L3: a number that can say what it is. ADDENDUM-03 Part 4, DESIGN.md 4b.
 *
 * "No number in this app is ever unexplained." The explanation opens under the number
 * as a block rather than over the screen as a dialog, because L1 established that
 * shape for the app explaining itself and one sentence does not need a whole surface.
 *
 * Every number a person sees goes through this. A figure with nothing to say about
 * itself is a figure that should not be on the screen.
 */
@Composable
fun Explained(
    text: String,
    explanation: String,
    style: TextStyle,
    colour: Color,
    modifier: Modifier = Modifier,
    tint: Color = SteadyPalette.Sand,
    explanationColour: Color = SteadyPalette.Ink2,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(R.string.explain_this)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
        ) {
            SteadyText(text = text, style = style, color = colour)
            Box(
                modifier = Modifier
                    .size(SteadySpacing.TapTarget)
                    .clickable(role = Role.Button) { open = !open }
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(DOT)
                        .background(tint, CircleShape)
                        .border(SteadySpacing.Outline, tint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SteadyText(
                        text = stringResource(R.string.explain_mark),
                        style = SteadyType.Caption,
                        color = explanationColour,
                    )
                }
            }
        }

        AnimatedVisibility(visible = open) {
            SteadyText(
                text = explanation,
                style = SteadyType.Body,
                color = explanationColour,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SteadySpacing.Tight)
                    .clip(RoundedCornerShape(SteadySpacing.Tight))
                    .background(tint)
                    .padding(SteadySpacing.InsideTight),
            )
        }
    }
}

/** Small enough to sit beside a number without competing with it. */
private val DOT = 24.dp
