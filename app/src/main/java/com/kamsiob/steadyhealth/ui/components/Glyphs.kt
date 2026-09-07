package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes

/**
 * The illustration language from DESIGN.md section 2, and nothing outside it.
 *
 * "Every glyph is a circle, an arc, a dotted path, a bubble, or a rounded
 * rectangle." No faces, no doodles, no cartoon people, no stick figures, and never
 * a hand-drawn silhouette. That is a short enough list to hold in one file, which
 * is the point: a screen that needs a picture picks one of these rather than
 * inventing a sixth kind of drawing.
 *
 * The three daily glyphs are fixed and never swap, because somebody learns that
 * the blue bubble is the one where they say how the day went long before they read
 * the words.
 */

/**
 * Weigh in: a filled dot in a sand circle.
 *
 * Every glyph fills the slot it is given unless the caller says otherwise, which
 * is why the canvas starts from `fillMaxSize()` and puts the caller's modifier
 * after it. A glyph with no constraints draws nothing at all, and a missing icon
 * reads as a broken app rather than as a bug in a default.
 */
@Composable
fun WeighGlyph(
    modifier: Modifier = Modifier,
    ring: Color = SteadyPalette.Sand,
    dot: Color = SteadyPalette.Orange,
) {
    Canvas(Modifier.fillMaxSize().then(modifier)) {
        drawCircle(ring, radius = size.minDimension / 2f)
        drawCircle(dot, radius = size.minDimension * DOT_RATIO)
    }
}

/** Say how today went: a sky speech bubble with two white dots. */
@Composable
fun TalkGlyph(
    modifier: Modifier = Modifier,
    bubble: Color = SteadyPalette.Sky,
    dots: Color = SteadyPalette.White,
) {
    Canvas(Modifier.fillMaxSize().then(modifier)) {
        val w = size.width
        val h = size.height
        val body = Size(w, h * BUBBLE_BODY)
        val corner = androidx.compose.ui.geometry.CornerRadius(w * BUBBLE_CORNER)
        drawRoundRect(color = bubble, size = body, cornerRadius = corner)

        // The tail, a triangle under the left of the body.
        val tail = Path().apply {
            moveTo(w * TAIL_LEFT, h * BUBBLE_BODY)
            lineTo(w * TAIL_RIGHT, h * BUBBLE_BODY)
            lineTo(w * TAIL_LEFT, h)
            close()
        }
        drawPath(tail, bubble)

        val dotY = h * BUBBLE_BODY * HALF
        val dotR = w * SPEECH_DOT
        drawCircle(dots, dotR, Offset(w * DOT_ONE, dotY))
        drawCircle(dots, dotR, Offset(w * DOT_TWO, dotY))
    }
}

/** Move: a green dotted arc ending in a dot. */
@Composable
fun MoveGlyph(
    modifier: Modifier = Modifier,
    line: Color = SteadyPalette.Green,
) {
    Canvas(Modifier.fillMaxSize().then(modifier)) {
        val w = size.width
        val h = size.height
        val arc = Path().apply {
            moveTo(0f, h * ARC_START_Y)
            cubicTo(w * ARC_C1, h * ARC_START_Y, w * ARC_C2, h * ARC_TOP, w * HALF, h * ARC_TOP)
            cubicTo(w * ARC_C3, h * ARC_TOP, w * ARC_C4, h * ARC_START_Y, w, h * ARC_START_Y)
        }
        drawPath(
            path = arc,
            color = line,
            style = Stroke(
                width = w * ARC_STROKE,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(w * DASH_ON, w * DASH_OFF),
                    0f,
                ),
            ),
        )
        drawCircle(line, w * END_DOT, Offset(w, h * ARC_START_Y))
    }
}

/**
 * The sun: two concentric circles. The hero art, DESIGN.md section 2.
 */
fun DrawScope.drawSun(centre: Offset, outerRadius: Float) {
    drawCircle(SteadyPalette.OrangeL, radius = outerRadius, center = centre)
    drawCircle(SteadyPalette.Peach, radius = outerRadius * SUN_INNER, center = centre)
}

/** The moon: one butter circle, with small stars scattered around it. */
fun DrawScope.drawMoon(centre: Offset, radius: Float, stars: List<Triple<Float, Float, Float>>) {
    drawCircle(SteadyPalette.Butter, radius = radius, center = centre)
    stars.forEach { (x, y, r) ->
        drawCircle(SteadyPalette.White, radius = r, center = Offset(x, y), alpha = STAR_ALPHA)
    }
}

/**
 * The four ability glyphs.
 *
 * They have to be four different shapes rather than one shape in four colours,
 * because the accessibility floor says nothing is carried by colour alone, and
 * because a person learns the tile by its shape. They also have to avoid the
 * three fixed daily glyphs: the dot in a circle, the speech bubble and the dotted
 * arc already mean weigh, say and move, and reusing one here would make the same
 * drawing mean two things on the same screen.
 *
 * Get up rises. Go travels. Carry is a weight held. Steady is a point held still.
 */

