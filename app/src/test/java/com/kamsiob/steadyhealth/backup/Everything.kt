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

/** A sentence with everything in it that has ever broken a file format. */
const val SENTENCE = "Walked to the shop, said \"not today\" to the hill,\nand went anyway 🙂"

/** Empty, and not null. The two mean different things and both have to survive. */
const val NOTHING = ""

/** Far enough out that the number needs all of a Long. */
const val FAR_OFF_DAY = 9_999_999_999L

/**
 * A backup with something awkward in every table.
 *
 * The sentences are the part of this file nobody else could reproduce, so they
 * carry a comma, a quotation mark, a newline and an emoji. Every nullable column
 * is null in one row and filled in another, because null and zero are different
 * answers everywhere in this app and a format that loses the difference loses the
 * question somebody was asked. There is a negative number, an empty string that is
 * not a null, and a day far enough out to need every digit of a Long.
 *
 * It is built by calling [Backup] with all thirty-one lists, so a table added to
 * the database and not to the backup stops this file compiling.
 */
@Suppress("LongMethod") // One row per table is thirty-one rows, and that is the point.
fun everything(): Backup = Backup(
    header = BackupHeader(
        app = Backup.MARK,
        appVersion = "0.1.0",
        schema = Backup.SCHEMA_VERSION,
        writtenOn = "2026-09-09",
    ),
    weighIns = listOf(
        WeighInEntity(20_000, 1_757_000_000_000, 82.4, 82.35, "scale"),
        WeighInEntity(FAR_OFF_DAY, 1_757_100_000_000, -1.5, 0.0, NOTHING),
    ),
    waist = listOf(WaistEntity(20_001, 1_757_000_000_001, 94.5)),
    bloodPressure = listOf(BloodPressureEntity(3, 20_002, 1_757_000_000_002, 128, -1)),
    photos = listOf(PhotoEntity(20_003, 1_757_000_000_003, "sunday-20003.jpg")),
    checkIns = listOf(
        CheckInEntity(1, 20_004, 1_757_000_000_004, SENTENCE, 15, "good", true),
        CheckInEntity(2, 20_005, 1_757_000_000_005, NOTHING, null, null, false),
    ),
    checkInTags = listOf(
        CheckInTagEntity(1, "sleep", true),
        CheckInTagEntity(2, "knees", false),
    ),
    sessions = listOf(
        SessionEntity(1, 20_006, 1L, 2L, 600, "walk", 3, "talk", 900, 1.5, "same", 20_007),
        SessionEntity(2, 20_007, 3L, 4L, 0, "chair", 0, null, null, null, null, null),
    ),
    runs = listOf(
        RunEntity(1, 20_008, 5L, 6L, "finished", "about_right", false, true),
        RunEntity(2, 20_009, 7L, 8L, "hurt", null, true, false),
    ),
    runMovements = listOf(RunMovementEntity(1, 1, "sit_to_stand", 10, 8, true, false, false)),
    soreAreas = listOf(
        SoreAreaEntity(1, "knee", 20_010, 20_011),
        SoreAreaEntity(2, "shoulder", 20_012, null),
    ),
    checks = listOf(CheckEntity(1, 20_013, 1_757_000_000_006, "on_feet")),
    measureResults = listOf(
        MeasureResultEntity(1, 1, "chair_stand", "strength", 20_013, 9L, 14.0, "phone"),
        MeasureResultEntity(2, null, "walk_speed", "walking", 20_014, 10L, -0.25, "person"),
    ),
    itemRatings = listOf(
        ItemRatingEntity(1, 20_015, 7, 11L, 4),
        ItemRatingEntity(1, 20_016, 0, 12L, null),
    ),
    remindersSent = listOf(ReminderSentEntity(1, "walk", 1_757_000_000_007)),
    dailyPrompts = listOf(DailyPromptEntity(20_017, true), DailyPromptEntity(20_018, false)),
    visitSummaries = listOf(
        VisitSummaryEntity(1, 13L, 20_000, 20_019, SENTENCE, NOTHING, "{}", false),
    ),
    weeklyNotes = listOf(WeeklyNoteEntity(20_020, 14L, SENTENCE, true)),
    patterns = listOf(
        PatternEntity(1, "sleep", "chair_stand", "up", 4, 3, 2, 5, SENTENCE, NOTHING, 20_021),
    ),
    experiments = listOf(
        ExperimentEntity(1, "walk", "chair_stand", 20_022, 20_023, 20_024, 15L, "more", SENTENCE),
        ExperimentEntity(2, "sleep", "walk_speed", 20_025, 20_026, 20_027, null, null, null),
    ),
    synonyms = listOf(PersonSynonymEntity("done in", "tired", 1_757_000_000_008)),
    stepNames = listOf(StepNameEntity("walk", 3, "the postbox and back", 16L)),
    ladderState = listOf(
        LadderStateEntity("walk", 3, 20_028, 20_029, 20_030, 20_031),
        LadderStateEntity("chair", 0, null, null, null, null),
    ),
    trackedItems = listOf(
        TrackedItemEntity(1, SENTENCE, "walking", 17L, 20_032),
        TrackedItemEntity(2, NOTHING, "strength", 18L, null),
    ),
    documents = listOf(DocumentEntity(1, 20_033, "letter", NOTHING, 19L)),
    documentPages = listOf(
        DocumentPageEntity(1, 1, 0, byteArrayOf(0, 1, -1, 127, -128), SENTENCE),
    ),
    plans = listOf(
        PlanEntity(1, "Physio", 20L, 20_034, 20_035, 20_036),
        PlanEntity(2, NOTHING, 21L, null, null, null),
    ),
    planItems = listOf(
        PlanItemEntity(1, 1, SENTENCE, "sit_to_stand", "reps", 10, 3, "week", 3, true, 22L),
        PlanItemEntity(2, 1, NOTHING, null, "hold", 30, 1, "day", 1, false, 23L),
    ),
    notices = listOf(NoticeEntity("soft_wall", 1_757_000_000_009)),
    settings = listOf(
        SettingEntity("units", "metric"),
        SettingEntity("name", SENTENCE),
    ),
    exclusions = listOf(ExclusionEntity("weight", 1_757_000_000_010)),
    readiness = listOf(ReadinessEntity("chest_pain", false, 1_757_000_000_011)),
)
