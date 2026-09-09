package com.kamsiob.steadyhealth.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Every table, in one file, designed once.
 *
 * The whole schema is defined at version 1 rather than grown a table per phase.
 * Two reasons. The standards require that anything the app can store, the export
 * contains and the import restores, and a table that appears in phase five is
 * exactly the kind that gets left out of the export. And LOGIC.md already says
 * what the app stores, so there is nothing to discover later that would justify a
 * migration.
 *
 * Ids are the stable strings from the domain enums, never ordinals, because an
 * ordinal is a number that changes when somebody reorders an enum and a row
 * written last year has no way to know.
 */

/** One morning on the scale. LOGIC.md section 1. */
@Serializable
@Entity(tableName = "weigh_ins", indices = [Index(value = ["epochDay"], unique = true)])
data class WeighInEntity(
    @PrimaryKey val epochDay: Long,
    val recordedAt: Long,
    val rawKg: Double,
    /** The exponentially weighted average, alpha 0.10, seeded by the first reading. */
    val smoothedKg: Double,
    val source: String,
)

/** One day said out loud or typed. LOGIC.md section 3. */
@Serializable
@Entity(tableName = "check_ins", indices = [Index(value = ["epochDay"], unique = true)])
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val recordedAt: Long,
    /** Stored verbatim and shown back. Never edited by the app. */
    val sentence: String,
    val sleepHalfHours: Int?,
    val dayRating: String?,
    val spoken: Boolean,
)

/** A tag on a day. Only ever one of the twenty-four. */
@Serializable
@Entity(
    tableName = "check_in_tags",
    primaryKeys = ["checkInId", "tag"],
    indices = [Index("checkInId"), Index("tag")],
)
data class CheckInTagEntity(
    val checkInId: Long,
    val tag: String,
    /** True when the reader proposed it and the person kept it. */
    val fromReader: Boolean = false,
)

/**
 * A phrase this person corrected the app on, and what they meant by it.
 *
 * AI.md job 2: corrections are stored and passed back in later calls so the
 * mapping improves without the vocabulary growing.
 */
@Serializable
@Entity(tableName = "person_synonyms", indices = [Index(value = ["phrase"], unique = true)])
data class PersonSynonymEntity(
    @PrimaryKey val phrase: String,
    val tag: String,
    val learnedAt: Long,
)

/** One session of moving. LOGIC.md section 3 and 6. */
@Serializable
@Entity(tableName = "sessions", indices = [Index("epochDay"), Index("ladder")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Int,
    val ladder: String,
    val stepIndex: Int,
    val talkTest: String?,
    val steps: Int?,
    val distanceMetres: Double?,
    val nextDayFeel: String?,
    val nextDayAskedOnDay: Long?,
)

/** The person's own name for a step, asked the first time they do it. */
@Serializable
@Entity(tableName = "step_names", primaryKeys = ["ladder", "stepIndex"])
data class StepNameEntity(
    val ladder: String,
    val stepIndex: Int,
    val name: String,
    val namedAt: Long,
)

/** Where a person is on one ladder, and what the app has offered them. */
@Serializable
@Entity(tableName = "ladder_state")
data class LadderStateEntity(
    @PrimaryKey val ladder: String,
    val currentStepIndex: Int,
    val lastOfferedDay: Long?,
    val offerDeclinedUntilDay: Long?,
    val easingUntilDay: Long?,
    val lastSessionDay: Long?,
)

/**
 * One thing the person said they would like to be able to do, in their own words.
 *
 * LOGIC.md 3b. The verbatim text is never rewritten, because it is the whole
 * point: a number only means something next to a life, and this is the life.
 */
@Serializable
@Entity(tableName = "tracked_items", indices = [Index("domain")])
data class TrackedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val domain: String,
    val createdAt: Long,
    val archivedAt: Long?,
)

/** A rating of one tracked item, 0 to 10, re-asked monthly. */
@Serializable
@Entity(
    tableName = "item_ratings",
    primaryKeys = ["itemId", "epochDay"],
    indices = [Index("itemId")],
)
data class ItemRatingEntity(
    val itemId: Long,
    val epochDay: Long,
    val rating: Int,
    val recordedAt: Long,
    /**
     * How sure they feel about it, 0 to 10. ADDENDUM-03 Part 18.
     *
     * On the same row as the doing rather than in a table of its own, because the two
     * are asked in the same breath about the same thing on the same day, and a second
     * table would let them drift apart.
     *
     * Nullable, and it stays nullable. Part 18 calls it "a second optional monthly
     * rating": null means the question was passed over, which is a different thing
     * from a zero, and Confidence.wayOf is built on that difference.
     */
    val sureness: Int? = null,
)

/** One monthly check, and the measures taken inside it. LOGIC.md 7b. */
@Serializable
@Entity(tableName = "checks", indices = [Index(value = ["epochDay"], unique = true)])
data class CheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val completedAt: Long,
    val gettingAround: String,
)

/**
 * One measure taken during a check.
 *
 * [countedBy] records whether the phone timed it, the camera counted it, or the
 * person tapped it, because AI.md requires the app to be honest about that and
 * because a manual count and a sensor count are not the same evidence.
 */
