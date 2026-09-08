package com.kamsiob.steadyhealth.ui.session

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * Whether motion runs at all.
 *
 * DESIGN.md section 5: under reduce motion, numbers change instantly, fills are
 * immediate, and nothing is lost. Read once here so every piece of the session agrees.
 */
@Composable
fun motionOn(): Boolean {
    val context = LocalContext.current
    val scale = android.provider.Settings.Global.getFloat(
        context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale > 0f
}

/**
 * The count. The largest type in the app, and the reason the live screen exists.
 *
 * It has to be readable from two feet away by somebody standing up out of a chair, so
 * it fills the width and the spring on each change is the whole of DESIGN.md's "count
 * up" motion. Its own semantics, because a screen reader should hear "nine of twelve"
 * rather than "nine" and then "of twelve" from somewhere underneath.
 */
@Composable
fun BigCount(
    value: Int,
    spoken: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val shown by animateIntAsState(
        targetValue = value,
        animationSpec = if (animate) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        } else {
            tween(durationMillis = 0)
        },
        label = "count",
    )
    SteadyText(
        text = "$shown",
        style = SteadyType.SessionCount,
        color = SteadyPalette.Navy,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken },
        textAlign = TextAlign.Center,
    )
}

/**
 * The ring, filling toward the target.
 *
 * Not a progress bar: a bar implies a bar to clear, and reaching the target is not
 * the point of a set. The ring is the count's frame, and going past it simply fills.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    colour: Color = SteadyPalette.OrangeD,
    animate: Boolean = true,
    content: @Composable () -> Unit,
) {
    val filled by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (animate) tween(durationMillis = FILL_MILLIS) else tween(0),
        label = "ring",
    )
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val stroke = size.minDimension * RING_THICKNESS
            val inset = stroke / 2
            val box = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = SteadyPalette.Sand,
                startAngle = START_ANGLE,
                sweepAngle = FULL_CIRCLE,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = box,
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawArc(
                color = colour,
                startAngle = START_ANGLE,
                sweepAngle = FULL_CIRCLE * filled,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = box,
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        content()
    }
}

/**
 * The rest, as a shrinking arc rather than a ticking number.
 *
 * ADDENDUM-03 Part 1 asks for the arc by name. A number counting down is a thing to
 * watch; an arc is a thing to glance at, and rest is not the interesting part.
 */
@Composable
fun ShrinkingArc(
    remaining: Int,
    total: Int,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    content: @Composable () -> Unit,
) {
    ProgressRing(
        progress = if (total <= 0) 0f else remaining.toFloat() / total,
        modifier = modifier,
        colour = SteadyPalette.Green,
        animate = animate,
        content = content,
    )
}

/** The sand block every screen uses to explain itself once. DESIGN.md 4b. */
@Composable
fun SaysOnce(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteadySpacing.Inside))
            .background(SteadyPalette.Sand)
            .padding(SteadySpacing.InsideTight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
    ) {
        SteadyText(
            text = text,
            style = SteadyType.Body,
            color = SteadyPalette.Ink2,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .heightIn(min = SteadySpacing.TapTarget)
                .clip(RoundedCornerShape(SteadySpacing.Tight))
                .clickable(onClick = onDismiss)
                .padding(horizontal = SteadySpacing.Tight),
            contentAlignment = Alignment.Center,
        ) {
            SteadyText(
                text = "×",
                style = SteadyType.SectionTitle,
                color = SteadyPalette.Ink3Text,
            )
        }
    }
}

private const val FILL_MILLIS = 600
private const val RING_THICKNESS = 0.06f
private const val START_ANGLE = -90f
private const val FULL_CIRCLE = 360f
