package com.kamsiob.steadyhealth.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.BuildConfig
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.backup.Backup
import com.kamsiob.steadyhealth.backup.BackupFile
import com.kamsiob.steadyhealth.backup.BackupRead
import com.kamsiob.steadyhealth.backup.BackupRepository
import com.kamsiob.steadyhealth.backup.RestoreStep
import com.kamsiob.steadyhealth.data.AbilityRepository
import com.kamsiob.steadyhealth.data.ContextRepository
import com.kamsiob.steadyhealth.data.DailyPromptRepository
import com.kamsiob.steadyhealth.data.DataRepository
import com.kamsiob.steadyhealth.data.PlanRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.ReminderRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.entity.TrackedItemEntity
import com.kamsiob.steadyhealth.domain.AbilityDomain
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
import com.kamsiob.steadyhealth.export.TextFile
import com.kamsiob.steadyhealth.remind.ReminderWorker
import com.kamsiob.steadyhealth.remind.Reminding
import com.kamsiob.steadyhealth.session.ChairHeight
import com.kamsiob.steadyhealth.session.Kit
import com.kamsiob.steadyhealth.ui.screens.KitUiState
import com.kamsiob.steadyhealth.ui.screens.ListedItem
import com.kamsiob.steadyhealth.ui.screens.PlanRow
import com.kamsiob.steadyhealth.ui.screens.PlansUiState
import com.kamsiob.steadyhealth.ui.screens.SettingsUiState
import com.kamsiob.steadyhealth.ui.screens.YourListUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

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
    private val backups get() = BackupRepository(db)
    private val abilities get() = AbilityRepository(db)
    private val plans get() = PlanRepository(db)
    private val context get() = ContextRepository(db)

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
        val live = plans.live()

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
            weekTarget = profile.weekTarget(),
            dailyGaveUp = profile.dailyGaveUp(),
            hasPlan = live.isNotEmpty(),
            hasAppointment = live.any { it.reviewDay != null },
            extras = profile.extras(),
            dailyOn = profile.reminderOn(ReminderKind.Daily),
            audio = Audio.on(db),
            loaded = true,
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
        // Weighing in can be turned off by a way of getting around and never back on
        // by one. In bed a daily weight is not part of the picture, so it goes; on
        // feet it is a thing somebody chose, and switching back from the bed version
        // of the app should not undo a switch they set themselves.
        if (!WaysOfGettingAround.forWay(value).weighsIn) profile.setWeighsIn(false)
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
        // Turning the daily one back on starts from nothing, which is what on means.
        // Otherwise it would be off again in a day, having counted the week somebody
        // was away as eight more dismissals.
        if (kind == ReminderKind.Daily && on) {
            profile.setDailyGaveUp(false)
            DailyPromptRepository(db).forget()
        }
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
     * Everything out, as ordinary files, and the backup in the same zip.
     *
     * PRIVACY.md: spreadsheets, your photos and a one-page summary, in a zip anyone
     * can open. The photographs are the scanned pages, written out as the JPEGs
     * they already are rather than left as Base64 inside the backup, because a
     * photograph nobody can look at is not a photograph.
     *
     * The backup goes in beside them rather than into a second file somebody has
     * to keep separately, because the export is the thing people already know to
     * take, and a backup that lives somewhere else is the one that is not there
     * when it is needed.
     *
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
        val backup = backups.read(BuildConfig.VERSION_NAME, LocalDate.now())
        val file = DataExport.write(
            context = context,
            sheets = data.sheets(),
            extras = listOf(summary),
            name = "steady-health-export.zip",
            files = listOf(TextFile(Backup.FILE_NAME, BackupFile.write(backup))),
            pictures = data.pictures(),
        )
        Share.file(
            context = context,
            file = file,
            mimeType = "application/zip",
            title = context.getString(R.string.data_export),
        )
    }

    private val _restore = MutableStateFlow<RestoreStep>(RestoreStep.Idle)

    /** Where putting a backup back has got to. Idle unless somebody is doing it. */
    val restore: StateFlow<RestoreStep> = _restore.asStateFlow()

    /**
     * Somebody picked a file, so read it and say what it is.
     *
     * This is the only file this app ever reads that it did not write, and it is
     * read here rather than acted on: the person is asked afterwards, once, with
     * the date on the backup in front of them. A file that cannot be used is turned
     * away now, before anybody is asked to agree to anything.
     */
    fun readBackup(uri: Uri) = viewModelScope.launch {
        val bytes = withContext(Dispatchers.IO) { bytesOf(uri) }
        _restore.value = when (val read = bytes?.let(BackupFile::read)) {
            is BackupRead.Ready -> RestoreStep.Asking(read.backup)
            null -> RestoreStep.Refused(BackupRead.NotOurs)
            else -> RestoreStep.Refused(read)
        }
    }

    /**
     * A file somebody chose, as bytes, or nothing.
     *
     * A content URI can be withdrawn between the picker closing and the read, and
     * a provider can hand back an error rather than a stream. Neither is worth a
     * crash: both mean the same thing to the person, which is that the app could
     * not read that file.
     */
    private fun bytesOf(uri: Uri): ByteArray? = runCatching {
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()

    fun keepWhatIsHere() {
        _restore.value = RestoreStep.Idle
    }

    /**
     * Replace everything with what is in the file.
     *
     * Only from [RestoreStep.Asking], which is only reached by a person answering
     * the question on the screen. Afterwards the settings are read again, because
     * the ones in the file are the ones that count now, and the daily job is
     * started or stopped to match what the restored setup asks for.
     */
    fun restoreEverything() = viewModelScope.launch {
        val asking = _restore.value as? RestoreStep.Asking ?: return@launch
        // Working is also what stops a second tap starting a second restore: the
        // line above only lets this through while there is a question on the screen.
        _restore.value = RestoreStep.Working
        backups.restore(asking.backup)
        _restore.value = RestoreStep.Done
        val context = getApplication<Application>()
        if (profile.anyReminderOn()) {
            ReminderWorker.schedule(context)
        } else {
            ReminderWorker.stop(context)
        }
        refresh()
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

    fun setExtras(on: Boolean) = viewModelScope.launch {
        profile.setExtras(on)
        refresh()
    }

    fun setWeekTarget(value: Int) = viewModelScope.launch {
        profile.setWeekTarget(value)
        refresh()
    }

    fun labelFor(value: Exclusion) = Labels.forExclusion(value)

    /** The voice in a session, from You. The session's own switch writes the same setting. */
    fun setAudio(on: Boolean) = viewModelScope.launch {
        Audio.set(db, on)
        refreshSettings()
    }

    // --- Your list -----------------------------------------------------------

    private val _yourList = MutableStateFlow(YourListUiState())

    /** The list, and whichever of it is open. ADDENDUM-03 Part 20 puts it first on You. */
    val yourList: StateFlow<YourListUiState> = _yourList.asStateFlow()

    fun openList() = viewModelScope.launch { refreshList() }

    private suspend fun refreshList() {
        val numbersOn = profile.showNumbers()
        val rows = abilities.items().map { item ->
            ListedItem(
                id = item.id,
                text = item.text,
                domain = AbilityDomain.fromId(item.domain) ?: AbilityDomain.Go,
                said = ratingSaid(item.id, numbersOn),
            )
        }
        val open = _yourList.value.editing?.id
        _yourList.value = YourListUiState(
            items = rows,
            editing = rows.firstOrNull { it.id == open },
            typed = _yourList.value.typed,
        )
    }

    /**
     * The last rating, as a number, or nothing at all.
     *
     * Nothing when numbers are off, rather than a direction word. A word here would
     * be about the month before, and this screen is where the list is changed rather
     * than where it is read back; Progress is where a rating becomes a word.
     */
    private suspend fun ratingSaid(itemId: Long, numbersOn: Boolean): String {
        if (!numbersOn) return ""
        val rating = abilities.latestRating(itemId) ?: return ""
        return getApplication<Application>().getString(R.string.rating_now, rating.rating)
    }

    fun openItem(item: ListedItem) {
        _yourList.value = _yourList.value.copy(editing = item, typed = item.text)
    }

    fun setItemText(text: String) {
        _yourList.value = _yourList.value.copy(typed = text)
    }

    /**
     * One item's words, changed.
     *
     * The row is rewritten rather than replaced, so every monthly rating already
     * against it stays attached to it. Somebody wording the same thing better has not
     * started a different thing, and losing a year of ratings to a typo being fixed
     * would teach people not to touch this screen.
     */
    fun saveItem(onDone: () -> Unit) = viewModelScope.launch {
        val open = _yourList.value.editing
        val text = _yourList.value.typed.trim()
        if (open != null && text.isNotBlank()) {
            rewrite(open.id) { it.copy(text = text) }
            refreshList()
        }
        onDone()
    }

    /**
     * One item off the list.
     *
     * Put away rather than deleted, which is what lets the screen promise that
     * everything already said about it stays where it is. A monthly rating is a fact
     * about a month, and somebody changing what they are working towards does not
     * make those months untrue.
     */
    fun removeItem(onDone: () -> Unit) = viewModelScope.launch {
        val open = _yourList.value.editing
        if (open != null) {
            rewrite(open.id) { it.copy(archivedAt = System.currentTimeMillis()) }
            _yourList.value = _yourList.value.copy(editing = null, typed = "")
            refreshList()
        }
        onDone()
    }

    /**
     * One item, changed in place.
     *
     * AbilityRepository can add an item, rate one and read them back, and has nothing
     * that changes one, because until Part 20 gave the list a screen nothing could.
     * This reaches past it to the same upsert it would call rather than going around
     * it, and on the day that repository grows a rename and an archive these two uses
     * become those. It is one place rather than two so there is one thing to move.
     */
    private suspend fun rewrite(id: Long, change: (TrackedItemEntity) -> TrackedItemEntity) {
        abilities.items().firstOrNull { it.id == id }?.let {
            db.abilities().upsertItem(change(it))
        }
    }

    /** Part 18's job 1, asked again: one sentence, kept exactly as it was given. */
    fun addItem(text: String, domain: AbilityDomain, onDone: () -> Unit) = viewModelScope.launch {
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) abilities.add(trimmed, domain, System.currentTimeMillis())
        refreshList()
        onDone()
    }

    // --- Equipment and the chair ---------------------------------------------

    private val _kit = MutableStateFlow(KitUiState())

    /** What is in the room, and which chair. */
    val kit: StateFlow<KitUiState> = _kit.asStateFlow()

    fun openKit() = viewModelScope.launch { refreshKit() }

    private suspend fun refreshKit() {
        _kit.value = KitUiState(
            kit = profile.kit(),
            chairHasArms = profile.chairHasArms(),
            chairHeight = context.theChair().height,
        )
    }

    /**
     * One piece of equipment, on or off.
     *
     * [Kit.None] goes back in whatever happens. It is not a thing anybody owns; it is
     * what a movement that needs nothing is matched against, and a set without it
     * would leave the plainest movements in the library needing something.
     */
    fun toggleKit(piece: Kit) = viewModelScope.launch {
        val held = profile.kit()
        val next = if (piece in held) held - piece else held + piece
        profile.setKit(next + Kit.None)
        refreshKit()
    }

    fun setChairArms(value: Boolean) = viewModelScope.launch {
        profile.setChairHasArms(value)
        refreshKit()
    }

    /**
     * The chair, named for the first time or changed.
     *
     * TheChair decides which of those it is and whether the app owes anybody a
     * sentence about numbers done from the old seat. Answering the same height again
     * is not a change and does not move that boundary, which is why this hands the
     * answer over rather than deciding anything itself.
     */
    fun setChairHeight(height: ChairHeight) = viewModelScope.launch {
        context.setTheChair(context.theChair().changedTo(height, LocalDate.now().toEpochDay()))
        refreshKit()
    }

    // --- Their plans ---------------------------------------------------------

    private val _plans = MutableStateFlow(PlansUiState())

    /** Every live plan, each under its own name. ADDENDUM-03 Part 6. */
    val plansState: StateFlow<PlansUiState> = _plans.asStateFlow()

    fun openPlans() = viewModelScope.launch { refreshPlans() }

    private suspend fun refreshPlans() {
        val app = getApplication<Application>()
        val word = app.getString(R.string.plan_them)
        _plans.value = PlansUiState(
            plans = plans.liveWithItems().map { (plan, items) ->
                PlanRow(
                    id = plan.id,
                    // Somebody will leave the label empty, and a heading with nothing
                    // in it is worse than a plan that does not name whose it is. The
                    // same fallback and the same word as the card on Today.
                    label = plan.label.trim().ifBlank { word },
                    howMany = app.resources.getQuantityString(
                        R.plurals.plan_how_many,
                        items.size,
                        items.size,
                    ),
                    reviewDay = plan.reviewDay,
                )
            },
        )
    }

    /**
     * When they next go in, set or moved or taken off.
     *
     * The settings screen is read again afterwards because the reminders screen only
     * offers the appointment switch once there is an appointment, and that answer has
     * just changed.
     */
    fun setReviewDay(planId: Long, day: Long?) = viewModelScope.launch {
        plans.setReviewDay(planId, day)
        refreshPlans()
        refreshSettings()
    }

    /** A plan put away. Nothing done from it goes with it; see PlanRepository.archive. */
    fun archivePlan(planId: Long) = viewModelScope.launch {
        plans.archive(planId, System.currentTimeMillis())
        refreshPlans()
        refreshSettings()
    }

    private companion object {
        const val DAYS_IN_WEEK = 7

        /** Anything above this is not an envelope any more. */
        const val MAX_ENVELOPE_MINUTES = 120
    }
}