@Serializable
@Entity(
    tableName = "measure_results",
    indices = [Index("checkId"), Index("measureId"), Index("epochDay")],
)
data class MeasureResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checkId: Long?,
    val measureId: String,
    val domain: String,
    val epochDay: Long,
    val recordedAt: Long,
    val value: Double,
    val countedBy: String,
)

/** Waist, in centimetres. LOGIC.md section 2. */
@Serializable
@Entity(tableName = "waist", indices = [Index(value = ["epochDay"], unique = true)])
data class WaistEntity(
    @PrimaryKey val epochDay: Long,
    val recordedAt: Long,
    val centimetres: Double,
)

/** Blood pressure, entered by the person, never interpreted by the app. */
@Serializable
@Entity(tableName = "blood_pressure", indices = [Index("epochDay")])
data class BloodPressureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val recordedAt: Long,
    val systolic: Int,
    val diastolic: Int,
)

/** A Sunday photo. The file lives in app-private storage and never leaves it. */
@Serializable
@Entity(tableName = "photos", indices = [Index(value = ["epochDay"], unique = true)])
data class PhotoEntity(
    @PrimaryKey val epochDay: Long,
    val takenAt: Long,
    val fileName: String,
)

/** The Sunday write-up, kept so it reads the same tomorrow as it did today. */
@Serializable
@Entity(tableName = "weekly_notes", indices = [Index(value = ["weekStartDay"], unique = true)])
data class WeeklyNoteEntity(
    @PrimaryKey val weekStartDay: Long,
    val writtenAt: Long,
    val paragraphs: String,
    /** True when the reader wrote it, false when the engine filled the template. */
    val byModel: Boolean,
)

/** A pattern the engine found and the model worded. LOGIC.md section 11. */
@Serializable
@Entity(tableName = "patterns", indices = [Index("tag")])
data class PatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tag: String,
    val measure: String,
    val direction: String,
    val weeksWith: Int,
    val weeksWithout: Int,
    val splitNumerator: Int,
    val splitDenominator: Int,
    val sentence: String,
    val detail: String,
    val foundOnDay: Long,
)

/**
 * A two-week comparison the person agreed to run. LOGIC.md 9b.
 *
 * [stoppedAt] exists because the person may stop at any time and nothing is
 * recorded as a failure, which needs a field that is not an outcome.
 */
@Serializable
@Entity(tableName = "experiments")
data class ExperimentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val variable: String,
    val measureId: String,
    val startedOnDay: Long,
    val switchOnDay: Long,
    val endsOnDay: Long,
    val stoppedAt: Long?,
    val resultHeadline: String?,
    val resultDetail: String?,
)

/**
 * A generated visit summary, with the window it covered.
 *
 * Stored because regenerating replaces it and because a page somebody may have
 * shown a clinician should still exist afterwards. The brief is kept beside the
 * paragraphs so the validator's work can be re-checked against exactly what the
 * model was given.
 */
@Serializable
@Entity(tableName = "visit_summaries", indices = [Index("generatedAt")])
data class VisitSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val generatedAt: Long,
    val windowStartDay: Long,
    val windowEndDay: Long,
    val paragraphs: String,
    val questions: String,
    val briefJson: String,
    /** False when the template wrote it, because the reader was absent or failed. */
    val byModel: Boolean,
)

/**
 * Every one-time note the app has shown, and when.
 *
 * This is what stops the app repeating itself: the soft wall note, the sore rule,
 * the smoothed-weight explanation, the summary introduction. "Shown once" is a
 * fact on disk rather than a hope.
 */
@Serializable
@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey val noticeId: String,
    val shownAt: Long,
)

/** Every reminder actually sent, so the two-a-week ceiling can be counted. */
@Serializable
@Entity(tableName = "reminders_sent", indices = [Index("sentAt")])
data class ReminderSentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val sentAt: Long,
)

/** Everything the settings screen holds, as strings, because they are settings. */
@Serializable
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/** One thing the person said to leave out. No reason is ever stored. */
@Serializable
@Entity(tableName = "exclusions")
data class ExclusionEntity(
    @PrimaryKey val exclusion: String,
    val chosenAt: Long,
)

/** One readiness answer. Never blocks anything; shows one note once. */
@Serializable
@Entity(tableName = "readiness")
data class ReadinessEntity(
    @PrimaryKey val flag: String,
    val answeredYes: Boolean,
    val answeredAt: Long,
)

/**
 * One session, as it was actually run. ADDENDUM-03 Part 1.
 *
 * Separate from the older `sessions` table, which recorded a walk on a ladder. That
 * table is still read by the measures and the summary, so both exist rather than one
 * being bent into the other's shape.
 */
