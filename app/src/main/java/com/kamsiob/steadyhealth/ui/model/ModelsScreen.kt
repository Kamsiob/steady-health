package com.kamsiob.steadyhealth.ui.model

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.model.CanAdd
import com.kamsiob.steadyhealth.model.Choice
import com.kamsiob.steadyhealth.model.Licence
import com.kamsiob.steadyhealth.model.ManualPath
import com.kamsiob.steadyhealth.model.ModelRoom
import com.kamsiob.steadyhealth.model.OptionalModel
import com.kamsiob.steadyhealth.ui.components.Explained
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * "What the app can read", from ADDENDUM-03 Part 7. Reached from You.
 *
 * The whole screen is a view of one [ModelRoom]. Nothing here works out whether a
 * download would fit, what is in the way, or how much room would be left: that
 * arithmetic is the decision layer's, it is tested without a phone, and a screen that
 * repeated any of it would eventually disagree with it. The screen's own job is the
 * two things the decision layer deliberately cannot do, which are showing a licence
 * and taking the acceptance, and choosing the sentence for each answer.
 *
 * The order of the file follows the order of the screen, because the order of the
 * screen is the argument: nothing extra comes first, is drawn exactly like the two
 * downloads rather than as a lesser fourth option, and carries the words for what
 * that person has rather than for what they have not got.
 */

/**
 * What the screen draws.
 *
 * [room] is the whole of the phone's side of it. [fellBackTo] is the removal's one
 * sentence and is emptied by the wiring once it has been seen, because Part 7 says a
 * removal says so once and once is not a thing a screen can remember for itself.
 *
 * The default [room] offers nothing at all, which is the honest default for this
 * build: nothing downloads yet, and a screen constructed with no state should not
 * grow buttons that would do nothing. When the downloader exists, the wiring passes
 * a room whose `offered` set names what it can actually fetch.
 */
data class ModelsUiState(
    val room: ModelRoom = ModelRoom(freeBytes = 0L, offered = emptySet()),
    /** The model whose licence is on screen, or null when nothing is being agreed to. */
    val showTermsFor: OptionalModel? = null,
    /** What went back to being done by hand, from [ModelRoom.remove]'s answer. */
    val fellBackTo: Set<ManualPath> = emptySet(),
)

/**
 * What the screen can do, in one bag.
 *
 * Grouped the way SettingsActions is, because six lambdas in the signature would be
 * longer than the screen and would say less: these are the four things a person can
 * do here and the two ways out of the licence.
 */
data class ModelsActions(
    val onAdd: (OptionalModel) -> Unit,
    val onRemove: (OptionalModel) -> Unit,
    /** Show the licence. The screen routes here rather than to [onAdd] on its own. */
    val onTerms: (OptionalModel) -> Unit,
    /** Agreed to on the licence screen, which is also the tap that starts it. */
    val onAgree: (OptionalModel) -> Unit,
    val onCloseTerms: () -> Unit,
    val onByHandSeen: () -> Unit,
)

/**
 * The screen, or the licence when one is being shown.
 *
 * The licence takes the whole screen rather than sitting in a sheet over the choices,
 * because Part 7 puts acceptance before the download rather than beside it, and
 * something a person agrees to should not be readable as an aside on a list.
 */
@Composable
fun ModelsScreen(
    state: ModelsUiState,
    actions: ModelsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showTermsFor != null) {
        LicenceGate(model = state.showTermsFor, actions = actions, modifier = modifier)
        return
    }

    SteadyScreen(title = stringResource(R.string.models_title), onBack = onBack, modifier = modifier) {
        if (state.fellBackTo.isNotEmpty()) {
            ByHandNote(paths = state.fellBackTo, onSeen = actions.onByHandSeen)
        }
        Paragraph(stringResource(R.string.models_intro))
        FreeSpace(room = state.room)
        Paragraph(stringResource(R.string.models_only_on_data))

        NothingExtra(room = state.room)
        state.room.choices.forEach { choice -> ModelChoice(choice = choice, actions = actions) }
        BothOfThem(room = state.room)

        SteadyText(
            text = stringResource(R.string.models_credit),
            style = SteadyType.Caption,
            color = SteadyPalette.Ink3Text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The removal's one sentence, and the promise underneath it.
 *
 * Sand rather than anything louder. Nothing went wrong here: a person took something
 * off their own phone, everything it wrote is still there, and all this says is which
 * job comes back to them.
 */
@Composable
private fun ByHandNote(paths: Set<ManualPath>, onSeen: () -> Unit) {
    val said = paths.map { stringResource(sentenceFor(it)) } + stringResource(R.string.models_by_hand_kept)
    NoteBlock(
        text = said.joinToString(" "),
        heading = stringResource(R.string.models_by_hand_title),
        onDismiss = onSeen,
        dismissLabel = stringResource(R.string.models_by_hand_got_it),
    )
}

/**
 * The phone's free space, with the margin explained where the number is.
 *
 * DESIGN.md 4b, L3: no number in this app is unexplained. This one needs it more than
 * most, because the free space the phone reports and the room a download can have are
 * a gigabyte apart, and somebody comparing two numbers on this screen would otherwise
 * be right to think the app cannot subtract.
 */
@Composable
private fun FreeSpace(room: ModelRoom) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
    ) {
        SteadyText(
            text = stringResource(R.string.models_free_label),
            style = SteadyType.Caption,
            color = SteadyPalette.Ink2,
        )
        Explained(
            text = sizeSaid(room.freeBytes),
            explanation = stringResource(R.string.models_free_explained),
            style = SteadyType.BlockValue,
            colour = SteadyPalette.Navy,
        )
    }
}

