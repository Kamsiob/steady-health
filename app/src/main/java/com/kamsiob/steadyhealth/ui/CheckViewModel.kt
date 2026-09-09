package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.AbilityRepository
import com.kamsiob.steadyhealth.data.CheckRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.engine.AbilityEngine
import com.kamsiob.steadyhealth.engine.Confidence
import com.kamsiob.steadyhealth.engine.HowCounted
import com.kamsiob.steadyhealth.engine.LifeSentences
import com.kamsiob.steadyhealth.engine.Measure
import com.kamsiob.steadyhealth.engine.MeasureUnit
import com.kamsiob.steadyhealth.engine.Measures
import com.kamsiob.steadyhealth.engine.SurerLine
import com.kamsiob.steadyhealth.engine.Warmth
import com.kamsiob.steadyhealth.sensing.Motion
import com.kamsiob.steadyhealth.sensing.RepCounter
import com.kamsiob.steadyhealth.ui.screens.CheckDoneUiState
import com.kamsiob.steadyhealth.ui.screens.CheckIntroUiState
import com.kamsiob.steadyhealth.ui.screens.CheckMeasureUiState
import com.kamsiob.steadyhealth.ui.screens.CheckResultRow
import com.kamsiob.steadyhealth.ui.screens.QuieterUiState
import com.kamsiob.steadyhealth.ui.screens.RateAgainItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * The monthly check, start to finish.
 *
 * Its own view model because it is a flow with a beginning and an end rather than
 * a screen that is always there, and because it is the only thing in the app that
 * holds a sensor open. When this leaves, the accelerometer goes with it.
 *
 * The engine decides everything that matters: which measures this person takes,
 * what a repetition is, and whether an ability is Better, Same or Quieter. This
 * assembles and displays.
 */
class CheckViewModel(application: Application) : AndroidViewModel(application) {

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
    private val checks get() = CheckRepository(db)
    private val abilities get() = AbilityRepository(db)
    private val motion by lazy { Motion(application) }

    private val _intro = MutableStateFlow(CheckIntroUiState())
    val intro: StateFlow<CheckIntroUiState> = _intro.asStateFlow()

    private val _measure = MutableStateFlow(CheckMeasureUiState())
    val measure: StateFlow<CheckMeasureUiState> = _measure.asStateFlow()

    private val _done = MutableStateFlow(CheckDoneUiState())
    val done: StateFlow<CheckDoneUiState> = _done.asStateFlow()

    private val _rateAgain = MutableStateFlow<List<RateAgainItem>>(emptyList())

    /** The person's own list, to be rated again. LOGIC.md 3b, monthly with the check. */
    val rateAgain: StateFlow<List<RateAgainItem>> = _rateAgain.asStateFlow()

    private val _quieter = MutableStateFlow<QuieterUiState?>(null)

    /** Non-null when an ability has gone one way for three checks and may say so. */
    val quieter: StateFlow<QuieterUiState?> = _quieter.asStateFlow()

    /** True when the last measure has been taken and the result is ready. */
    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private var plan: List<Measure> = emptyList()
    private var at = 0
    private var taken = mutableMapOf<String, Double>()
    private var counting: Job? = null
    private var byHandCount = 0

    fun open() = viewModelScope.launch {
        plan = Measures.check(profile.gettingAround(), profile.exclusions())
        at = 0
        taken = mutableMapOf()
        _finished.value = false
        _intro.value = CheckIntroUiState(measures = plan.map { string(nameOf(it)) })
    }

    fun start() {
        at = 0
        showMeasure()
    }

    private fun showMeasure() {
        counting?.cancel()
        counting = null
        byHandCount = 0
        val measure = plan.getOrNull(at) ?: return
        _measure.value = CheckMeasureUiState(
            name = string(nameOf(measure)),
            how = string(howOf(measure)),
            safety = string(safetyOf(measure)),
            index = at + 1,
            total = plan.size,
            value = 0,
            unit = unitLabel(measure, 0),
            // A held measure counts up and waits for the person, because the
            // number is how long they lasted. Showing a countdown beside it would
            // read as a target, and this one has none.
            secondsLeft = measure.seconds.takeIf { measure.unit != MeasureUnit.Seconds },
            countingLine = string(countingLineOf(measure)),
            byHand = measure.counted != HowCounted.ByMotion,
            running = false,
        )
    }

