package com.kamsiob.steadyhealth.backup

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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What was written, by what, and when.
 *
 * [app] is the only thing that makes a file ours. It is checked before anything
 * else is read, so a photograph or somebody's tax return picked by mistake is
 * turned away by name rather than by a parser failing halfway through.
 *
 * [schema] is the shape of the database, not the version of the app. It is the
 * one number restore has to reason about: this build knows how to store schema
 * [Backup.SCHEMA_VERSION] and everything before it, and knows nothing about what
 * came after.
 */
@Serializable
data class BackupHeader(
    val app: String,
    val appVersion: String,
    val schema: Int,
    /** The day it was written, as an ordinary date, for the person reading it. */
    val writtenOn: String,
)

/**
 * Everything in the database, as one file.
 *
 * The CSVs beside this in the same zip are for a person to read, and they are
 * lossy on purpose: they format dates, drop the ids that join one table to
 * another, and flatten tags into a column. You cannot rebuild an app from them
 * and this does not try. This is the other half of the promise: the same zip
 * holds the spreadsheets somebody opens and the file the app can read back.
 *
 * The rows are the database's own row types, not a second set of shapes written
 * beside them. A copy would be a place for a column to go missing, and the column
 * that goes missing is never noticed until somebody restores. Serialising the
 * entities themselves means the file has exactly the columns the tables have, for
 * as long as both exist.
 *
 * Every table is one property, and none of them has a default. That is the whole
 * design: adding a table to the database and forgetting it here has to be a
 * compile error where the backup is built, rather than a list that is quietly one
 * shorter than the database. The names in the file are the table names, so the
 * file says which table it is carrying and a test can hold it against the schema
 * Room writes at build time.
 */
@Suppress("LongParameterList") // One parameter per table, and no defaults, on purpose.
@Serializable
data class Backup(
    val header: BackupHeader,
    @SerialName("weigh_ins") val weighIns: List<WeighInEntity>,
    @SerialName("waist") val waist: List<WaistEntity>,
    @SerialName("blood_pressure") val bloodPressure: List<BloodPressureEntity>,
    @SerialName("photos") val photos: List<PhotoEntity>,
    @SerialName("check_ins") val checkIns: List<CheckInEntity>,
    @SerialName("check_in_tags") val checkInTags: List<CheckInTagEntity>,
    @SerialName("sessions") val sessions: List<SessionEntity>,
    @SerialName("runs") val runs: List<RunEntity>,
    @SerialName("run_movements") val runMovements: List<RunMovementEntity>,
    @SerialName("sore_areas") val soreAreas: List<SoreAreaEntity>,
    @SerialName("checks") val checks: List<CheckEntity>,
    @SerialName("measure_results") val measureResults: List<MeasureResultEntity>,
    @SerialName("item_ratings") val itemRatings: List<ItemRatingEntity>,
    @SerialName("reminders_sent") val remindersSent: List<ReminderSentEntity>,
    @SerialName("daily_prompts") val dailyPrompts: List<DailyPromptEntity>,
    @SerialName("visit_summaries") val visitSummaries: List<VisitSummaryEntity>,
    @SerialName("weekly_notes") val weeklyNotes: List<WeeklyNoteEntity>,
    @SerialName("patterns") val patterns: List<PatternEntity>,
    @SerialName("experiments") val experiments: List<ExperimentEntity>,
    @SerialName("person_synonyms") val synonyms: List<PersonSynonymEntity>,
    @SerialName("step_names") val stepNames: List<StepNameEntity>,
    @SerialName("ladder_state") val ladderState: List<LadderStateEntity>,
    @SerialName("tracked_items") val trackedItems: List<TrackedItemEntity>,
    @SerialName("documents") val documents: List<DocumentEntity>,
    @SerialName("document_pages") val documentPages: List<DocumentPageEntity>,
    @SerialName("plans") val plans: List<PlanEntity>,
    @SerialName("plan_items") val planItems: List<PlanItemEntity>,
    @SerialName("notices") val notices: List<NoticeEntity>,
    @SerialName("settings") val settings: List<SettingEntity>,
    @SerialName("exclusions") val exclusions: List<ExclusionEntity>,
    @SerialName("readiness") val readiness: List<ReadinessEntity>,
) {
    companion object {
        /** The word that says a file is ours. Never translated, never changed. */
        const val MARK = "steady-health-backup"

        /**
         * The database shape this build can store, which is the version on
         * `@Database`. Written into every backup and checked on the way back in.
         *
         * It is here rather than read off the database class because Room's
         * annotations are not kept in the built code, so nothing can ask the
         * database what version it is at runtime. BackupCompletenessTest holds this
         * number against the schema files Room writes at build time, so the two
         * cannot drift apart quietly.
         */
        const val SCHEMA_VERSION = 7

        /** What the file is called inside the export. Plain, because people see it. */
        const val FILE_NAME = "backup.json"
    }
}
