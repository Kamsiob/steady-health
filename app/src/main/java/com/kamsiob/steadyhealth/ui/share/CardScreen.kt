package com.kamsiob.steadyhealth.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.share.CardRules
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TextEntry
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette

/**
 * Writing the card, before it goes anywhere. ADDENDUM-03 Part 11.
 *
 * The suggested line is already in the field, and the field is the first thing on the
 * screen, because the point of this screen is that the sentence is theirs. There is no
 * preview of the picture: the card is the sentence, and a preview would make the
 * layout look like the thing being chosen when it is not.
 *
 * The note about what is on Part 11's never list is a note and not a block. It is
 * their card and their words. An app that refused to render somebody's own sentence
 * would be a worse thing than one that says what it noticed and gets out of the way.
 */
@Composable
fun CardScreen(
    state: CardUiState,
    onLine: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.card_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            if (state.canSend) {
                PrimaryButton(label = stringResource(R.string.card_send), onClick = onSend)
            }
        },
    ) {
        Paragraph(stringResource(R.string.card_intro))

        SectionTitle(stringResource(R.string.card_line))
        TextEntry(
            value = state.line,
            onValue = onLine,
            hint = stringResource(R.string.card_hint),
            imeAction = ImeAction.Done,
        )

        if (state.tooLong) {
            NoteBlock(
                stringResource(R.string.card_too_long, CardRules.MOST_CHARACTERS),
                tint = SteadyPalette.SkyL,
            )
        }

        if (state.worthAWord.isNotEmpty()) {
            NoteBlock(
                heading = stringResource(R.string.card_check_heading),
                text = stringResource(
                    R.string.card_check,
                    state.worthAWord.joinToString(", "),
                ),
                tint = SteadyPalette.SkyL,
            )
        }

        NoteBlock(stringResource(R.string.card_private))
    }
}
