package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * The blocks and cards from DESIGN.md section 3.
 *
 * Every one of them merges its own text into a single semantics node, so a screen
 * reader announces "weighed in, done, 16:22" as one thing rather than reading a
 * label, a value and a state as three separate stops. That is the difference
 * between a screen that can be used with TalkBack and one that technically has
 * labels.
 */

/** A block: label above, value below, white or tinted. Two-up for pairs. */
@Composable
fun Block(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color = SteadyPalette.White,
    valueStyle: androidx.compose.ui.text.TextStyle = SteadyType.BlockValue,
    onClick: (() -> Unit)? = null,
) {
    val spoken = "$label, $value"
    Column(
        modifier = modifier
            .clip(SteadyShapes.Card)
            .background(tint)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(SteadySpacing.InsideTight)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
    ) {
        SteadyText(text = label, style = SteadyType.CardTitle, color = SteadyPalette.Ink2)
        SteadyText(text = value, style = valueStyle, color = SteadyPalette.Navy)
    }
}

/** Two blocks side by side, which is the only grid DESIGN.md has. */
@Composable
fun TwoUp(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        content = content,
    )
}

/**
 * A list item: 22 radius white, a 44 dp tinted glyph tile, heading, subtitle, and
 * an optional value on the right.
 *
 * Done carries a green-l outline and next is navy with white text, because
 * DESIGN.md's accessibility floor says every colour state also carries a shape or
 * a word. The outline is the shape; the caller supplies the word.
 */
@Composable
fun ListItem(
    heading: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    tileTint: Color = SteadyPalette.Sand,
    done: Boolean = false,
    next: Boolean = false,
    onClick: (() -> Unit)? = null,
    spokenAs: String? = null,
    glyph: (@Composable () -> Unit)? = null,
) {
    val background = if (next) SteadyPalette.Navy else SteadyPalette.White
    val headingColour = if (next) SteadyPalette.White else SteadyPalette.Navy
    val subtitleColour = if (next) SteadyPalette.White else SteadyPalette.Ink2
    val spoken = spokenAs ?: listOfNotNull(heading, subtitle, value).joinToString(", ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SteadyShapes.ListItem)
            .background(background)
            .then(
                if (done) {
                    Modifier.border(SteadySpacing.Outline, SteadyPalette.GreenL, SteadyShapes.ListItem)
                } else {
                    Modifier
                },
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .heightIn(min = SteadySpacing.TapTarget)
            .padding(SteadySpacing.Tight)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
    ) {
        if (glyph != null) {
            GlyphTile(tint = tileTint) { glyph() }
        }
        Column(modifier = Modifier.weight(1f)) {
            SteadyText(
                text = heading,
                style = SteadyType.ListItemHeading,
                color = headingColour,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                SteadyText(
                    text = subtitle,
                    style = SteadyType.Caption,
                    color = subtitleColour,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            SteadyText(
                text = value,
                style = SteadyType.ListItemHeading,
                color = headingColour,
            )
        }
    }
}

/**
 * One of the three daily cards. Three across, 150 dp, tinted, and white with a
 * green check disc when it is done.
 *
 * The morph from tint to white is the one signature moment DESIGN.md section 5
 * allows, and the check disc is what carries the state for anybody who does not
 * see the colour change.
 */
@Composable
fun DailyCard(
    title: String,
    subtitle: String,
    tint: Color,
    done: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    val spoken = if (done) "$title, done. $subtitle" else "$title. $subtitle"
    Box(
        modifier = modifier
            .height(DAILY_HEIGHT)
            .clip(SteadyShapes.Card)
            .background(if (done) SteadyPalette.White else tint)
            .then(
                if (done) {
                    Modifier.border(SteadySpacing.Outline, SteadyPalette.GreenL, SteadyShapes.Card)
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(SteadySpacing.Tight)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(SteadySpacing.GlyphInAbilityTile)) { glyph() }
            Spacer(Modifier.weight(1f))
            SteadyText(
                text = title,
                style = SteadyType.CardTitle,
                color = SteadyPalette.Navy,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SteadyText(
                text = subtitle,
                style = SteadyType.Caption,
                color = SteadyPalette.Ink2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (done) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(CHECK_DISC)
                    .background(SteadyPalette.Green, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CheckMark(modifier = Modifier.size(CHECK_MARK))
            }
        }
    }
}

/** The tick inside the done disc. A shape, so the state is not colour alone. */
@Composable
private fun CheckMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * TICK_START_X, size.height * TICK_MID_Y)
            lineTo(size.width * TICK_TURN_X, size.height * TICK_LOW_Y)
            lineTo(size.width * TICK_END_X, size.height * TICK_TOP_Y)
        }
        drawPath(
            path = path,
            color = SteadyPalette.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = size.width * TICK_STROKE,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

private val DAILY_HEIGHT = 150.dp
private val CHECK_DISC = 26.dp
private val CHECK_MARK = 14.dp
private const val TICK_START_X = 0.16f
private const val TICK_TURN_X = 0.42f
private const val TICK_END_X = 0.84f
private const val TICK_MID_Y = 0.54f
private const val TICK_LOW_Y = 0.78f
private const val TICK_TOP_Y = 0.26f
private const val TICK_STROKE = 0.16f