/**
 * Nothing extra: first, and drawn as the thing it is rather than as an absence.
 *
 * Part 7 and DESIGN.md section 7. It gets the same row as the two downloads, the
 * outline that marks the one you have, and the word for it on the right, so the
 * default reads as chosen rather than as not yet done. Nothing about it is greyed,
 * nothing about it is last, and the copy names what this person has.
 *
 * The line underneath is [ModelRoom.byHand], which is what the app is doing itself
 * right now. That is the honest version of what a download would change, and it is
 * built from the jobs rather than written per model, so it stays true when a job
 * moves.
 */
@Composable
private fun NothingExtra(room: ModelRoom) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
    ) {
        ListItem(
            heading = stringResource(R.string.models_nothing_title),
            subtitle = stringResource(R.string.models_nothing_sub),
            value = if (room.nothingExtra) stringResource(R.string.models_nothing_here) else null,
            done = room.nothingExtra,
        )
        Paragraph(stringResource(R.string.models_nothing_body))
        if (room.byHand.isEmpty()) {
            Paragraph(stringResource(R.string.models_nothing_kept))
        } else {
            // map rather than joinToString's own lambda, which is not inline and so
            // cannot call a composable.
            Paragraph(room.byHand.map { stringResource(sentenceFor(it)) }.joinToString(" "))
        }
    }
}

/**
 * One download: what it does, what it costs, and what can be done about it now.
 *
 * The size is on the row whatever the answer is, because Part 7 asks the screen to
 * show each size and because a person deciding what to delete needs the number even
 * when the download is not on offer.
 */
@Composable
private fun ModelChoice(choice: Choice, actions: ModelsActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
    ) {
        ListItem(
            heading = stringResource(nameOf(choice.model)),
            subtitle = stringResource(subOf(choice.model)),
            value = sizeSaid(choice.model.bytes),
            done = choice.canAdd is CanAdd.AlreadyHere,
        )
        Paragraph(stateLine(choice))
        Action(choice = choice, actions = actions)
    }
}

/**
 * The button, where there is one, and nothing at all where there is not.
 *
 * A download that cannot start does not get a button that looks like it will start
 * one. Three of the six answers are reasons nothing can happen yet, and each of them
 * is one sentence and no control, which is also why the sentence has to carry the
 * whole answer.
 *
 * The metered case gets its own label rather than the same word on a different
 * background. Part 7: neither downloads on a metered connection without an explicit
 * tap, and a tap is only explicit if what it spends is written on it.
 */
@Composable
private fun Action(choice: Choice, actions: ModelsActions) {
    val model = choice.model
    // The licence gate is the screen's, because only a screen can show terms and take
    // an acceptance. Asking the licence rather than checking which model this is means
    // a third download under HAI-DEF would arrive already gated.
    val add = {
        if (model.licence.termsAcceptedBeforeDownload) actions.onTerms(model) else actions.onAdd(model)
    }
    when (choice.canAdd) {
        is CanAdd.AlreadyHere -> SecondaryButton(
            label = stringResource(R.string.models_remove),
            onClick = { actions.onRemove(model) },
        )

        is CanAdd.Ready -> PrimaryButton(label = stringResource(R.string.models_add), onClick = add)

        is CanAdd.NeedsATap -> SecondaryButton(
            label = stringResource(R.string.models_add_on_data),
            onClick = add,
        )

        is CanAdd.NotOfferedYet, is CanAdd.NotConnected, is CanAdd.NotEnoughRoom -> Unit
    }
}

/**
 * Both, as Part 7's fourth choice, and only while there is a second one to add.
 *
 * It says what is left to add rather than five gigabytes flat, so somebody who
 * already has one is not quoted a number that includes what is on their phone. There
 * is no button: both are added one at a time, and a control that started two
 * downloads at once would be the one way this screen could fill a device.
 */
