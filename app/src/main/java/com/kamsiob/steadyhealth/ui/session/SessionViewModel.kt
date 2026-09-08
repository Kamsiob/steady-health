package com.kamsiob.steadyhealth.ui.session

import android.app.Application
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.session.Area
import com.kamsiob.steadyhealth.session.Counted
import com.kamsiob.steadyhealth.session.Ending
import com.kamsiob.steadyhealth.session.Felt
import com.kamsiob.steadyhealth.session.SessionEngine
import com.kamsiob.steadyhealth.session.SessionInputs
import com.kamsiob.steadyhealth.session.SessionPlan
import com.kamsiob.steadyhealth.session.SessionRunner
import com.kamsiob.steadyhealth.session.Speech
import com.kamsiob.steadyhealth.session.Stage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Running one session.
 *
 * The state machine is [SessionRunner] and it is pure; this is the part that has a
 * clock, a voice and a screen. Keeping the split means every rule about what a session
 * does is tested without a device, and what is left here is plumbing that either works
 * or obviously does not.
 *
 * The voice is started when a session starts and stopped when it ends, however it
 * ends, which is what makes "never speaks outside a session" true rather than
 * intended.
 */
class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val speech = Speech(application)
    private val db get() = SteadyDatabase.get(getApplication())
    private val runs get() = RunRepository(db)
    private val profile get() = ProfileRepository(db)

    private val _runner = MutableStateFlow<SessionRunner?>(null)
    val runner: StateFlow<SessionRunner?> = _runner.asStateFlow()

    private val _done = MutableStateFlow(DoneUiState())
    val done: StateFlow<DoneUiState> = _done.asStateFlow()

    /** True once the person has pressed the pain button and owes nobody a reason. */
    private val _askingWhereItHurts = MutableStateFlow(false)
    val askingWhereItHurts: StateFlow<Boolean> = _askingWhereItHurts.asStateFlow()

    private var clock: Job? = null
    private var spokenCount = 0
    private var saidMoreThanLast = false

    /** When the session was paused, so an hour away can be noticed. */
    private var pausedAt = 0L
    private var startedAt = 0L
    private var saved = false

    var speaking: Boolean = true
        private set

    /**
     * Plan today's session from what is actually stored, and run it.
     *
     * The one entry point, so nothing anywhere else has to know how a session is put
     * together.
     */
    fun startTodays() = viewModelScope.launch {
        val inputs = inputsFor(profile, runs)
        start(SessionEngine.plan(inputs), first = runs.history().isEmpty())
    }

    /** Run a session somebody already has, for the offer card and the extras. */
    fun start(plan: SessionPlan, first: Boolean = false) {
        startedAt = System.currentTimeMillis()
        _runner.value = SessionRunner(plan)
        _done.value = DoneUiState(first = first)
        _askingWhereItHurts.value = false
        spokenCount = 0
        speech.start { }
        speech.on = speaking
        sayReady()
        runClock()
    }

    /**
     * One tick a second, and only while a session is live.
     *
     * It is cancelled the moment the session ends, so nothing keeps counting behind a
     * screen somebody has left.
     */
    private fun runClock() {
        clock?.cancel()
        clock = viewModelScope.launch {
            var running = true
            while (isActive && running) {
                delay(A_SECOND)
                val before = _runner.value
                if (before == null) {
                    running = false
                } else {
                    val after = before.tick()
                    _runner.value = after
                    speakTick(before, after)
                    if (after.finished) {
                        finish()
                        running = false
                    }
                }
            }
        }
    }

    // --- what the person does -------------------------------------------------

    fun ready() {
        _runner.value = _runner.value?.ready()
        sayCountIn()
    }

    fun skipCountIn() {
        _runner.value = _runner.value?.go()
    }

    /** One rep, from a tap. The camera and the motion sensor call the same thing. */
    fun rep() {
        val before = _runner.value ?: return
        val after = before.rep()
        _runner.value = after
        if (after.count != before.count) speakRep(after)
    }

    fun endSet() {
        val after = _runner.value?.endSet() ?: return
        _runner.value = after
        spokenCount = 0
        saidMoreThanLast = false
        if (after.finished) finish() else sayRest(after)
    }

    fun skipRest() {
        _runner.value = _runner.value?.skipRest()
        _runner.value?.let { if (it.finished) finish() else sayReady() }
    }

    fun pause() {
        val runner = _runner.value ?: return
        if (runner.paused) {
            _runner.value = runner.resume()
            pausedAt = 0
        } else {
            _runner.value = runner.pause()
            pausedAt = SystemClock.elapsedRealtime()
            speech.stop()
        }
    }

    /**
     * True when the session has been paused longer than an hour.
     *
     * ADDENDUM-03 Part 1: somebody will need to answer the door. After an hour the
     * session is saved as far as it got and the app says so plainly.
     */
    fun pausedTooLong(): Boolean =
        pausedAt != 0L && SystemClock.elapsedRealtime() - pausedAt > AN_HOUR

    fun makeItEasier() {
        val after = _runner.value?.makeItEasier() ?: return
        _runner.value = after
        speech.say(string(R.string.exit_eased))
    }

    fun skipThis() {
        val after = _runner.value?.skipThis() ?: return
        _runner.value = after
        if (after.finished) finish() else sayReady()
    }

    fun enough() {
        _runner.value = _runner.value?.enough()
        finish()
    }

    /** One press. The area is asked for afterwards, on its own screen. */
    fun hurts() {
        _runner.value = _runner.value?.hurts()
        _askingWhereItHurts.value = true
        finish()
    }

    fun hurtsIn(area: Area?) = viewModelScope.launch {
        _askingWhereItHurts.value = false
        val chosen = area ?: return@launch
        _runner.value = _runner.value?.hurtsIn(chosen)
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        runs.reportSore(chosen, today)
    }

    /**
     * The one question, and what it changes.
     *
     * The next session is re-planned here and shown on the same screen, because the
     * whole point of asking is that the answer changes something and the person
     * should be able to see that it did.
     */
    fun setFelt(felt: Felt) = viewModelScope.launch {
        _done.value = _done.value.copy(felt = felt)
        runs.saveFelt(felt)
        _done.value = _done.value.copy(nextTime = nextTimeLine(felt))
    }

    private suspend fun nextTimeLine(felt: Felt): String {
        val next = SessionEngine.plan(
            inputsFor(profile, runs).copy(lastFelt = felt, feltBefore = runs.lastFelt()),
        )
        val first = next.main.firstOrNull() ?: return ""
        return string(R.string.done_next_line, first.target, first.movement.name.lowercase())
    }

    /**
     * Everything the engine needs, read back from storage.
     *
     * One place, so the session somebody is offered and the session the done screen
     * predicts are planned from exactly the same inputs.
     */
    private suspend fun inputsFor(
        profile: ProfileRepository,
        runs: RunRepository,
    ): SessionInputs {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        return SessionInputs(
            way = profile.gettingAround(),
            exclusions = profile.exclusions(),
            kit = profile.kit(),
            sore = runs.soreAreas(today),
            history = runs.history(),
            lastFelt = runs.lastFelt(),
            feltBefore = runs.feltBefore(),
            today = today,
            lastSessionDay = runs.lastSessionDay(),
            strengthRunLength = runs.strengthRunLength(),
        )
    }

    /**
     * Keep what was done, once, whatever ended the session.
     *
     * Guarded rather than trusted: the done screen can be reached by four different
     * paths and none of them should write a second row.
     */
    private fun save(runner: SessionRunner) = viewModelScope.launch {
        if (saved) return@launch
        saved = true
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        runs.save(
            epochDay = today,
            startedAt = startedAt,
            endedAt = System.currentTimeMillis(),
            ending = runner.ending?.name.orEmpty(),
            felt = null,
            small = runner.plan.small,
            results = runner.results,
        )
        runner.hurtArea?.let { runs.reportSore(it, today) }
    }

    fun toggleSpeaker() {
        speaking = !speaking
        speech.on = speaking
        if (!speaking) speech.stop()
    }

    // --- the voice ------------------------------------------------------------

    private fun sayReady() {
        val runner = _runner.value ?: return
        val movement = runner.movement ?: return
        speech.say("${movement.name}. ${movement.setup} ${movement.stopRule}")
    }

    private fun sayCountIn() {
        speech.say(string(R.string.say_count_in_three))
        speech.queue(string(R.string.say_count_in_two))
        speech.queue(string(R.string.say_count_in_one))
        speech.queue(string(R.string.say_count_in_go))
    }

    /**
     * Every rep as it is counted, plus two mid-set lines and nothing else.
     *
     * Never a number the engine did not produce: the count comes off the runner, and
     * "more than last time" is only said when there is a last time to be more than.
     */
    private fun speakRep(runner: SessionRunner) {
        val step = runner.step ?: return
        speech.say(string(R.string.say_number, runner.count))
        spokenCount = runner.count

        val left = step.target - runner.count
        if (left == FIVE_MORE) speech.queue(string(R.string.say_five_more))

        val last = step.lastResult
        if (!saidMoreThanLast && last != null && runner.count == last + 1) {
            saidMoreThanLast = true
            speech.queue(string(R.string.say_more_than_last))
        }
    }

    private fun speakTick(before: SessionRunner, after: SessionRunner) {
        val stage = after.stage
        if (before.stage is Stage.Live && stage !is Stage.Live) {
            speech.say(string(R.string.say_set_done))
        }
        if (stage is Stage.Rest && stage.secondsLeft == FIVE_SECONDS) {
            speech.say(string(R.string.say_five_seconds))
        }
    }

    private fun sayRest(runner: SessionRunner) {
        val next = runner.next?.movement?.name ?: return
        speech.say(string(R.string.say_rest_next, next))
    }

    // --- ending ---------------------------------------------------------------

    private fun finish() {
        clock?.cancel()
        clock = null
        val runner = _runner.value ?: return
        save(runner)
        if (runner.ending != Ending.Hurt) speech.say(string(R.string.say_session_done))
        speech.stop()

        _done.value = _done.value.copy(
            rows = runner.results.map { result ->
                val movement = com.kamsiob.steadyhealth.session.Movements.byId(result.movementId)
                DoneRow(
                    name = movement?.name.orEmpty(),
                    value = value(result.count, runner.plan, result.movementId),
                    skipped = result.skipped,
                )
            },
        )
    }

    /** "14, from 12", or "14, the first time". Never a judgement either way. */
    private fun value(count: Int, plan: SessionPlan, movementId: String): String {
        val last = plan.steps.firstOrNull { it.movement.id == movementId }?.lastResult
        return if (last == null) {
            string(R.string.done_first_time, count)
        } else {
            string(R.string.done_against_last, count, last)
        }
    }

    override fun onCleared() {
        clock?.cancel()
        speech.shutdown()
    }

    private fun string(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private companion object {
        const val A_SECOND = 1000L
        const val AN_HOUR = 60L * 60 * 1000
        const val FIVE_MORE = 5
        const val FIVE_SECONDS = 5
    }
}

/** The one place that turns a runner into what the screens draw. */
fun SessionRunner.toUiState(speaking: Boolean, unit: String, stepOf: String): SessionUiState {
    val movement = movement
    return SessionUiState(
        movementName = movement?.name.orEmpty(),
        setup = movement?.setup.orEmpty(),
        stopRule = movement?.stopRule.orEmpty(),
        target = step?.target ?: 0,
        lastResult = step?.lastResult,
        count = reached,
        progress = progress,
        unit = unit,
        countInLeft = (stage as? Stage.CountIn)?.secondsLeft ?: 0,
        restLeft = (stage as? Stage.Rest)?.secondsLeft ?: 0,
        restTotal = SessionRunner.REST_SECONDS,
        nextName = next?.movement?.name,
        justDid = results.lastOrNull()?.let { "${it.count}" },
        paused = paused,
        speaking = speaking,
        counted = movement?.counted ?: Counted.Reps,
        stepOf = stepOf,
        eased = easedIds.isNotEmpty(),
    )
}
