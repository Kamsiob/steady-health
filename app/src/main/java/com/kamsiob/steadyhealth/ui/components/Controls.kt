package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * The buttons, pills and selectors from DESIGN.md section 3.
 *
 * The primary button is filled orange-d rather than orange. White at 16/800 on
 * the approved orange is 2.54:1, which fails even the 3.0 Android allows at that
 * size and weight; orange-d gives 3.22. DESIGN.md section 2 carries the change and
 * `ContrastTest` holds it.
 */

/** Primary: orange-d, full round, white 16/800. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SteadyShapes.Round)
            .background(if (enabled) SteadyPalette.OrangeD else SteadyPalette.Sand)
            .heightIn(min = SteadySpacing.TapTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = BUTTON_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        SteadyText(
            text = label,
            style = SteadyType.Button,
            color = if (enabled) SteadyPalette.White else SteadyPalette.Ink3Text,
            textAlign = TextAlign.Center,
        )
    }
}

/** Secondary: white with a 2 dp sand inset outline, navy text. */
@Composable
fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SteadyShapes.Round)
            .background(SteadyPalette.White)
            .border(SteadySpacing.Outline, SteadyPalette.Sand, SteadyShapes.Round)
            .heightIn(min = SteadySpacing.TapTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = BUTTON_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        SteadyText(
            text = label,
            style = SteadyType.Button,
            color = SteadyPalette.Navy,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A pill. White with a sand outline, navy when it is the one chosen.
 *
 * [asChoice] decides what a screen reader hears. Most pills are one of a set
 * somebody picks between and announce as a radio button that is chosen or not. A
 * few are not choices at all, and announcing a link as "radio button, not chosen"
 * tells somebody using TalkBack they failed to select something when what is
 * there is a way to go somewhere.
 */
@Composable
fun Pill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    asChoice: Boolean = true,
) {
    val base = modifier
        .clip(SteadyShapes.Round)
        .background(if (selected) SteadyPalette.Navy else SteadyPalette.White)
        .then(
            if (selected) {
                Modifier
            } else {
                Modifier.border(SteadySpacing.Outline, SteadyPalette.Sand, SteadyShapes.Round)
            },
        )
        .heightIn(min = SteadySpacing.TapTarget)

    Box(
        modifier = if (asChoice) {
            base
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { stateDescription = if (selected) CHOSEN else NOT_CHOSEN }
        } else {
            base.clickable(role = Role.Button, onClick = onClick)
        }.padding(horizontal = PILL_SIDE, vertical = PILL_TOP),
        contentAlignment = Alignment.Center,
    ) {
        SteadyText(
            text = label,
            style = SteadyType.CardTitle,
            color = if (selected) SteadyPalette.White else SteadyPalette.Navy,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Three equal cells, from DESIGN.md section 3: the capability questions, the day
 * rating, the talk test. Selected is butter for a choice about energy and green-l
 * with a green outline for the talk test.
 */
@Composable
fun ThreeUpChoice(
    options: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedFill: Color = SteadyPalette.Butter,
    selectedOutline: Color? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
    ) {
        options.forEachIndexed { index, option ->
            val chosen = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(SteadyShapes.ListItem)
                    .background(if (chosen) selectedFill else SteadyPalette.White)
                    .border(
                        SteadySpacing.Outline,
                        when {
                            chosen && selectedOutline != null -> selectedOutline
                            chosen -> selectedFill
                            else -> SteadyPalette.Sand
                        },
                        SteadyShapes.ListItem,
                    )
                    .heightIn(min = SteadySpacing.TapTarget)
                    .selectable(
                        selected = chosen,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    )
                    .semantics { stateDescription = if (chosen) CHOSEN else NOT_CHOSEN }
                    .padding(horizontal = SteadySpacing.Tight, vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                SteadyText(
                    text = option,
                    style = SteadyType.CardTitle,
                    color = SteadyPalette.Navy,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** A plain text link, for the rows DESIGN.md draws as words rather than as cards. */
@Composable
fun TextLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SteadyText(
        text = label,
        style = SteadyType.CardTitle,
        color = SteadyPalette.OrangeText,
        modifier = modifier
            .heightIn(min = SteadySpacing.TapTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 13.dp),
    )
}

private const val CHOSEN = "chosen"
private const val NOT_CHOSEN = "not chosen"
private val BUTTON_PADDING = 16.dp
private val PILL_SIDE = 14.dp
private val PILL_TOP = 10.dp
