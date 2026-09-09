package com.kamsiob.steadyhealth.data

import com.kamsiob.steadyhealth.ai.Synonym
import com.kamsiob.steadyhealth.ai.TagCount
import com.kamsiob.steadyhealth.ai.TagReader
import com.kamsiob.steadyhealth.ai.VisitWindow
import com.kamsiob.steadyhealth.ai.WeekBrief
import com.kamsiob.steadyhealth.ai.WeekNote
import com.kamsiob.steadyhealth.ai.WeightDirection
import com.kamsiob.steadyhealth.data.entity.CheckEntity
import com.kamsiob.steadyhealth.data.entity.CheckInEntity
import com.kamsiob.steadyhealth.data.entity.CheckInTagEntity
import com.kamsiob.steadyhealth.data.entity.DailyPromptEntity
import com.kamsiob.steadyhealth.data.entity.DocumentEntity
import com.kamsiob.steadyhealth.data.entity.DocumentPageEntity
import com.kamsiob.steadyhealth.data.entity.ExclusionEntity
import com.kamsiob.steadyhealth.data.entity.ExperimentEntity
import com.kamsiob.steadyhealth.data.entity.ItemRatingEntity
import com.kamsiob.steadyhealth.data.entity.LadderStateEntity
import com.kamsiob.steadyhealth.data.entity.MeasureResultEntity
import com.kamsiob.steadyhealth.data.entity.NoticeEntity
import com.kamsiob.steadyhealth.data.entity.PersonSynonymEntity
import com.kamsiob.steadyhealth.data.entity.PlanEntity
import com.kamsiob.steadyhealth.data.entity.PlanItemEntity
import com.kamsiob.steadyhealth.data.entity.ReadinessEntity
import com.kamsiob.steadyhealth.data.entity.ReminderSentEntity
import com.kamsiob.steadyhealth.data.entity.RunEntity
import com.kamsiob.steadyhealth.data.entity.RunMovementEntity
import com.kamsiob.steadyhealth.data.entity.SessionEntity
import com.kamsiob.steadyhealth.data.entity.SettingEntity
import com.kamsiob.steadyhealth.data.entity.SoreAreaEntity
import com.kamsiob.steadyhealth.data.entity.StepNameEntity
import com.kamsiob.steadyhealth.data.entity.TrackedItemEntity
import com.kamsiob.steadyhealth.data.entity.WeeklyNoteEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.domain.Anchor
import com.kamsiob.steadyhealth.domain.ChairEase
import com.kamsiob.steadyhealth.domain.DayRating
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.FloorAccess
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.Ladder
import com.kamsiob.steadyhealth.domain.NextDayFeel
import com.kamsiob.steadyhealth.domain.PemAnswer
import com.kamsiob.steadyhealth.domain.ReadinessFlag
import com.kamsiob.steadyhealth.domain.Stairs
import com.kamsiob.steadyhealth.domain.TalkTest
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.domain.WalkTolerance
import com.kamsiob.steadyhealth.domain.WeightSource
import com.kamsiob.steadyhealth.engine.DoneSession
import com.kamsiob.steadyhealth.engine.Envelope
import com.kamsiob.steadyhealth.engine.Experiment
import com.kamsiob.steadyhealth.engine.ExperimentEngine
import com.kamsiob.steadyhealth.engine.Gap
import com.kamsiob.steadyhealth.engine.ItemHistory
import com.kamsiob.steadyhealth.engine.Ladders
import com.kamsiob.steadyhealth.engine.MeasureResult
import com.kamsiob.steadyhealth.engine.Measures
import com.kamsiob.steadyhealth.engine.Mention
import com.kamsiob.steadyhealth.engine.MonthOfSessions
import com.kamsiob.steadyhealth.engine.PacingEngine
import com.kamsiob.steadyhealth.engine.Pattern
import com.kamsiob.steadyhealth.engine.PatternMeasure
import com.kamsiob.steadyhealth.engine.Patterns
import com.kamsiob.steadyhealth.engine.Prompted
import com.kamsiob.steadyhealth.engine.Reading
import com.kamsiob.steadyhealth.engine.ReminderKind
import com.kamsiob.steadyhealth.engine.Reminders
import com.kamsiob.steadyhealth.engine.Smoothed
import com.kamsiob.steadyhealth.engine.Variable
import com.kamsiob.steadyhealth.engine.VisitInputs
import com.kamsiob.steadyhealth.engine.VisitSummaryEngine
import com.kamsiob.steadyhealth.engine.WeekOfDays
import com.kamsiob.steadyhealth.engine.WeightEngine
import com.kamsiob.steadyhealth.export.Sheet
import com.kamsiob.steadyhealth.session.Area
import com.kamsiob.steadyhealth.session.Done
import com.kamsiob.steadyhealth.session.Felt
import com.kamsiob.steadyhealth.session.Kit
import com.kamsiob.steadyhealth.session.Movements
import com.kamsiob.steadyhealth.session.Piece
import com.kamsiob.steadyhealth.session.Result
import com.kamsiob.steadyhealth.session.Week
import java.time.LocalDate

/**
 * The only things that touch the database.
 *
 * A screen never reads a table and the engine never reads a repository. The
 * engine takes values and returns values; the repositories turn rows into those
 * values and back. That is what lets every rule in LOGIC.md be tested without a
 * device, and it is the one boundary in this app worth being strict about.
 */

/** Weight, LOGIC.md section 1. */
class WeightRepository(private val db: SteadyDatabase) {

    suspend fun save(epochDay: Long, kg: Double, at: Long, source: WeightSource = WeightSource.Entered) {
        val previous = db.weighIns().allOnce().lastOrNull { it.epochDay < epochDay }
        db.weighIns().upsert(
            WeighInEntity(
                epochDay = epochDay,
                recordedAt = at,
                rawKg = kg,
                smoothedKg = WeightEngine.next(previous?.smoothedKg, kg),
                source = source.id,
            ),
        )
        resmoothFrom(epochDay)
    }

    /**
     * Rewrite the smoothed values after [epochDay].
     *
     * Somebody can add a morning they missed, and every reading after it then has
     * the wrong average behind it. Recomputing the tail is cheap and is the only
     * way the stored series stays the series the engine would produce.
     */
    private suspend fun resmoothFrom(epochDay: Long) {
        val all = db.weighIns().allOnce()
        if (all.none { it.epochDay > epochDay }) return
        val smoothed = WeightEngine.smooth(all.map { Reading(it.epochDay, it.rawKg) })
        smoothed.forEach { value ->
            val row = all.first { it.epochDay == value.epochDay }
            if (row.smoothedKg != value.smoothedKg) {
                db.weighIns().upsert(row.copy(smoothedKg = value.smoothedKg))
            }
        }
    }

    suspend fun latest(): WeighInEntity? = db.weighIns().latest()

    suspend fun forDay(epochDay: Long): WeighInEntity? = db.weighIns().forDay(epochDay)

    suspend fun series(): List<Smoothed> =
        db.weighIns().allOnce().map { Smoothed(it.epochDay, it.rawKg, it.smoothedKg) }

    suspend fun count(): Int = db.weighIns().allOnce().size
}

/** One day, as the person described it. */
data class DayEntry(
    val epochDay: Long,
    val sentence: String,
    val sleepHalfHours: Int?,
    val dayRating: DayRating?,
    val spoken: Boolean,
)

class DayRepository(private val db: SteadyDatabase) {

