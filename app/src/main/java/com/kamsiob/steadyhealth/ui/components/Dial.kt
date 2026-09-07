package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The dial from DESIGN.md section 3: a sand track, an orange arc, a white knob
 * with an orange ring, and the number in the middle.
 *
 * It has a drag and it also has two buttons, because the accessibility floor says
 * every drag has a tap alternative. The buttons are not a lesser path: on a phone
 * held one-handed by somebody whose hands are not steady, they are the path.
 *
 * The drag is vertical rather than circular. A circular drag on a 230 dp dial
 * asks for a level of pointing accuracy that is exactly what this app should not
 * assume, and up and down is what a scale feels like anyway.
 */
@Composable
fun Dial(
    value: Double,
    displayValue: String,
    unit: String,
    onValue: (Double) -> Unit,
    modifier: Modifier = Modifier,
    minimum: Double = MIN_KG,
    maximum: Double = MAX_KG,
    step: Double = STEP_KG,
    spoken: String = displayValue,
) {
    val fraction = ((value - minimum) / (maximum - minimum)).coerceIn(0.0, 1.0).toFloat()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
    ) {
        Box(
            modifier = Modifier
                .size(DIAL)
                .pointerInput(minimum, maximum, step) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Up is more. A drag of the dial's own height covers the
                        // whole range, so the gesture is proportional to the thing
                        // it is moving rather than to an invented sensitivity.
                        val perPixel = (maximum - minimum) / size.height
                        val next = value - dragAmount * perPixel
                        onValue(round(next.coerceIn(minimum, maximum), step))
                    }
                }
                .semantics { contentDescription = spoken },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(DIAL)) {
                val stroke = size.width * TRACK_RATIO
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = SteadyPalette.Sand,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    ),
                )
                drawArc(
                    color = SteadyPalette.Orange,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    ),
                )

                val angle = Math.toRadians((START_ANGLE + SWEEP * fraction).toDouble())
                val radius = (size.width - stroke) / 2f
                val knob = Offset(
                    x = size.width / 2f + (radius * cos(angle)).toFloat(),
                    y = size.height / 2f + (radius * sin(angle)).toFloat(),
                )
                drawCircle(SteadyPalette.Orange, radius = stroke * KNOB_RING, center = knob)
                drawCircle(SteadyPalette.White, radius = stroke * KNOB_INNER, center = knob)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SteadyText(
                    text = displayValue,
                    style = SteadyType.HeroNumber,
                    color = SteadyPalette.Navy,
                )
                SteadyText(text = unit, style = SteadyType.CardTitle, color = SteadyPalette.Ink2)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight)) {
            DialButton("−", "less") { onValue(round((value - step).coerceAtLeast(minimum), step)) }
            DialButton("+", "more") { onValue(round((value + step).coerceAtMost(maximum), step)) }
        }
    }
}

@Composable
private fun DialButton(glyph: String, spoken: String, onClick: () -> Unit) {
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

private fun round(value: Double, step: Double): Double =
    (value / step).roundToInt() * step

private val DIAL = 230.dp
private const val TRACK_RATIO = 0.096f
private const val START_ANGLE = 135f
private const val SWEEP = 270f
private const val KNOB_RING = 0.62f
private const val KNOB_INNER = 0.42f
private const val MIN_KG = 30.0
private const val MAX_KG = 250.0
private const val STEP_KG = 0.1
