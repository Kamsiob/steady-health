package com.kamsiob.steadyhealth.data.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.kamsiob.steadyhealth.data.entity.BloodPressureEntity
import com.kamsiob.steadyhealth.data.entity.CheckEntity
import com.kamsiob.steadyhealth.data.entity.CheckInEntity
import com.kamsiob.steadyhealth.data.entity.CheckInTagEntity
import com.kamsiob.steadyhealth.data.entity.DailyPromptEntity
import com.kamsiob.steadyhealth.data.entity.ExclusionEntity
import com.kamsiob.steadyhealth.data.entity.ExperimentEntity
import com.kamsiob.steadyhealth.data.entity.ItemRatingEntity
import com.kamsiob.steadyhealth.data.entity.LadderStateEntity
import com.kamsiob.steadyhealth.data.entity.MeasureResultEntity
import com.kamsiob.steadyhealth.data.entity.NoticeEntity
import com.kamsiob.steadyhealth.data.entity.PatternEntity
import com.kamsiob.steadyhealth.data.entity.PersonSynonymEntity
import com.kamsiob.steadyhealth.data.entity.PhotoEntity
import com.kamsiob.steadyhealth.data.entity.ReadinessEntity
import com.kamsiob.steadyhealth.data.entity.ReminderSentEntity
import com.kamsiob.steadyhealth.data.entity.RunEntity
import com.kamsiob.steadyhealth.data.entity.RunMovementEntity
import com.kamsiob.steadyhealth.data.entity.SessionEntity
import com.kamsiob.steadyhealth.data.entity.SettingEntity
import com.kamsiob.steadyhealth.data.entity.SoreAreaEntity
import com.kamsiob.steadyhealth.data.entity.StepNameEntity
import com.kamsiob.steadyhealth.data.entity.TrackedItemEntity
import com.kamsiob.steadyhealth.data.entity.VisitSummaryEntity
import com.kamsiob.steadyhealth.data.entity.WaistEntity
import com.kamsiob.steadyhealth.data.entity.WeeklyNoteEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity
import kotlinx.coroutines.flow.Flow

/**
 * Every query the app makes.
 *
 * Two conventions throughout. A `Flow` is what a screen observes and a `once`
 * suffix is what the export and the engine read, because a Flow inside a
 * calculation is a subscription nobody closes. And `deleteAll` exists on every
 * one of them, because "delete everything, immediately, and there is no other
 * copy" is a promise the app makes on a screen and has to be able to keep.
 */

@Dao
interface WeighInDao {
    @Upsert
    suspend fun upsert(weighIn: WeighInEntity)

    @Query("SELECT * FROM weigh_ins ORDER BY epochDay")
    fun all(): Flow<List<WeighInEntity>>

    @Query("SELECT * FROM weigh_ins ORDER BY epochDay")
    suspend fun allOnce(): List<WeighInEntity>

    @Query("SELECT * FROM weigh_ins WHERE epochDay = :epochDay")
    suspend fun forDay(epochDay: Long): WeighInEntity?

    @Query("SELECT * FROM weigh_ins ORDER BY epochDay DESC LIMIT 1")
    suspend fun latest(): WeighInEntity?

    @Query("SELECT * FROM weigh_ins WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    suspend fun between(from: Long, to: Long): List<WeighInEntity>

    @Query("DELETE FROM weigh_ins")
    suspend fun deleteAll()
}

@Dao
interface CheckInDao {
    @Upsert
    suspend fun upsert(checkIn: CheckInEntity): Long

    @Query("SELECT * FROM check_ins ORDER BY epochDay")
    suspend fun allOnce(): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE epochDay = :epochDay")
    suspend fun forDay(epochDay: Long): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    suspend fun between(from: Long, to: Long): List<CheckInEntity>

    @Query("SELECT COUNT(*) FROM check_ins")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<CheckInTagEntity>)

    @Query("DELETE FROM check_in_tags WHERE checkInId = :checkInId")
    suspend fun clearTags(checkInId: Long)

    @Query("SELECT * FROM check_in_tags WHERE checkInId = :checkInId")
    suspend fun tagsFor(checkInId: Long): List<CheckInTagEntity>

    @Query("SELECT * FROM check_in_tags")
    suspend fun allTagsOnce(): List<CheckInTagEntity>

    @Query("DELETE FROM check_ins")
    suspend fun deleteAll()

    @Query("DELETE FROM check_in_tags")
    suspend fun deleteAllTags()
}

@Dao
interface SynonymDao {
    @Upsert
    suspend fun upsert(synonym: PersonSynonymEntity)

    @Query("SELECT * FROM person_synonyms ORDER BY learnedAt")
    suspend fun all(): List<PersonSynonymEntity>