    suspend fun forDay(epochDay: Long): DayEntry? =
        db.checkIns().forDay(epochDay)?.let {
            DayEntry(
                epochDay = it.epochDay,
                sentence = it.sentence,
                sleepHalfHours = it.sleepHalfHours,
                dayRating = it.dayRating?.let(DayRating::fromId),
                spoken = it.spoken,
            )
        }

    suspend fun save(entry: DayEntry, at: Long) {
        val existing = db.checkIns().forDay(entry.epochDay)
        db.checkIns().upsert(
            CheckInEntity(
                id = existing?.id ?: 0,
                epochDay = entry.epochDay,
                recordedAt = at,
                sentence = entry.sentence.trim(),
                sleepHalfHours = entry.sleepHalfHours,
                dayRating = entry.dayRating?.id,
                spoken = entry.spoken,
            ),
        )
    }

    suspend fun count(): Int = db.checkIns().count()

    /**
     * The tags on one day.
     *
     * Stored against the check-in rather than the day, because a day with no
     * sentence has no tags either: the tags are about what somebody said.
     */
    suspend fun tagsFor(epochDay: Long): List<String> =
        db.checkIns().forDay(epochDay)?.let { row ->
            db.checkIns().tagsFor(row.id).map { it.tag }
        }.orEmpty()

    suspend fun setTags(epochDay: Long, tags: List<String>, fromReader: Set<String> = emptySet()) {
        val row = db.checkIns().forDay(epochDay) ?: return
        db.checkIns().clearTags(row.id)
        db.checkIns().insertTags(
            tags.map { CheckInTagEntity(checkInId = row.id, tag = it, fromReader = it in fromReader) },
        )
    }

    /** Every tag ever chosen, with the day it was on. For the week and the patterns. */
    suspend fun tagDays(): List<Pair<Long, String>> {
        val days = db.checkIns().allOnce().associate { it.id to it.epochDay }
        return db.checkIns().allTagsOnce().mapNotNull { tag ->
            days[tag.checkInId]?.let { it to tag.tag }
        }
    }

    /** What the person has taught the app one of their own phrases means. */
    suspend fun synonyms(): List<Synonym> =
        db.synonyms().all().map { Synonym(it.phrase, it.tag) }

    suspend fun learn(phrase: String, tag: String, at: Long) {
        if (!TagReader.learnable(phrase)) return
        db.synonyms().upsert(PersonSynonymEntity(phrase.trim().lowercase(), tag, at))
    }
}

/** Sessions and where the person is on each ladder. */
class MovementRepository(private val db: SteadyDatabase) {

    suspend fun state(ladder: Ladder): LadderStateEntity =
        db.ladders().state(ladder.id) ?: LadderStateEntity(
            ladder = ladder.id,
            currentStepIndex = 0,
            lastOfferedDay = null,
            offerDeclinedUntilDay = null,
            easingUntilDay = null,
            lastSessionDay = null,
        ).also { db.ladders().upsertState(it) }

    suspend fun moveTo(ladder: Ladder, stepIndex: Int) {
        db.ladders().upsertState(state(ladder).copy(currentStepIndex = stepIndex))
    }

    suspend fun nameFor(ladder: Ladder, stepIndex: Int): String? =
        db.ladders().nameFor(ladder.id, stepIndex)

    suspend fun name(ladder: Ladder, stepIndex: Int, name: String, at: Long) {
        db.ladders().upsertName(StepNameEntity(ladder.id, stepIndex, name.trim(), at))
    }

    suspend fun record(
        ladder: Ladder,
        stepIndex: Int,
        epochDay: Long,
        startedAt: Long,
        endedAt: Long,
        seconds: Int,
        talkTest: TalkTest?,
    ): Long {
        val id = db.sessions().upsert(
            SessionEntity(
                epochDay = epochDay,
                startedAt = startedAt,
                endedAt = endedAt,
                durationSeconds = seconds,
                ladder = ladder.id,
                stepIndex = stepIndex,
                talkTest = talkTest?.id,
                steps = null,
                distanceMetres = null,
                nextDayFeel = null,
                nextDayAskedOnDay = null,
            ),
        )
        db.ladders().upsertState(state(ladder).copy(lastSessionDay = epochDay))
        return id
    }

    suspend fun setTalkTest(sessionId: Long, answer: TalkTest) {
        db.sessions().allOnce().firstOrNull { it.id == sessionId }?.let {
            db.sessions().upsert(it.copy(talkTest = answer.id))
        }
    }

    suspend fun sessionsOn(epochDay: Long): List<SessionEntity> = db.sessions().forDay(epochDay)

    suspend fun sessionsBetween(from: Long, to: Long): List<SessionEntity> =
        db.sessions().between(from, to)

    suspend fun latest(): SessionEntity? = db.sessions().latest()

    /** Every session on one ladder, in the shape the progression rules take. */
    suspend fun doneSessions(ladder: Ladder): List<DoneSession> =
        db.sessions().allOnce()
            .filter { it.ladder == ladder.id }
            .map {
                DoneSession(
                    epochDay = it.epochDay,
                    stepIndex = it.stepIndex,
                    durationSeconds = it.durationSeconds,
                    talkTest = it.talkTest?.let(TalkTest::fromId),
                    nextDayFeel = it.nextDayFeel?.let(NextDayFeel::fromId),
                )
            }

    suspend fun declineOffer(ladder: Ladder, until: Long) {
        db.ladders().upsertState(state(ladder).copy(offerDeclinedUntilDay = until))
    }

    suspend fun recordOffered(ladder: Ladder, day: Long) {
        db.ladders().upsertState(state(ladder).copy(lastOfferedDay = day))
    }

    /** After a long gap: a fortnight during which nothing is offered. */
    suspend fun ease(ladder: Ladder, untilDay: Long) {
        db.ladders().upsertState(state(ladder).copy(easingUntilDay = untilDay))
    }
}

/** The person's own list, and their ratings of it. */
class AbilityRepository(private val db: SteadyDatabase) {

    suspend fun items(): List<TrackedItemEntity> = db.abilities().itemsOnce()

    suspend fun add(text: String, domain: AbilityDomain, at: Long): Long =
        db.abilities().upsertItem(
            TrackedItemEntity(text = text.trim(), domain = domain.id, createdAt = at, archivedAt = null),
        )

    suspend fun rate(itemId: Long, epochDay: Long, rating: Int, at: Long) {
        db.abilities().upsertRating(ItemRatingEntity(itemId, epochDay, rating.coerceIn(0, MAX_RATING), at))
    }

    suspend fun latestRating(itemId: Long): ItemRatingEntity? =
        db.abilities().ratingsFor(itemId).maxByOrNull { it.epochDay }

    suspend fun ratings(itemId: Long): List<ItemRatingEntity> = db.abilities().ratingsFor(itemId)

    private companion object {
        const val MAX_RATING = 10
    }
}

/** Settings, exclusions, readiness, and the notices that have been shown once. */
class ProfileRepository(private val db: SteadyDatabase) {

    private suspend fun put(key: String, value: String) = db.profile().put(SettingEntity(key, value))

    private suspend fun get(key: String): String? = db.profile().get(key)

    /**
     * Whether a one-time sand block has been read.
     *
     * Stored under its own key rather than in a list, so a screen added later cannot
     * arrive already dismissed and nothing has to be migrated when one is removed.
     */
    suspend fun seen(key: String): Boolean = get(key).toBoolean()