    /**
     * Start this measure, or add one to it.
     *
     * The same button does both because the screen has one primary action at a
     * time and swapping its meaning is clearer than two buttons where one is
     * always disabled.
     */
    fun tap() {
        val measure = plan.getOrNull(at) ?: return
        if (!_measure.value.running) {
            begin(measure)
            return
        }
        if (measure.counted != HowCounted.ByMotion && measure.unit != MeasureUnit.Seconds) {
            byHandCount += 1
            _measure.update { it.copy(value = byHandCount, unit = unitLabel(measure, byHandCount)) }
        }
    }

    private fun begin(measure: Measure) {
        _measure.update { it.copy(running = true, value = 0) }
        counting = viewModelScope.launch {
            if (measure.counted == HowCounted.ByMotion) watchMotion(measure)
            runClock(measure)
        }
    }

    private fun watchMotion(measure: Measure) {
        val counter = if (measure.id == Measures.chairStand.id) {
            RepCounter.forStands()
        } else {
            RepCounter.forSteps()
        }
        viewModelScope.launch {
            motion.counts(counter).collect { count ->
                _measure.update { it.copy(value = count, unit = unitLabel(measure, count)) }
            }
        }
    }

    /**
     * The clock for one measure.
     *
     * For a fixed-length measure it counts down and stops on its own. For a held
     * one, like standing on one foot, it counts up and waits for the person, since
     * the number is how long they lasted.
     */
    private suspend fun runClock(measure: Measure) {
        val length = measure.seconds ?: return
        val held = measure.unit == MeasureUnit.Seconds
        var elapsed = 0
        while (elapsed < length && _measure.value.running) {
            delay(A_SECOND)
            elapsed += 1
            _measure.update {
                it.copy(
                    secondsLeft = (length - elapsed).takeIf { _ -> !held },
                    value = if (held) elapsed else it.value,
                    unit = unitLabel(measure, if (held) elapsed else it.value),
                )
            }
        }
        if (!held && _measure.value.running) stop()
    }

    /** Take what is on screen as the result and move on. */
    fun stop() {
        counting?.cancel()
        counting = null
        val measure = plan.getOrNull(at) ?: return
        taken[measure.id] = _measure.value.value.toDouble()
        next()
    }

    /** Skip this one. Nothing is recorded, and nothing anywhere says it was skipped. */
    fun skip() {
        counting?.cancel()
        counting = null
        next()
    }

    private fun next() {
        at += 1
        if (at < plan.size) showMeasure() else loadRatings()
    }

    /**
     * Load the person's list for re-rating, or go straight to the result.
     *
     * Somebody with nothing on their list does not get an empty screen asking
     * them to rate nothing.
     */
    private fun loadRatings() = viewModelScope.launch {
        val items = abilities.items().map { item ->
            val last = abilities.latestRating(item.id)
            val before = last?.rating ?: DEFAULT_RATING
            RateAgainItem(
                id = item.id,
                text = item.text,
                rating = before,
                before = before,
                // Last month's answer is not carried forward as this month's. The
                // second question starts unanswered every time, because a prefilled
                // number that nobody touched would be recorded as an answer.
                sureness = null,
            )
        }
        _rateAgain.value = items
        if (items.isEmpty()) finish()
    }

    fun rate(itemId: Long, rating: Int) {
        _rateAgain.update { list ->
            list.map { if (it.id == itemId) it.copy(rating = rating) else it }
        }
    }

    /** The second question. ADDENDUM-03 Part 18. Answered only if it is touched. */
    fun sureness(itemId: Long, sureness: Int) {
        _rateAgain.update { list ->
            list.map { if (it.id == itemId) it.copy(sureness = sureness) else it }
        }
    }