    @Query("DELETE FROM person_synonyms")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY startedAt")
    suspend fun allOnce(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE epochDay = :epochDay ORDER BY startedAt")
    suspend fun forDay(epochDay: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    suspend fun between(from: Long, to: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC LIMIT 1")
    suspend fun latest(): SessionEntity?

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

@Dao
interface LadderDao {
    @Upsert
    suspend fun upsertName(name: StepNameEntity)

    @Query("SELECT * FROM step_names")
    suspend fun namesOnce(): List<StepNameEntity>

    @Query("SELECT name FROM step_names WHERE ladder = :ladder AND stepIndex = :stepIndex")
    suspend fun nameFor(ladder: String, stepIndex: Int): String?

    @Upsert
    suspend fun upsertState(state: LadderStateEntity)

    @Query("SELECT * FROM ladder_state")
    suspend fun statesOnce(): List<LadderStateEntity>

    @Query("SELECT * FROM ladder_state WHERE ladder = :ladder")
    suspend fun state(ladder: String): LadderStateEntity?

    @Query("DELETE FROM step_names")
    suspend fun deleteAllNames()

    @Query("DELETE FROM ladder_state")
    suspend fun deleteAllStates()
}

@Dao
interface AbilityDao {
    @Upsert
    suspend fun upsertItem(item: TrackedItemEntity): Long

    @Query("SELECT * FROM tracked_items WHERE archivedAt IS NULL ORDER BY createdAt")
    suspend fun itemsOnce(): List<TrackedItemEntity>

    @Query("SELECT * FROM tracked_items ORDER BY createdAt")
    suspend fun allItemsOnce(): List<TrackedItemEntity>

    @Query("SELECT * FROM tracked_items WHERE archivedAt IS NULL ORDER BY createdAt")
    fun items(): Flow<List<TrackedItemEntity>>

    @Upsert
    suspend fun upsertRating(rating: ItemRatingEntity)

    @Query("SELECT * FROM item_ratings WHERE itemId = :itemId ORDER BY epochDay")
    suspend fun ratingsFor(itemId: Long): List<ItemRatingEntity>

    @Query("SELECT * FROM item_ratings ORDER BY epochDay")
    suspend fun allRatingsOnce(): List<ItemRatingEntity>

    @Query("DELETE FROM tracked_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM item_ratings")
    suspend fun deleteAllRatings()
}

@Dao
interface CheckDao {
    @Upsert
    suspend fun upsertCheck(check: CheckEntity): Long

    @Query("SELECT * FROM checks ORDER BY epochDay")
    suspend fun allOnce(): List<CheckEntity>

    @Query("SELECT * FROM checks ORDER BY epochDay DESC LIMIT 1")
    suspend fun latest(): CheckEntity?

    @Query("SELECT COUNT(*) FROM checks WHERE epochDay BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Upsert
    suspend fun upsertMeasure(result: MeasureResultEntity)

    @Query("SELECT * FROM measure_results ORDER BY epochDay")
    suspend fun allMeasuresOnce(): List<MeasureResultEntity>

    @Query("SELECT * FROM measure_results WHERE measureId = :measureId ORDER BY epochDay")
    suspend fun measureHistory(measureId: String): List<MeasureResultEntity>

    @Query("SELECT * FROM measure_results WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    suspend fun measuresBetween(from: Long, to: Long): List<MeasureResultEntity>

    @Query("DELETE FROM checks")
    suspend fun deleteAllChecks()

    @Query("DELETE FROM measure_results")
    suspend fun deleteAllMeasures()
}

@Dao
interface BodyDao {
    @Upsert
    suspend fun upsertWaist(waist: WaistEntity)

    @Query("SELECT * FROM waist ORDER BY epochDay")
    suspend fun allWaist(): List<WaistEntity>

    @Upsert
    suspend fun upsertBloodPressure(reading: BloodPressureEntity)

    @Query("SELECT * FROM blood_pressure ORDER BY epochDay")
    suspend fun allBloodPressure(): List<BloodPressureEntity>

    @Upsert
    suspend fun upsertPhoto(photo: PhotoEntity)

    @Query("SELECT * FROM photos ORDER BY epochDay")
    suspend fun photosOnce(): List<PhotoEntity>

    @Query("DELETE FROM waist")
    suspend fun deleteAllWaist()

    @Query("DELETE FROM blood_pressure")
    suspend fun deleteAllBloodPressure()

    @Query("DELETE FROM photos")
    suspend fun deleteAllPhotos()
}

@Dao
interface NotesDao {
    @Upsert
    suspend fun upsertNote(note: WeeklyNoteEntity)

    @Query("SELECT * FROM weekly_notes ORDER BY weekStartDay")
    suspend fun notesOnce(): List<WeeklyNoteEntity>

    @Query("SELECT * FROM weekly_notes WHERE weekStartDay = :weekStartDay")
    suspend fun note(weekStartDay: Long): WeeklyNoteEntity?

    @Upsert
    suspend fun upsertPattern(pattern: PatternEntity)

    @Query("SELECT * FROM patterns ORDER BY foundOnDay")
    suspend fun patternsOnce(): List<PatternEntity>

    @Upsert
    suspend fun upsertExperiment(experiment: ExperimentEntity): Long

    @Query("SELECT * FROM experiments ORDER BY startedOnDay")
    suspend fun experimentsOnce(): List<ExperimentEntity>

    @Upsert
    suspend fun upsertSummary(summary: VisitSummaryEntity): Long

    @Query("SELECT * FROM visit_summaries ORDER BY generatedAt DESC LIMIT 1")
    suspend fun latestSummary(): VisitSummaryEntity?

    @Query("SELECT * FROM visit_summaries ORDER BY generatedAt")
    suspend fun summariesOnce(): List<VisitSummaryEntity>

    @Query("DELETE FROM weekly_notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM patterns")
    suspend fun deleteAllPatterns()

    @Query("DELETE FROM experiments")
    suspend fun deleteAllExperiments()

    @Query("DELETE FROM visit_summaries")
    suspend fun deleteAllSummaries()
}

@Dao
interface NoticeDao {
    @Query("SELECT * FROM notices WHERE noticeId = :id LIMIT 1")
    suspend fun get(id: String): NoticeEntity?

    @Upsert
    suspend fun put(notice: NoticeEntity)

    @Query("SELECT * FROM notices")
    suspend fun all(): List<NoticeEntity>

    @Query("DELETE FROM notices")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Upsert
    suspend fun record(reminder: ReminderSentEntity)

    /**
     * The rolling seven-day window, inclusive of the boundary.
     *
     * LOGIC.md section 12 is explicit that a reminder sent exactly seven days ago
     * still counts, and that any query counting them must use the same boundary,
     * because an exclusive one silently turns the ceiling into three on one day a
     * week. Hence `>=` and not `>`.
     */
    @Query("SELECT COUNT(*) FROM reminders_sent WHERE sentAt >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT * FROM reminders_sent WHERE sentAt >= :since ORDER BY sentAt")
    suspend fun since(since: Long): List<ReminderSentEntity>

    @Query("DELETE FROM reminders_sent")
    suspend fun deleteAll()
}

@Dao
interface ProfileDao {
    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM settings WHERE key = :key")
    fun watch(key: String): Flow<String?>

    @Upsert
    suspend fun put(setting: SettingEntity)

    @Query("SELECT * FROM settings")
    suspend fun allOnce(): List<SettingEntity>

    @Query("SELECT exclusion FROM exclusions")
    suspend fun exclusionIds(): List<String>

    @Query("SELECT * FROM exclusions")
    suspend fun exclusionsOnce(): List<ExclusionEntity>

    @Upsert
    suspend fun putExclusion(exclusion: ExclusionEntity)

    @Query("DELETE FROM exclusions")
    suspend fun clearExclusions()

    @Query("SELECT * FROM readiness")
    suspend fun readinessOnce(): List<ReadinessEntity>

    @Upsert
    suspend fun putReadiness(readiness: ReadinessEntity)

    @Query("DELETE FROM readiness")
    suspend fun clearReadiness()

    @Query("DELETE FROM settings")
    suspend fun deleteAllSettings()
}

@Dao
interface DailyPromptDao {
    @Upsert
    suspend fun put(row: DailyPromptEntity)

    @Query("SELECT * FROM daily_prompts ORDER BY epochDay")
    suspend fun all(): List<DailyPromptEntity>

    @Query("SELECT * FROM daily_prompts WHERE epochDay = :day")
    suspend fun forDay(day: Long): DailyPromptEntity?

    @Query("DELETE FROM daily_prompts")
    suspend fun clear()
}

@Dao
interface RunDao {
    @Upsert
    suspend fun upsertRun(run: RunEntity): Long

    @Upsert
    suspend fun upsertMovement(movement: RunMovementEntity)

    @Query("SELECT * FROM runs ORDER BY epochDay")
    suspend fun allOnce(): List<RunEntity>

    @Query("SELECT * FROM runs ORDER BY epochDay DESC LIMIT 1")
    suspend fun latest(): RunEntity?

    @Query("SELECT * FROM runs WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    suspend fun between(from: Long, to: Long): List<RunEntity>

    @Query("SELECT * FROM run_movements ORDER BY id")
    suspend fun allMovementsOnce(): List<RunMovementEntity>

    @Query("SELECT * FROM run_movements WHERE runId = :runId")
    suspend fun movementsFor(runId: Long): List<RunMovementEntity>

    @Delete
    suspend fun deleteMovement(movement: RunMovementEntity)

    @Query("DELETE FROM runs WHERE id = :runId")
    suspend fun deleteRun(runId: Long)

    @Upsert
    suspend fun upsertSore(area: SoreAreaEntity)

    @Query("SELECT * FROM sore_areas WHERE clearedOnDay IS NULL ORDER BY reportedOnDay")
    suspend fun soreOnce(): List<SoreAreaEntity>

    @Query("SELECT * FROM sore_areas ORDER BY reportedOnDay")
    suspend fun allSoreOnce(): List<SoreAreaEntity>

    @Query("DELETE FROM runs")
    suspend fun deleteAllRuns()

    @Query("DELETE FROM run_movements")
    suspend fun deleteAllRunMovements()

    @Query("DELETE FROM sore_areas")
    suspend fun deleteAllSore()
}
