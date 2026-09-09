package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.entity.RunEntity
import com.kamsiob.steadyhealth.session.Ending
import com.kamsiob.steadyhealth.session.Felt
import com.kamsiob.steadyhealth.session.Movements
import com.kamsiob.steadyhealth.session.Result
import com.kamsiob.steadyhealth.session.Week
import com.kamsiob.steadyhealth.ui.screens.HistoryRow
import com.kamsiob.steadyhealth.ui.screens.LogPastUiState
import com.kamsiob.steadyhealth.ui.screens.PastDay
import com.kamsiob.steadyhealth.ui.screens.PastMovement
import com.kamsiob.steadyhealth.ui.screens.PastSessionUiState
import com.kamsiob.steadyhealth.ui.screens.SessionsUiState
import com.kamsiob.steadyhealth.ui.screens.SundayUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * The Sessions tab, and logging a session that already happened.
 *
 * Its own view model rather than more of SteadyViewModel, which has now grown past
 * detekt's size rule four times. The split works for the same reason the others did:
 * this tab reloads what it needs when it comes back into view rather than being told
 * to by whoever changed something.
 */
class SessionsViewModel(application: Application) : AndroidViewModel(application) {

    private val db get() = SteadyDatabase.get(getApplication())
    private val runs get() = RunRepository(db)
    private val profile get() = ProfileRepository(db)
    private val cards get() = TodayCards(getApplication(), db)

    private val _state = MutableStateFlow(SessionsUiState())
    val state: StateFlow<SessionsUiState> = _state.asStateFlow()

    private val _logPast = MutableStateFlow(LogPastUiState())
    val logPast: StateFlow<LogPastUiState> = _logPast.asStateFlow()

    fun refresh() = viewModelScope.launch {
        val today = today()
        val way = profile.gettingAround()
        val exclusions = profile.exclusions()
        _state.value = SessionsUiState(
            session = cards.sessionCard(today, way, exclusions),
            library = cards.library(way, exclusions, today),
            history = history(today),
        )
    }

    /**
     * The app's extras, off or back on, from the card itself. ADDENDUM-03 Part 6.
     *
     * The Sessions tab shows the same card Today does, so it carries the same one
     * tap. Both write the one setting, and this tab reloads what it needs when it
     * comes back into view, so the two cards cannot end up disagreeing.
     */
    fun toggleExtras() = viewModelScope.launch {
        profile.setExtras(!profile.extras())
        refresh()
    }

    private val _past = MutableStateFlow(PastSessionUiState())
    val past: StateFlow<PastSessionUiState> = _past.asStateFlow()

    private val _sunday = MutableStateFlow(SundayUiState())
    val sundayReview: StateFlow<SundayUiState> = _sunday.asStateFlow()