    /** Save the ratings, then read the result. */
    fun ratingsDone() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        _rateAgain.value.forEach { abilities.rate(it.id, today(), it.rating, now, it.sureness) }
        finish()
    }

    /**
     * The confidence sentence, or nothing. ADDENDUM-03 Part 18.
     *
     * One item, the first that has something to say, so that a person with four
     * things on their list gets one sentence rather than four. The ability state is
     * the one the measures just produced for the ability that item belongs to, which
     * is why this is worked out here and not in Progress: this is the only moment
     * where both halves are from the same morning.
     */
    private suspend fun surer(states: Map<String, AbilityState>): String {
        return abilities.items().firstNotNullOfOrNull { item ->
            val months = abilities.months(item.id).sortedBy { it.epochDay }
            val line = Confidence.line(
                // Same when nothing was measured for that ability this month.
                // "We did not look" is not a finding, and the sentence for Same
                // is the honest one either way.
                state = states[item.domain] ?: AbilityState.Same,
                was = months.getOrNull(months.size - 2),
                now = months.lastOrNull(),
                itemText = item.text,
            )
            when (line) {
                is SurerLine.BothMoved -> string(R.string.surer_both, line.itemText)
                is SurerLine.NotYet -> string(R.string.surer_not_yet, line.itemText)
                is SurerLine.SurerAnyway -> string(R.string.surer_anyway, line.itemText)
                SurerLine.Quiet -> null
            }
        }.orEmpty()
    }

    /**
     * The fourth warm place, at the moment it happens. ADDENDUM-03 Part 16.
     *
     * A measure crossing the point where it stops being a number and starts being
     * something somebody can do, named against the words they used for it. Said once
     * ever, which is what the notices table is for, and only when the person actually
     * named something for that ability: saying "you just did it" about a thing nobody
     * asked for is the app applauding itself.
     */
    private suspend fun justDidIt(values: Map<String, Double>): String {
        val items = abilities.items()
        return items.firstNotNullOfOrNull { item ->
            val domain = AbilityDomain.fromId(item.domain) ?: return@firstNotNullOfOrNull null
            val said = Warmth.justDidIt(
                itemText = item.text,
                domain = domain,
                values = values,
                saidAlready = emptySet(),
            ) ?: return@firstNotNullOfOrNull null
            val fresh = profile.showOnce("crossed_${'$'}{said.sentenceId}", System.currentTimeMillis())
            if (fresh) string(R.string.just_did_it, said.itemText) else null
        }.orEmpty()
    }

    /**
     * What this month's measures said, one answer per ability.
     *
     * Quieter wins over Better within an ability, which is AbilityEngine's own rule:
     * an ability where one measure went up and another went the other way is not
     * Better, and reporting it as such is the app choosing the flattering half.
     */
    private fun statesByDomain(
        measured: List<Measure>,
        rows: List<CheckResultRow>,
    ): Map<String, AbilityState> =
        measured.zip(rows)
            .groupBy { it.first.domain.id }
            .mapValues { (_, pairs) ->
                val states = pairs.map { it.second.state }
                when {
                    states.any { it == AbilityState.Quieter } -> AbilityState.Quieter
                    states.any { it == AbilityState.Better } -> AbilityState.Better
                    else -> AbilityState.Same
                }
            }

    private fun finish() = viewModelScope.launch {
        val previous = checks.results()
        val measured = plan.filter { it.id in taken }
        val rows = measured.map { measure ->
            val history = previous.filter { it.measureId == measure.id }.sortedBy { it.epochDay }
            row(measure, taken.getValue(measure.id), history.lastOrNull()?.value)
        }
        val values = previous.associate { it.measureId to it.value } + taken
        val sentence = plan.map { it.domain }.distinct()
            .firstNotNullOfOrNull { LifeSentences.forDomain(it, values) }

        // Whatever was actually done. Somebody who skipped every measure and
        // only re-rated their own list has still done something, and a screen
        // saying "Done" with nothing under it makes it look like they have not.
        val ratingRows = _rateAgain.value
            .filter { it.rating != it.before || rows.isEmpty() }
            .map { item ->
                CheckResultRow(
                    name = item.text,
                    value = string(R.string.rating_now, item.rating),
                    state = ratingState(item.rating - item.before),
                    stateLabel = string(stateLabelOf(ratingState(item.rating - item.before))),
                )
            }

        _done.value = CheckDoneUiState(
            lifeSentence = sentence?.let { string(lifeStringOf(it.id)) }.orEmpty(),
            // Only when the sentence is about the ability they actually named
            // something for. Saying "that's the thing you said you wanted" about
            // an ability they never mentioned is the app not listening.
            wanted = if (sentence != null && abilities.items().any { it.domain == sentence.domain.id }) {
                string(R.string.check_wanted)
            } else {
                ""
            },
            rows = rows + ratingRows,
            anySame = (rows + ratingRows).any { it.state == AbilityState.Same },
            surer = surer(statesByDomain(measured, rows)),
            justDidIt = justDidIt(values),
        )
        _finished.value = true
    }

    /**
     * Save the check, then decide whether anything needs saying about it.
     *
     * The Quieter screen is the only thing in this app that tells somebody a
     * number went the other way, and LOGIC.md 3b holds it to once per ability per
     * six months. That is enforced here, against a notice row, rather than by the
     * screen remembering.
     */
    fun save(onSaved: (Boolean) -> Unit) = viewModelScope.launch {
        val today = today()
        checks.save(
            epochDay = today,
            at = System.currentTimeMillis(),
            way = profile.gettingAround(),
            values = taken,
        )
        onSaved(quieterToSay(today))
    }

    private suspend fun quieterToSay(today: Long): Boolean {
        val results = checks.results()
        val domain = AbilityDomain.entries.firstOrNull {
            AbilityEngine.quieterRun(it, results) && sayableAgain(it, today)
        } ?: return false

        val changes = AbilityEngine.lastChanges(domain, results)
        val going = changes.firstOrNull { it.quieter } ?: return false
        val history = results
            .filter { it.measureId == going.measure.id }
            .sortedBy { it.epochDay }
            .takeLast(AbilityEngine.CHECKS_BEFORE_QUIETER + 1)

        profile.showOnce("$QUIETER_NOTICE:${domain.id}", System.currentTimeMillis())
        _quieter.value = QuieterUiState(
            ability = string(nameFor(domain)),
            sentence = string(R.string.quieter_sentence, string(nameOf(going.measure)).lowercase()),
            numbers = history.joinToString(", ") { it.value.toInt().toString() } + ".",
            held = AbilityEngine.whatHeld(domain, results)
                .joinToString(", ") { string(nameOf(it)) }
                .ifBlank { string(R.string.quieter_nothing_else) },
        )
        return true
    }

    /**
     * True when this ability has not had its say in six months.
     *
     * The notice row is keyed by ability only, so the check is against how long
     * ago it was written. LOGIC.md 3b: once per domain per six months, and never
     * repeated.
     */
    private suspend fun sayableAgain(domain: AbilityDomain, today: Long): Boolean {
        val shown = profile.shownOn("$QUIETER_NOTICE:${domain.id}") ?: return true
        val days = (System.currentTimeMillis() - shown) / MILLIS_PER_DAY
        return days >= AbilityEngine.QUIETER_SILENCE_DAYS && today > 0
    }

    fun dismissQuieter() {
        _quieter.value = null
    }

    /**
     * One row of the result: what it is, what it was, and the word for the change.
     *
     * The comparison is against this measure's own previous value and nothing
     * else. There is no norm on this screen, no percentage, and no combined
     * anything.
     */
    /**
     * What a change in somebody's own rating means.
     *
     * Two points or more is a change; one point is inside the range these ratings
     * move in anyway and is shown without being announced. LOGIC.md 3b, from the
     * Patient-Specific Functional Scale's own detectable change of about 1.3 to 3.
     */
    private fun ratingState(moved: Int) = when {
        moved >= RATING_CHANGE -> AbilityState.Better
        moved <= -RATING_CHANGE -> AbilityState.Quieter
        else -> AbilityState.Same
    }

    private fun row(measure: Measure, value: Double, before: Double?): CheckResultRow {
        val now = valueLabel(measure, value)
        val state = when {
            before == null -> AbilityState.Same
            measure.detectableChange == null -> AbilityState.Same
            else -> {
                val moved = (value - before).let { if (measure.moreIsBetter) it else -it }
                when {
                    moved >= measure.detectableChange -> AbilityState.Better
                    -moved >= measure.detectableChange -> AbilityState.Quieter
                    else -> AbilityState.Same
                }
            }
        }
        return CheckResultRow(
            name = string(nameOf(measure)),
            value = when {
                before == null -> string(R.string.check_value_first, now)
                before == value -> string(R.string.check_value_same, now)
                else -> string(R.string.check_value_from, now, valueLabel(measure, before))
            },
            state = state,
            stateLabel = string(stateLabelOf(state)),
        )
    }

    private fun valueLabel(measure: Measure, value: Double): String {
        val whole = value.toInt()
        return when (measure.unit) {
            MeasureUnit.Seconds -> plural(R.plurals.check_secs, whole)
            MeasureUnit.Repetitions ->
                measure.seconds
                    ?.let { string(R.string.check_in_seconds, whole, it) }
                    ?: "$whole"

            else -> "$whole"
        }
    }

    /**
     * The word after the running count.
     *
     * Its own word per measure rather than a generic "reps", because somebody
     * standing up out of a chair should see "9 stands" and not a gym word for a
     * thing they are doing in their front room.
     */
    private fun unitLabel(measure: Measure, count: Int): String =
        plural(unitPluralOf(measure), count)

    private fun unitPluralOf(measure: Measure) = when (measure.id) {
        Measures.chairStand.id, Measures.sitToEdge.id -> R.plurals.check_stands
        Measures.twoMinuteStep.id -> R.plurals.check_steps
        Measures.wallPushUps.id -> R.plurals.check_push_ups
        Measures.bandRows.id -> R.plurals.check_rows
        Measures.wheelingMinutes.id -> R.plurals.check_pushes
        Measures.breathHold.id -> R.plurals.check_breaths
        Measures.seatedReach.id, Measures.ankleRange.id -> R.plurals.check_times
        else -> R.plurals.check_secs
    }

    private fun countingLineOf(measure: Measure) = when (measure.counted) {
        HowCounted.ByMotion -> R.string.check_by_motion
        HowCounted.ByClock -> R.string.check_by_clock
        else -> R.string.check_by_hand
    }

    private fun stateLabelOf(state: AbilityState) = when (state) {
        AbilityState.Better -> R.string.state_better
        AbilityState.Same -> R.string.state_same
        AbilityState.Quieter -> R.string.state_quieter
    }

    private fun string(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private fun plural(id: Int, count: Int): String =
        getApplication<Application>().resources.getQuantityString(id, count, count)

    private fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private fun nameFor(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_get_up
        AbilityDomain.Go -> R.string.ability_go
        AbilityDomain.Carry -> R.string.ability_carry
        AbilityDomain.Steady -> R.string.ability_steady
    }

    private companion object {
        const val A_SECOND = 1000L

        /** The middle of the scale, the same starting point setup uses. */
        const val DEFAULT_RATING = 5

        /** Two points or more. LOGIC.md 3b. */
        const val RATING_CHANGE = 2
        const val MILLIS_PER_DAY = 86_400_000L
        const val QUIETER_NOTICE = "quieter"
    }
}