    suspend fun markSeen(key: String) = put(key, true.toString())

    suspend fun onboardingComplete(): Boolean = get(ONBOARDED).toBoolean()

    suspend fun setOnboardingComplete() = put(ONBOARDED, true.toString())

    suspend fun gettingAround(): GettingAround =
        get(GETTING_AROUND)?.let(GettingAround::fromId) ?: GettingAround.OnFeet

    /**
     * The same answer, without the default.
     *
     * Onboarding needs to tell "not asked yet" from "on my feet", because the first
     * session is planned from it and planning one before the question is answered is
     * how somebody in a wheelchair gets offered heel raises.
     */
    suspend fun gettingAroundOrNull(): GettingAround? =
        get(GETTING_AROUND)?.let(GettingAround::fromId)

    /**
     * Where the first run stopped, so closing the app halfway resumes there.
     *
     * Kept as the plain name rather than the enum, because the screens are a user
     * interface concern and the data layer has no business importing one.
     */
    suspend fun onboardingStep(): String? = get(ONBOARDING_STEP)

    suspend fun setOnboardingStep(name: String) = put(ONBOARDING_STEP, name)

    /** A chair with arms is still a chair; it changes the variant, not the movement. */
    suspend fun chairHasArms(): Boolean = get(CHAIR_ARMS).toBoolean()

    suspend fun setChairHasArms(value: Boolean) = put(CHAIR_ARMS, value.toString())

    /**
     * How many sessions a week the person is aiming for. Three, four or five.
     *
     * Coerced on the way out as well as on the way in, so a value written by an older
     * build or a hand-edited row cannot produce a week nobody can meet.
     */
    suspend fun weekTarget(): Int =
        get(WEEK_TARGET)?.toIntOrNull()?.coerceIn(Week.CHOICES.first(), Week.CHOICES.last())
            ?: Week.DEFAULT

    suspend fun setWeekTarget(value: Int) = put(WEEK_TARGET, value.toString())

    /** The day the person started, for "since you began" and for nothing else. */
    suspend fun anchorDay(): Long? = get(ANCHOR_DAY)?.toLongOrNull()

    suspend fun setAnchorDay(day: Long) = put(ANCHOR_DAY, day.toString())

    suspend fun setGettingAround(value: GettingAround) = put(GETTING_AROUND, value.id)

    suspend fun withTherapist(): Boolean = get(WITH_THERAPIST).toBoolean()

    suspend fun setWithTherapist(value: Boolean) = put(WITH_THERAPIST, value.toString())

    suspend fun units(): Units =
        Units.entries.firstOrNull { it.id == get(UNITS) } ?: Units.Imperial

    suspend fun setUnits(value: Units) = put(UNITS, value.id)

    suspend fun heightCm(): Double? = get(HEIGHT)?.toDoubleOrNull()

    suspend fun setHeightCm(value: Double) = put(HEIGHT, value.toString())

    suspend fun age(): Int? = get(AGE)?.toIntOrNull()

    suspend fun setAge(value: Int?) {
        if (value == null) db.profile().put(SettingEntity(AGE, "")) else put(AGE, value.toString())
    }

    suspend fun chair(): ChairEase? = ChairEase.entries.firstOrNull { it.id == get(CHAIR) }

    suspend fun setChair(value: ChairEase) = put(CHAIR, value.id)

    suspend fun stairs(): Stairs? = Stairs.entries.firstOrNull { it.id == get(STAIRS) }

    suspend fun setStairs(value: Stairs) = put(STAIRS, value.id)

    suspend fun walkTolerance(): WalkTolerance? =
        WalkTolerance.entries.firstOrNull { it.id == get(WALK) }

    suspend fun setWalkTolerance(value: WalkTolerance) = put(WALK, value.id)

    suspend fun floorAccess(): FloorAccess? = FloorAccess.entries.firstOrNull { it.id == get(FLOOR) }

    suspend fun setFloorAccess(value: FloorAccess) = put(FLOOR, value.id)

    suspend fun pem(): PemAnswer? = PemAnswer.entries.firstOrNull { it.id == get(PEM) }

    suspend fun setPem(value: PemAnswer) = put(PEM, value.id)

    suspend fun anchor(): Anchor? = Anchor.entries.firstOrNull { it.id == get(ANCHOR) }

    suspend fun setAnchor(value: Anchor) = put(ANCHOR, value.id)

    suspend fun weighsIn(): Boolean = get(WEIGHS_IN)?.toBoolean() ?: true

    suspend fun setWeighsIn(value: Boolean) = put(WEIGHS_IN, value.toString())

    /**
     * Pacing mode, and the envelope that goes with it.
     *
     * The starting envelope is kept as well as the current one, because the
     * settings screen has to be able to say how far it has come down without
     * asking the person to remember.
     */
    suspend fun pacing(): Boolean = get(PACING).toBoolean()

    suspend fun setPacing(value: Boolean) = put(PACING, value.toString())

    suspend fun envelope(): Envelope = Envelope(
        minutes = get(ENVELOPE_MINUTES)?.toIntOrNull() ?: PacingEngine.DEFAULT.minutes,
        daysPerWeek = get(ENVELOPE_DAYS)?.toIntOrNull() ?: PacingEngine.DEFAULT.daysPerWeek,
    )

    suspend fun setEnvelope(value: Envelope) {
        put(ENVELOPE_MINUTES, value.minutes.toString())
        put(ENVELOPE_DAYS, value.daysPerWeek.toString())
        if (get(ENVELOPE_START) == null) put(ENVELOPE_START, value.minutes.toString())
    }

    suspend fun envelopeStart(): Int = get(ENVELOPE_START)?.toIntOrNull() ?: PacingEngine.DEFAULT.minutes

    /**
     * Whether one kind of reminder is on. All four are off until somebody says
     * otherwise, which LOGIC.md section 12 makes a default and not a suggestion.
     */
    /**
     * Whether one kind of reminder is on.
     *
     * The daily prompt is the only one on by default, which is ADDENDUM-03 Part 13.
     * Everything else is off until somebody turns it on, and all of them together are
     * capped at two a week.
     */
    suspend fun reminderOn(kind: ReminderKind): Boolean =
        get("remind_${kind.id}")?.toBoolean() ?: (kind == ReminderKind.Daily)

    suspend fun setReminderOn(kind: ReminderKind, value: Boolean) =
        put("remind_${kind.id}", value.toString())

    /**
     * Whether the app's own suggestions appear beneath a therapist's plan.
     *
     * ADDENDUM-03 Part 6: the person can turn the extras off entirely, in one tap,
     * and many will. On by default, and it changes nothing about the plan itself.
     */
    suspend fun extras(): Boolean = get(EXTRAS)?.toBoolean() ?: true

    suspend fun setExtras(value: Boolean) = put(EXTRAS, value.toString())

    /** True when the daily prompt turned itself off, so Settings can say why. */
    suspend fun dailyGaveUp(): Boolean = get(DAILY_GAVE_UP).toBoolean()

    suspend fun setDailyGaveUp(value: Boolean) = put(DAILY_GAVE_UP, value.toString())

    suspend fun anyReminderOn(): Boolean = ReminderKind.entries.any { reminderOn(it) }

    /** Try it and see. On by default, and never on for anybody in pacing mode. */
    suspend fun tryItAndSee(): Boolean = get(TRY_IT)?.toBoolean() ?: true

