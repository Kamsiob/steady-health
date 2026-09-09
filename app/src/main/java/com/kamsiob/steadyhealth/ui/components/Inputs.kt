package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * A stepper: a value with a minus and a plus.
 *
 * A stepper rather than a slider wherever the number matters, because the
 * accessibility floor requires a tap alternative to every drag and this is the
 * version where the tap is the only gesture. Both buttons carry their own spoken
 * label, since "minus" on its own tells a screen reader user nothing about what
 * it decreases.
 */
@Composable
fun Stepper(
    label: String,
    value: String,
    supporting: String?,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
    downLabel: String = label,
    upLabel: String = label,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SteadyShapes.Card)
            .background(SteadyPalette.White)
            .padding(SteadySpacing.InsideTight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SteadyText(text = value, style = SteadyType.BlockValue, color = SteadyPalette.Navy)
            if (supporting != null) {
                SteadyText(text = supporting, style = SteadyType.Caption, color = SteadyPalette.Ink2)
            }
        }
        StepButton(glyph = "−", spoken = "less $downLabel", onClick = onDown)
        StepButton(glyph = "+", spoken = "more $upLabel", onClick = onUp)
    }
}

@Composable
private fun StepButton(glyph: String, spoken: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SteadySpacing.TapTarget)
            .clip(CircleShape)
            .background(SteadyPalette.Sand)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        SteadyText(text = glyph, style = SteadyType.SectionTitle, color = SteadyPalette.Navy)
    }
}

/**
 * A free text field, white with a sand outline.
 *
 * Single line where the answer is a phrase, because the return key should submit
 * rather than type a line break into a field the person will not scroll.
 *
 * [minHeight] is the tap target for a phrase and a box several lines deep where the
 * answer is a list. A list needs the room shown before anything is typed, because a
 * field one line tall asks for one line however the sentence above it is worded.
 */
@Composable
@Suppress("LongParameterList") // One field, one option for each way it is used.
fun TextEntry(
    value: String,
    onValue: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    minHeight: Dp = SteadySpacing.TapTarget,
    onSubmit: () -> Unit = {},
) {
    val align = if (singleLine) Alignment.CenterStart else Alignment.TopStart
    // A single line field is unchanged. A box several lines deep needs room above the
    // first line, or the text sits against the outline.
    val inset = if (singleLine) 0.dp else SteadySpacing.ListGap
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SteadyShapes.Card)
            .background(SteadyPalette.White)
            .border(SteadySpacing.Outline, SteadyPalette.Sand, SteadyShapes.Card)
            .padding(horizontal = SteadySpacing.InsideTight),
        contentAlignment = align,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            textStyle = SteadyType.Body.copy(color = SteadyPalette.Ink),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() },
                onGo = { onSubmit() },
                onSearch = { onSubmit() },
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .heightIn(min = minHeight)
                        .padding(vertical = inset),
                    contentAlignment = align,
                ) {
                    if (value.isEmpty()) {
                        SteadyText(text = hint, style = SteadyType.Body, color = SteadyPalette.Ink3Text)
                    }
                    inner()
                }
            },
        )
    }
}

/**
 * The week row: seven circles with day letters under them.
 *
 * Walked is orange, today is white with a navy ring, empty is the empty tone. The
 * ring is what carries "today" for anybody who does not see the colour, and the
 * whole row is one spoken sentence rather than seven unlabelled circles.
 */
@Composable
fun WeekRow(
    dayLetters: List<String>,
    walked: List<Boolean>,
    todayIndex: Int,
    spoken: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        dayLetters.forEachIndexed { index, letter ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            ) {
                Box(
                    modifier = Modifier
                        .size(DAY_CIRCLE)
                        .clip(CircleShape)
                        .background(
                            when {
                                walked.getOrNull(index) == true -> SteadyPalette.Orange
                                index == todayIndex -> SteadyPalette.White
                                else -> SteadyPalette.Empty
                            },
                        )
                        .then(
                            if (index == todayIndex) {
                                Modifier.border(TODAY_RING, SteadyPalette.Navy, CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                )
                SteadyText(
                    text = letter,
                    style = SteadyType.Caption,
                    color = SteadyPalette.Ink3Text,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val DAY_CIRCLE = 26.dp
private val TODAY_RING = 2.5.dp
