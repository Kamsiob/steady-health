package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ai.Tags
import com.kamsiob.steadyhealth.ai.WeekFilter
import com.kamsiob.steadyhealth.ai.WeekWriter
import com.kamsiob.steadyhealth.data.AbilityRepository
import com.kamsiob.steadyhealth.data.CheckRepository
import com.kamsiob.steadyhealth.data.DayEntry
import com.kamsiob.steadyhealth.data.DayRepository
import com.kamsiob.steadyhealth.data.MovementRepository
import com.kamsiob.steadyhealth.data.PlanRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.WeekRepository
import com.kamsiob.steadyhealth.data.WeightRepository
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.DayRating
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.Ladder
import com.kamsiob.steadyhealth.domain.TalkTest
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.engine.AbilityEngine
import com.kamsiob.steadyhealth.engine.DoneSession
import com.kamsiob.steadyhealth.engine.Envelope
import com.kamsiob.steadyhealth.engine.Ladders
import com.kamsiob.steadyhealth.engine.LifeSentences
import com.kamsiob.steadyhealth.engine.NumbersOff
import com.kamsiob.steadyhealth.engine.PacingEngine
import com.kamsiob.steadyhealth.engine.Progression
import com.kamsiob.steadyhealth.engine.ProgressionEngine
import com.kamsiob.steadyhealth.engine.Returning
import com.kamsiob.steadyhealth.engine.ReturningEngine
import com.kamsiob.steadyhealth.engine.Step
import com.kamsiob.steadyhealth.engine.StepMeasure
import com.kamsiob.steadyhealth.engine.Warmth
import com.kamsiob.steadyhealth.engine.WayOfGettingAround
import com.kamsiob.steadyhealth.engine.WaysOfGettingAround
import com.kamsiob.steadyhealth.engine.WeightEngine
import com.kamsiob.steadyhealth.engine.Went
import com.kamsiob.steadyhealth.plan.ReviewDate
import com.kamsiob.steadyhealth.plan.ReviewPrompt
import com.kamsiob.steadyhealth.remind.ReminderWorker
import com.kamsiob.steadyhealth.session.Area
import com.kamsiob.steadyhealth.session.ComingBack
import com.kamsiob.steadyhealth.session.WhyAway
import com.kamsiob.steadyhealth.ui.screens.AbilitiesUiState
import com.kamsiob.steadyhealth.ui.screens.AbilityRowState
import com.kamsiob.steadyhealth.ui.screens.AbilityTileState
import com.kamsiob.steadyhealth.ui.screens.BringBack
import com.kamsiob.steadyhealth.ui.screens.FirstMonthCard
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
import java.time.format.TextStyle
import java.util.Locale

/**
 * The state every screen reads, and the only place that talks to the
 * repositories.
 *
 * A screen never touches a repository and the engine never touches this. What is
 * here is assembly: reading rows, handing values to pure functions, and turning
 * the answers into something a screen can draw.
 */
/*
 * detekt is right that this class is too large, and it has been told so five times.
 * Four subjects have already moved out: the Sessions tab, the therapist page, coming
 * back after a gap, and the sentences about somebody's own history.
 *
 * What is left is two halves that do not belong together: Today, and the walk. The
 * walk half is the old plan's, it shares four pieces of mutable state with the rest of
 * this class, and ADDENDUM-03 Part 21 rebuilds it in Phase 5. Splitting it now means
 * untangling that state twice, once here and once when the walk is replaced, so the
 * suppression is a note about a job with a date on it rather than a decision to leave
 * it alone. HANDOFF.md carries the same note.
 */