    suspend fun setTryItAndSee(value: Boolean) = put(TRY_IT, value.toString())

    /** What a finished test settled, if it settled anything. */
    suspend fun strengthInTheEvening(): Boolean = get(EVENING_SET).toBoolean()

    suspend fun setStrengthInTheEvening(value: Boolean) = put(EVENING_SET, value.toString())

    /**
     * What is in the room. ADDENDUM-03 Part 15.
     *
     * Nothing needing equipment somebody does not have is ever suggested, so the
     * default is the three things almost every home has and the rest is opt-in.
     */
    suspend fun kit(): Set<Kit> {
        val stored = get(KIT)?.split(",")?.mapNotNull { id ->
            Kit.entries.firstOrNull { it.id == id.trim() }
        }
        return (stored?.toSet() ?: DEFAULT_KIT) + Kit.None
    }

    suspend fun setKit(value: Set<Kit>) = put(KIT, value.joinToString(",") { it.id })

    suspend fun showNumbers(): Boolean = get(SHOW_NUMBERS)?.toBoolean() ?: true

    suspend fun setShowNumbers(value: Boolean) = put(SHOW_NUMBERS, value.toString())

    suspend fun exclusions(): Set<Exclusion> =
        db.profile().exclusionIds().mapNotNull(Exclusion::fromId).toSet()

    suspend fun setExclusions(values: Set<Exclusion>, at: Long) {
        db.profile().clearExclusions()
        values.forEach { db.profile().putExclusion(ExclusionEntity(it.id, at)) }
    }

    suspend fun readiness(): Map<ReadinessFlag, Boolean> =
        db.profile().readinessOnce().mapNotNull { row ->
            ReadinessFlag.fromId(row.flag)?.let { it to row.answeredYes }
        }.toMap()

    suspend fun setReadiness(flag: ReadinessFlag, yes: Boolean, at: Long) {
        db.profile().putReadiness(ReadinessEntity(flag.id, yes, at))
    }

    /** True the first time only. What stops the app repeating itself. */
    suspend fun showOnce(noticeId: String, at: Long): Boolean {
        if (db.notices().get(noticeId) != null) return false
        db.notices().put(NoticeEntity(noticeId, at))
        return true
    }

    suspend fun hasShown(noticeId: String): Boolean = db.notices().get(noticeId) != null

    /** When a notice was shown, for the ones that may come round again. */
    suspend fun shownOn(noticeId: String): Long? = db.notices().get(noticeId)?.shownAt

    companion object {
        const val ONBOARDED = "onboarding_complete"
        const val GETTING_AROUND = "getting_around"
        const val ONBOARDING_STEP = "onboarding_step"
        const val CHAIR_ARMS = "chair_arms"
        const val ANCHOR_DAY = "anchor_day"
        const val WEEK_TARGET = "week_target"
        const val DAILY_GAVE_UP = "daily_gave_up"
        const val EXTRAS = "plan_extras"
        const val WITH_THERAPIST = "with_therapist"
        const val UNITS = "units"
        const val HEIGHT = "height_cm"
        const val AGE = "age"
        const val CHAIR = "chair"
        const val STAIRS = "stairs"
        const val WALK = "walk_tolerance"
        const val FLOOR = "floor_access"
        const val PEM = "pem"
        const val ANCHOR = "anchor"
        const val SHOW_NUMBERS = "show_numbers"
        const val KIT = "kit"

        /** A chair and a wall. Nearly every home has both, and nothing else is assumed. */
        val DEFAULT_KIT = setOf(Kit.None, Kit.Chair, Kit.Wall)
        const val TRY_IT = "try_it_and_see"
        const val EVENING_SET = "strength_in_the_evening"
        const val WEIGHS_IN = "weighs_in"
        const val PACING = "pacing"
        const val ENVELOPE_MINUTES = "envelope_minutes"
        const val ENVELOPE_DAYS = "envelope_days"
        const val ENVELOPE_START = "envelope_start_minutes"
    }
}

/**
 * The Sunday write-up, and everything it is allowed to know.
 *
 * The brief is assembled here, from rows, and handed to [WeekWriter] or to the
 * model as a value. Neither of them can reach past it: the weight arrives as a
 * direction word rather than a number, and the restriction tags are filtered on
 * the way out rather than trusted to be ignored.
 */
class WeekRepository(private val db: SteadyDatabase) {

    /**
     * Assemble the week that starts on [weekStartDay].
     *
     * Returns null when there is nothing at all, so the caller can say nothing
     * rather than say something about a week that did not happen.
     */
    @Suppress("LongParameterList")
    suspend fun brief(
        weekStartDay: Long,
        walkName: String,
        whatTheyWant: String,
        showNumbers: Boolean,
        stepOffered: Boolean,
    ): WeekBrief {
        val last = weekStartDay + DAYS_IN_WEEK - 1
        val sessions = db.sessions().between(weekStartDay, last)
        val counted = sessions.filter { it.durationSeconds >= Ladders.COUNTS_AS_A_SESSION_SECONDS }
        val checkIns = db.checkIns().allOnce().filter { it.epochDay in weekStartDay..last }
        val tagsByDay = tagCounts(weekStartDay, last)

        return WeekBrief(
            daysMoved = counted.map { it.epochDay }.distinct().size,
            minutes = if (showNumbers) counted.sumOf { it.durationSeconds } / SECONDS_PER_MINUTE else null,
            tags = tagsByDay,
            dayRatings = checkIns.mapNotNull { it.dayRating?.let(DayRating::fromId) },
            sleepAverageHours = checkIns.mapNotNull { it.sleepHalfHours }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.div(2),
            walkName = walkName,
            stepOffered = stepOffered,
            talkResults = sessions.mapNotNull { it.talkTest?.let(TalkTest::fromId) },
            weightDirection = direction(weekStartDay, last),
            whatTheyWant = whatTheyWant,
        )
    }

    private suspend fun tagCounts(from: Long, to: Long): List<TagCount> {
        val days = db.checkIns().allOnce()
            .filter { it.epochDay in from..to }
            .associate { it.id to it.epochDay }
        return db.checkIns().allTagsOnce()
            .filter { it.checkInId in days.keys }
            .groupBy { it.tag }
            .map { (tag, rows) -> TagCount(tag, rows.mapNotNull { days[it.checkInId] }.distinct().size) }
            .sortedByDescending { it.days }
    }

    /**
     * Which way the smoothed weight went, as a word.
     *
     * The number never leaves this function. LOGIC.md section 10 is explicit that
     * the model is not passed one, and the simplest way to guarantee that is for
     * the brief to have nowhere to put it.
     */
    private suspend fun direction(from: Long, to: Long): WeightDirection? {
        val readings = db.weighIns().allOnce().filter { it.epochDay in from..to }
        val first = readings.minByOrNull { it.epochDay } ?: return null
        val last = readings.maxByOrNull { it.epochDay } ?: return null
        if (first.epochDay == last.epochDay) return null
        val change = last.smoothedKg - first.smoothedKg
        return when {
            change < -WeightEngine.SAME_BAND_KG -> WeightDirection.ALittleLower
            change > WeightEngine.SAME_BAND_KG -> WeightDirection.ALittleHigher
            else -> WeightDirection.AboutTheSame
        }
    }

    suspend fun saved(weekStartDay: Long): WeekNote? = db.notes().note(weekStartDay)?.let {
        WeekNote(it.paragraphs.split(PARAGRAPH_BREAK).filter(String::isNotBlank), it.byModel)
    }

