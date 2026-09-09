@file:Suppress("MatchingDeclarationName") // The screens of one scan, and their state.

package com.kamsiob.steadyhealth.ui.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.scan.PageKind
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TextEntry
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** One page taken, kept in memory until the person says that is all of it. */
data class TakenPage(val image: Bitmap, val text: String)

/** Where a scan has got to. */
data class ScanUiState(
    val pages: List<TakenPage> = emptyList(),
    val reading: Boolean = false,
    val nothingRead: Boolean = false,
    val kind: PageKind? = null,
    val fromWho: String = "",
    val allowed: Boolean = false,
    val hasCamera: Boolean = true,
)

/**
 * Taking the photograph. ADDENDUM-03 Part 5.
 *
 * One button for every piece of paper, and the sentence about where it stays is on
 * this screen rather than in Settings, because the moment somebody points a camera at
 * their own medical letter is the moment they want to know.
 *
 * A document can be several pages. They are taken in sequence and handled as one, so
 * the button after the first says "add another page" rather than starting again.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun ScanScreen(
    state: ScanUiState,
    onTake: () -> Unit,
    onAllow: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    preview: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.scan_title),
        onBack = onBack,
        modifier = modifier,
        footer = { ScanFooter(state, onTake, onAllow, onFinish) },
    ) {
        NoteBlock(stringResource(R.string.scan_stays_here))

        when {
            !state.hasCamera -> Paragraph(stringResource(R.string.scan_no_camera))
            !state.allowed -> Paragraph(stringResource(R.string.scan_needs_camera))
            else -> preview()
        }

        if (state.reading) Paragraph(stringResource(R.string.scan_reading))
        if (state.nothingRead) NoteBlock(stringResource(R.string.scan_nothing_read))

        if (state.pages.isNotEmpty()) {
            SectionTitle(
                pluralStringResource(R.plurals.scan_pages, state.pages.size, state.pages.size),
            )
            state.pages.forEach { page ->
                Image(
                    bitmap = page.image.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ScanFooter(
    state: ScanUiState,
    onTake: () -> Unit,
    onAllow: () -> Unit,
    onFinish: () -> Unit,
) {
    if (!state.hasCamera) return
    if (!state.allowed) {
        PrimaryButton(label = stringResource(R.string.scan_allow), onClick = onAllow)
        return
    }
    if (state.reading) return

    PrimaryButton(
        label = stringResource(
            if (state.pages.isEmpty()) R.string.scan_take else R.string.scan_another_page,
        ),
        onClick = onTake,
    )
    if (state.pages.isNotEmpty()) {
        SecondaryButton(label = stringResource(R.string.scan_done), onClick = onFinish)
    }
}

/**
 * The one question, after the app has looked. ADDENDUM-03 Part 5.
 *
 * Every path ends with the page kept, and "Just keep it" is on every version of this
 * screen because Part 5 says it is always available and is a legitimate outcome. The
 * app also says what it saw, so a wrong guess is something the person can see the
 * reason for rather than something that just happened to them.
 */
@Composable
@Suppress("LongParameterList") // One screen, one callback for each thing on it.
fun PageFoundScreen(
    state: ScanUiState,
    onWho: (String) -> Unit,
    onAddExercises: () -> Unit,
    onExplain: () -> Unit,
    onKeep: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kind = state.kind
    SteadyScreen(
        title = null,
        onBack = onBack,
        modifier = modifier,
        footer = { PrimaryButton(label = stringResource(R.string.found_keep), onClick = onKeep) },
    ) {
        SteadyText(
            text = stringResource(asked(kind)),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.semantics { heading() },
        )

        if (offersExercises(kind)) {
            ListItem(heading = stringResource(R.string.found_add_them), onClick = onAddExercises)
        }
        if (offersReading(kind)) {
            ListItem(heading = stringResource(R.string.found_explain), onClick = onExplain)
        }

        SectionTitle(stringResource(R.string.found_who))
        TextEntry(
            value = state.fromWho,
            onValue = onWho,
            hint = stringResource(R.string.found_who_hint),
            imeAction = ImeAction.Done,
        )

        // Never for an out of scope page. Part 5 gives that outcome exactly one
        // sentence and it does not name the kind of document, and the clues that
        // fired would name it: telling somebody the app decided their page was a
        // blood test is the app saying something about their health.
        if (kind !is PageKind.OutOfScope) {
            kind?.signals?.takeIf { it.isNotEmpty() }?.let { signals ->
                SectionTitle(stringResource(R.string.found_saw))
                Paragraph(signals.joinToString(", ") { it.name })
            }
        }
    }
}

/** Which of Part 5's five questions this page gets. */
private fun asked(kind: PageKind?) = when (kind) {
    is PageKind.Exercises, is PageKind.Both -> R.string.found_exercises
    is PageKind.ReportOrLetter -> R.string.found_report
    is PageKind.OutOfScope -> R.string.found_out_of_scope
    else -> R.string.found_unclear
}

/** Unclear offers both, which is Part 5's answer to not knowing. */
private fun offersExercises(kind: PageKind?): Boolean =
    kind is PageKind.Exercises || kind is PageKind.Both || kind is PageKind.Unclear

private fun offersReading(kind: PageKind?): Boolean =
    kind is PageKind.ReportOrLetter || kind is PageKind.Both || kind is PageKind.Unclear