/** Get up: an arc rising from low left to high right. */
@Composable
fun RiseGlyph(modifier: Modifier = Modifier, line: Color = SteadyPalette.OrangeD) {
    Canvas(Modifier.fillMaxSize().then(modifier)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * EDGE, h * (1f - EDGE))
            cubicTo(
                x1 = w * RISE_C1X,
                y1 = h * RISE_C1Y,
                x2 = w * RISE_C2X,
                y2 = h * RISE_C2Y,
                x3 = w * (1f - EDGE),
                y3 = h * EDGE,
            )
        }
        drawPath(
            path = path,
            color = line,
            style = Stroke(width = w * THICK, cap = StrokeCap.Round),
        )
        drawCircle(line, w * TIP, Offset(w * (1f - EDGE), h * EDGE))
    }
}

/** Go: a dotted line travelling across, level rather than arced. */
@Composable
fun TravelGlyph(modifier: Modifier = Modifier, line: Color = SteadyPalette.Green) {
    Canvas(Modifier.fillMaxSize().then(modifier)) {
        val w = size.width
        val y = size.height * HALF
        drawLine(
            color = line,
            start = Offset(w * EDGE, y),
            end = Offset(w * (1f - EDGE * 2f), y),
            strokeWidth = w * THICK,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * DASH_ON, w * DASH_OFF), 0f),
        )
        drawCircle(line, w * TIP, Offset(w * (1f - EDGE), y))
    }
}

/** Carry: a rounded rectangle, held. */
@Composable
fun CarryGlyph(modifier: Modifier = Modifier, fill: Color = SteadyPalette.Sky) {
    Canvas(Modifier.fillMaxSize().then(modifier)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = fill,
            topLeft = Offset(w * BOX_INSET, h * BOX_TOP),
            size = Size(w * (1f - BOX_INSET * 2f), h * BOX_HEIGHT),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * BOX_CORNER),
        )
        // The handle, so it reads as something carried rather than a card.
        drawArc(
            color = fill,
            startAngle = HANDLE_START,
            sweepAngle = HANDLE_SWEEP,
            useCenter = false,
            topLeft = Offset(w * HANDLE_INSET, h * HANDLE_TOP),
            size = Size(w * (1f - HANDLE_INSET * 2f), h * HANDLE_HEIGHT),
            style = Stroke(width = w * THIN, cap = StrokeCap.Round),
        )
    }
}

/** Steady: a point held inside a ring. */
@Composable
fun SteadyGlyph(
    modifier: Modifier = Modifier,
    ring: Color = SteadyPalette.Plum,
    dot: Color = SteadyPalette.Navy,
) {
    Canvas(Modifier.fillMaxSize().then(modifier)) {
        val radius = size.minDimension * HALF
        drawCircle(ring, radius = radius, style = Stroke(width = size.width * THICK))
        drawCircle(dot, radius = radius * INNER_DOT)
    }
}

/** The size a glyph is drawn at inside a card, from DESIGN.md section 2. */
val GlyphInCard = 52.dp
val GlyphInTile = 28.dp

/** A glyph in its rounded tile, which is how a list row carries one. */
@Composable
fun GlyphTile(
    tint: Color,
    modifier: Modifier = Modifier,
    glyph: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(TILE)
            .clip(SteadyShapes.GlyphTile)
            .background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(GlyphInTile)) { glyph() }
    }
}

private val TILE = 44.dp

private const val DOT_RATIO = 0.22f
private const val BUBBLE_BODY = 0.74f
private const val BUBBLE_CORNER = 0.26f
private const val TAIL_LEFT = 0.24f
private const val TAIL_RIGHT = 0.46f
private const val SPEECH_DOT = 0.075f
private const val DOT_ONE = 0.36f
private const val DOT_TWO = 0.62f
private const val HALF = 0.5f
private const val ARC_C1 = 0.25f
private const val ARC_C2 = 0.22f
private const val ARC_C3 = 0.78f
private const val ARC_C4 = 0.75f
private const val ARC_START_Y = 0.78f
private const val ARC_TOP = 0.24f
private const val ARC_STROKE = 0.11f
private const val DASH_ON = 0.02f
private const val DASH_OFF = 0.13f
private const val END_DOT = 0.1f
private const val SUN_INNER = 0.643f
private const val EDGE = 0.12f
private const val RISE_C1X = 0.35f
private const val RISE_C1Y = 0.86f
private const val RISE_C2X = 0.5f
private const val RISE_C2Y = 0.3f
private const val THICK = 0.1f
private const val THIN = 0.075f
private const val TIP = 0.09f
private const val BOX_INSET = 0.14f
private const val BOX_TOP = 0.42f
private const val BOX_HEIGHT = 0.44f
private const val BOX_CORNER = 0.12f
private const val HANDLE_INSET = 0.3f
private const val HANDLE_TOP = 0.2f
private const val HANDLE_HEIGHT = 0.36f
private const val HANDLE_START = 180f
private const val HANDLE_SWEEP = 180f
private const val INNER_DOT = 0.42f
private const val STAR_ALPHA = 0.6f