/**
 * The resource for one measure's name, its instruction, and its safety line.
 *
 * Written out rather than found by resource name, for the reason in
 * [TagLabels.labelFor]: a resource found by name is one lint cannot see.
 */
internal fun nameOf(measure: Measure) = when (measure.id) {
    Measures.chairStand.id -> R.string.measure_chair_stand_30
    Measures.twoMinuteStep.id -> R.string.measure_two_minute_step
    Measures.wallPushUps.id -> R.string.measure_wall_push_ups
    Measures.bandRows.id -> R.string.measure_band_rows
    Measures.singleLegStance.id -> R.string.measure_single_leg_stance
    Measures.fourStageBalance.id -> R.string.measure_four_stage_balance
    Measures.seatedReach.id -> R.string.measure_seated_reach
    Measures.seatedBalance.id -> R.string.measure_seated_balance
    Measures.wheelingMinutes.id -> R.string.measure_wheeling_two_minute
    Measures.sitToEdge.id -> R.string.measure_sit_to_edge
    Measures.breathHold.id -> R.string.measure_slow_breaths
    Measures.gripHold.id -> R.string.measure_grip_hold
    else -> R.string.measure_ankle_pumps
}

internal fun howOf(measure: Measure) = when (measure.id) {
    Measures.chairStand.id -> R.string.measure_chair_stand_30_how
    Measures.twoMinuteStep.id -> R.string.measure_two_minute_step_how
    Measures.wallPushUps.id -> R.string.measure_wall_push_ups_how
    Measures.bandRows.id -> R.string.measure_band_rows_how
    Measures.singleLegStance.id -> R.string.measure_single_leg_stance_how
    Measures.fourStageBalance.id -> R.string.measure_four_stage_balance_how
    Measures.seatedReach.id -> R.string.measure_seated_reach_how
    Measures.seatedBalance.id -> R.string.measure_seated_balance_how
    Measures.wheelingMinutes.id -> R.string.measure_wheeling_two_minute_how
    Measures.sitToEdge.id -> R.string.measure_sit_to_edge_how
    Measures.breathHold.id -> R.string.measure_slow_breaths_how
    Measures.gripHold.id -> R.string.measure_grip_hold_how
    else -> R.string.measure_ankle_pumps_how
}