@Serializable
@Entity(tableName = "runs", indices = [Index("epochDay")])
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val startedAt: Long,
    val endedAt: Long,
    /** finished, enough_for_today, or hurt. Never "cancelled" and never "incomplete". */
    val ending: String,
    /** easy, about_right, or hard. Null when the person did not answer. */
    val felt: String?,
    /** True for the ninety second version, which counts the same. */
    val small: Boolean = false,
    /** True when a therapist's plan was what ran. */
    val fromPlan: Boolean = false,
    /**
     * The optional line at the end, as tag ids joined by spaces. AI.md job 7.
     *
     * On the run rather than in the day's check-in, and that is a decision with a
     * reason. The check-in table means "somebody said something about today", and
     * Today reads whether a row exists to know whether the daily question has been
     * answered. Writing a blank check-in here to hang three tags off would tell
     * somebody they had answered a question nobody asked them.
     *
     * A joined string rather than a table because there are at most three of them
     * from a fixed vocabulary of twenty four, and a table for that is a join on
     * every read of the noticing engine to store nine characters.
     */
    @ColumnInfo(defaultValue = "") val note: String = "",
)

/** One movement inside one session. */
@Serializable
@Entity(tableName = "run_movements", indices = [Index("runId"), Index("movementId")])
data class RunMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val movementId: String,
    val target: Int,
    val count: Int,
    val selfReported: Boolean,
    val madeEasier: Boolean,
    val skipped: Boolean,
)

/**
 * One piece of paper somebody photographed. ADDENDUM-03 Parts 5 and 7.
 *
 * The photograph is always kept and always viewable beside anything the app made
 * from it, so this row outlives whatever was extracted. [kind] is what the classifier
 * decided, kept so the app can say what it thought it was looking at rather than
 * asking again, and [fromWho] is whatever the person typed, which may be nothing.
 */
@Serializable
@Entity(tableName = "documents", indices = [Index("epochDay")])
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val kind: String,
    val fromWho: String,
    val savedAt: Long,
)

/**
 * One page of one document, as it was photographed.
 *
 * The bytes live in the encrypted database rather than in a file beside it, so that
 * "documents live in the encrypted store" is a property of where they are rather than
 * of remembering to encrypt them. It also means export and delete reach them without
 * either having to know about a second place.
 *
 * The frames used for reading text are never written here. Only the page somebody
 * chose to keep.
 */
@Serializable
@Entity(tableName = "document_pages", indices = [Index("documentId")])
data class DocumentPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val at: Int,
    @Serializable(with = Base64Bytes::class) val image: ByteArray,
    /** The text read off this page, kept so a reading can cite it without the photo. */
    val text: String,
) {
    // A ByteArray in a data class means equals compares references, which is wrong
    // for a row. Room does not care, but anything that puts these in a set would.
    override fun equals(other: Any?): Boolean =
        this === other || (other is DocumentPageEntity && id == other.id)

    override fun hashCode(): Int = id.hashCode()
}

/**
 * A programme somebody was given. ADDENDUM-03 Part 6.
 *
 * More than one can exist at once, each labelled, because a physio plan and an OT
 * plan are two plans and merging them would be the app deciding they are one.
 * [reviewDay] is when they next see whoever gave it to them, and is the only date in
 * this app that looks forward.
 */
@Serializable
@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val createdAt: Long,
    val reviewDay: Long? = null,
    val archivedAt: Long? = null,
    /**
     * The appointment day whose one prompt has already gone out.
     *
     * Kept here rather than alongside the reminders because the reminders table
     * answers "how much has the app said this week" and this answers "has this
     * appointment been mentioned", which survives the week rolling over. Storing
     * the day rather than a flag is what makes moving the appointment earn the new
     * date its own prompt and leave the old one with the one it already had.
     */
    val reviewPromptedFor: Long? = null,
)

/**
 * One line of one plan, as it was given and as the app matched it.
 *
 * [line] is never rewritten. The numbers are stored as their parts rather than as
 * text, so that nothing has to parse them again and so a hold can never be read back
 * as repetitions. Nothing in the app writes to these columns except the person.
 */
@Serializable
@Entity(tableName = "plan_items", indices = [Index("planId")])
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val line: String,
    val movementId: String?,
    val manyKind: String,
    val manyValue: Int,
    val manySets: Int,
    val oftenKind: String,
    val oftenTimes: Int,
    val eachSide: Boolean,
    val confirmedAt: Long,
)

/**
 * One daily prompt that went out, and whether it was opened.
 *
 * One row a day at most, which is what the day being the key says. Whether it was
 * opened is the only thing the app ever asks about a notification, and it is asked so
 * the app can stop rather than so it can count anything.
 */
@Serializable
@Entity(tableName = "daily_prompts")
data class DailyPromptEntity(
    @PrimaryKey val epochDay: Long,
    val opened: Boolean = false,
)

/**
 * An area somebody said hurts, and the week it is left out for.
 *
 * Kept as rows rather than a setting because the same area can be reported more than
 * once, and the rule about a second report inside a month needs the dates.
 */
@Serializable
@Entity(tableName = "sore_areas", indices = [Index("reportedOnDay")])
data class SoreAreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val area: String,
    val reportedOnDay: Long,
    /** Set when the person answered the "bring them back?" question. */
    val clearedOnDay: Long? = null,
)
