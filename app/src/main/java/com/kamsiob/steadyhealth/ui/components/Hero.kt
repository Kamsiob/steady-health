@file:Suppress("MatchingDeclarationName")

package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** Which sky a hero wears. */
enum class HeroSky { Morning, Evening, Plain }

/**
 * The hero from DESIGN.md section 3: a colour block, a sun or moon and hills
 * behind, then a label, a number, a line, and an optional glass tag.
 *
 * The one hard problem here is that the words sit on the art. The approved render
 * draws white text over hills at 3.2:1 and 3.9:1, which fails the floor in section
 * 7, so the hills use the darker pair recorded in section 2 and the sky above them
 * keeps the approved orange.
 *
 * The second problem is that the art has to cover the words wherever they land.
 * The render's own hill curve wanders between y 161 and y 208 across its width, so
 * anchoring it by its base and scaling until the crest clears the text leaves the
 * shorter parts of the skyline below the words and a label at the edge of the
 * block sitting on flat sky. Anchoring by the **deepest point** of the front
 * hill's top edge puts every part of the silhouette at or above the first line,
 * whatever the words are and however long, while the crests still break the
 * skyline as curves.
 */
@Composable
fun Hero(
    sky: HeroSky,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = MIN_HEIGHT,
    content: @Composable () -> Unit,
) {
    var contentHeight by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(SteadyShapes.Hero),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                color = when (sky) {
                    HeroSky.Morning -> SteadyPalette.Orange
                    HeroSky.Evening, HeroSky.Plain -> SteadyPalette.Navy
                },
            )
            if (sky != HeroSky.Plain) drawSkyArt(sky, contentHeight.toFloat())
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SteadySpacing.Inside)
                .onSizeChanged { contentHeight = it.height },
            verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        ) {
            content()
        }
    }
}

/** The label above a hero number, at 92% opacity per DESIGN.md section 3. */
@Composable
fun HeroLabel(text: String, modifier: Modifier = Modifier) {
    SteadyText(
        text = text,
        style = SteadyType.CardTitle,
        color = SteadyPalette.White.copy(alpha = LABEL_ALPHA),
        modifier = modifier,
    )
}

/** The big number. */
@Composable
fun HeroNumber(value: String, unit: String?, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.Bottom,
    ) {
        SteadyText(text = value, style = SteadyType.HeroNumber, color = SteadyPalette.White)
        if (unit != null) {
            SteadyText(
                text = " $unit",
                style = SteadyType.SectionTitle,
                color = SteadyPalette.White,
                modifier = Modifier.padding(bottom = UNIT_BASELINE),
            )
        }
    }
}

/** The welcome headline, which is a hero without a number. */
@Composable
fun HeroHeadline(text: String, modifier: Modifier = Modifier) {
    SteadyText(text = text, style = SteadyType.HeroHeadline, color = SteadyPalette.White, modifier = modifier)
}

/** The one explanatory line under a hero. */
@Composable
fun HeroExplain(text: String, modifier: Modifier = Modifier) {
    SteadyText(
        text = text,
        style = SteadyType.Body,
        color = SteadyPalette.White.copy(alpha = EXPLAIN_ALPHA),
        modifier = modifier,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun DrawScope.drawSkyArt(sky: HeroSky, contentHeight: Float) {
    val factor = size.width / HERO_BOX_W
    val front = if (sky == HeroSky.Morning) SteadyPalette.HillFront else SteadyPalette.NavyL
    val back = if (sky == HeroSky.Morning) SteadyPalette.HillBack else SteadyPalette.Navy

    val groundLine = (size.height - contentHeight - HORIZON_MARGIN * factor)
        .coerceIn(size.height * LEAST_SKY, size.height)
    val drop = groundLine - FRONT_LOWEST * factor

    // The sky first, then the ground over it, which is the order the render uses:
    // the sun sits behind the horizon rather than pasted on top of the land.
    when (sky) {
        HeroSky.Morning -> drawSun(
            centre = Offset(SUN_X * factor, SUN_Y * factor),
            outerRadius = SUN_R * factor,
        )
        HeroSky.Evening -> drawMoon(
            centre = Offset(MOON_X * factor, MOON_Y * factor),
            radius = MOON_R * factor,
            stars = STARS.map { (x, y, r) -> Triple(x * factor, y * factor, r * factor) },
        )
        HeroSky.Plain -> Unit
    }

    drawPath(hill(HILL_FRONT, drop, factor), front)
    drawPath(hill(HILL_BACK, drop, factor), back)
}

/**
 * One hill: the render's curve across the top, then straight down to the bottom
 * of the block and back, so the fill underneath is solid however tall the hero
 * turns out to be. Both hills take the same [drop], so they keep the overlap the
 * render draws them with.
 */
private fun DrawScope.hill(shape: List<Offset>, drop: Float, factor: Float): Path = Path().apply {
    fun x(v: Float) = v * factor
    fun y(v: Float) = v * factor + drop
    moveTo(x(shape[0].x), y(shape[0].y))
    var i = 1
    while (i + 2 <= shape.lastIndex) {
        cubicTo(
            x1 = x(shape[i].x),
            y1 = y(shape[i].y),
            x2 = x(shape[i + 1].x),
            y2 = y(shape[i + 1].y),
            x3 = x(shape[i + 2].x),
            y3 = y(shape[i + 2].y),
        )
        i += POINTS_PER_CURVE
    }
    lineTo(size.width + 1f, size.height + 1f)
    lineTo(x(shape[0].x) - 1f, size.height + 1f)
    close()
}

private val MIN_HEIGHT = 200.dp
private val UNIT_BASELINE = 10.dp
private const val LABEL_ALPHA = 0.92f
private const val EXPLAIN_ALPHA = 0.95f

/** The width the approved render draws the hero art in. Everything scales from it. */
private const val HERO_BOX_W = 354f
private const val POINTS_PER_CURVE = 3

// The two hills, exactly the curves in the approved renders: a start point and
// then three points per cubic. Not redrawn by eye.
private val HILL_FRONT = listOf(
    Offset(-20f, 200f),
    Offset(60f, 150f),
    Offset(120f, 150f),
    Offset(200f, 190f),
    Offset(260f, 220f),
    Offset(320f, 210f),
    Offset(380f, 180f),
)
private val HILL_BACK = listOf(
    Offset(-20f, 225f),
    Offset(80f, 180f),
    Offset(160f, 190f),
    Offset(240f, 220f),
    Offset(300f, 240f),
    Offset(340f, 236f),
    Offset(380f, 222f),
)

/**
 * The lowest point of the front hill's top edge, found on its second cubic near
 * t 0.4 and rounded up so the rounding falls the safe way. Every word sits at or
 * below this, which is what makes the coverage a property of the shape rather
 * than of how long the label happens to be.
 */
private const val FRONT_LOWEST = 208.2f

private const val HORIZON_MARGIN = 26f

/** The least of a hero left as sky, so the sun is never sitting on a hillside. */
private const val LEAST_SKY = 0.3f

private const val SUN_X = 306f
private const val SUN_Y = 58f
private const val SUN_R = 56f
private const val MOON_X = 270f
private const val MOON_Y = 70f
private const val MOON_R = 46f
private val STARS = listOf(
    Triple(96f, 52f, 2f),
    Triple(150f, 30f, 2.5f),
    Triple(40f, 90f, 1.5f),
)
