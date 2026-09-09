@file:Suppress("MatchingDeclarationName") // Four screens and their state, one subject.

package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.backup.BackupRead
import com.kamsiob.steadyhealth.backup.RestoreStep
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.PemAnswer
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.engine.ReminderKind
import com.kamsiob.steadyhealth.session.Week
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.Stepper
import com.kamsiob.steadyhealth.ui.components.SwitchRow
import com.kamsiob.steadyhealth.ui.components.ThreeUpChoice
import com.kamsiob.steadyhealth.ui.help.Place
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette

/** What Settings draws. Grid screen 22. */
data class SettingsUiState(
    val gettingAround: GettingAround = GettingAround.OnFeet,
    val gettingAroundLabel: String = "",
    val withTherapist: Boolean = false,
    val weighsIn: Boolean = true,
    val showNumbers: Boolean = true,
    val units: Units = Units.Imperial,
    val exclusions: Set<Exclusion> = emptySet(),
    val exclusionsLabel: String = "",
    val pacing: Boolean = false,
    val pemLabel: String = "",
    val tryItAndSee: Boolean = true,
    val remindersOn: Set<ReminderKind> = emptySet(),
    val remindersLeft: Int = 0,
    val remindersBlocked: Boolean = false,
    val hasAppointment: Boolean = false,
    val envelopeMinutes: Int = 0,
    val envelopeDays: Int = 0,
    /** Three, four or five sessions a week. ADDENDUM-03 Part 10. */
    val weekTarget: Int = Week.DEFAULT,
    /** True when the daily prompt stopped itself, so the screen can say why. */
    val dailyGaveUp: Boolean = false,
    /** True when a therapist's plan exists, which is the only time extras mean anything. */
    val hasPlan: Boolean = false,
    val extras: Boolean = true,
    val dailyOn: Boolean = true,
    /** Whether a session speaks. ADDENDUM-03 Part 1 puts the switch here as well. */
    val audio: Boolean = true,
    /**
     * True once the profile has actually been read.
     *
     * Every other value here has a sensible default, which is exactly the problem
     * for the ones a screen branches on. Adding something to the list offers six
     * chips chosen by how somebody gets around, and the default is on two feet, so
     * a screen drawn before the read finishes shows a wheelchair user a list of
     * things to want that are not theirs and then swaps them a frame later.
     */
    val loaded: Boolean = false,
)

/**
 * What Settings can do, in one bag.
 *
 * Settings is a list of unrelated switches and rows, and passing nine lambdas
 * separately made the signature longer than the screen. Grouping them says the
 * true thing: these are the ways out of this one screen.
 */
data class SettingsActions(
    val onGettingAround: () -> Unit,
    val onTherapist: (Boolean) -> Unit,
    val onWeighsIn: (Boolean) -> Unit,
    val onShowNumbers: (Boolean) -> Unit,
    val onExclusions: () -> Unit,
    val onPattern: () -> Unit,
    val onPacing: () -> Unit,
    val onData: () -> Unit,
    val onReminders: () -> Unit,
    val onTryItAndSee: (Boolean) -> Unit,
    val onAsk: () -> Unit,
    val onWeekTarget: (Int) -> Unit,
    val onDaily: (Boolean) -> Unit,
    val onScan: () -> Unit,
    val onExtras: (Boolean) -> Unit,
    val onDocuments: () -> Unit,
    val onModels: () -> Unit,
    val onList: () -> Unit,
    val onKit: () -> Unit,
    val onPlans: () -> Unit,
    val onPlaces: () -> Unit,
    val onAudio: (Boolean) -> Unit,
    val onAbout: () -> Unit,
)

/**
 * You, from grid screen 22, in the order ADDENDUM-03 Part 20 sets out.
 *
 * The order is the argument. Your own list is first, because a tab called You opens
 * on the person's own words rather than on a switch. Then the four answers that
 * decide what the app offers at all: how you get around, what is in the room,
 * anything to leave out, and whose plan you are on. Then the things the app holds
 * for you, then the ways it can speak to you, then your data and what this is.
 *
 * Nothing on this screen is behind a confirmation, because everything on it is
 * reversible by tapping it again. The three rows that only exist sometimes say so in
 * their own comments; a row that would open a screen with nothing on it is not shown.
 */