    /** Open the Sunday review. Never sent, never notified: opened. */
    fun openSunday() = viewModelScope.launch {
        val from = LocalDate.now(ZoneId.systemDefault())
        val names = (0 until Week.DAYS).map {
            from.plusDays(it.toLong())
                .dayOfWeek
                .getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
        // The day names are read here rather than in the sentence maker because a day
        // of the week is a locale's business.
        val review = cards.sunday(today(), names)

        // ADDENDUM-03 Part 11: the card is offered once, at the second Sunday
        // review. Counted by the reviews actually opened rather than by the weeks
        // that have passed, because somebody who has never opened one has not had a
        // first review to have a second one after.
        //
        // showOnce answers and records in the same call, so the first line is false
        // on the first review and true from the second, and the second line is true
        // exactly once. Declining is scrolling past, and it does not come back here.
        val now = System.currentTimeMillis()
        val secondOrLater = !profile.showOnce(SUNDAY_SEEN, now)
        val offer = secondOrLater && profile.showOnce(CARD_OFFERED, now)

        _sunday.value = review.copy(offerCard = offer)
    }

    /** Open one session already done, to read it, fix it, or remove it. */
    fun openPast(runId: Long) = viewModelScope.launch {
        val (run, movements) = runs.session(runId) ?: return@launch
        _past.value = PastSessionUiState(
            runId = runId,
            whenIt = whenSaid(today() - run.epochDay),
            how = howItWent(run),
            movements = movements.filterNot { it.skipped }.map {
                PastMovement(
                    movementId = it.movementId,
                    name = Movements.byId(it.movementId)?.name.orEmpty(),
                    count = it.count,
                )
            },
        )
    }

    /** A number changed by hand, written straight through. The app says nothing. */
    fun correctPast(movementId: String, count: Int) = viewModelScope.launch {
        val state = _past.value
        val wanted = count.coerceAtLeast(0)
        runs.correct(state.runId, movementId, wanted)
        _past.value = state.copy(
            movements = state.movements.map {
                if (it.movementId == movementId) it.copy(count = wanted) else it
            },
        )
        refresh()
    }

    fun removePast(onDone: () -> Unit) = viewModelScope.launch {
        runs.remove(_past.value.runId)
        _past.value = PastSessionUiState()
        refresh()
        onDone()
    }

    /** Open the log screen with the last seven days on it, today included. */
    fun openLogPast() = viewModelScope.launch {
        val today = today()
        _logPast.value = LogPastUiState(
            days = (0 until Week.DAYS).map { back ->
                PastDay(epochDay = today - back, label = whenSaid(back.toLong()))
            },
            chosenDay = today,
            movements = cards
                .library(profile.gettingAround(), profile.exclusions(), today)
                .filterNot { it.leftOut },
        )
    }

    fun chooseLogDay(day: Long) = _logPast.update { it.copy(chosenDay = day) }

    fun toggleLogMovement(id: String) = _logPast.update { state ->
        state.copy(chosen = if (id in state.chosen) state.chosen - id else state.chosen + id)
    }

    /**
     * Write a session that already happened. ADDENDUM-03 Part 14.
     *
     * Every movement is recorded as self reported with no number, because somebody
     * logging Tuesday's walk on Thursday does not remember how many, and being asked
     * would turn three taps into an interrogation. It counts as a session everywhere
     * a session counts.
     */
    fun saveLogPast(onDone: () -> Unit) = viewModelScope.launch {
        val state = _logPast.value
        val day = state.chosenDay ?: return@launch
        if (state.chosen.isEmpty()) return@launch
        val at = System.currentTimeMillis()
        runs.save(
            epochDay = day,
            startedAt = at,
            endedAt = at,
            ending = Ending.Finished.name,
            felt = null,
            small = false,
            results = state.chosen.map { id ->
                Result(movementId = id, target = 0, count = 0, selfReported = true)
            },
        )
        _logPast.value = LogPastUiState()
        refresh()
        onDone()
    }

    /**
     * What has been done, newest first, said the way somebody would say it.
     *
     * "Yesterday" rather than a date, because that is how anybody talks about the
     * session before this one, and a date is only useful much further back.
     */
    private suspend fun history(today: Long): List<HistoryRow> =
        runs.sessions().map { (run, movements) ->
            val kept = movements.filterNot { it.skipped }
            HistoryRow(
                runId = run.id,
                whenIt = whenSaid(today - run.epochDay),
                what = plural(R.plurals.history_movements, kept.size, kept.size),
                how = howItWent(run),
            )
        }

    private fun whenSaid(daysAgo: Long): String = when (daysAgo) {
        0L -> string(R.string.history_today)
        1L -> string(R.string.history_yesterday)
        else -> string(R.string.history_days_ago, daysAgo.toInt())
    }

    private fun howItWent(run: RunEntity): String? = when {
        run.ending == Ending.Hurt.name -> string(R.string.history_something_hurt)
        run.felt == Felt.Easy.id -> string(R.string.history_felt_easy)
        run.felt == Felt.AboutRight.id -> string(R.string.history_felt_right)
        run.felt == Felt.Hard.id -> string(R.string.history_felt_hard)
        run.ending == Ending.EnoughForToday.name -> string(R.string.history_stopped_early)
        else -> null
    }

    private fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private fun string(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        getApplication<Application>().resources.getQuantityString(id, count, *args)

    private companion object {
        /** The first Sunday review, so the second one can be told from it. */
        const val SUNDAY_SEEN = "sunday_review_seen"

        /** The one offer of the card. Part 11 says once. */
        const val CARD_OFFERED = "card_offered_on_sunday"
    }
}