    suspend fun save(weekStartDay: Long, note: WeekNote, at: Long) {
        db.notes().upsertNote(
            WeeklyNoteEntity(
                weekStartDay = weekStartDay,
                writtenAt = at,
                paragraphs = note.paragraphs.joinToString(PARAGRAPH_BREAK),
                byModel = note.fromModel,
            ),
        )
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val SECONDS_PER_MINUTE = 60
        const val PARAGRAPH_BREAK = "\n\n"
    }
}

/**
 * The monthly check and everything it measured.
 *
 * Results are stored one row per measure per day, never as a check-shaped blob,
 * so a measure taken outside a check (somebody who wanted to try one foot again)
 * sits in the same history as one taken inside it.
 */
class CheckRepository(private val db: SteadyDatabase) {

    suspend fun latest(): CheckEntity? = db.checks().latest()

    suspend fun countSince(day: Long): Int = db.checks().countBetween(day, Long.MAX_VALUE)

    suspend fun results(): List<MeasureResult> = db.checks().allMeasuresOnce()
        .map { MeasureResult(it.measureId, it.epochDay, it.value) }

    /** The most recent value of every measure, for the life sentence. */
    suspend fun latestValues(): Map<String, Double> = db.checks().allMeasuresOnce()
        .groupBy { it.measureId }
        .mapValues { (_, rows) -> rows.maxBy { it.epochDay }.value }

    suspend fun history(measureId: String): List<MeasureResult> =
        db.checks().measureHistory(measureId).map { MeasureResult(it.measureId, it.epochDay, it.value) }

    /**
     * Write one finished check.
     *
     * A measure the person skipped is simply absent. There is no row meaning
     * "did not do", because the app has no use for one and a table of things
     * somebody did not manage is not what this is.
     */
    suspend fun save(
        epochDay: Long,
        at: Long,
        way: GettingAround,
        values: Map<String, Double>,
    ) {
        val checkId = db.checks().upsertCheck(
            CheckEntity(epochDay = epochDay, completedAt = at, gettingAround = way.id),
        )
        values.forEach { (measureId, value) ->
            val measure = Measures.byId(measureId) ?: return@forEach
            db.checks().upsertMeasure(
                MeasureResultEntity(
                    checkId = checkId,
                    measureId = measureId,
                    domain = measure.domain.id,
                    epochDay = epochDay,
                    recordedAt = at,
                    value = value,
                    countedBy = measure.counted.name,
                ),
            )
        }
    }
}

/**
 * Everything the visit summary is allowed to know, gathered from rows.
 *
 * All the selection and arithmetic happen in [VisitSummaryEngine]; this only
 * fetches. The split matters because the engine is where the rules are, and a
 * rule buried in a query is a rule nobody can test.
 */
class VisitRepository(private val db: SteadyDatabase) {

    suspend fun firstDay(): Long? = db.weighIns().allOnce().minOfOrNull { it.epochDay }

    @Suppress("LongParameterList")
    suspend fun inputs(
        window: VisitWindow,
        abilityNames: Map<AbilityDomain, String>,
        measureNames: Map<String, String>,
        stateNames: Map<AbilityState, String>,
    ): VisitInputs {
        val from = window.fromDay
        val to = window.toDay
        val results = db.checks().measuresBetween(from, to)
            .map { MeasureResult(it.measureId, it.epochDay, it.value) }
        val sessions = db.sessions().between(from, to)
        val weighIns = db.weighIns().allOnce().filter { it.epochDay in from..to }

        return VisitInputs(
            window = window,
            weeks = ((to - from) / DAYS_IN_WEEK).toInt(),
            checkCount = db.checks().countBetween(from, to),
            results = results,
            items = items(from, to),
            mentions = mentions(from, to),
            months = months(from, to),
            gaps = gaps(sessions.map { it.epochDay }.distinct().sorted(), from, to),
            currentStep = currentStep(),
            weightFirstKg = weighIns.minByOrNull { it.epochDay }?.smoothedKg,
            weightLastKg = weighIns.maxByOrNull { it.epochDay }?.smoothedKg,
            weighInCount = weighIns.size,
            fastestWeeklyLossKg = fastestLoss(weighIns.map { it.epochDay to it.smoothedKg }),
            abilityNames = abilityNames,
            measureNames = measureNames,
            stateNames = stateNames,
            pacing = ProfileRepository(db).pacing(),
            anyExclusions = db.profile().exclusionIds().isNotEmpty(),
            wayChanged = false,
        )
    }

    private suspend fun items(from: Long, to: Long): List<ItemHistory> =
        db.abilities().itemsOnce().mapNotNull { item ->
            val ratings = db.abilities().ratingsFor(item.id)
                .filter { it.epochDay in from..to }
                .sortedBy { it.epochDay }
            if (ratings.isEmpty()) return@mapNotNull null
            ItemHistory(
                text = item.text,
                firstRating = ratings.first().rating,
                firstDay = ratings.first().epochDay,
                lastRating = ratings.last().rating,
                lastDay = ratings.last().epochDay,
            )
        }

    /**
     * Tags, with up to three of the person's own sentences each.
     *
     * The sentences are the only raw journal text that reaches the brief, and
     * LOGIC.md 13b caps it at three per passed tag for exactly that reason.
     */
    private suspend fun mentions(from: Long, to: Long): List<Mention> {
        val checkIns = db.checkIns().allOnce().filter { it.epochDay in from..to }
        val byId = checkIns.associateBy { it.id }
        return db.checkIns().allTagsOnce()
            .filter { it.checkInId in byId.keys }
            .groupBy { it.tag }
            .map { (tag, rows) ->
                val days = rows.mapNotNull { byId[it.checkInId] }
                Mention(
                    tag = tag,
                    days = days.map { it.epochDay }.distinct().size,
                    recent = days.sortedByDescending { it.epochDay }
                        .map { it.sentence }
                        .filter { it.isNotBlank() }
                        .take(THREE),
                )
            }
    }

    private suspend fun months(from: Long, to: Long): List<MonthOfSessions> =
        db.sessions().between(from, to)
            .filter { it.durationSeconds >= Ladders.COUNTS_AS_A_SESSION_SECONDS }
            .groupBy { LocalDate.ofEpochDay(it.epochDay).month }
            .map { (month, rows) ->
                MonthOfSessions(
                    month = month.name.lowercase().replaceFirstChar { it.uppercase() },
                    daysMoved = rows.map { it.epochDay }.distinct().size,
                    minutes = rows.sumOf { it.durationSeconds } / SECONDS_PER_MINUTE,
                )
            }

    /** Every stretch with no session in it, from the days that had one. */
    private fun gaps(days: List<Long>, from: Long, to: Long): List<Gap> {
        if (days.isEmpty()) return listOf(Gap(from, to))
        return (listOf(from) + days + listOf(to))
            .zipWithNext { a, b -> Gap(a, b) }
            .filter { it.days >= VisitSummaryEngine.GAP_DAYS }
    }

    private suspend fun currentStep(): String {
        val way = ProfileRepository(db).gettingAround()
        val ladder = when (way) {
            GettingAround.OnFeet, GettingAround.Walker -> Ladder.Walking
            GettingAround.Wheelchair -> Ladder.Wheeling
            GettingAround.InBed -> Ladder.InBed
        }
        val index = db.ladders().state(ladder.id)?.currentStepIndex ?: 0
        val steps = when (ladder) {
            Ladder.Walking -> Ladders.walking
            Ladder.Wheeling -> Ladders.wheeling
            else -> Ladders.inBed
        }
        return db.ladders().nameFor(ladder.id, index) ?: steps.getOrNull(index)?.name.orEmpty()
    }