@Composable
@Suppress("LongMethod") // One list of rows, in the order Part 20 names them.
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        // Headed by the name on the tab. Somebody who taps You and lands on a screen
        // headed Settings has to work out that they are the same place. It keeps the
        // Settings title when it is reached as a screen rather than as the tab.
        title = stringResource(if (onBack == null) R.string.tab_you else R.string.settings_title),
        onBack = onBack,
        modifier = modifier,
        help = Place.Settings,
    ) {
        ListItem(
            heading = stringResource(R.string.your_list_row),
            subtitle = stringResource(R.string.your_list_sub),
            onClick = actions.onList,
        )

        ListItem(
            heading = stringResource(R.string.settings_getting_around),
            subtitle = state.gettingAroundLabel,
            onClick = actions.onGettingAround,
        )

        ListItem(
            heading = stringResource(R.string.kit_row),
            subtitle = stringResource(R.string.kit_row_sub),
            onClick = actions.onKit,
        )

        ListItem(
            heading = stringResource(R.string.settings_leave_out),
            subtitle = state.exclusionsLabel,
            onClick = actions.onExclusions,
        )

        SwitchRow(
            label = stringResource(R.string.settings_therapist),
            subtitle = stringResource(R.string.settings_therapist_sub),
            checked = state.withTherapist,
            onChange = actions.onTherapist,
        )

        // The plans and their dates, once there is a plan. A row offering to show
        // somebody the plans they have when they have none is a row that opens an
        // apology, and the way in to a first plan is Scan something, three rows down.
        if (state.hasPlan) {
            ListItem(
                heading = stringResource(R.string.plans_row),
                subtitle = stringResource(R.string.plans_row_sub),
                onClick = actions.onPlans,
            )

            // Set once and changeable, which is what makes it a choice rather than a
            // number the app decided for somebody.
            SwitchRow(
                label = stringResource(R.string.settings_extras),
                subtitle = stringResource(R.string.settings_extras_sub),
                checked = state.extras,
                onChange = actions.onExtras,
            )
        }

        ListItem(
            heading = stringResource(R.string.scan_something),
            subtitle = stringResource(R.string.scan_sub),
            onClick = actions.onScan,
        )

        ListItem(
            heading = stringResource(R.string.documents_title),
            subtitle = stringResource(R.string.documents_sub),
            onClick = actions.onDocuments,
        )

        ListItem(
            heading = stringResource(R.string.models_title),
            subtitle = stringResource(R.string.models_sub),
            onClick = actions.onModels,
        )

        // ADDENDUM-03 Part 20 moves this here from the bottom of Progress. It is a
        // thing about the rooms somebody lives in rather than about what changed.
        ListItem(
            heading = stringResource(R.string.places_offer),
            subtitle = stringResource(R.string.places_offer_sub),
            onClick = actions.onPlaces,
        )

        ListItem(
            heading = stringResource(R.string.settings_reminders),
            subtitle = if (state.remindersOn.isEmpty()) {
                stringResource(R.string.settings_reminders_off)
            } else {
                pluralStringResource(
                    R.plurals.settings_reminders_left,
                    state.remindersLeft,
                    state.remindersLeft,
                )
            },
            onClick = actions.onReminders,
        )

        SwitchRow(
            label = stringResource(R.string.settings_daily),
            subtitle = stringResource(R.string.settings_daily_sub),
            checked = state.dailyOn,
            onChange = actions.onDaily,
        )
        if (state.dailyGaveUp) NoteBlock(stringResource(R.string.settings_daily_off))

        // The second half of ADDENDUM-03 Part 1's "on by default, speaker toggle in
        // the session top bar and in You". One setting behind both switches, so
        // turning it off mid session is still off tomorrow.
        SwitchRow(
            label = stringResource(R.string.settings_audio),
            subtitle = stringResource(R.string.settings_audio_sub),
            checked = state.audio,
            onChange = actions.onAudio,
        )

        // Asking a question moved here when the help dot took the top right corner of
        // every screen. Two question marks on Today was one too many.
        ListItem(
            heading = stringResource(R.string.ask_title),
            subtitle = stringResource(R.string.settings_ask_sub),
            onClick = actions.onAsk,
        )

        SectionTitle(stringResource(R.string.settings_week))
        Paragraph(stringResource(R.string.settings_week_sub))
        ThreeUpChoice(
            options = Week.CHOICES.map { stringResource(R.string.week_choice, it) },
            selectedIndex = Week.CHOICES.indexOf(state.weekTarget).takeIf { it >= 0 },
            onSelect = { actions.onWeekTarget(Week.CHOICES[it]) },
        )

        SwitchRow(
            label = stringResource(R.string.settings_weigh_in),
            subtitle = stringResource(R.string.settings_weigh_in_sub),
            checked = state.weighsIn,
            onChange = actions.onWeighsIn,
        )

        SwitchRow(
            label = stringResource(R.string.settings_show_numbers),
            subtitle = stringResource(R.string.settings_show_numbers_sub),
            checked = state.showNumbers,
            onChange = actions.onShowNumbers,
        )

        SwitchRow(
            label = stringResource(R.string.settings_try),
            subtitle = stringResource(R.string.settings_try_sub),
            checked = state.tryItAndSee && !state.pacing,
            onChange = actions.onTryItAndSee,
        )

        ListItem(
            heading = stringResource(R.string.settings_pattern),
            subtitle = state.pemLabel,
            onClick = actions.onPattern,
        )

        if (state.pacing) {
            ListItem(
                heading = stringResource(R.string.settings_pacing),
                subtitle = pluralStringResource(
                    R.plurals.settings_pacing_sub,
                    state.envelopeMinutes,
                    state.envelopeMinutes,
                    state.envelopeDays,
                ),
                onClick = actions.onPacing,
            )
        }

        ListItem(
            heading = stringResource(R.string.data_title),
            subtitle = stringResource(R.string.data_row),
            onClick = actions.onData,
        )

        ListItem(
            heading = stringResource(R.string.about_row),
            subtitle = stringResource(R.string.about_row_sub),
            onClick = actions.onAbout,
        )
    }
}