internal fun safetyOf(measure: Measure) = when (measure.id) {
    Measures.chairStand.id -> R.string.measure_chair_stand_30_safe
    Measures.twoMinuteStep.id -> R.string.measure_two_minute_step_safe
    Measures.wallPushUps.id -> R.string.measure_wall_push_ups_safe
    Measures.bandRows.id -> R.string.measure_band_rows_safe
    Measures.singleLegStance.id -> R.string.measure_single_leg_stance_safe
    Measures.fourStageBalance.id -> R.string.measure_four_stage_balance_safe
    Measures.seatedReach.id -> R.string.measure_seated_reach_safe
    Measures.seatedBalance.id -> R.string.measure_seated_balance_safe
    Measures.wheelingMinutes.id -> R.string.measure_wheeling_two_minute_safe
    Measures.sitToEdge.id -> R.string.measure_sit_to_edge_safe
    Measures.breathHold.id -> R.string.measure_slow_breaths_safe
    Measures.gripHold.id -> R.string.measure_grip_hold_safe
    else -> R.string.measure_ankle_pumps_safe
}

/**
 * The short form of one life sentence, for a tile.
 *
 * Telegraphic, the way grid screens 5 and 8 write them. A full sentence in a tile
 * gets an ellipsis through the middle of it and says less than either version.
 */
