package com.kamsiob.steadyhealth.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.kamsiob.steadyhealth.data.entity.BloodPressureEntity
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
import com.kamsiob.steadyhealth.data.entity.PatternEntity
import com.kamsiob.steadyhealth.data.entity.PersonSynonymEntity
import com.kamsiob.steadyhealth.data.entity.PhotoEntity
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
import com.kamsiob.steadyhealth.data.entity.VisitSummaryEntity
import com.kamsiob.steadyhealth.data.entity.WaistEntity
import com.kamsiob.steadyhealth.data.entity.WeeklyNoteEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity

/**
 * Every row of every table, for the backup and for nothing else.
 *
 * The queries elsewhere answer a screen's question: the ones that read a whole
 * table often leave something out on purpose, so an archived item or an
 * appointment that has been and gone does not turn up where it is not wanted. A
 * backup wants exactly the opposite, and reusing one of those queries by mistake
 * is the quiet kind of loss that is only found when somebody restores. So the
 * backup reads through its own queries, and every one of them is the same
 * sentence: everything in this table, unfiltered.
 *
 * It is two interfaces rather than one because thirty-one queries in one place
 * trips a counter that is there to catch a class doing too much. The seam is
 * between what happened, which has a date on it, and what the app is holding now.
 */
@Dao
interface BackupHistoryDao {

    @Query("SELECT * FROM weigh_ins")
    suspend fun weighIns(): List<WeighInEntity>

    @Query("SELECT * FROM waist")
    suspend fun waist(): List<WaistEntity>

    @Query("SELECT * FROM blood_pressure")
    suspend fun bloodPressure(): List<BloodPressureEntity>

    @Query("SELECT * FROM photos")
    suspend fun photos(): List<PhotoEntity>

    @Query("SELECT * FROM check_ins")
    suspend fun checkIns(): List<CheckInEntity>

    @Query("SELECT * FROM check_in_tags")
    suspend fun checkInTags(): List<CheckInTagEntity>

    @Query("SELECT * FROM sessions")
    suspend fun sessions(): List<SessionEntity>

    @Query("SELECT * FROM runs")
    suspend fun runs(): List<RunEntity>

    @Query("SELECT * FROM run_movements")
    suspend fun runMovements(): List<RunMovementEntity>

    @Query("SELECT * FROM sore_areas")
    suspend fun soreAreas(): List<SoreAreaEntity>

    @Query("SELECT * FROM checks")
    suspend fun checks(): List<CheckEntity>

    @Query("SELECT * FROM measure_results")
    suspend fun measureResults(): List<MeasureResultEntity>

    @Query("SELECT * FROM item_ratings")
    suspend fun itemRatings(): List<ItemRatingEntity>

    @Query("SELECT * FROM reminders_sent")
    suspend fun remindersSent(): List<ReminderSentEntity>

    @Query("SELECT * FROM daily_prompts")
    suspend fun dailyPrompts(): List<DailyPromptEntity>

    @Query("SELECT * FROM visit_summaries")
    suspend fun visitSummaries(): List<VisitSummaryEntity>

    @Query("SELECT * FROM weekly_notes")
    suspend fun weeklyNotes(): List<WeeklyNoteEntity>

    @Query("SELECT * FROM patterns")
    suspend fun patterns(): List<PatternEntity>

    @Query("SELECT * FROM experiments")
    suspend fun experiments(): List<ExperimentEntity>
}

/** The other half: what the app is holding now, and what somebody was given. */
@Dao
interface BackupStateDao {

    @Query("SELECT * FROM person_synonyms")
    suspend fun synonyms(): List<PersonSynonymEntity>

    @Query("SELECT * FROM step_names")
    suspend fun stepNames(): List<StepNameEntity>

    @Query("SELECT * FROM ladder_state")
    suspend fun ladderState(): List<LadderStateEntity>

    @Query("SELECT * FROM tracked_items")
    suspend fun trackedItems(): List<TrackedItemEntity>

    @Query("SELECT * FROM documents")
    suspend fun documents(): List<DocumentEntity>

    @Query("SELECT * FROM document_pages")
    suspend fun documentPages(): List<DocumentPageEntity>

    @Query("SELECT * FROM plans")
    suspend fun plans(): List<PlanEntity>

    @Query("SELECT * FROM plan_items")
    suspend fun planItems(): List<PlanItemEntity>

    @Query("SELECT * FROM notices")
    suspend fun notices(): List<NoticeEntity>

    @Query("SELECT * FROM settings")
    suspend fun settings(): List<SettingEntity>

    @Query("SELECT * FROM exclusions")
    suspend fun exclusions(): List<ExclusionEntity>

    @Query("SELECT * FROM readiness")
    suspend fun readiness(): List<ReadinessEntity>
}
