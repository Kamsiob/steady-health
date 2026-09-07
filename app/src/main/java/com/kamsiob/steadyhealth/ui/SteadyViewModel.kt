package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.AbilityRepository
import com.kamsiob.steadyhealth.data.DayEntry
import com.kamsiob.steadyhealth.data.DayRepository
import com.kamsiob.steadyhealth.data.MovementRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.WeightRepository
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.domain.DayRating
import com.kamsiob.steadyhealth.domain.Ladder
import com.kamsiob.steadyhealth.domain.TalkTest
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.engine.Ladders
import com.kamsiob.steadyhealth.engine.Progression
import com.kamsiob.steadyhealth.engine.ProgressionEngine
import com.kamsiob.steadyhealth.ui.screens.AbilitiesUiState
import com.kamsiob.steadyhealth.ui.screens.AbilityRowState
import com.kamsiob.steadyhealth.ui.screens.AbilityTileState
import com.kamsiob.steadyhealth.ui.screens.MoveItem
import com.kamsiob.steadyhealth.ui.screens.MoveUiState
import com.kamsiob.steadyhealth.ui.screens.OfferUiState
import com.kamsiob.steadyhealth.ui.screens.SayHowUiState
import com.kamsiob.steadyhealth.ui.screens.TodayUiState
import com.kamsiob.steadyhealth.ui.screens.TrackedItemState
import com.kamsiob.steadyhealth.ui.screens.WalkDoneUiState
import com.kamsiob.steadyhealth.ui.screens.WalkingUiState
import com.kamsiob.steadyhealth.ui.screens.WeighInUiState
import com.kamsiob.steadyhealth.util.Convert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The state every screen reads, and the only place that talks to the
 * repositories.
 *
 * A screen never touches a repository and the engine never touches this. What is
 * here is assembly: reading rows, handing values to pure functions, and turning
 * the answers into something a screen can draw.
 */
class SteadyViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy { SteadyDatabase.get(application) }
    internal val profile by lazy { ProfileRepository(db) }
    internal val weight by lazy { WeightRepository(db) }
    internal val days by lazy { DayRepository(db) }
    internal val abilities by lazy { AbilityRepository(db) }
    internal val movement by lazy { MovementRepository(db) }

    private val _onboardingComplete = MutableStateFlow<Boolean?>(null)
    val onboardingComplete: StateFlow<Boolean?> = _onboardingComplete.asStateFlow()

    private val _today = MutableStateFlow(TodayUiState())
    val today: StateFlow<TodayUiState> = _today.asStateFlow()

    private val _weighIn = MutableStateFlow(WeighInUiState())
    val weighIn: StateFlow<WeighInUiState> = _weighIn.asStateFlow()

    private val _sayHow = MutableStateFlow(SayHowUiState())
    val sayHow: StateFlow<SayHowUiState> = _sayHow.asStateFlow()

    private val _move = MutableStateFlow(MoveUiState())
    val move: StateFlow<MoveUiState> = _move.asStateFlow()

    private val _walking = MutableStateFlow(WalkingUiState())
    val walking: StateFlow<WalkingUiState> = _walking.asStateFlow()

    private val _walkDone = MutableStateFlow(WalkDoneUiState())
    val walkDone: StateFlow<WalkDoneUiState> = _walkDone.asStateFlow()

    private val _offer = MutableStateFlow<OfferUiState?>(null)

    /** Non-null when the engine decided a longer walk is ready. Offered, never assigned. */
    val offer: StateFlow<OfferUiState?> = _offer.asStateFlow()

    private val _abilities = MutableStateFlow(AbilitiesUiState())
    val abilitiesState: StateFlow<AbilitiesUiState> = _abilities.asStateFlow()

    private var walkStartedAt = 0L
    private var walkSessionId: Long? = null

    init {
        viewModelScope.launch {
            val done = profile.onboardingComplete()
            _onboardingComplete.value = done
            if (done) refresh()
        }
    }

    fun onboardingFinished() = viewModelScope.launch {
        _onboardingComplete.value = true
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        refreshToday()
        refreshMove()
        refreshAbilities()
    }

    // --- Today ---------------------------------------------------------------

    private suspend fun refreshToday() {
        val context = getApplication<Application>()
        val today = today()
        val units = profile.units()
        val showNumbers = profile.showNumbers()
        val latest = weight.latest()
        val weighedToday = weight.forDay(today)
        val day = days.forDay(today)
        val sessions = movement.sessionsOn(today)
        val moved = sessions.any { it.durationSeconds >= Ladders.COUNTS_AS_A_SESSION_SECONDS }

        val date = LocalDate.now(ZoneId.systemDefault())
        val monday = date.with(DayOfWeek.MONDAY)
        val week = movement.sessionsBetween(monday.toEpochDay(), monday.toEpochDay() + DAYS_IN_WEEK - 1)
        val walkedDays = week
            .filter { it.durationSeconds >= Ladders.COUNTS_AS_A_SESSION_SECONDS }
            .map { it.epochDay }
            .toSet()

        val walkStep = movement.state(Ladder.Walking).currentStepIndex
        val walkName = movement.nameFor(Ladder.Walking, walkStep)
            ?: Ladders.walking.getOrNull(walkStep)?.name.orEmpty()

        _today.value = TodayUiState(
            date = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
            greeting = context.getString(greetingFor(LocalTime.now())),
            abilities = abilityTiles(),
            weightValue = latest?.takeIf { showNumbers }?.let { Convert.weightLabel(it.smoothedKg, units) },
            weightUnit = if (showNumbers) unitLabel(units) else null,
            weightExplain = weightExplain(weighedToday?.rawKg, units, showNumbers),
            morning = LocalTime.now().hour < EVENING_HOUR,
            weighedIn = weighedToday != null,
            saidHowItWent = day != null,
            moved = moved,
            nextWalkName = walkName,
            dayLetters = dayLetters(context),
            walkedThisWeek = (0 until DAYS_IN_WEEK).map { monday.toEpochDay() + it in walkedDays },
            todayIndex = date.dayOfWeek.value - 1,
            daysWalked = walkedDays.size,
        )
    }

    private fun weightExplain(rawKg: Double?, units: Units, showNumbers: Boolean): String {
        val context = getApplication<Application>()
        if (!showNumbers) return ""
        if (rawKg == null) return context.getString(R.string.weight_first_week)
        return context.getString(
            R.string.weight_daily_line,
            "${Convert.weightLabel(rawKg, units)} ${unitLabel(units)}",
        )
    }

    private fun unitLabel(units: Units): String = getApplication<Application>().getString(
        if (units == Units.Imperial) R.string.unit_lb else R.string.unit_kg,
    )

    private suspend fun abilityTiles(): List<AbilityTileState> {
        val context = getApplication<Application>()
        val items = abilities.items()
        return AbilityDomain.entries.map { domain ->
            val mine = items.filter { it.domain == domain.id }
            AbilityTileState(
                domain = domain,
                name = context.getString(nameFor(domain)),
                // Until the first monthly check there is no measured sentence, so
                // the tile carries the person's own words instead of an empty
                // space or an invented claim.
                lifeSentence = mine.firstOrNull()?.text.orEmpty(),
            )
        }
    }

    // --- The daily three -----------------------------------------------------

    fun openWeighIn() = viewModelScope.launch {
        val units = profile.units()
        val existing = weight.forDay(today())
        val latest = weight.latest()
        _weighIn.value = WeighInUiState(
            kg = existing?.rawKg ?: latest?.rawKg ?: DEFAULT_KG,
            units = units,
        )
    }

    fun setWeight(kg: Double) = _weighIn.update { it.copy(kg = kg) }

    fun saveWeighIn() = viewModelScope.launch {
        weight.save(today(), _weighIn.value.kg, System.currentTimeMillis())
        refreshToday()
    }

    fun openSayHow() = viewModelScope.launch {
        val existing = days.forDay(today())
        _sayHow.value = SayHowUiState(
            sentence = existing?.sentence.orEmpty(),
            sleepHalfHours = existing?.sleepHalfHours ?: DEFAULT_SLEEP_HALF_HOURS,
            dayRating = existing?.dayRating,
        )
    }

    fun setSentence(text: String) = _sayHow.update { it.copy(sentence = text) }

    fun setSleep(halfHours: Int) = _sayHow.update { it.copy(sleepHalfHours = halfHours) }

    fun setDayRating(rating: DayRating) = _sayHow.update { it.copy(dayRating = rating) }

    fun saveDay() = viewModelScope.launch {
        val said = _sayHow.value
        days.save(
            DayEntry(
                epochDay = today(),
                sentence = said.sentence,
                sleepHalfHours = said.sleepHalfHours,
                dayRating = said.dayRating,
                spoken = false,
            ),
            at = System.currentTimeMillis(),
        )
        refreshToday()
    }

    // --- Move ----------------------------------------------------------------

    private suspend fun refreshMove() {
        val step = movement.state(Ladder.Walking).currentStepIndex
        val walk = Ladders.walking.getOrNull(step)
        val name = movement.nameFor(Ladder.Walking, step) ?: walk?.name.orEmpty()
        val exclusions = profile.exclusions()
        val plan = Ladders.visibleLadders(exclusions)
        val chairStep = movement.state(Ladder.ChairAndStanding).currentStepIndex
        val strength = plan.steps(Ladder.ChairAndStanding).getOrNull(chairStep)

        _move.value = MoveUiState(
            whatTheyWant = abilities.items().firstOrNull()?.text.orEmpty(),
            walkName = name,
            walkInstruction = walk?.instruction.orEmpty(),
            strength = strength?.let {
                MoveItem(
                    name = it.name,
                    instruction = it.instruction,
                    amount = amountLabel(it),
                )
            },
        )
    }

    fun startWalk() = viewModelScope.launch {
        val step = movement.state(Ladder.Walking).currentStepIndex
        val walk = Ladders.walking.getOrNull(step)
        _walking.value = WalkingUiState(
            walkName = movement.nameFor(Ladder.Walking, step) ?: walk?.name.orEmpty(),
            elapsed = ZERO,
            instruction = walk?.instruction.orEmpty(),
        )
        walkStartedAt = System.currentTimeMillis()
    }

    fun tickWalk(seconds: Int) = _walking.update { it.copy(elapsed = elapsed(seconds)) }

    fun stopWalk(seconds: Int) = viewModelScope.launch {
        val step = movement.state(Ladder.Walking).currentStepIndex
        val ended = System.currentTimeMillis()
        walkSessionId = movement.record(
            ladder = Ladder.Walking,
            stepIndex = step,
            epochDay = today(),
            startedAt = walkStartedAt,
            endedAt = ended,
            seconds = seconds,
            talkTest = null,
        )
        _walkDone.value = WalkDoneUiState(
            summary = _walking.value.walkName,
            elapsed = elapsed(seconds),
            talkTest = null,
        )
        refreshToday()
        refreshMove()
    }

    fun setTalkTest(answer: TalkTest) = _walkDone.update { it.copy(talkTest = answer) }

    /**
     * Save the talk test, then ask the engine what happens next.
     *
     * This is the one place progression runs, after the answer that decides it.
     * A step back is applied immediately, because it is a decision about safety
     * and there is nothing to accept. An offer is only ever shown.
     */
    fun saveWalkDone() = viewModelScope.launch {
        val answer = _walkDone.value.talkTest ?: return@launch
        walkSessionId?.let { movement.setTalkTest(it, answer) }

        val state = movement.state(Ladder.Walking)
        val decision = ProgressionEngine.decide(
            currentStepIndex = state.currentStepIndex,
            sessions = movement.doneSessions(Ladder.Walking),
            stepAmounts = Ladders.walking.map { it.amount },
            today = today(),
            offerDeclinedUntilDay = state.offerDeclinedUntilDay,
        )

        when (decision) {
            is Progression.StepBack -> movement.moveTo(Ladder.Walking, decision.toStepIndex)
            is Progression.Offer -> {
                movement.recordOffered(Ladder.Walking, today())
                val step = Ladders.walking[decision.toStepIndex]
                _offer.value = OfferUiState(
                    walkName = movement.nameFor(Ladder.Walking, decision.toStepIndex) ?: step.name,
                    instruction = step.instruction,
                )
            }
            Progression.Stay -> Unit
        }

        refreshToday()
        refreshMove()
    }

    /** They said yes. The step moves and the offer is gone. */
    fun acceptOffer() = viewModelScope.launch {
        val state = movement.state(Ladder.Walking)
        movement.moveTo(Ladder.Walking, state.currentStepIndex + 1)
        _offer.value = null
        refreshToday()
        refreshMove()
    }

    /** They said not yet, which costs nothing and is not asked again for a fortnight. */
    fun declineOffer() = viewModelScope.launch {
        movement.declineOffer(
            Ladder.Walking,
            today() + ProgressionEngine.OFFER_DECLINED_DAYS,
        )
        _offer.value = null
    }

    // --- Abilities -----------------------------------------------------------

    private suspend fun refreshAbilities() {
        val context = getApplication<Application>()
        val items = abilities.items()
        val ratings = items.mapNotNull { item ->
            abilities.latestRating(item.id)?.let { item to it.rating }
        }

        _abilities.value = AbilitiesUiState(
            abilities = AbilityDomain.entries.map { domain ->
                AbilityRowState(
                    domain = domain,
                    name = context.getString(nameFor(domain)),
                    lifeSentence = items.firstOrNull { it.domain == domain.id }?.text.orEmpty(),
                    // Before the first check there is nothing measured to compare,
                    // and Same is the honest answer rather than Better.
                    state = AbilityState.Same,
                )
            },
            items = ratings.map { (item, rating) ->
                TrackedItemState(
                    text = item.text,
                    domain = AbilityDomain.fromId(item.domain) ?: AbilityDomain.GetUp,
                    rating = rating,
                )
            },
            waiting = true,
        )
    }

    /** "8 times", "1 minute", "10 seconds": the amount in the words for its measure. */
    private fun amountLabel(step: com.kamsiob.steadyhealth.engine.Step): String {
        val context = getApplication<Application>()
        val plural = when (step.measure) {
            com.kamsiob.steadyhealth.engine.StepMeasure.Minutes -> R.plurals.amount_minutes
            com.kamsiob.steadyhealth.engine.StepMeasure.Repetitions -> R.plurals.amount_times
            com.kamsiob.steadyhealth.engine.StepMeasure.Seconds -> R.plurals.amount_seconds
        }
        return context.resources.getQuantityString(plural, step.amount, step.amount)
    }

    private fun nameFor(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_get_up
        AbilityDomain.Go -> R.string.ability_go
        AbilityDomain.Carry -> R.string.ability_carry
        AbilityDomain.Steady -> R.string.ability_steady
    }

    private fun greetingFor(now: LocalTime) = when {
        now.hour < NOON -> R.string.today_morning
        now.hour < EVENING_HOUR -> R.string.today_afternoon
        else -> R.string.today_evening
    }

    private fun dayLetters(context: Application) = listOf(
        R.string.day_monday,
        R.string.day_tuesday,
        R.string.day_wednesday,
        R.string.day_thursday,
        R.string.day_friday,
        R.string.day_saturday,
        R.string.day_sunday,
    ).map(context::getString)

    private fun elapsed(seconds: Int): String =
        String.format(Locale.US, "%d:%02d", seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)

    private fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val NOON = 12
        const val EVENING_HOUR = 18
        const val DEFAULT_KG = 80.0
        const val DEFAULT_SLEEP_HALF_HOURS = 14
        const val SECONDS_PER_MINUTE = 60
        const val ZERO = "0:00"
    }
}
