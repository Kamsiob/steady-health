package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.DataRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.ReminderRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.PemAnswer
import com.kamsiob.steadyhealth.engine.Envelope
import com.kamsiob.steadyhealth.engine.PacingEngine
import com.kamsiob.steadyhealth.engine.ReminderKind
import com.kamsiob.steadyhealth.engine.WaysOfGettingAround
import com.kamsiob.steadyhealth.export.DataExport
import com.kamsiob.steadyhealth.export.Share
import com.kamsiob.steadyhealth.export.SummaryPages
import com.kamsiob.steadyhealth.export.SummaryPdf
import com.kamsiob.steadyhealth.remind.ReminderWorker
import com.kamsiob.steadyhealth.remind.Reminding
import com.kamsiob.steadyhealth.ui.screens.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Settings, on its own.
 *
 * Separate from [SteadyViewModel] because settings writes and the rest reads:
 * every other tab loads the profile again when it comes back into view, so
 * nothing here has to reach across and tell them. That is also why changing how
 * somebody gets around is safe from here. It writes one row, and the app is a
 * different app the next time Today is drawn.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * The database, resolved on every use rather than held.
     *
     * Deleting everything closes the database and destroys its key, and anything
     * holding the old instance then throws "Database is closed" on its next
     * write. That happened on the phone, on the first screen of setup, right
     * after somebody had deleted everything, which is the worst possible moment
     * for this app to crash.
     *
     * The repositories are stateless wrappers, so resolving them per call costs
     * an object allocation and removes the whole class of bug.
     */
    private val db get() = SteadyDatabase.get(getApplication())
    private val profile get() = ProfileRepository(db)
    private val data get() = DataRepository(db)
    private val reminders get() = ReminderRepository(db)

    private val _settings = MutableStateFlow(SettingsUiState())
    val settings: StateFlow<SettingsUiState> = _settings.asStateFlow()

    private val _pattern = MutableStateFlow<PemAnswer?>(null)

    /** The pattern question's current answer, for its own screen. */
    val pattern: StateFlow<PemAnswer?> = _pattern.asStateFlow()

    private var pacing = false
    private var envelope: Envelope = PacingEngine.DEFAULT
    private var exclusions: Set<Exclusion> = emptySet()

    fun openSettings() = viewModelScope.launch { refreshSettings() }

    private suspend fun refreshSettings() {
        val context = getApplication<Application>()
        val chosen = profile.exclusions()
        exclusions = chosen
        pacing = profile.pacing()
        envelope = profile.envelope()
        _pattern.value = profile.pem()
        val on = ReminderKind.entries.filter { profile.reminderOn(it) }.toSet()

        _settings.value = SettingsUiState(
            gettingAround = profile.gettingAround(),
            gettingAroundLabel = context.getString(labelFor(profile.gettingAround())),
            withTherapist = profile.withTherapist(),
            weighsIn = profile.weighsIn(),
            showNumbers = profile.showNumbers(),
            units = profile.units(),
            exclusions = chosen,
            exclusionsLabel = if (chosen.isEmpty()) {
                context.getString(R.string.settings_leave_out_none)
            } else {
                chosen.joinToString(", ") { context.getString(labelFor(it)) }
            },
            pacing = pacing,
            pemLabel = context.getString(labelFor(_pattern.value)),
            // Off, and not offered, for anybody in pacing mode. LOGIC.md 9b.
            tryItAndSee = profile.tryItAndSee() && !pacing,
            remindersOn = on,
            remindersLeft = reminders.leftThisWeek(System.currentTimeMillis()),
            remindersBlocked = on.isNotEmpty() && !Reminding.allowed(context),
            envelopeMinutes = envelope.minutes,
            envelopeDays = envelope.daysPerWeek,
        )
    }

    /**
     * Change how somebody gets around, after setup.
     *
     * Everything they have done stays where it is: sessions carry the ladder they
     * were done on, so a walk from before a wheelchair is still a walk. What
     * changes is what the app offers from here.
     */
    fun setGettingAround(value: GettingAround) = viewModelScope.launch {
        profile.setGettingAround(value)
        profile.setWeighsIn(WaysOfGettingAround.forWay(value).weighsIn)
        refresh()
    }

    fun setTherapist(value: Boolean) = viewModelScope.launch {
        profile.setWithTherapist(value)
        refreshSettings()
    }

    fun setWeighsIn(value: Boolean) = viewModelScope.launch {
        profile.setWeighsIn(value)
        refreshSettings()
    }

    fun setShowNumbers(value: Boolean) = viewModelScope.launch {
        profile.setShowNumbers(value)
        refreshSettings()
    }

    fun toggleExclusion(value: Exclusion) = viewModelScope.launch {
        val next = if (value in exclusions) exclusions - value else exclusions + value
        profile.setExclusions(next, System.currentTimeMillis())
        refresh()
    }

    fun setEnvelopeMinutes(value: Int) = viewModelScope.launch {
        setEnvelope(envelope.copy(minutes = value.coerceIn(PacingEngine.FLOOR_MINUTES, MAX_ENVELOPE_MINUTES)))
    }

    fun setEnvelopeDays(value: Int) = viewModelScope.launch {
        setEnvelope(envelope.copy(daysPerWeek = value.coerceIn(1, DAYS_IN_WEEK)))
    }

    private suspend fun setEnvelope(value: Envelope) {
        profile.setEnvelope(value)
        envelope = value
        refreshSettings()
    }

    private val _confirmingDelete = MutableStateFlow(false)

    /** True while the delete screen is asking. It asks once and only once. */
    val confirmingDelete: StateFlow<Boolean> = _confirmingDelete.asStateFlow()

    /**
     * Turn one reminder on or off.
     *
     * Turning the first one on is the only moment this app asks for the
     * notification permission, which ONBOARDING.md is explicit about: never at
     * setup, never before somebody has asked for something that needs it.
     *
     * The daily job runs only while at least one is on, and is cancelled outright
     * when the last one goes off. A job that wakes a phone to decide not to speak
     * is still a job waking a phone.
     */
    fun setReminder(kind: ReminderKind, on: Boolean) = viewModelScope.launch {
        profile.setReminderOn(kind, on)
        val context = getApplication<Application>()
        if (profile.anyReminderOn()) {
            ReminderWorker.schedule(context)
        } else {
            ReminderWorker.stop(context)
        }
        refreshSettings()
    }

    fun setTryItAndSee(on: Boolean) = viewModelScope.launch {
        profile.setTryItAndSee(on)
        refreshSettings()
    }

    fun askToDelete() {
        _confirmingDelete.value = true
    }

    fun keepEverything() {
        _confirmingDelete.value = false
    }

    /**
     * Everything out, as ordinary files.
     *
     * PRIVACY.md: spreadsheets and a one-page summary, in a zip anyone can open.
     * Nothing is sent anywhere; the person picks where it goes from the share
     * sheet, and the app has no network permission with which to do otherwise.
     */
    fun exportEverything() = viewModelScope.launch {
        val context = getApplication<Application>()
        // PRIVACY.md promises spreadsheets and a one-page summary, so the summary
        // goes in the zip. It is built by the same pipeline the screen uses, from
        // the same place, so the two cannot drift apart.
        val summary = SummaryPdf.write(
            context = context,
            page = SummaryPages.build(context, db),
            name = "summary.pdf",
        )
        val file = DataExport.write(
            context = context,
            sheets = data.sheets(),
            extras = listOf(summary),
            name = "steady-health-export.zip",
        )
        Share.file(
            context = context,
            file = file,
            mimeType = "application/zip",
            title = context.getString(R.string.data_export),
        )
    }

    /**
     * Everything gone, immediately.
     *
     * Every table, then the database file and its key. The app restarts at the
     * beginning because there is genuinely nothing left for it to open.
     */
    fun deleteEverything(onDone: () -> Unit) = viewModelScope.launch {
        data.deleteEverything(getApplication())
        _confirmingDelete.value = false
        onDone()
    }

    /** Leaving pacing mode. The person's decision, and nothing else's. */
    fun stopPacing() = viewModelScope.launch {
        profile.setPacing(false)
        pacing = false
        refresh()
    }

    /**
     * Read everything back after a change.
     *
     * Only this screen's own state: Today, Move and Abilities each read the
     * profile again when they come back into view, which is the whole reason
     * settings can live in a view model of its own.
     */
    fun refresh() = viewModelScope.launch {
        pacing = profile.pacing()
        envelope = profile.envelope()
        refreshSettings()
    }

    private fun labelFor(value: PemAnswer?) = when (value) {
        PemAnswer.Yes -> R.string.start_pem_yes
        PemAnswer.Sometimes -> R.string.start_pem_sometimes
        else -> R.string.start_pem_no
    }

    /**
     * The pattern question, answered again.
     *
     * Yes turns pacing mode on, because LOGIC.md section 7 makes it a trigger.
     * Changing the answer back does not turn pacing mode off: leaving is its own
     * decision, made on its own screen, and the app does not make it for anybody.
     */
    fun setPattern(value: PemAnswer) = viewModelScope.launch {
        profile.setPem(value)
        if (value == PemAnswer.Yes && !pacing) {
            profile.setPacing(true)
            profile.setEnvelope(envelope)
        }
        refresh()
    }

    private fun labelFor(value: GettingAround) = Labels.forGettingAround(value)

    fun labelFor(value: Exclusion) = Labels.forExclusion(value)

    private companion object {
        const val DAYS_IN_WEEK = 7

        /** Anything above this is not an envelope any more. */
        const val MAX_ENVELOPE_MINUTES = 120
    }
}