@Composable
private fun BothOfThem(room: ModelRoom) {
    if (room.installed.size < OptionalModel.entries.size) {
        val stillToAdd = ModelRoom.needed(OptionalModel.entries.toSet(), room.installed)
        val fits = ModelRoom.bothFit(room.freeBytes, room.installed)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        ) {
            ListItem(
                heading = stringResource(R.string.models_both_title),
                subtitle = stringResource(R.string.models_both_sub),
                value = sizeSaid(stillToAdd),
            )
            Paragraph(stringResource(if (fits) R.string.models_both_fits else R.string.models_both_tight))
        }
    }
}

/**
 * The licence, before the download and not after it. Part 7, LICENSING.
 *
 * What is being agreed to is on the screen with the thing it covers, and the button
 * that agrees is the button that adds, so there is no state in which somebody has
 * accepted terms for a download they did not then choose to start.
 */
@Composable
private fun LicenceGate(model: OptionalModel, actions: ModelsActions, modifier: Modifier = Modifier) {
    SteadyScreen(
        title = stringResource(R.string.models_terms_title),
        onBack = actions.onCloseTerms,
        modifier = modifier,
        footer = {
            PrimaryButton(
                label = stringResource(R.string.models_terms_agree),
                onClick = { actions.onAgree(model) },
            )
            SecondaryButton(
                label = stringResource(R.string.models_terms_not_now),
                onClick = actions.onCloseTerms,
            )
        },
    ) {
        ListItem(
            heading = stringResource(nameOf(model)),
            subtitle = stringResource(subOf(model)),
            value = sizeSaid(model.bytes),
        )
        Paragraph(stringResource(termsOf(model.licence)))
        Paragraph(stringResource(R.string.models_terms_stays))
        SteadyText(
            text = stringResource(R.string.models_credit),
            style = SteadyType.Caption,
            color = SteadyPalette.Ink3Text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * One sentence per answer, and none of them describes the phone or the person as
 * lacking something.
 *
 * [CanAdd.Ready] prints what the app could still use afterwards rather than what
 * would be free on the phone, because the two differ by the margin and the type's own
 * documentation says so. The wording is "the app could still use" for that reason.
 */
@Composable
private fun stateLine(choice: Choice): String = when (val canAdd = choice.canAdd) {
    is CanAdd.AlreadyHere -> stringResource(R.string.models_on_phone)
    is CanAdd.NotOfferedYet -> stringResource(R.string.models_not_yet)
    is CanAdd.Ready -> stringResource(R.string.models_room_for_it, sizeSaid(canAdd.roomLeftAfter))
    is CanAdd.NeedsATap -> stringResource(R.string.models_needs_a_tap, sizeSaid(choice.model.bytes))
    is CanAdd.NotConnected -> stringResource(R.string.models_not_connected)
    is CanAdd.NotEnoughRoom -> stringResource(R.string.models_short_by, sizeSaid(canAdd.shortBy))
}

/**
 * A number of bytes, said the way the phone's own storage screen says it.
 *
 * The platform formatter rather than arithmetic here, so that the app's "2.5 GB" is
 * the same 2.5 GB somebody reads in Settings when they go looking for room, and so
 * that the unit is translated by the system rather than by this app.
 */
@Composable
private fun sizeSaid(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

@StringRes
private fun nameOf(model: OptionalModel): Int = when (model) {
    OptionalModel.YourOwnWords -> R.string.models_words_title
    OptionalModel.DocumentsFromYourTherapist -> R.string.models_docs_title
}

@StringRes
private fun subOf(model: OptionalModel): Int = when (model) {
    OptionalModel.YourOwnWords -> R.string.models_words_sub
    OptionalModel.DocumentsFromYourTherapist -> R.string.models_docs_sub
}

/**
 * What a person does themselves when a model is not on the phone.
 *
 * One sentence per [ManualPath] rather than per model, which is the point of the
 * enum: the row for nothing extra and the sentence after a removal are built from the
 * same four sentences, so they cannot drift apart.
 */
@StringRes
private fun sentenceFor(path: ManualPath): Int = when (path) {
    ManualPath.TypeIt -> R.string.by_hand_type_it
    ManualPath.PickTags -> R.string.by_hand_pick_tags
    ManualPath.LookAtTheWeek -> R.string.by_hand_the_week
    ManualPath.LookAtThePhoto -> R.string.by_hand_the_photo
}

/**
 * The terms, by licence rather than by model.
 *
 * Apache-2.0 has nothing to accept, so its branch is never reached from the screen as
 * it stands. It is written out anyway because the day a second Apache download does
 * need a page of its own, the missing branch would be a crash rather than a sentence.
 */
@StringRes
private fun termsOf(licence: Licence): Int = when (licence) {
    Licence.HaiDef -> R.string.models_terms_haidef
    Licence.Apache2 -> R.string.models_terms_apache
}
