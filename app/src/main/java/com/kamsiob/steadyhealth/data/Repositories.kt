package com.kamsiob.steadyhealth.data

import com.kamsiob.steadyhealth.ai.Synonym
import com.kamsiob.steadyhealth.ai.TagCount
import com.kamsiob.steadyhealth.ai.TagReader
import com.kamsiob.steadyhealth.ai.WeekBrief
import com.kamsiob.steadyhealth.ai.WeekNote
import com.kamsiob.steadyhealth.ai.WeightDirection
import com.kamsiob.steadyhealth.data.entity.CheckEntity
import com.kamsiob.steadyhealth.data.entity.CheckInEntity
import com.kamsiob.steadyhealth.data.entity.CheckInTagEntity
import com.kamsiob.steadyhealth.data.entity.ExclusionEntity
import com.kamsiob.steadyhealth.data.entity.ItemRatingEntity
import com.kamsiob.steadyhealth.data.entity.LadderStateEntity
import com.kamsiob.steadyhealth.data.entity.MeasureResultEntity
import com.kamsiob.steadyhealth.data.entity.NoticeEntity
import com.kamsiob.steadyhealth.data.entity.PersonSynonymEntity
import com.kamsiob.steadyhealth.data.entity.ReadinessEntity
import com.kamsiob.steadyhealth.data.entity.SessionEntity
import com.kamsiob.steadyhealth.data.entity.SettingEntity
import com.kamsiob.steadyhealth.data.entity.StepNameEntity
import com.kamsiob.steadyhealth.data.entity.TrackedItemEntity
import com.kamsiob.steadyhealth.data.entity.WeeklyNoteEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity
import com.kamsiob.steadyhealth.domain.AbilityDomain
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
import com.kamsiob.steadyhealth.engine.Ladders
import com.kamsiob.steadyhealth.engine.MeasureResult
import com.kamsiob.steadyhealth.engine.Measures
import com.kamsiob.steadyhealth.engine.PacingEngine
import com.kamsiob.steadyhealth.engine.Reading
import com.kamsiob.steadyhealth.engine.Smoothed
import com.kamsiob.steadyhealth.engine.WeightEngine

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

    suspend fun onboardingComplete(): Boolean = get(ONBOARDED).toBoolean()

    suspend fun setOnboardingComplete() = put(ONBOARDED, true.toString())

    suspend fun gettingAround(): GettingAround =
        get(GETTING_AROUND)?.let(GettingAround::fromId) ?: GettingAround.OnFeet

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

    companion object {
        const val ONBOARDED = "onboarding_complete"
        const val GETTING_AROUND = "getting_around"
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