    /**
     * The fastest four-week stretch of losing, in kilos a week.
     *
     * One of the seven question-candidate rules turns on it. Computed here rather
     * than in the engine only because it needs the whole series; the threshold it
     * is compared against lives in the engine with the other six.
     */
    private fun fastestLoss(series: List<Pair<Long, Double>>): Double? {
        if (series.size < 2) return null
        val sorted = series.sortedBy { it.first }
        return sorted.indices.mapNotNull { i ->
            val later = sorted.lastOrNull { it.first <= sorted[i].first + FOUR_WEEKS }
            if (later == null || later.first == sorted[i].first) return@mapNotNull null
            val weeks = (later.first - sorted[i].first) / DAYS_IN_WEEK.toDouble()
            if (weeks < 1) null else (sorted[i].second - later.second) / weeks
        }.maxOrNull()
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val SECONDS_PER_MINUTE = 60
        const val THREE = 3
        const val FOUR_WEEKS = 28L
    }
}

/**
 * Everything out, and everything gone.
 *
 * PRIVACY.md promises both in those words: ordinary files anyone can open, and
 * deletion that is immediate and complete with no copy anywhere else. Both
 * promises are kept here, and the second one is the harder of the two to keep
 * honestly.
 */
class DataRepository(private val db: SteadyDatabase) {

    /** Every table, as a spreadsheet somebody can open in anything. */
    suspend fun sheets(): List<Sheet> = listOf(
        Sheet(
            name = "weigh-ins",
            rows = listOf(listOf("date", "weight_kg", "smoothed_kg", "source")) +
                db.weighIns().allOnce().map {
                    listOf(
                        date(it.epochDay),
                        it.rawKg.toString(),
                        it.smoothedKg.toString(),
                        it.source,
                    )
                },
        ),
        Sheet(
            name = "days",
            rows = listOf(listOf("date", "what_you_said", "sleep_hours", "how_it_went", "tags")) +
                daysWithTags(),
        ),
        Sheet(
            name = "sessions",
            rows = listOf(listOf("date", "kind", "step", "minutes", "could_you_talk")) +
                db.sessions().allOnce().map {
                    listOf(
                        date(it.epochDay),
                        it.ladder,
                        it.stepIndex.toString(),
                        (it.durationSeconds / SECONDS_PER_MINUTE).toString(),
                        it.talkTest.orEmpty(),
                    )
                },
        ),
        Sheet(
            name = "checks",
            rows = listOf(listOf("date", "measure", "ability", "value")) +
                db.checks().allMeasuresOnce().map {
                    listOf(date(it.epochDay), it.measureId, it.domain, it.value.toString())
                },
        ),
        Sheet(
            name = "your-list",
            rows = listOf(listOf("date", "what_you_wanted", "ability", "rating")) +
                itemRatings(),
        ),
    )

    private suspend fun daysWithTags(): List<List<String>> {
        val tags = db.checkIns().allTagsOnce().groupBy { it.checkInId }
        return db.checkIns().allOnce().map { day ->
            listOf(
                date(day.epochDay),
                day.sentence,
                day.sleepHalfHours?.let { (it / 2.0).toString() }.orEmpty(),
                day.dayRating.orEmpty(),
                tags[day.id].orEmpty().joinToString(" ") { it.tag },
            )
        }
    }

    private suspend fun itemRatings(): List<List<String>> =
        db.abilities().itemsOnce().flatMap { item ->
            db.abilities().ratingsFor(item.id).map { rating ->
                listOf(date(rating.epochDay), item.text, item.domain, rating.rating.toString())
            }
        }

    /**
     * Delete everything, immediately.
     *
     * Every table, then the database file and its key. PRIVACY.md says there is no
     * copy anywhere else to delete, and that sentence is only true if this leaves
     * nothing: not an empty database, not an unused key in the Keystore, not a
     * stray write-ahead log.
     */
    suspend fun deleteEverything(context: android.content.Context) {
        db.weighIns().deleteAll()
        db.checkIns().deleteAll()
        db.checkIns().deleteAllTags()
        db.synonyms().deleteAll()
        db.sessions().deleteAll()
        db.ladders().deleteAllStates()
        db.ladders().deleteAllNames()
        db.abilities().deleteAllItems()
        db.abilities().deleteAllRatings()
        db.checks().deleteAllChecks()
        db.checks().deleteAllMeasures()
        db.runs().deleteAllRuns()
        db.runs().deleteAllRunMovements()
        db.runs().deleteAllSore()
        db.notes().deleteAllNotes()
        db.notes().deleteAllPatterns()
        db.notices().deleteAll()
        db.profile().deleteAllSettings()
        db.profile().clearExclusions()
        db.profile().clearReadiness()
        SteadyDatabase.destroy(context)
    }

    private fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).toString()

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}

/** What has been sent, so the ceiling can be counted honestly. */
class ReminderRepository(private val db: SteadyDatabase) {

    suspend fun sentSince(millis: Long): List<Long> = db.reminders().since(millis).map { it.sentAt }

    suspend fun record(kind: ReminderKind, at: Long) {
        db.reminders().record(ReminderSentEntity(type = kind.id, sentAt = at))
    }

    /** How many of the two are left, for the line settings shows. */
    suspend fun leftThisWeek(now: Long): Int =
        Reminders.leftThisWeek(sentSince(now - A_WEEK), now)

    private companion object {
        const val A_WEEK = 7L * 24 * 60 * 60 * 1000
    }
}

/**
 * Try it and see: the pattern that earns an offer, and the one test at a time.
 *
 * One at a time is a rule rather than a simplification. Two overlapping tests
 * cannot be read, because whatever changed could have been either of them, and an
 * app that ran both and reported on one would be making something up.
 */
class ExperimentRepository(private val db: SteadyDatabase) {

    /** The running test, or null. Ended ones are kept and are not running. */
    suspend fun running(): Experiment? = db.notes().experimentsOnce()
        .lastOrNull { it.stoppedAt == null }
        ?.let {
            Experiment(
                variable = Variable.entries.firstOrNull { v -> v.id == it.variable }
                    ?: Variable.TimeOfDay,
                conditionA = it.resultHeadline.orEmpty(),
                conditionB = it.resultDetail.orEmpty(),
                measureId = it.measureId,
                startedOnDay = it.startedOnDay,
            )
        }

    suspend fun start(experiment: Experiment) {
        db.notes().upsertExperiment(
            ExperimentEntity(
                variable = experiment.variable.id,
                measureId = experiment.measureId,
                startedOnDay = experiment.startedOnDay,
                switchOnDay = experiment.startedOnDay + ARM_DAYS,
                endsOnDay = experiment.startedOnDay + 2 * ARM_DAYS,
                stoppedAt = null,
                // The two conditions, in the person's own words on the offer.
                resultHeadline = experiment.conditionA,
                resultDetail = experiment.conditionB,
            ),
        )
    }

    /** Ended, whichever way it went. Nothing anywhere records it as a failure. */
    suspend fun finish(keep: Boolean) {
        val row = db.notes().experimentsOnce().lastOrNull { it.stoppedAt == null } ?: return
        db.notes().upsertExperiment(row.copy(stoppedAt = System.currentTimeMillis()))
        if (!keep) ProfileRepository(db).setStrengthInTheEvening(true)
    }