/**
 * How you get around, changed after setup.
 *
 * The same four cards and the same words as setup, because it is the same
 * question and asking it differently the second time would imply the first answer
 * was a mistake.
 */
@Composable
fun GettingAroundScreen(
    selected: GettingAround,
    onChoose: (GettingAround) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.settings_getting_around),
        onBack = onBack,
        modifier = modifier,
    ) {
        SectionTitle(stringResource(R.string.around_question))

        listOf(
            GettingAround.OnFeet to stringResource(R.string.around_on_feet),
            GettingAround.Walker to stringResource(R.string.around_walker),
            GettingAround.Wheelchair to stringResource(R.string.around_wheelchair),
            GettingAround.InBed to stringResource(R.string.around_in_bed),
        ).forEach { (value, label) ->
            ListItem(
                heading = label,
                next = selected == value,
                onClick = { onChoose(value) },
            )
        }

        Paragraph(stringResource(R.string.around_why))
    }
}

/**
 * The pattern question, asked again.
 *
 * It is in settings rather than only at setup because the pattern it describes
 * can start at any time, and because LOGIC.md section 7 says the app re-asks it
 * whenever somebody leaves pacing mode. Answering yes turns pacing mode on; the
 * app never asks why.
 */
@Composable
fun PatternScreen(
    selected: PemAnswer?,
    onChoose: (PemAnswer) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.settings_pattern),
        onBack = onBack,
        modifier = modifier,
    ) {
        SectionTitle(stringResource(R.string.start_pem))

        listOf(
            PemAnswer.No to stringResource(R.string.start_pem_no),
            PemAnswer.Sometimes to stringResource(R.string.start_pem_sometimes),
            PemAnswer.Yes to stringResource(R.string.start_pem_yes),
        ).forEach { (value, label) ->
            ListItem(
                heading = label,
                next = selected == value,
                onClick = { onChoose(value) },
            )
        }

        Paragraph(stringResource(R.string.start_pem_why))
    }
}

