@file:Suppress("MatchingDeclarationName") // The list, one of its items, and their state.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TextEntry

/** One thing on the list, as You shows it: the words, and where it was rated. */
data class ListedItem(
    val id: Long,
    val text: String,
    val domain: AbilityDomain,
    /** The last rating in the words this person sees, or nothing before the first check. */
    val said: String = "",
)

/** The list, and whichever one of it is open. */
data class YourListUiState(
    val items: List<ListedItem> = emptyList(),
    val editing: ListedItem? = null,
    /** What is in the field on the item screen, which starts as what is on the list. */
    val typed: String = "",
)

/**
 * Your list. ADDENDUM-03 Part 20 puts it first on You.
 *
 * The same items Progress shows, here because Progress reports and You changes.
 * Nothing on this screen is behind a confirmation and nothing is counted: it is a
 * list of sentences somebody wrote about their own life, in the order they wrote
 * them.
 */
@Composable
fun YourListScreen(
    state: YourListUiState,
    onOpen: (ListedItem) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.your_list_row),
        onBack = onBack,
        modifier = modifier,
    ) {
        if (state.items.isEmpty()) NoteBlock(stringResource(R.string.your_list_empty))

        state.items.forEach { item ->
            ListItem(
                heading = item.text,
                subtitle = item.said.takeIf { it.isNotBlank() },
                onClick = { onOpen(item) },
            )
        }

        ListItem(
            heading = stringResource(R.string.progress_add),
            subtitle = stringResource(R.string.progress_add_sub),
            onClick = onAdd,
        )
    }
}

/**
 * One of yours, in your words, with a way to take it off.
 *
 * Taking one off archives it rather than deleting it, which is why the sentence
 * under the row can promise that everything already said about it stays where it is.
 * A monthly rating is a fact about a month, and a person changing their mind about
 * what they are working towards does not make those months untrue.
 *
 * Saving is a primary button and taking it off is the quiet one, because the reason
 * somebody opened this row is nearly always the first of the two.
 */
@Composable
fun YourItemScreen(
    state: YourListUiState,
    onTyped: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.your_item_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            PrimaryButton(
                label = stringResource(R.string.your_item_save),
                onClick = onSave,
                enabled = state.typed.isNotBlank(),
            )
            SecondaryButton(
                label = stringResource(R.string.your_item_remove),
                onClick = onRemove,
            )
        },
    ) {
        TextEntry(
            value = state.typed,
            onValue = onTyped,
            hint = stringResource(R.string.o3_hint),
            imeAction = ImeAction.Done,
            onSubmit = onSave,
        )

        state.editing?.said?.takeIf { it.isNotBlank() }?.let { NoteBlock(it) }

        Paragraph(stringResource(R.string.your_item_remove_why))
    }
}