@Suppress("CyclomaticComplexMethod") // A table of constants, not a decision.
internal fun shortLifeStringOf(id: String) = when (id) {
    "get_up_low_chair" -> R.string.short_get_up_low_chair
    "get_up_no_hands" -> R.string.short_get_up_no_hands
    "get_up_floor" -> R.string.short_get_up_floor
    "go_one_flight" -> R.string.short_go_one_flight
    "go_no_stop" -> R.string.short_go_no_stop
    "go_two_flights" -> R.string.short_go_two_flights
    "carry_bags" -> R.string.short_carry_bags
    "carry_one_trip" -> R.string.short_carry_one_trip
    "carry_overhead" -> R.string.short_carry_overhead
    "steady_sock" -> R.string.short_steady_sock
    "steady_trousers" -> R.string.short_steady_trousers
    "steady_kerb" -> R.string.short_steady_kerb
    "transfer_bed" -> R.string.short_transfer_bed
    "transfer_alone" -> R.string.short_transfer_alone
    "wheel_block" -> R.string.short_wheel_block
    "wheel_shops" -> R.string.short_wheel_shops
    "carry_lap" -> R.string.short_carry_lap
    "carry_shelf" -> R.string.short_carry_shelf
    "seated_reach_far" -> R.string.short_seated_reach_far
    "bed_sit_up" -> R.string.short_bed_sit_up
    "bed_edge_alone" -> R.string.short_bed_edge_alone
    "bed_breaths" -> R.string.short_bed_breaths
    "bed_grip" -> R.string.short_bed_grip
    else -> R.string.short_bed_ankles
}

/** The hand-written sentence for one life-sentence id. */
@Suppress("CyclomaticComplexMethod") // A table of constants, not a decision.
internal fun lifeStringOf(id: String) = when (id) {
    "get_up_low_chair" -> R.string.life_get_up_low_chair
    "get_up_no_hands" -> R.string.life_get_up_no_hands
    "get_up_floor" -> R.string.life_get_up_floor
    "go_one_flight" -> R.string.life_go_one_flight
    "go_no_stop" -> R.string.life_go_no_stop
    "go_two_flights" -> R.string.life_go_two_flights
    "carry_bags" -> R.string.life_carry_bags
    "carry_one_trip" -> R.string.life_carry_one_trip
    "carry_overhead" -> R.string.life_carry_overhead
    "steady_sock" -> R.string.life_steady_sock
    "steady_trousers" -> R.string.life_steady_trousers
    "steady_kerb" -> R.string.life_steady_kerb
    "transfer_bed" -> R.string.life_transfer_bed
    "transfer_alone" -> R.string.life_transfer_alone
    "wheel_block" -> R.string.life_wheel_block
    "wheel_shops" -> R.string.life_wheel_shops
    "carry_lap" -> R.string.life_carry_lap
    "carry_shelf" -> R.string.life_carry_shelf
    "seated_reach_far" -> R.string.life_seated_reach_far
    "bed_sit_up" -> R.string.life_bed_sit_up
    "bed_edge_alone" -> R.string.life_bed_edge_alone
    "bed_breaths" -> R.string.life_bed_breaths
    "bed_grip" -> R.string.life_bed_grip
    else -> R.string.life_bed_ankles
}