/**
 * Reminders, and the ceiling above them.
 *
 * The ceiling is stated on the screen, in the sentence that says it is a rule in
 * the app rather than a setting. It is the reason four switches here are not four
 * ways to be interrupted more.
 *
 * Nothing here refers to a day somebody missed, on the screen or in the
 * notifications it turns on.
 */
@Composable
fun RemindersScreen(
    on: Set<ReminderKind>,
    left: Int,
    blocked: Boolean,
    hasAppointment: Boolean,
    onToggle: (ReminderKind, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.reminders_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        NoteBlock(stringResource(R.string.reminders_ceiling))

        if (blocked) NoteBlock(stringResource(R.string.reminders_denied), tint = SteadyPalette.SkyL)

        // The appointment switch appears only once there is an appointment. A row
        // offering to remind somebody about a date they have not set is a setting
        // for nothing, and the plan screen is where a date gets set.
        buildList {
            if (hasAppointment) {
                val review = R.string.reminders_review to R.string.reminders_review_sub
                add(ReminderKind.Review to review)
            }
            add(ReminderKind.Walk to (R.string.reminders_walk to R.string.reminders_walk_sub))
            add(ReminderKind.StepReady to (R.string.reminders_step to R.string.reminders_step_sub))
            add(ReminderKind.WeekNote to (R.string.reminders_week to R.string.reminders_week_sub))
            add(ReminderKind.Photo to (R.string.reminders_photo to R.string.reminders_photo_sub))
        }.forEach { (kind, labels) ->
            SwitchRow(
                label = stringResource(labels.first),
                subtitle = stringResource(labels.second),
                checked = kind in on,
                onChange = { onToggle(kind, it) },
            )
        }

        if (on.isNotEmpty()) {
            Paragraph(
                pluralStringResource(R.plurals.settings_reminders_left, left, left),
            )
        }
    }
}

/**
 * Your data: everything out, everything back, and everything gone.
 *
 * The three are on one screen because they are the same promise from three sides,
 * and because somebody who is about to replace or delete everything should be one
 * tap from taking a copy first.
 *
 * Both of the two that cannot be undone ask once, in the same shape: the plain
 * sentence about what is about to happen, "keep it" under the thumb as the
 * primary button, and the one that does the thing as the quiet one. Two
 * destructive answers on one screen have to be asked the same way, or the shape of
 * the question stops meaning anything.
 */