    suspend fun results(): List<MeasureResult> = db.checks().allMeasuresOnce()
        .map { MeasureResult(it.measureId, it.epochDay, it.value) }

    suspend fun declineUntil(day: Long) = db.profile().put(SettingEntity(DECLINED, day.toString()))

    /**
     * A pattern worth offering a test about, or null.
     *
     * Everything the offer needs is checked here: six weeks of check-ins, the
     * feature on, not in pacing mode, nothing already running, and not inside a
     * fortnight of somebody saying not now.
     */
    suspend fun offerablePattern(today: Long): Pattern? {
        val profile = ProfileRepository(db)
        if (!ExperimentEngine.mayOffer(weeksOfCheckIns(), profile.pacing(), profile.tryItAndSee())) {
            return null
        }
        if (running() != null) return null
        val declined = db.profile().get(DECLINED)?.toLongOrNull()
        if (declined != null && today < declined) return null

        // Weight is never the thing a test is offered about, so patterns about it
        // are computed and kept for the summary and are not offered here.
        return Patterns.find(weeks()).firstOrNull { it.measure != PatternMeasure.WeightDirection }
    }

    private suspend fun weeksOfCheckIns(): Int {
        val days = db.checkIns().allOnce().map { it.epochDay }
        val first = days.minOrNull() ?: return 0
        val last = days.maxOrNull() ?: return 0
        return ((last - first) / DAYS_IN_WEEK).toInt() + 1
    }

    /** Every week of check-ins, reduced to the three things a pattern turns on. */
    private suspend fun weeks(): List<WeekOfDays> {
        val checkIns = db.checkIns().allOnce()
        if (checkIns.isEmpty()) return emptyList()
        val tags = db.checkIns().allTagsOnce().groupBy { it.checkInId }
        val sessions = db.sessions().allOnce()
        val weighIns = db.weighIns().allOnce()
        val first = checkIns.minOf { it.epochDay }

        return checkIns
            .groupBy { (it.epochDay - first) / DAYS_IN_WEEK }
            .map { (week, days) ->
                val start = first + week * DAYS_IN_WEEK
                val range = start until start + DAYS_IN_WEEK
                val onThreeDays = days
                    .flatMap { day -> tags[day.id].orEmpty().map { it.tag to day.epochDay } }
                    .groupBy({ it.first }, { it.second })
                    .filterValues { it.distinct().size >= Patterns.DAYS_IN_A_WEEK_WITH }
                    .keys
                val inWeek = weighIns.filter { it.epochDay in range }.sortedBy { it.epochDay }
                WeekOfDays(
                    weekStartDay = start,
                    tagsOnThreeDays = onThreeDays,
                    sleepAverageHours = days.mapNotNull { it.sleepHalfHours }
                        .takeIf { it.isNotEmpty() }
                        ?.average()
                        ?.div(2),
                    daysMoved = sessions
                        .filter { it.epochDay in range }
                        .filter { it.durationSeconds >= Ladders.COUNTS_AS_A_SESSION_SECONDS }
                        .map { it.epochDay }
                        .distinct()
                        .size,
                    weightChangeKg = if (inWeek.size >= 2) {
                        inWeek.last().smoothedKg - inWeek.first().smoothedKg
                    } else {
                        null
                    },
                )
            }
    }

    private companion object {
        const val ARM_DAYS = Experiment.WEEKS_PER_ARM * Experiment.DAYS_IN_WEEK
        const val DAYS_IN_WEEK = 7
        const val DECLINED = "try_declined_until"
    }
}

/**
 * Sessions: what was planned, what was done, and what hurts.
 *
 * The three things the session engine needs back from storage, in the shapes it
 * already speaks: [Done] rows for its history, and the areas somebody said hurt with
 * their seven days still running.
 */
/**
 * Documents somebody photographed, and the plans pulled out of them.
 *
 * ADDENDUM-03 Parts 5, 6 and 7. Kept together because a plan usually comes from a
 * document and the two are read back beside each other, and because both are the
 * same promise: the photograph is always kept, always viewable, and goes wherever
 * export and delete go.
 */
class DocumentRepository(private val db: SteadyDatabase) {

    /** Save one photographed document with its pages, in the order they were taken. */
    suspend fun save(
        epochDay: Long,
        kind: String,
        fromWho: String,
        pages: List<Pair<ByteArray, String>>,
        at: Long,
    ): Long {
        val id = db.documents().put(
            DocumentEntity(epochDay = epochDay, kind = kind, fromWho = fromWho, savedAt = at),
        )
        pages.forEachIndexed { index, (image, text) ->
            db.documents().putPage(
                DocumentPageEntity(documentId = id, at = index, image = image, text = text),
            )
        }
        return id
    }

    suspend fun all(): List<DocumentEntity> = db.documents().all()

    suspend fun pagesOf(id: Long): List<DocumentPageEntity> = db.documents().pagesOf(id)

    /** Everything read off one document, in page order, as one piece of text. */
    suspend fun textOf(id: Long): String =
        db.documents().pagesOf(id).joinToString("\n") { it.text }

    /**
     * Take one document away entirely, pages and all.
     *
     * The pages go first, so a crash between the two leaves orphaned pages rather
     * than a document whose pages have vanished. Orphans are invisible and harmless;
     * a document that cannot show what it is is not.
     */
    suspend fun remove(id: Long) {
        db.documents().deletePages(id)
        db.documents().delete(id)
    }
}

/**
 * A programme somebody was given, exactly as they were given it.
 *
 * ADDENDUM-03 Part 6. There is deliberately no method here that changes a number: the
 * app runs a plan as given, and reps and frequency change only when the person changes
 * them. Adding a `progress` to this class would be adding one to the app.
 */
class PlanRepository(private val db: SteadyDatabase) {

    suspend fun save(label: String, at: Long, reviewDay: Long? = null): Long =
        db.plans().put(PlanEntity(label = label, createdAt = at, reviewDay = reviewDay))

    suspend fun addItem(planId: Long, item: PlanItemEntity) =
        db.plans().putItem(item.copy(planId = planId))

    suspend fun live(): List<PlanEntity> = db.plans().live()

    suspend fun itemsOf(planId: Long): List<PlanItemEntity> = db.plans().itemsOf(planId)

    /** Every item of every live plan, which is what a session is built from. */
    suspend fun liveItems(): List<PlanItemEntity> =
        db.plans().live().flatMap { db.plans().itemsOf(it.id) }

    suspend fun setReviewDay(planId: Long, day: Long?) {
        val plan = db.plans().all().firstOrNull { it.id == planId } ?: return
        db.plans().put(plan.copy(reviewDay = day))
    }

    /** The soonest appointment across every live plan, for the one prompt about it. */
    suspend fun nextReviewDay(): Long? = db.plans().live().mapNotNull { it.reviewDay }.minOrNull()

    /** Put a plan away without deleting what was done from it. */
    suspend fun archive(planId: Long, at: Long) {
        val plan = db.plans().all().firstOrNull { it.id == planId } ?: return
        db.plans().put(plan.copy(archivedAt = at))
    }

    suspend fun remove(planId: Long) {
        db.plans().itemsOf(planId).forEach { db.plans().deleteItem(it) }
        db.plans().delete(planId)
    }
}

