package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * The four ability components from DESIGN.md section 3.
 *
 * The rule that shapes all of them: **Same is never styled as lesser.** It has the
 * same type, the same size and the same weight as Better, and only the pill
 * colour differs. Quieter uses the same pill colour as Same rather than one of
 * its own, because an alarm colour is exactly what the app must not do with it.
 *
 * Holding a number for a year is not a flat line. Strength falls between one and
 * a half and five percent a year from midlife if nothing is done, so Same is a
 * year of that not happening, and the interface says so by refusing to whisper it.
 */

/** The pill on the right of an ability: Better, Same, or Quieter. */
@Composable
fun StatePill(state: AbilityState, label: String, modifier: Modifier = Modifier) {
    val fill = when (state) {
        AbilityState.Better -> SteadyPalette.GreenL
        AbilityState.Same, AbilityState.Quieter -> SteadyPalette.Sand
    }
    val ink = when (state) {
        AbilityState.Better -> SteadyPalette.GreenText
        AbilityState.Same, AbilityState.Quieter -> SteadyPalette.Navy
    }
    Box(
        modifier = modifier
            .clip(SteadyShapes.Round)
            .background(fill)
            .padding(horizontal = PILL_SIDE, vertical = PILL_TOP),
    ) {
        SteadyText(text = label, style = SteadyType.Caption, color = ink)
    }
}

/**
 * An ability tile. Four in a two by two grid on Today, tinted by ability, with one
 * sentence about life.
 */
@Composable
fun AbilityTile(
    name: String,
    lifeSentence: String,
    tint: Color,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    newLabel: String = "",
    onClick: (() -> Unit)? = null,
    glyph: @Composable () -> Unit,
) {
    val spoken = listOfNotNull(name, lifeSentence.ifBlank { null }, newLabel.takeIf { isNew })
        .joinToString(", ")
    Box(
        modifier = modifier
            // A minimum rather than a fixed height. Two of these sit side by
            // side and a Row makes them match, so they stay equal; what changes
            // is that a two-line sentence gets its second line instead of an
            // ellipsis through the middle of the person's own words, and text at
            // twice the size has somewhere to go.
            .heightIn(min = TILE_HEIGHT)
            .fillMaxHeight()
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
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(SteadySpacing.GlyphInAbilityTile)) { glyph() }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            SteadyText(text = name, style = SteadyType.ListItemHeading, color = SteadyPalette.Navy)
            if (lifeSentence.isNotBlank()) {
                SteadyText(
                    text = lifeSentence,
                    style = SteadyType.Caption,
                    color = SteadyPalette.Ink2,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(SteadyShapes.Round)
                    .background(SteadyPalette.White)
                    .padding(horizontal = NEW_SIDE, vertical = NEW_TOP),
            ) {
                SteadyText(text = newLabel, style = SteadyType.Caption, color = SteadyPalette.Navy)
            }
        }
    }
}

/** An ability row, in the Abilities tab: glyph tile, name, one line, state pill. */
@Composable
fun AbilityRow(
    name: String,
    lifeSentence: String,
    tint: Color,
    state: AbilityState,
    stateLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glyph: @Composable () -> Unit,
) {
    val spoken = listOfNotNull(name, lifeSentence.ifBlank { null }, stateLabel).joinToString(", ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SteadyShapes.ListItem)
            .background(SteadyPalette.White)
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
        GlyphTile(tint = tint) { glyph() }
        Column(modifier = Modifier.weight(1f)) {
            SteadyText(text = name, style = SteadyType.ListItemHeading, color = SteadyPalette.Navy)
            if (lifeSentence.isNotBlank()) {
                SteadyText(
                    text = lifeSentence,
                    style = SteadyType.Caption,
                    color = SteadyPalette.Ink2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        StatePill(state = state, label = stateLabel)
    }
}

/**
 * The life card: what the person can do now, and what it was before.
 *
 * The sentence pattern from DESIGN.md section 6, and the reason the app exists.
 * Never the instrument's language, never the seconds first.
 */
@Composable
fun LifeCard(
    sentence: String,
    before: String,
    tint: Color,
    modifier: Modifier = Modifier,
    glyph: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SteadyShapes.LifeCard)
            .background(SteadyPalette.White)
            .padding(SteadySpacing.Inside)
            .semantics(mergeDescendants = true) { contentDescription = "$sentence $before" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
    ) {
        Box(
            modifier = Modifier
                .size(SteadySpacing.GlyphInCard)
                .clip(SteadyShapes.GlyphTile)
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(SteadySpacing.GlyphInTile)) { glyph() }
        }
        Column(modifier = Modifier.weight(1f)) {
            SteadyText(text = sentence, style = SteadyType.SectionTitle, color = SteadyPalette.Navy)
            if (before.isNotBlank()) {
                SteadyText(text = before, style = SteadyType.Caption, color = SteadyPalette.Ink2)
            }
        }
    }
}

/**
 * The rating row: ten flat blocks filled to the person's own number.
 *
 * This is the Patient-Specific Functional Scale, and the app never says so. Ten
 * blocks rather than a slider because a slider needs a drag and the accessibility
 * floor says every drag has a tap alternative; here the tap is the only gesture.
 */
@Composable
fun RatingRow(
    rating: Int,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    previous: Int? = null,
    label: String = "",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BLOCK_GAP),
        ) {
            for (step in 1..STEPS) {
                val filled = step <= rating
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(BLOCK_HEIGHT)
                        .clip(SteadyShapes.GlyphTile)
                        .background(if (filled) SteadyPalette.Orange else SteadyPalette.Empty)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "$label $step",
                            onClick = { onRate(step) },
                        ),
                )
            }
        }
        SteadyText(
            text = if (previous == null) "$rating" else "$rating, was $previous",
            style = SteadyType.Caption,
            color = SteadyPalette.Ink2,
        )
    }
}

private val TILE_HEIGHT = 120.dp
private val PILL_SIDE = 10.dp
private val PILL_TOP = 5.dp
private val NEW_SIDE = 8.dp
private val NEW_TOP = 4.dp
private val BLOCK_HEIGHT = 26.dp
private val BLOCK_GAP = 4.dp
private const val STEPS = 10