@Composable
fun DataScreen(
    onExport: () -> Unit,
    onSummary: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    confirming: Boolean = false,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
    restore: RestoreStep = RestoreStep.Idle,
    onRestoreConfirm: () -> Unit = {},
    onRestoreCancel: () -> Unit = {},
) {
    val asking = restore as? RestoreStep.Asking
    SteadyScreen(
        title = stringResource(R.string.data_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            if (confirming) {
                PrimaryButton(
                    label = stringResource(R.string.data_delete_cancel),
                    onClick = onCancel,
                )
                SecondaryButton(
                    label = stringResource(R.string.data_delete_confirm),
                    onClick = onConfirm,
                )
            }
            if (asking != null) {
                PrimaryButton(
                    label = stringResource(R.string.data_restore_cancel),
                    onClick = onRestoreCancel,
                )
                SecondaryButton(
                    label = stringResource(R.string.data_restore_confirm),
                    onClick = onRestoreConfirm,
                )
            }
        },
    ) {
        if (confirming) {
            SectionTitle(stringResource(R.string.data_delete_title))
            NoteBlock(stringResource(R.string.data_delete_why))
            return@SteadyScreen
        }

        if (asking != null) {
            SectionTitle(stringResource(R.string.data_restore_title))
            NoteBlock(stringResource(R.string.data_restore_ask))
            Paragraph(
                stringResource(
                    R.string.data_restore_written,
                    asking.backup.header.writtenOn,
                    asking.backup.header.appVersion,
                ),
            )
            return@SteadyScreen
        }

        if (restore is RestoreStep.Working) {
            SectionTitle(stringResource(R.string.data_restore_title))
            NoteBlock(stringResource(R.string.data_restoring))
            return@SteadyScreen
        }

        RestoreNote(restore)

        ListItem(
            heading = stringResource(R.string.data_summary_row),
            subtitle = stringResource(R.string.data_summary_why),
            onClick = onSummary,
        )

        ListItem(
            heading = stringResource(R.string.data_export),
            subtitle = stringResource(R.string.data_export_why),
            onClick = onExport,
        )

        ListItem(
            heading = stringResource(R.string.data_restore),
            subtitle = stringResource(R.string.data_restore_why),
            onClick = onRestore,
        )

        ListItem(
            heading = stringResource(R.string.data_delete),
            subtitle = stringResource(R.string.data_delete_why),
            onClick = onDelete,
        )
    }
}

/**
 * What happened last time, said plainly and only once.
 *
 * A file the app cannot use gets a sentence about that file rather than a shrug,
 * because "no" without a reason leaves somebody trying the same file again. Every
 * one of these says that nothing has changed, which is the thing they actually
 * want to know.
 */
@Composable
private fun RestoreNote(step: RestoreStep) {
    val said = when (step) {
        is RestoreStep.Done -> R.string.data_restored
        is RestoreStep.Refused -> when (step.why) {
            is BackupRead.FromLater -> R.string.data_restore_later
            is BackupRead.Damaged -> R.string.data_restore_damaged
            else -> R.string.data_restore_not_ours
        }
        else -> null
    }
    if (said != null) NoteBlock(stringResource(said))
}

/** Anything to leave out, changed after setup. The same list as setup. */
@Composable
fun LeaveOutSettingsScreen(
    selected: Set<Exclusion>,
    labels: List<Pair<Exclusion, String>>,
    onToggle: (Exclusion) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.settings_leave_out),
        onBack = onBack,
        modifier = modifier,
    ) {
        Paragraph(stringResource(R.string.leave_out_why))
        labels.forEach { (value, label) ->
            ListItem(
                heading = label,
                done = value in selected,
                onClick = { onToggle(value) },
            )
        }
    }
}

/**
 * The limit, and the way out of pacing mode.
 *
 * Leaving is here and nowhere else, because LOGIC.md section 7 says the person
 * is the only one who ends it. The app can put somebody into this mode and can
 * lower the limit, and it can never do the opposite.
 */
@Composable
fun PacingScreen(
    minutes: Int,
    days: Int,
    onMinutes: (Int) -> Unit,
    onDays: (Int) -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.settings_pacing),
        onBack = onBack,
        modifier = modifier,
        footer = {
            PrimaryButton(label = stringResource(R.string.pacing_stop), onClick = onStop)
        },
    ) {
        NoteBlock(stringResource(R.string.move_pacing_note))

        SectionTitle(stringResource(R.string.pacing_minutes))
        Stepper(
            label = stringResource(R.string.pacing_minutes),
            value = pluralStringResource(R.plurals.amount_minutes, minutes, minutes),
            supporting = null,
            onDown = { onMinutes(minutes - 1) },
            onUp = { onMinutes(minutes + 1) },
        )

        SectionTitle(stringResource(R.string.pacing_days))
        Stepper(
            label = stringResource(R.string.pacing_days),
            value = pluralStringResource(R.plurals.pacing_days_value, days, days),
            supporting = null,
            onDown = { onDays(days - 1) },
            onUp = { onDays(days + 1) },
        )

        Paragraph(stringResource(R.string.pacing_stop_why))
    }
}
