package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ai.Tags
import com.kamsiob.steadyhealth.ai.WeekFilter
import com.kamsiob.steadyhealth.ai.WeekWriter
import com.kamsiob.steadyhealth.data.AbilityRepository
import com.kamsiob.steadyhealth.data.DayEntry
import com.kamsiob.steadyhealth.data.DayRepository
import com.kamsiob.steadyhealth.data.MovementRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.WeekRepository
import com.kamsiob.steadyhealth.data.WeightRepository
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.domain.DayRating
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.Ladder
import com.kamsiob.steadyhealth.domain.TalkTest
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.engine.DoneSession
import com.kamsiob.steadyhealth.engine.Envelope
import com.kamsiob.steadyhealth.engine.Ladders
import com.kamsiob.steadyhealth.engine.PacingEngine
import com.kamsiob.steadyhealth.engine.Progression
import com.kamsiob.steadyhealth.engine.ProgressionEngine
import com.kamsiob.steadyhealth.engine.Returning
import com.kamsiob.steadyhealth.engine.ReturningEngine
import com.kamsiob.steadyhealth.engine.Step
import com.kamsiob.steadyhealth.engine.StepMeasure
import com.kamsiob.steadyhealth.engine.WayOfGettingAround
import com.kamsiob.steadyhealth.engine.WaysOfGettingAround
import com.kamsiob.steadyhealth.ui.screens.AbilitiesUiState
import com.kamsiob.steadyhealth.ui.screens.AbilityRowState
import com.kamsiob.steadyhealth.ui.screens.AbilityTileState
import com.kamsiob.steadyhealth.ui.screens.MoveItem
import com.kamsiob.steadyhealth.ui.screens.MoveUiState
import com.kamsiob.steadyhealth.ui.screens.OfferUiState
import com.kamsiob.steadyhealth.ui.screens.SayHowUiState
import com.kamsiob.steadyhealth.ui.screens.TagSection
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
    internal val weeks by lazy { WeekRepository(db) }

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

    private val _notice = MutableStateFlow<String?>(null)

    /** One sentence the app owes the person, shown once and then gone. */
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _abilities = MutableStateFlow(AbilitiesUiState())
    val abilitiesState: StateFlow<AbilitiesUiState> = _abilities.asStateFlow()

    private var walkStartedAt = 0L
    private var walkSessionId: Long? = null

    /**
     * The profile, held between refreshes.
     *
     * Every screen depends on it and it changes about once a year, so reading it
     * from the database on every assembly would be six queries to answer a
     * question whose answer is already known. It is loaded in [loadProfile] and
     * nowhere else.
     */
    private var way: WayOfGettingAround = WaysOfGettingAround.onFeet
    private var exclusions: Set<Exclusion> = emptySet()
    private var pacing = false
    private var envelope: Envelope = PacingEngine.DEFAULT

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
        loadProfile()
        applyTimeAway()
        refreshToday()
        refreshMove()
        refreshAbilities()
    }

    private suspend fun loadProfile() {
        way = WaysOfGettingAround.forWay(profile.gettingAround())
        exclusions = profile.exclusions()
        pacing = profile.pacing()
        envelope = profile.envelope()
    }

    /** The ladder a session is recorded against, for this way of getting around. */
    private val primaryLadder: Ladder
        get() = when (way.way) {
            GettingAround.OnFeet, GettingAround.Walker -> Ladder.Walking
            GettingAround.Wheelchair -> Ladder.Wheeling
            GettingAround.InBed -> Ladder.InBed
        }

    /** The steps of that ladder. The bed set is a set, so it answers separately. */
    private val primarySteps: List<Step>
        get() = when (primaryLadder) {
            Ladder.Walking -> Ladders.walking
            Ladder.Wheeling -> Ladders.wheeling
            else -> Ladders.inBed
        }

    /**
     * Gap decay, run when the app opens rather than after a session, because
     * somebody who has been away for a month meets it before they do anything.
     *
     * It is applied once: the notice is recorded against the day it fired, so
     * opening the app twice does not take four steps off anybody.
     */
    private suspend fun applyTimeAway() {
        val ladder = primaryLadder
        val state = movement.state(ladder)
        val gap = ReturningEngine.decide(state.lastSessionDay, today())
        if (gap !is Returning.AfterAGap) {
            welcomeBack = null
            return
        }
        val noticeId = "$WELCOME_BACK_NOTICE:${ladder.id}:${state.lastSessionDay}"
        if (profile.showOnce(noticeId, System.currentTimeMillis())) {
            movement.moveTo(ladder, (state.currentStepIndex - gap.stepsBack).coerceAtLeast(0))
            gap.easingUntilDay?.let { movement.ease(ladder, it) }
        }
        val context = getApplication<Application>()
        welcomeBack = context.getString(
            when {
                gap.askAgain -> R.string.welcome_back_ask
                gap.stepsBack > 1 -> R.string.welcome_back_long
                else -> R.string.welcome_back_short
            },
        )
    }

    private var welcomeBack: String? = null

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

        val walkStep = movement.state(primaryLadder).currentStepIndex
        // Screen 7 has no dark row at the bottom: in bed the Move card above it
        // already says "Two minutes in bed", and repeating it would be the screen
        // saying the same sentence twice.
        val walkName = if (way.way == GettingAround.InBed) "" else nextThingName(walkStep)
        val weighsIn = way.weighsIn && profile.weighsIn()

        _today.value = TodayUiState(
            date = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
            greeting = context.getString(greetingFor(LocalTime.now())),
            abilities = abilityTiles(),
            weightValue = latest?.takeIf { showNumbers && weighsIn }
                ?.let { Convert.weightLabel(it.smoothedKg, units) },
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
            weighsIn = weighsIn,
            moveTitle = context.getString(moveCardTitle()),
            moveSubtitle = context.getString(moveCardSubtitle()),
            nextLabel = context.getString(nextLabel()),
            welcomeBack = welcomeBack,
            notice = _notice.value,
        )
    }

    /**
     * What the next thing is called: the person's own name for it if they have
     * given one, the ladder's otherwise, and the whole set for somebody in bed.
     */
    private suspend fun nextThingName(stepIndex: Int): String =
        movement.nameFor(primaryLadder, stepIndex)
            ?: primarySteps.getOrNull(stepIndex)?.name.orEmpty()

    private fun moveCardTitle() = when {
        pacing -> R.string.today_move_pacing
        way.way == GettingAround.InBed -> R.string.today_move_bed
        else -> R.string.today_move
    }

    private fun moveCardSubtitle() = when {
        pacing -> R.string.today_move_sub_pacing
        way.way == GettingAround.InBed -> R.string.today_move_sub_bed
        way.way == GettingAround.Wheelchair -> R.string.today_move_sub_wheel
        else -> R.string.today_move_sub
    }

    private fun nextLabel() = when {
        pacing -> R.string.move_pacing_label
        way.way == GettingAround.OnFeet -> R.string.today_next_walk
        way.way == GettingAround.InBed -> R.string.today_move_bed
        else -> R.string.today_next_go
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
                // The four ids never change; only what they are called does.
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
        chosenTags = days.tagsFor(today()).toSet()
        suggestedTags = emptySet()
        _sayHow.value = SayHowUiState(
            sentence = existing?.sentence.orEmpty(),
            sleepHalfHours = existing?.sleepHalfHours ?: DEFAULT_SLEEP_HALF_HOURS,
            dayRating = existing?.dayRating,
            tags = tagSections(),
            fromReader = false,
        )
    }

    /**
     * The grid, in its six groups.
     *
     * Without the reader installed this is how tags get chosen, so it is not a
     * confirmation step behind a suggestion; it is the feature. AI.md: "without
     * the model, tags are chosen from the grid by hand."
     */
    private fun tagSections(): List<TagSection> =
        TagLabels.sections(getApplication(), chosen = chosenTags, suggested = suggestedTags)

    /**
     * Tap a tag.
     *
     * Turning off something the reader suggested is a correction, and AI.md says
     * corrections are how the mapping improves without the vocabulary growing. It
     * is stored when the day is saved, against the words that were on screen.
     */
    fun toggleTag(id: String) {
        if (id !in Tags.ids) return
        chosenTags = if (id in chosenTags) chosenTags - id else chosenTags + id
        _sayHow.update { it.copy(tags = tagSections()) }
    }

    fun setSentence(text: String) = _sayHow.update { it.copy(sentence = text) }

    fun setSleep(halfHours: Int) = _sayHow.update { it.copy(sleepHalfHours = halfHours) }

    fun setDayRating(rating: DayRating) = _sayHow.update { it.copy(dayRating = rating) }

    private var chosenTags: Set<String> = emptySet()
    private var suggestedTags: Set<String> = emptySet()

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
        days.setTags(today(), chosenTags.toList(), suggestedTags)
        refreshToday()
    }

    // --- Move ----------------------------------------------------------------

    /**
     * Move, assembled for whichever version of the app this person is using.
     *
     * The layout is the same in all four. What changes is the ladder behind the
     * hero, whether there is one thing or a set of them, and two blocks: the
     * note about somebody being nearby with a walker, and what pacing mode is.
     */
    private suspend fun refreshMove() {
        val context = getApplication<Application>()
        val step = movement.state(primaryLadder).currentStepIndex
        val plan = Ladders.visibleLadders(way.way, exclusions)
        val inBed = way.way == GettingAround.InBed

        val bed = if (inBed) Ladders.bedSet(step, exclusions) else emptyList()
        val parts = bed.map(::moveItem)
        val strength = if (inBed) null else strengthItem(plan)
        val next = primarySteps.getOrNull(step)

        val daysThisWeek = daysMovedThisWeek()
        val rest = pacing && PacingEngine.suggestedMinutes(envelope, daysThisWeek) == 0

        _move.value = MoveUiState(
            whatTheyWant = abilities.items().firstOrNull()?.text.orEmpty(),
            // In pacing mode the ladder is not the headline, because there is no
            // ladder: the limit is. Naming the step would be the app suggesting a
            // number the person did not choose.
            walkName = when {
                pacing -> pacingLine()
                inBed -> bedSetTitle(bed)
                else -> nextThingName(step)
            },
            walkInstruction = when {
                inBed -> context.resources.getQuantityString(
                    R.plurals.move_bed_explain,
                    bed.size,
                    bed.size,
                )

                else -> next?.instruction.orEmpty()
            },
            strength = strength,
            heroLabel = context.getString(
                if (inBed && !pacing) R.string.move_set else nextLabel(),
            ),
            set = parts,
            helper = way.way == GettingAround.Walker,
            pacingNote = context.getString(R.string.move_pacing_note).takeIf { pacing },
            restToday = rest,
        )
    }

    /** The strength set for today, from the first ladder that is not the main one. */
    private suspend fun strengthItem(plan: com.kamsiob.steadyhealth.engine.LadderPlan): MoveItem? {
        val ladder = way.ladders.firstOrNull { it != primaryLadder } ?: return null
        val steps = plan.steps(ladder)
        if (steps.isEmpty()) return null
        val index = movement.state(ladder).currentStepIndex.coerceIn(0, steps.lastIndex)
        return moveItem(steps[index])
    }

    private fun moveItem(step: Step) =
        MoveItem(name = step.name, instruction = step.instruction, amount = amountLabel(step))

    /**
     * "Two minutes in bed", or three, from what the set actually adds up to.
     *
     * The number comes from the steps rather than from the title, so adding a
     * fourth part changes the words without anybody having to remember to.
     */
    private fun bedSetTitle(parts: List<Step>): String {
        val minutes = nearestMinutes(parts.sumOf { it.amount })
        return getApplication<Application>().resources.getQuantityString(
            R.plurals.bed_set_title,
            minutes,
            minutes,
        )
    }

    /**
     * To the nearest minute, not up.
     *
     * The set on screen 18 runs a hundred and forty seconds and the approved copy
     * calls it two minutes in bed. Rounding up would call it three, which asks
     * somebody for more than the app is about to ask them for.
     */
    private fun nearestMinutes(seconds: Int): Int =
        ((seconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE).coerceAtLeast(1)

    /** In pacing mode the hero says the limit, and never more than it. */
    private fun pacingLine(): String = getApplication<Application>().resources.getQuantityString(
        R.plurals.move_pacing_within,
        envelope.minutes,
        envelope.minutes,
    )

    private suspend fun daysMovedThisWeek(): Int {
        val monday = LocalDate.now(ZoneId.systemDefault()).with(DayOfWeek.MONDAY).toEpochDay()
        return movement.sessionsBetween(monday, monday + DAYS_IN_WEEK - 1)
            .filter { it.durationSeconds >= Ladders.COUNTS_AS_A_SESSION_SECONDS }
            .map { it.epochDay }
            .distinct()
            .size
    }

    fun startWalk() = viewModelScope.launch {
        val step = movement.state(primaryLadder).currentStepIndex
        val inBed = way.way == GettingAround.InBed
        val bed = if (inBed) Ladders.bedSet(step, exclusions) else emptyList()
        _walking.value = WalkingUiState(
            walkName = when {
                pacing -> pacingLine()
                inBed -> bedSetTitle(bed)
                else -> nextThingName(step)
            },
            elapsed = ZERO,
            instruction = primarySteps.getOrNull(step)?.instruction.orEmpty(),
            parts = bed.map(::moveItem),
        )
        walkStartedAt = System.currentTimeMillis()
    }

    fun tickWalk(seconds: Int) = _walking.update { it.copy(elapsed = elapsed(seconds)) }

    fun stopWalk(seconds: Int) = viewModelScope.launch {
        val step = movement.state(primaryLadder).currentStepIndex
        val ended = System.currentTimeMillis()
        walkSessionId = movement.record(
            ladder = primaryLadder,
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
            askHowYouFeel = way.way == GettingAround.InBed,
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

        val ladder = primaryLadder
        val sessions = movement.doneSessions(ladder)

        // Pacing mode turns itself on the second time a session leaves somebody
        // much worse the next day. LOGIC.md section 7 makes that a trigger, and
        // the app says so once rather than changing quietly.
        if (!pacing && PacingEngine.shouldTurnOn(sessions, today())) {
            enterPacing()
            refreshToday()
            refreshMove()
            return@launch
        }

        // In pacing mode nothing goes up, so the decision chain is not run at
        // all. There is no offer to suppress because there is no offer.
        if (!pacing) evaluateProgression(ladder, sessions)

        refreshToday()
        refreshMove()
    }

    private suspend fun evaluateProgression(ladder: Ladder, sessions: List<DoneSession>) {
        val state = movement.state(ladder)
        val easing = state.easingUntilDay?.takeIf { today() < it }
        val decision = ProgressionEngine.decide(
            currentStepIndex = state.currentStepIndex,
            sessions = sessions,
            stepAmounts = primarySteps.map { it.amount },
            today = today(),
            // The fortnight after a long gap suppresses offers the same way a
            // "not yet" does, which is the one thing both of them mean.
            offerDeclinedUntilDay = maxOfNullable(state.offerDeclinedUntilDay, easing),
        )

        when (decision) {
            is Progression.StepBack -> movement.moveTo(ladder, decision.toStepIndex)
            is Progression.Offer -> {
                movement.recordOffered(ladder, today())
                val step = primarySteps[decision.toStepIndex]
                _offer.value = OfferUiState(
                    walkName = movement.nameFor(ladder, decision.toStepIndex) ?: step.name,
                    instruction = step.instruction,
                )
            }

            Progression.Stay -> Unit
        }
    }

    private fun maxOfNullable(a: Long?, b: Long?): Long? =
        listOfNotNull(a, b).maxOrNull()

    /**
     * Turn pacing mode on, and say so once.
     *
     * Entering is automatic because LOGIC.md makes it a trigger and because it is
     * the careful state: nothing increases while it is on. Leaving is not
     * automatic, and only ever happens from settings.
     */
    private suspend fun enterPacing() {
        profile.setPacing(true)
        profile.setEnvelope(envelope)
        pacing = true
        _notice.value = getApplication<Application>().getString(R.string.pacing_on_notice)
    }

    /** A worse day takes a fifth off the limit, and the app says that it did. */
    fun reduceEnvelope() = viewModelScope.launch {
        val reduced = PacingEngine.reduce(envelope)
        profile.setEnvelope(reduced)
        envelope = reduced
        val context = getApplication<Application>()
        val amount = context.resources.getQuantityString(
            R.plurals.amount_minutes,
            reduced.minutes,
            reduced.minutes,
        )
        _notice.value = context.getString(R.string.pacing_reduced, amount)
        refreshMove()
    }

    fun dismissNotice() = viewModelScope.launch {
        _notice.value = null
        refreshToday()
    }

    /** They said yes. The step moves and the offer is gone. */
    fun acceptOffer() = viewModelScope.launch {
        val state = movement.state(primaryLadder)
        movement.moveTo(primaryLadder, state.currentStepIndex + 1)
        _offer.value = null
        refreshToday()
        refreshMove()
    }

    /** They said not yet, which costs nothing and is not asked again for a fortnight. */
    fun declineOffer() = viewModelScope.launch {
        movement.declineOffer(
            primaryLadder,
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
            week = weekNote(),
        )
    }

    /**
     * The Sunday write-up for the week just gone.
     *
     * Written once and stored, so it does not quietly change under somebody who
     * read it yesterday. Without the reader installed the engine fills a template,
     * which is what most people will see, so it is written the same way and held
     * to the same rules.
     */
    private suspend fun weekNote(): List<String> {
        val monday = LocalDate.now(ZoneId.systemDefault()).with(DayOfWeek.MONDAY).toEpochDay()
        weeks.saved(monday)?.let { return it.paragraphs }

        val step = movement.state(primaryLadder).currentStepIndex
        val brief = weeks.brief(
            weekStartDay = monday,
            walkName = nextThingName(step),
            whatTheyWant = abilities.items().firstOrNull()?.text.orEmpty(),
            showNumbers = profile.showNumbers(),
            stepOffered = movement.state(primaryLadder).lastOfferedDay != null,
        )
        if (brief.nothingHappened) return emptyList()

        val note = WeekFilter.clean(WeekWriter.write(brief))
        weeks.save(monday, note, System.currentTimeMillis())
        return note.paragraphs
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

    /**
     * What this person's four abilities are called.
     *
     * The domain is the same in all four versions of the app and so is the tint
     * and the glyph; only the word changes. Nothing here says one set is a
     * smaller version of another, because it is not.
     */
    private fun nameFor(domain: AbilityDomain) = when (way.way) {
        GettingAround.Wheelchair -> when (domain) {
            AbilityDomain.GetUp -> R.string.ability_transfer
            else -> defaultNameFor(domain)
        }

        GettingAround.InBed -> when (domain) {
            AbilityDomain.GetUp -> R.string.ability_sit_up
            AbilityDomain.Go -> R.string.ability_breathe
            AbilityDomain.Carry -> R.string.ability_grip
            AbilityDomain.Steady -> R.string.ability_ankles
        }

        else -> defaultNameFor(domain)
    }

    private fun defaultNameFor(domain: AbilityDomain) = when (domain) {
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

        /** Anything above this is not an envelope any more. */

        const val NOON = 12
        const val EVENING_HOUR = 18
        const val DEFAULT_KG = 80.0
        const val DEFAULT_SLEEP_HALF_HOURS = 14
        const val SECONDS_PER_MINUTE = 60
        const val ZERO = "0:00"

        /** Keyed by ladder and by the day they left, so it fires once per gap. */
        const val WELCOME_BACK_NOTICE = "welcome_back"
    }
}
