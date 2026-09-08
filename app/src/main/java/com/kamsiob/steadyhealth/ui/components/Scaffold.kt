@file:Suppress("MatchingDeclarationName")

package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.help.Help
import com.kamsiob.steadyhealth.ui.help.HelpDot
import com.kamsiob.steadyhealth.ui.help.HelpSheet
import com.kamsiob.steadyhealth.ui.help.HelpViewModel
import com.kamsiob.steadyhealth.ui.help.Place
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * The optional text action on the right of the top row.
 *
 * It lives here rather than in a file of its own because it is one small type
 * belonging to the row above, and a file per two-field data class is how a
 * component library becomes hard to read.
 */
data class ScreenAction(val label: String, val onClick: () -> Unit)

/**
 * The screen anatomy from DESIGN.md section 4, in one place.
 *
 * A 44 dp top row, then blocks on the 12 dp grid, then the primary button pinned
 * in an 8/18/14 footer. Every content screen goes through this, so the rhythm is
 * a property of the app rather than something each screen remembers.
 *
 * The status bar inset is taken here, once. A screen taking it itself is a screen
 * that will forget, and the symptom is a title drawn under the clock.
 */
@Composable
fun SteadyScreen(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    action: ScreenAction? = null,
    gap: androidx.compose.ui.unit.Dp = SteadySpacing.BetweenBlocks,
    help: Place? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val helpViewModel: HelpViewModel = viewModel()
    val seen by helpViewModel.seen.collectAsStateWithLifecycle()
    var asking by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SteadyPalette.Ground)
                .statusBarsPadding()
                // Without this the footer sits under the keyboard on every screen
                // with a text field, and the primary button is unreachable. Going
                // edge to edge means adjustResize no longer insets the content, so
                // the inset has to be taken here, once, like the status bar above.
                .imePadding(),
        ) {
            if (hasTopRow(title, onBack, action, help)) {
                TopRow(
                    title = title,
                    onBack = onBack,
                    action = action,
                    help = help?.let { { asking = true } },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SteadySpacing.Screen),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                if (help != null) {
                    ScreenSaysOnce(help, seen, onDismiss = { helpViewModel.dismiss(help) })
                }
                content()
                Spacer(Modifier.size(SteadySpacing.BetweenBlocks))
            }

            if (footer != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SteadySpacing.Screen,
                            end = SteadySpacing.Screen,
                            top = SteadySpacing.ListGap,
                            bottom = 14.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
                ) {
                    footer()
                }
            }
        }

        if (asking && help != null) {
            HelpSheet(place = help, onClose = { asking = false })
        }
    }
}

private fun hasTopRow(
    title: String?,
    onBack: (() -> Unit)?,
    action: ScreenAction?,
    help: Place?,
): Boolean = title != null || onBack != null || action != null || help != null

/**
 * L1, in the one place every screen passes through.
 *
 * Nothing is drawn until the answer is known, because a sand block that appears and
 * then vanishes when the database replies is worse than one that arrives a frame late.
 */
@Composable
private fun ScreenSaysOnce(place: Place, seen: Set<String>?, onDismiss: () -> Unit) {
    val says = Help.topic(place).says ?: return
    if (seen == null || place.seenKey in seen) return
    SaysOnce(text = stringResource(says), onDismiss = onDismiss)
}

/**
 * The top row. The back button is a round white disc with a sand inset outline,
 * drawn at 38 dp inside a 44 dp target, because the drawing and the thing a
 * finger has to hit are not the same size.
 */
@Composable
fun TopRow(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    action: ScreenAction? = null,
    help: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SteadySpacing.TapTarget)
            .padding(horizontal = SteadySpacing.Screen, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(SteadySpacing.TapTarget)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(BACK_DISC)
                        .background(SteadyPalette.White, CircleShape)
                        .border(SteadySpacing.Outline, SteadyPalette.Sand, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SteadyText(
                        text = stringResource(R.string.back_chevron),
                        style = SteadyType.ScreenTitle,
                        color = SteadyPalette.Navy,
                    )
                }
            }
        }

        if (title != null) {
            SteadyText(
                text = title,
                style = SteadyType.ScreenTitle,
                color = SteadyPalette.Navy,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (action != null) {
            SteadyText(
                text = action.label,
                style = SteadyType.CardTitle,
                color = SteadyPalette.OrangeText,
                modifier = Modifier
                    .heightIn(min = SteadySpacing.TapTarget)
                    .clickable(role = Role.Button, onClick = action.onClick)
                    .padding(horizontal = SteadySpacing.ListGap, vertical = 13.dp),
            )
        }

        if (help != null) {
            HelpDot(onClick = help)
        }
    }
}

/** A section title, 6 dp above its block. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, aside: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = SteadySpacing.AboveSectionTitle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SteadyText(
            text = text,
            style = SteadyType.SectionTitle,
            color = SteadyPalette.Navy,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (aside != null) {
            SteadyText(text = aside, style = SteadyType.Caption, color = SteadyPalette.Ink3Text)
        }
    }
}

/** Body copy, at the one size the type table gives it. */
@Composable
fun Paragraph(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = SteadyPalette.Ink2,
) {
    SteadyText(text = text, style = SteadyType.Body, color = color, modifier = modifier)
}

/** A hairline, for the rows inside a block that are separated rather than spaced. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 1.dp)
            .width(1.dp)
            .background(SteadyPalette.Hairline),
    )
}

private val BACK_DISC = 38.dp