/**
 * The daily prompt's own history: one row a day, and whether it was opened.
 *
 * Separate from the reminders table because the two answer different questions. That
 * one is a ceiling on how much the app may say; this one is how the app knows to stop
 * saying it.
 */
class DailyPromptRepository(private val db: SteadyDatabase) {

    suspend fun history(): List<Prompted> =
        db.dailyPrompts().all().map { Prompted(it.epochDay, it.opened) }

    suspend fun sent(day: Long) = db.dailyPrompts().put(DailyPromptEntity(day, opened = false))

    /** Somebody tapped it. That clears the run of dismissals on its own. */
    suspend fun opened(day: Long) = db.dailyPrompts().put(DailyPromptEntity(day, opened = true))

    /** Turning it back on starts again from nothing, which is what "on" means. */
    suspend fun forget() = db.dailyPrompts().clear()
}

class RunRepository(private val db: SteadyDatabase) {

    /** Save one session, however it ended. Nothing here can lose what was done. */
    suspend fun save(
        epochDay: Long,
        startedAt: Long,
        endedAt: Long,
        ending: String,
        felt: Felt?,
        small: Boolean,
        results: List<Result>,
    ): Long {
        val runId = db.runs().upsertRun(
            RunEntity(
                epochDay = epochDay,
                startedAt = startedAt,
                endedAt = endedAt,
                ending = ending,
                felt = felt?.id,
                small = small,
            ),
        )
        results.forEach { result ->
            db.runs().upsertMovement(
                RunMovementEntity(
                    runId = runId,
                    movementId = result.movementId,
                    target = result.target,
                    count = result.count,
                    selfReported = result.selfReported,
                    madeEasier = result.madeEasier,
                    skipped = result.skipped,
                ),
            )
        }
        return runId
    }

    /**
     * Change what one movement in one session is recorded as.
     *
     * ADDENDUM-03 Part 14. The number becomes self reported, because it is: somebody
     * told the app what happened. Nothing anywhere treats that as worth less, and
     * nothing anywhere records that it was changed.
     */
    suspend fun correct(runId: Long, movementId: String, count: Int) {
        val row = db.runs().movementsFor(runId).firstOrNull { it.movementId == movementId } ?: return
        db.runs().upsertMovement(
            row.copy(count = count.coerceAtLeast(0), selfReported = true, skipped = false),
        )
    }

    /**
     * Remove one session and everything in it.
     *
     * ADDENDUM-03 Part 14: any past session is deletable. It goes entirely, rather
     * than being marked as removed, because a row that means "somebody deleted this"
     * is still a record of them and they asked for it not to be there.
     */
    suspend fun remove(runId: Long) {
        db.runs().movementsFor(runId).forEach { db.runs().deleteMovement(it) }
        db.runs().deleteRun(runId)
    }

    /** One session, with its movements, for the screen that edits it. */
    suspend fun session(runId: Long): Pair<RunEntity, List<RunMovementEntity>>? =
        db.runs().allOnce().firstOrNull { it.id == runId }?.let { it to db.runs().movementsFor(runId) }

    /** The movements of one session, in the order they were done. */
    suspend fun movementsOf(runId: Long): List<String> =
        db.runs().movementsFor(runId).filterNot { it.skipped }.map { it.movementId }

    /** Every session, newest first, with its movements, for the history list. */
    suspend fun sessions(): List<Pair<RunEntity, List<RunMovementEntity>>> {
        val movements = db.runs().allMovementsOnce().groupBy { it.runId }
        return db.runs().allOnce()
            .sortedByDescending { it.epochDay }
            .map { run -> run to movements[run.id].orEmpty() }
    }

    /** Everything done, in the shape the engine plans from. Skipped rows are not history. */
    suspend fun history(): List<Done> {
        val days = db.runs().allOnce().associate { it.id to it.epochDay }
        return db.runs().allMovementsOnce()
            .filterNot { it.skipped }
            .mapNotNull { row ->
                days[row.runId]?.let {
                    Done(
                        movementId = row.movementId,
                        epochDay = it,
                        result = row.count,
                        target = row.target,
                        selfReported = row.selfReported,
                    )
                }
            }
    }

    /** The answer to the one question, written after the session row exists. */
    suspend fun saveFelt(felt: Felt) {
        val latest = db.runs().latest() ?: return
        db.runs().upsertRun(latest.copy(felt = felt.id))
    }

    /** How many times one movement has been done, for the pacing cue. */
    suspend fun timesDone(movementId: String): Int =
        db.runs().allMovementsOnce().count { it.movementId == movementId && !it.skipped }

    suspend fun lastFelt(): Felt? = db.runs().latest()?.felt?.let { id ->
        Felt.entries.firstOrNull { it.id == id }
    }

    /** The one before last, for the two-easy rule. */
    suspend fun feltBefore(): Felt? = db.runs().allOnce()
        .dropLast(1)
        .lastOrNull()
        ?.felt
        ?.let { id -> Felt.entries.firstOrNull { it.id == id } }

    suspend fun lastSessionDay(): Long? = db.runs().latest()?.epochDay

    suspend fun daysThisWeek(mondayEpochDay: Long): Int =
        db.runs().between(mondayEpochDay, mondayEpochDay + DAYS_IN_WEEK - 1)
            .map { it.epochDay }
            .distinct()
            .size

    /**
     * How many strength sessions in a row, for the easy-day rule.
     *
     * A session counts as strength when at least one of its movements was one, which
     * is every ordinary session and no easy day.
     */
    suspend fun strengthRunLength(): Int {
        val byRun = db.runs().allMovementsOnce().groupBy { it.runId }
        var run = 0
        db.runs().allOnce().reversed().forEach { session ->
            val strength = byRun[session.id].orEmpty().any { row ->
                Movements.byId(row.movementId)?.piece == Piece.Main
            }
            if (!strength) return run
            run += 1
        }
        return run
    }

    /** Areas still inside their seven days. ADDENDUM-03 Part 2. */
    suspend fun soreAreas(today: Long): Set<Area> = db.runs().soreOnce()
        .filter { today - it.reportedOnDay < SORE_DAYS }
        .mapNotNull { row -> Area.entries.firstOrNull { it.id == row.area } }
        .toSet()

    /** An area whose week is up, so the app can ask once whether to bring it back. */
    suspend fun soreAreaToAskAbout(today: Long): Area? = db.runs().soreOnce()
        .firstOrNull { today - it.reportedOnDay >= SORE_DAYS }
        ?.let { row -> Area.entries.firstOrNull { it.id == row.area } }

    suspend fun reportSore(area: Area, today: Long) {
        db.runs().upsertSore(SoreAreaEntity(area = area.id, reportedOnDay = today))
    }

    suspend fun clearSore(area: Area, today: Long) {
        db.runs().soreOnce()
            .filter { it.area == area.id }
            .forEach { db.runs().upsertSore(it.copy(clearedOnDay = today)) }
    }

    /**
     * True when the same area has been reported twice inside a month.
     *
     * The one line the app says about it, once, and never again: "Worth a word with
     * your doctor about that shoulder." No interpretation and no advice.
     */
    suspend fun reportedTwiceInAMonth(area: Area, today: Long): Boolean =
        db.runs().allSoreOnce()
            .filter { it.area == area.id && today - it.reportedOnDay <= A_MONTH }
            .size >= 2

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val SORE_DAYS = 7
        const val A_MONTH = 30
    }
}