@Suppress("LargeClass")
class SteadyViewModel(application: Application) : AndroidViewModel(application) {

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
    internal val profile get() = ProfileRepository(db)
    internal val weight get() = WeightRepository(db)
    internal val days get() = DayRepository(db)
    internal val abilities get() = AbilityRepository(db)
    internal val movement get() = MovementRepository(db)
    internal val weeks get() = WeekRepository(db)
    internal val checks get() = CheckRepository(db)
    internal val runs get() = RunRepository(db)
    private val away get() = ComingBackFrom(getApplication(), db)
    private val cards get() = TodayCards(getApplication(), db)

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
            if (done) {
                // Idempotent: the work is unique and enqueued with KEEP, so this is
                // how the one daily prompt survives a reinstall or a cleared job
                // queue without anybody having to visit Settings.
                ReminderWorker.schedule(getApplication())
                refresh()
            }
        }
    }

    /**
     * Everything was deleted, so the app is new again.
     *
     * Not a restart and not a crash: the database is gone, so setup is genuinely
     * where this person now is, and pretending otherwise would leave every screen
     * reading from something that is not there.
     */
    fun startAgain() {
        _onboardingComplete.value = false
    }

    fun onboardingFinished() = viewModelScope.launch {
        _onboardingComplete.value = true
        ReminderWorker.schedule(getApplication())
        refresh()
    }

    /**
     * The one answer to "ready to try those again?".
     *
     * Yes closes the episode and the movements come back. Not yet puts another quiet
     * week between the person and the question, which is the only honest thing to do
     * with an answer that means "ask me later".
     */
    fun bringBack(area: Area, yes: Boolean) = viewModelScope.launch {
        val today = today()
        if (yes) runs.clearSore(area, today) else runs.reportSore(area, today)
        refresh()
    }

    /**
     * The app's extras, off or back on, from the card itself. ADDENDUM-03 Part 6.
     *
     * "In one tap" is the whole of the requirement, so this is a toggle and not a
     * screen: the same row turns them off and turns them back on, and nothing asks
     * whether the person is sure. It changes nothing about the therapist's plan, and
     * it is the same setting the You tab shows, so turning it off here and looking
     * there does not find two different answers.
     */
    fun toggleExtras() = viewModelScope.launch {
        profile.setExtras(!profile.extras())
        refresh()
    }

    /** The one answer to "what has been happening?". ADDENDUM-03 Part 15. */
    fun answerWhyAway(why: WhyAway) = viewModelScope.launch {
        away.answer(why)
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
        refreshDirection()
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
        val nextThing = cards.theNextThing()
        val comingBack = away.now()

        _today.value = TodayUiState(
            date = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
            greeting = context.getString(greetingFor(LocalTime.now())),
            abilities = abilityTiles(),
            // Numbers off does not mean nothing here. DESIGN.md replaces the
            // figure with a direction word, so the block still says which way
            // things are going, which is the part that was ever useful.
            weightValue = latest?.takeIf { weighsIn }?.let {
                if (showNumbers) Convert.weightLabel(it.smoothedKg, units) else directionWord()
            },
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
            session = cards.sessionCard(date.toEpochDay(), way.way, exclusions),
            noticed = cards.noticedLines(date.toEpochDay()),
            week = cards.weekLine(date.toEpochDay()),
            want = nextThing?.first,
            towards = nextThing?.second,
            worthAWord = worthAWord(today),
            sunday = date.dayOfWeek == DayOfWeek.SUNDAY,
            askWhyAway = comingBack is ComingBack.Ask,
            afterAway = away.said(comingBack),
            appointmentSoon = appointmentSoon(),
            bringBack = bringBackQuestion(today),
        )
    }

    /**
     * "You see your physio on Thursday. Your page is ready." ADDENDUM-03 Part 6.
     *
     * Two days before, and only then. It is a line on Today rather than a
     * notification, because a notification about an appointment somebody already
     * knows about is the app telling them something they told it.
     *
     * Whose appointment it is comes from the plan carrying that date, not from
     * whichever plan is oldest. Somebody with a physio plan and an OT plan would
     * otherwise be sent to the wrong appointment by name.
     */
    private suspend fun appointmentSoon(): String? {
        val plans = PlanRepository(db).live()
        val soon = ReviewDate.due(plans.mapNotNull { it.reviewDay }, today())
        if (soon !is ReviewPrompt.Send) return null
        val label = plans.firstOrNull { it.reviewDay == soon.onDay }?.label.orEmpty()
            .ifBlank { string(R.string.plan_them) }
        val day = LocalDate.ofEpochDay(soon.onDay)
            .dayOfWeek
            .getDisplayName(TextStyle.FULL, Locale.getDefault())
        return string(R.string.plan_review_soon, label, day)
    }

    /**
     * "Worth a word with your doctor about that shoulder." ADDENDUM-03 Part 2.
     *
     * Once, ever, per area, after the same one has hurt twice inside a month. No
     * interpretation, no advice, and no second time: `showOnce` is what makes the
     * "once" true rather than intended.
     */
    private suspend fun worthAWord(today: Long): String? {
        val area = runs.soreAreas(today).firstOrNull { runs.reportedTwiceInAMonth(it, today) }
            ?: return null
        val said = profile.showOnce("$WORTH_A_WORD:${area.id}", System.currentTimeMillis())
        if (!said) return null
        return string(R.string.hurt_worth_a_word, string(Labels.forArea(area)).lowercase())
    }

    /** The one question about an area whose week is up. */
    private suspend fun bringBackQuestion(today: Long): BringBack? {
        val area = runs.soreAreaToAskAbout(today) ?: return null
        return BringBack(
            question = string(R.string.hurt_bring_back, string(Labels.forArea(area)).lowercase()),
            area = area,
        )
    }

    private fun string(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

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

    /**
     * The label above the next thing.
     *
     * A walker is walking, so it says walk. A wheelchair gets its own word rather
     * than the domain id: "Today, for Go" was the only one of the four that named a
     * category instead of a thing somebody is about to do.
     */
    private fun nextLabel() = when {
        pacing -> R.string.move_pacing_label
        way.way == GettingAround.Wheelchair -> R.string.today_next_wheel
        way.way == GettingAround.InBed -> R.string.today_move_bed
        else -> R.string.today_next_walk
    }

    private fun weightExplain(rawKg: Double?, units: Units, showNumbers: Boolean): String {
        val context = getApplication<Application>()
        // "Nothing to compare yet" followed by "than a month ago" is two halves of
        // a sentence that does not exist. The line under the word is only there
        // when the word is a comparison.
        if (!showNumbers) {
            val word = direction
            return if (word == context.getString(R.string.direction_first)) {
                ""
            } else {
                context.getString(R.string.direction_since)
            }
        }
        if (rawKg == null) return context.getString(R.string.weight_first_week)
        return context.getString(
            R.string.weight_daily_line,
            "${Convert.weightLabel(rawKg, units)} ${unitLabel(units)}",
        )
    }

    /**
     * Which way the smoothed weight has gone in the last month, as a word.
     *
     * The whole of numbers-off is this function and the places that call it. No
     * figure reaches a screen, and the direction still does, because somebody who
     * turned the numbers off did not ask to be told nothing.
     */
    private var direction: String = ""

    private fun directionWord(): String = direction

    private suspend fun refreshDirection() {
        val context = getApplication<Application>()
        val series = weight.series()
        val now = series.lastOrNull()
        val then = series.lastOrNull { it.epochDay <= (now?.epochDay ?: 0) - A_MONTH }
        direction = when {
            now == null || then == null -> context.getString(R.string.direction_first)
            now.smoothedKg < then.smoothedKg - WeightEngine.SAME_BAND_KG ->
                context.getString(R.string.direction_lower)

            now.smoothedKg > then.smoothedKg + WeightEngine.SAME_BAND_KG ->
                context.getString(R.string.direction_higher)

            else -> context.getString(R.string.direction_same)
        }
    }

    private fun unitLabel(units: Units): String = getApplication<Application>().getString(
        if (units == Units.Imperial) R.string.unit_lb else R.string.unit_kg,
    )

    private suspend fun abilityTiles(): List<AbilityTileState> {
        val context = getApplication<Application>()
        val items = abilities.items()
        val measured = checks.latestValues()
        return AbilityDomain.entries.map { domain ->
            AbilityTileState(
                domain = domain,
                // The four ids never change; only what they are called does.
                name = context.getString(nameFor(domain)),
                lifeSentence = sentenceFor(
                    domain,
                    measured,
                    items.firstOrNull { it.domain == domain.id }?.text,
                ),
            )
        }
    }

    /**
     * What one tile says about a life.
     *
     * The measured sentence when there is one, because it describes something the
     * person has actually done. Their own words until then, because a made-up
     * sentence about somebody's life is worse than none and an empty tile says
     * nothing at all.
     */
    private fun sentenceFor(
        domain: AbilityDomain,
        measured: Map<String, Double>,
        theirWords: String?,
    ): String {
        val context = getApplication<Application>()
        val sentence = LifeSentences.forDomain(domain, measured)
        return when {
            sentence != null -> context.getString(shortLifeStringOf(sentence.id))
            !theirWords.isNullOrBlank() -> theirWords
            // Nothing measured and nothing on their list. Say what the ability is
            // rather than leaving a tile with a word and an empty half.
            else -> context.getString(AbilityWords.plain(way.way, domain))
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
            weighsIn = way.weighsIn && profile.weighsIn(),
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

        val results = checks.results()
        val measured = checks.latestValues()
        val numbersOn = profile.showNumbers()

        _abilities.value = AbilitiesUiState(
            abilities = AbilityDomain.entries.map { domain ->
                AbilityRowState(
                    domain = domain,
                    name = context.getString(nameFor(domain)),
                    lifeSentence = sentenceFor(
                        domain,
                        measured,
                        items.firstOrNull { it.domain == domain.id }?.text,
                    ),
                    // Before the first check there is nothing measured to compare
                    // and the engine says Same, which is the honest answer.
                    state = AbilityEngine.stateOf(domain, results),
                )
            },
            items = ratings.map { (item, rating) ->
                TrackedItemState(
                    text = item.text,
                    domain = AbilityDomain.fromId(item.domain) ?: AbilityDomain.GetUp,
                    rating = rating,
                    said = ratingSaid(item.id, rating, numbersOn),
                )
            },
            waiting = true,
            week = weekNote(),
            weeks = cards.weekBars(numbersOn),
            weeksSaid = cards.weeksSaid(),
            lookBack = cards.lookBackLine(),
            firstMonth = firstMonthCard(),
            checkLede = string(checkLede()),
        )
    }

    /**
     * One item's rating, as a number or as a word.
     *
     * With numbers off it says which way it went since the month before rather than
     * where it stands, because "seven out of ten" turned into a word about seven is
     * still a place on a scale, and the point of the setting is that there is no
     * scale to be placed on.
     */
    private suspend fun ratingSaid(itemId: Long, rating: Int, numbersOn: Boolean): String {
        if (numbersOn) return string(R.string.rating_now, rating)
        val months = abilities.months(itemId).sortedBy { it.epochDay }
        val before = months.getOrNull(months.size - 2)?.rating?.toDouble()
        return string(
            when (NumbersOff.went(before, rating.toDouble())) {
                Went.Up -> R.string.rating_word_up
                Went.Down -> R.string.rating_word_down
                Went.Same -> R.string.rating_word_same
                Went.Unknown -> R.string.rating_word_first
            },
        )
    }

    /** What the monthly check needs, which is not the same in the four versions. */
    private fun checkLede() = when (way.way) {
        GettingAround.Wheelchair -> R.string.check_intro_lede_wheel
        GettingAround.InBed -> R.string.check_intro_lede_bed
        else -> R.string.check_intro_lede
    }

    /**
     * The first month card, or nothing. ADDENDUM-03 Part 16.
     *
     * The first thing the person named, because the card is about their own words and
     * the first one is what they gave before the app had asked them for anything else.
     * Every rule about when it appears is in Warmth, which is a pure function; this
     * only fetches the two dates and turns the answer into a sentence.
     */
    private suspend fun firstMonthCard(): FirstMonthCard? {
        val item = abilities.items().minByOrNull { it.createdAt } ?: return null
        val card = Warmth.firstMonth(
            itemText = item.text,
            firstDay = item.createdAt / MILLIS_PER_DAY,
            today = today(),
            months = abilities.months(item.id),
        ) ?: return null
        val words = when {
            !card.moved -> string(R.string.first_month_same, card.itemText, card.then)
            card.forward -> string(R.string.first_month_forward, card.itemText, card.then, card.now)
            else -> string(R.string.first_month_back, card.itemText, card.then, card.now)
        }
        return FirstMonthCard(heading = string(R.string.first_month_heading), line = words)
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
     * and the glyph; only the word changes. The table is in [AbilityWords] because
     * the ability page asks the same question and used to answer it differently.
     */
    private fun nameFor(domain: AbilityDomain) = AbilityWords.name(way.way, domain)

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
        const val A_MONTH = 30L

        /** Anything above this is not an envelope any more. */

        const val NOON = 12
        const val EVENING_HOUR = 18
        const val DEFAULT_KG = 80.0
        const val DEFAULT_SLEEP_HALF_HOURS = 14
        const val SECONDS_PER_MINUTE = 60
        const val ZERO = "0:00"

        /** Keyed by ladder and by the day they left, so it fires once per gap. */
        const val WELCOME_BACK_NOTICE = "welcome_back"
        const val WORTH_A_WORD = "worth_a_word"

        /** Milliseconds in a day, for turning a saved timestamp into a day. */
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
