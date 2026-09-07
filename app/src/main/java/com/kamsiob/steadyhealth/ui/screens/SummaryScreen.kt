package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.Pill
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/** How far back the summary looks: three months, six, or everything. */
@Composable
private fun WindowPills(selected: SummaryWindow, onWindow: (SummaryWindow) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
    ) {
        listOf(
            SummaryWindow.ThreeMonths to R.string.summary_window_three,
            SummaryWindow.SixMonths to R.string.summary_window_six,
            SummaryWindow.AllTime to R.string.summary_window_all,
        ).forEach { (window, label) ->
            Pill(
                label = stringResource(label),
                selected = selected == window,
                onClick = { onWindow(window) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** One row of the measures table. LOGIC.md section 13. */
data class SummaryNumber(val name: String, val value: String)

/** How far back the summary looks. */
enum class SummaryWindow { ThreeMonths, SixMonths, AllTime }

/** What the visit summary draws. */
data class SummaryUiState(
    val window: SummaryWindow = SummaryWindow.SixMonths,
    val loading: Boolean = false,
    /** Empty when there is not enough behind it, or when nothing validated. */
    val paragraphs: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
    val numbers: List<SummaryNumber> = emptyList(),
    val provenance: String = "",
    /** True when there is not yet enough data for a summary at all. */
    val notYet: Boolean = false,
    /** True when a summary was attempted and nothing survived the check. */
    val fellBack: Boolean = false,
)

/**
 * The visit summary. DESIGN.md, the visit summary page.
 *
 * One page somebody can hand to a clinician: what changed, what they said,
 * what has been happening, then the numbers. Never a fourth tab, never badged and
 * never announced, because a page about what your body is doing that pesters you
 * is a page you stop opening.
 *
 * When the written part cannot be produced, or has not been checked, the numbers
 * are still here and the page says so in one line. That is the whole failure
 * mode: never a partial sentence, never an unvalidated one, and never an error
 * that blames the person.
 */
@Composable
fun SummaryScreen(
    state: SummaryUiState,
    onWindow: (SummaryWindow) -> Unit,
    onExport: () -> Unit,
    onRegenerate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.summary_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.summary_export), onClick = onExport)
            if (!state.notYet) {
                SecondaryButton(
                    label = stringResource(R.string.summary_regenerate),
                    onClick = onRegenerate,
                )
            }
        },
    ) {
        WindowPills(state.window, onWindow)

        when {
            state.loading -> Paragraph(stringResource(R.string.summary_loading))

            state.notYet -> NoteBlock(
                heading = stringResource(R.string.summary_not_yet_title),
                text = stringResource(R.string.summary_not_yet),
            )

            state.fellBack -> NoteBlock(stringResource(R.string.summary_fallback))

            else -> state.paragraphs.forEach { Paragraph(it) }
        }

        if (!state.loading && !state.notYet) {
            SectionTitle(stringResource(R.string.summary_questions))
            if (state.questions.isEmpty()) {
                Paragraph(stringResource(R.string.summary_no_questions))
            } else {
                state.questions.forEach { ListItem(heading = it) }
            }
        }

        if (state.numbers.isNotEmpty()) {
            SectionTitle(stringResource(R.string.summary_numbers))
            state.numbers.forEach { ListItem(heading = it.name, value = it.value) }
            Paragraph(stringResource(R.string.summary_not_tracked))
        }

        if (state.provenance.isNotBlank()) {
            SteadyText(
                text = state.provenance,
                style = SteadyType.Caption,
                color = SteadyPalette.Ink3Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
