package com.kamsiob.steadyhealth.data

import com.kamsiob.steadyhealth.data.entity.CheckInEntity
import com.kamsiob.steadyhealth.data.entity.ExclusionEntity
import com.kamsiob.steadyhealth.data.entity.ItemRatingEntity
import com.kamsiob.steadyhealth.data.entity.LadderStateEntity
import com.kamsiob.steadyhealth.data.entity.NoticeEntity
import com.kamsiob.steadyhealth.data.entity.ReadinessEntity
import com.kamsiob.steadyhealth.data.entity.SessionEntity
import com.kamsiob.steadyhealth.data.entity.SettingEntity
import com.kamsiob.steadyhealth.data.entity.StepNameEntity
import com.kamsiob.steadyhealth.data.entity.TrackedItemEntity
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
    }
}
