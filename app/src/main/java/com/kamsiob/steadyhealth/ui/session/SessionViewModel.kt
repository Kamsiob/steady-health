package com.kamsiob.steadyhealth.ui.session

import android.app.Application
import android.os.SystemClock
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.sensing.Motion
import com.kamsiob.steadyhealth.sensing.RepCounter
import com.kamsiob.steadyhealth.session.Adaptation
import com.kamsiob.steadyhealth.session.Area
import com.kamsiob.steadyhealth.session.Buzz
import com.kamsiob.steadyhealth.session.Counted
import com.kamsiob.steadyhealth.session.Ending
import com.kamsiob.steadyhealth.session.Felt
import com.kamsiob.steadyhealth.session.Movements
import com.kamsiob.steadyhealth.session.Result
import com.kamsiob.steadyhealth.session.Sensed
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    private val buzz = Buzz(application)
    private val motion = Motion(application)
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
    private var sensing: Job? = null
    private var sensed = 0
    private var spokenCount = 0
    private var saidMoreThanLast = false

    /** When the session was paused, so an hour away can be noticed. */
    private var pausedAt = 0L

    /** True when the pause was the app's doing rather than the person's. */
    private var pausedByTheApp = false
    private var startedAt = 0L
    private var pacing = false
    private var paceUp = true
    private var saved = false

    /** The row this session was written to, so a correction can find it again. */
    private var savedRunId: Long? = null

    var speaking: Boolean = true
        private set

    /**
     * The accelerometer, on only while a set it can count is actually running.
     *
     * Driven off the state rather than off each button, because there are eight ways
     * out of a live set and a listener left registered by the one that was forgotten
     * is a battery leak nobody would find. Anything the sensor counts is a rep like
     * any other, so tapping still works for somebody holding the phone.
     */
    init {
        viewModelScope.launch {
            _runner
                .map { it?.let { runner -> Triple(runner.stage, runner.paused, runner.movement?.sensedBy) } }
                .distinctUntilChanged()
                .collect { key -> senseWhile(key?.first, key?.second == true, key?.third) }
        }
    }

    private fun senseWhile(stage: Stage?, paused: Boolean, sensedBy: Sensed?) {
        sensing?.cancel()
        sensing = null
        val counter = when {
            stage != Stage.Live || paused -> return
            sensedBy == Sensed.Steps -> RepCounter.forSteps()
            sensedBy == Sensed.Stands -> RepCounter.forStands()
            else -> return
        }
        sensed = 0
        sensing = viewModelScope.launch {
            motion.counts(counter).collect { count ->
                repeat((count - sensed).coerceAtLeast(0)) { rep() }
                sensed = count
            }
        }
    }

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

    /**
     * The ninety second version. ADDENDUM-03 Part 2, tiredness.
     *
     * One movement, and it is saved as a session like any other. Nothing anywhere
     * records that it was the short one, because a record of the days somebody could
     * only manage ninety seconds is not what this app is.
     */
    fun startSomethingSmall() = viewModelScope.launch {
        val inputs = inputsFor(profile, runs).copy(wantSmall = true)
        start(SessionEngine.plan(inputs), first = runs.history().isEmpty())
    }

    /**
     * One movement, on its own, chosen from the library.
     *
     * Planned through the same engine as everything else, so the number it asks for
     * is the number it would have asked for inside a session.
     */
    fun startOne(movementId: String) = viewModelScope.launch {
        val movement = Movements.byId(movementId) ?: return@launch
        val inputs = inputsFor(profile, runs)
        val plan = SessionPlan(
            steps = listOf(SessionEngine.stepFor(movement, inputs)),
            adaptation = Adaptation(Adaptation.Kind.None),
            small = true,
            feeds = listOf(movement.domain),
        )
        start(plan, first = runs.history().isEmpty())
    }

    /**
     * A session somebody has already done, offered again.
     *
     * The movements are theirs; the numbers are today's, because doing March again
     * with March's numbers is not what "do this again" means.
     */
    fun repeat(runId: Long) = viewModelScope.launch {
        val movements = runs.movementsOf(runId).mapNotNull { Movements.byId(it) }
        if (movements.isEmpty()) return@launch
        val inputs = inputsFor(profile, runs)
        val plan = SessionPlan(
            steps = movements.map { SessionEngine.stepFor(it, inputs) },
            adaptation = Adaptation(Adaptation.Kind.None),
            feeds = movements.map { it.domain }.distinct(),
        )
        start(plan, first = runs.history().isEmpty())
    }

    // --- a session done away from the phone. ADDENDUM-03 Part 14 ---------------

    private val _phoneFree = MutableStateFlow(PhoneFreeUiState())
    val phoneFree: StateFlow<PhoneFreeUiState> = _phoneFree.asStateFlow()

    /** Today's session, written out, for somebody who is going to put the phone down. */
    fun openPhoneFree() = viewModelScope.launch {
        val plan = SessionEngine.plan(inputsFor(profile, runs))
        _phoneFree.value = PhoneFreeUiState(
            rows = plan.steps.map { step ->
                PhoneFreeRow(
                    movementId = step.movement.id,
                    name = step.movement.name,
                    setup = step.movement.setup,
                    stopRule = step.movement.stopRule,
                    asked = step.target,
                    managed = step.target,
                    counted = step.movement.counted,
                )
            },
        )
    }

    /**
     * Read the whole session out, or stop reading it.
     *
     * The same voice the session uses, started and stopped here rather than by a
     * session, because there is no session running and there is not going to be one.
     */
    fun readPhoneFree() {
        val state = _phoneFree.value
        if (state.reading) {
            speech.stop()
            _phoneFree.value = state.copy(reading = false)
            return
        }
        speech.start { }
        speech.on = true
        state.rows.forEachIndexed { at, row ->
            val said = "${row.name}. ${row.setup} ${row.stopRule}"
            if (at == 0) speech.say(said) else speech.queue(said)
        }
        _phoneFree.value = state.copy(reading = true)
    }

    fun phoneFreeDone() {
        speech.stop()
        _phoneFree.update { it.copy(reading = false, logging = true) }
    }

    fun phoneFreeManaged(movementId: String, count: Int) = _phoneFree.update { state ->
        state.copy(
            rows = state.rows.map {
                if (it.movementId == movementId) it.copy(managed = count.coerceAtLeast(0)) else it
            },
        )
    }

    /**
     * Keep it. Every number is marked as the person's own, because it is.
     *
     * ADDENDUM-03 Part 14 asks for that to be a footnote rather than a demotion, and
     * nothing in the engine reads `selfReported` when it plans the next session.
     */
    fun savePhoneFree(onDone: () -> Unit) = viewModelScope.launch {
        val rows = _phoneFree.value.rows
        if (rows.isEmpty()) return@launch
        val at = System.currentTimeMillis()
        runs.save(
            epochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay(),
            startedAt = at,
            endedAt = at,
            ending = Ending.Finished.name,
            felt = null,
            small = false,
            results = rows.map { row ->
                Result(
                    movementId = row.movementId,
                    target = row.asked,
                    count = row.managed,
                    selfReported = true,
                )
            },
        )
        _phoneFree.value = PhoneFreeUiState()
        onDone()
    }

    /** Run a session somebody already has, for the offer card and the extras. */
    fun start(plan: SessionPlan, first: Boolean = false) {
        startedAt = System.currentTimeMillis()
        saved = false
        savedRunId = null
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

    fun ready() = viewModelScope.launch {
        val movement = _runner.value?.movement
        // On for the first three sessions of any new movement, then off unless the
        // person keeps it. Somebody who has done chair stands forty times does not
        // need to be told when to go up.
        pacing = movement != null && runs.timesDone(movement.id) < PACING_SESSIONS
        paceUp = true
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
        if (after.count != before.count) {
            buzz.rep()
            speakRep(after)
        }
    }

    fun endSet() {
        val after = _runner.value?.endSet() ?: return
        _runner.value = after
        spokenCount = 0
        saidMoreThanLast = false
        buzz.setDone()
        if (after.finished) finish() else sayRest(after)
    }

    fun skipRest() {
        _runner.value = _runner.value?.skipRest()
        _runner.value?.let { if (it.finished) finish() else sayReady() }
    }

    fun pause() {
        val runner = _runner.value ?: return
        pausedByTheApp = false
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
     * The phone was locked, or a call came in, or somebody switched away.
     *
     * ADDENDUM-03 Part 1 and Part 15b: a session must not keep counting behind a
     * screen nobody is looking at. Held sets would finish themselves and the person
     * would come back to a session that had done itself without them.
     */
    fun interrupted() {
        val runner = _runner.value ?: return
        if (runner.paused || runner.finished) return
        pausedByTheApp = true
        _runner.value = runner.pause()
        pausedAt = SystemClock.elapsedRealtime()
        speech.stop()
    }

    /**
     * Back again, and it picks up where it stopped.
     *
     * A pause the person pressed themselves stays pressed, because they meant it.
     * Only the one the app took is given back. After an hour the session is saved as
     * far as it got and says so, rather than resuming a set from before lunch.
     */
    fun returned() {
        if (!pausedByTheApp) return
        val runner = _runner.value ?: return
        pausedByTheApp = false
        if (!runner.paused) return
        if (pausedTooLong()) {
            _done.value = _done.value.copy(awayTooLong = true)
            _runner.value = runner.resume().enough()
            pausedAt = 0
            finish()
            return
        }
        _runner.value = runner.resume()
        pausedAt = 0
        val movement = runner.movement
        if (movement != null) speech.say("${string(R.string.session_back)} ${movement.name}")
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
            // The answer that was just given is the last one, and the one before it is
            // the session before this one. Reading lastFelt() here counted the answer
            // twice and told somebody the last two felt easy after a single session.
            inputsFor(profile, runs).copy(lastFelt = felt, feltBefore = runs.feltBefore()),
        )
        // The main movements first, because that is what somebody pictures, but an
        // easy day has none and saying nothing about it would make the answer look
        // like it changed nothing.
        val opening = next.main.firstOrNull() ?: next.steps.firstOrNull() ?: return ""
        val shape = plural(
            R.plurals.done_next_shape,
            next.minutes,
            next.minutes,
            opening.movement.name.lowercase(),
        )
        val why = adaptationLine(next.adaptation)
        return listOfNotNull(shape, why).joinToString(" ")
    }

    /**
     * The one sentence for whatever the engine changed.
     *
     * Exactly one, chosen by the engine, never two and never written by a model.
     */
    private fun adaptationLine(adaptation: Adaptation): String? = when (adaptation.kind) {
        Adaptation.Kind.Lighter -> string(R.string.next_lighter)
        Adaptation.Kind.Swapped ->
            adaptation.movementName?.let { string(R.string.next_swapped, it.lowercase()) }
        Adaptation.Kind.Shorter -> string(R.string.next_shorter)
        Adaptation.Kind.OneMore -> string(R.string.next_one_more)
        Adaptation.Kind.EasyDay -> string(R.string.next_easy_day)
        Adaptation.Kind.AfterUnwell -> string(R.string.next_after_unwell)
        Adaptation.Kind.None -> null
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
        savedRunId = runs.save(
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

    /** Turn the pacing cue on or off. It is on for the first three of a movement. */
    fun setPacing(value: Boolean) {
        pacing = value
    }

    // --- the voice ------------------------------------------------------------

    private fun sayReady() {
        val runner = _runner.value ?: return
        val movement = runner.movement ?: return
        speech.say("${movement.name}. ${movement.setup} ${movement.stopRule}")
        // Once, on the first movement. Saying it before every set would be nagging,
        // and by the second one the person already knows.
        if (runner.at == 0) speech.queue(string(R.string.say_starts_itself))
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
        if (before.stage is Stage.Rest && stage !is Stage.Rest) buzz.restOver()

        // Halfway to giving up on a set nothing is happening in, said once. The set
        // ends on its own a little later, and somebody who has put the phone down and
        // walked off should hear why rather than find it finished.
        if (stage is Stage.Live && after.sinceLastRep == SessionRunner.STALLED_SECONDS / 2) {
            speech.say(string(R.string.say_still_there))
        }

        // The pacing cue: "up... and down", on for the first three sessions of any
        // new movement and then off unless kept. It is the difference between
        // counting what happened and leading it.
        if (pacing && stage is Stage.Live && after.elapsed % PACE_SECONDS == 0) {
            speech.say(string(if (paceUp) R.string.say_up else R.string.say_down))
            paceUp = !paceUp
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

        _done.value = _done.value.copy(rows = runner.results.map { row(it, runner.plan) })
    }

    private fun row(result: Result, plan: SessionPlan) = DoneRow(
        movementId = result.movementId,
        name = Movements.byId(result.movementId)?.name.orEmpty(),
        value = value(result.count, plan, result.movementId),
        count = result.count,
        skipped = result.skipped,
        selfReported = result.selfReported,
    )

    /**
     * A number the person changed by hand. ADDENDUM-03 Part 14.
     *
     * The screen and the row both change, and the app says nothing about it. It is
     * written against the saved session rather than the runner, because by the time
     * the done screen is up the session is already kept.
     */
    fun correct(movementId: String, count: Int) = viewModelScope.launch {
        val runId = savedRunId ?: return@launch
        val plan = _runner.value?.plan ?: return@launch
        val wanted = count.coerceAtLeast(0)
        runs.correct(runId, movementId, wanted)
        _done.value = _done.value.copy(
            rows = _done.value.rows.map { row ->
                if (row.movementId != movementId) {
                    row
                } else {
                    row.copy(
                        value = value(wanted, plan, movementId),
                        count = wanted,
                        selfReported = true,
                    )
                }
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

    private fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        getApplication<Application>().resources.getQuantityString(id, count, *args)

    private companion object {
        const val A_SECOND = 1000L
        const val AN_HOUR = 60L * 60 * 1000
        const val FIVE_MORE = 5
        const val FIVE_SECONDS = 5

        /** One cue every two seconds: up on one, down on the next. */
        const val PACE_SECONDS = 2

        /** The pacing cue rides along for the first three sessions of a movement. */
        const val PACING_SESSIONS = 3
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
        sensed = movement?.sensedBy != null && movement.sensedBy != Sensed.None,
    )
}
