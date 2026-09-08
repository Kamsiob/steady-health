package com.kamsiob.steadyhealth.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * L2, the help dot.
 *
 * A question mark in the top right of every screen that has one, the same place and
 * the same size on all of them, because help somebody has to look for is help that
 * does not exist.
 */
@Composable
fun HelpDot(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spoken = stringResource(R.string.help_dot)
    Box(
        modifier = modifier
            .size(SteadySpacing.TapTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(DOT)
                .background(SteadyPalette.White, CircleShape)
                .border(SteadySpacing.Outline, SteadyPalette.Sand, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            SteadyText(
                text = stringResource(R.string.help_dot_mark),
                style = SteadyType.ScreenTitle,
                color = SteadyPalette.Navy,
            )
        }
    }
}

/**
 * The sheet behind the dot.
 *
 * It covers the screen rather than sliding half way up it, because half a screen of
 * help under half a screen of the thing being explained is two things to read at
 * once. Tapping anywhere closes it, which is the only gesture it asks for.
 */
@Composable
fun HelpSheet(place: Place, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val topic = Help.topic(place)
    val nothing = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SteadyPalette.Navy)
            .clickable(interactionSource = nothing, indication = null, onClick = onClose),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(SteadySpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(SteadySpacing.BetweenBlocks),
        ) {
            SteadyText(
                text = stringResource(R.string.help_what_is),
                style = SteadyType.ScreenTitleBig,
                color = SteadyPalette.White,
                modifier = Modifier.semantics { heading() },
            )
            SteadyText(
                text = stringResource(topic.what),
                style = SteadyType.Body,
                color = SteadyPalette.White,
            )

            if (topic.points.isNotEmpty()) {
                SteadyText(
                    text = stringResource(R.string.help_on_screen),
                    style = SteadyType.CardTitle,
                    color = SteadyPalette.Sand,
                    modifier = Modifier.semantics { heading() },
                )
                topic.points.forEach { point ->
                    SteadyText(
                        text = stringResource(point),
                        style = SteadyType.Body,
                        color = SteadyPalette.White,
                    )
                }
            }

            if (topic.questions.isNotEmpty()) {
                SteadyText(
                    text = stringResource(R.string.help_questions),
                    style = SteadyType.CardTitle,
                    color = SteadyPalette.Sand,
                    modifier = Modifier.semantics { heading() },
                )
                topic.questions.forEach { question ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                SteadyPalette.NavyL,
                                RoundedCornerShape(SteadySpacing.Inside),
                            )
                            .padding(SteadySpacing.InsideTight),
                        verticalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
                    ) {
                        SteadyText(
                            text = stringResource(question.ask),
                            style = SteadyType.BodyStrong,
                            color = SteadyPalette.White,
                        )
                        SteadyText(
                            text = stringResource(question.answer),
                            style = SteadyType.Body,
                            color = SteadyPalette.Sand,
                        )
                    }
                }
            }

            SteadyText(
                text = stringResource(R.string.help_dismiss),
                style = SteadyType.Caption,
                color = SteadyPalette.Sand,
                modifier = Modifier.heightIn(min = SteadySpacing.TapTarget),
            )
        }
    }
}

/** The same disc as the back button, so the two ends of the top row match. */
private val DOT = 38.dp
