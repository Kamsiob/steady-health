@file:Suppress("MatchingDeclarationName") // The screen and the state it draws.

package com.kamsiob.steadyhealth.ui.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen

/** One document in the list, said the way somebody would say it. */
data class DocumentRow(val id: Long, val whenIt: String, val fromWho: String?)

/** What the documents screen draws. */
data class DocumentsUiState(
    val rows: List<DocumentRow> = emptyList(),
    val openId: Long? = null,
    val pages: List<Bitmap> = emptyList(),
)

/**
 * Everything photographed, kept forever. ADDENDUM-03 Part 5.
 *
 * "The photo is saved, labelled by date and by who it came from, and is viewable
 * forever beside anything the app produced from it." Forever is the word that decides
 * this screen: there is no tidying, no archive, no expiry, and the only way anything
 * leaves is the person removing it.
 *
 * The photograph is shown at full width rather than as a thumbnail, because the
 * reason to come here is to read something, and a document somebody has to pinch at
 * is a document they will photograph again with a different app.
 */
@Composable
fun DocumentsScreen(
    state: DocumentsUiState,
    onOpen: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.documents_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        NoteBlock(stringResource(R.string.scan_stays_here))

        if (state.rows.isEmpty()) {
            SectionTitle(stringResource(R.string.documents_none))
            return@SteadyScreen
        }

        state.rows.forEach { row ->
            ListItem(
                heading = row.whenIt,
                subtitle = row.fromWho?.let { stringResource(R.string.documents_from, it) },
                value = stringResource(R.string.documents_open),
                onClick = { onOpen(row.id) },
            )

            if (state.openId == row.id) {
                state.pages.forEach { page ->
                    Image(
                        bitmap = page.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SecondaryButton(
                    label = stringResource(R.string.documents_remove),
                    onClick = { onRemove(row.id) },
                )
            }
        }
    }
}
