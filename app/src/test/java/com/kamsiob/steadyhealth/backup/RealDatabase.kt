package com.kamsiob.steadyhealth.backup

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.kamsiob.steadyhealth.data.SteadyDatabase
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
import kotlinx.coroutines.Dispatchers

/**
 * The real database, opened on a laptop.
 *
 * The app opens SQLCipher through a driver, and SQLCipher is a native library that
 * a JVM test does not have. Room 3 takes the driver as a parameter, so the same
 * generated code, the same schema and the same DAOs run here against the ordinary
 * SQLite that Robolectric brings. Only the encryption is missing, and nothing
 * about writing or putting back a backup depends on it.
 *
 * This is worth the trouble because the half of backup and restore that can lose
 * somebody's history is the half that talks to a database, and every test written
 * for it before this one stopped at the file.
 */
fun aDatabase(): SteadyDatabase = Room.inMemoryDatabaseBuilder(
    ApplicationProvider.getApplicationContext(),
    SteadyDatabase::class.java,
)
    .setDriver(AndroidSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()

/**
 * The ids the database handed out, so a test can ask what still hangs off them.
 *
 * None of these is the number a fresh table would give out first. That is the
 * point of them: a restore that quietly renumbers looks perfect until you ask a
 * parent for its children.
 */
data class Ids(
    val checkIn: Long,
    val run: Long,
    val check: Long,
    val item: Long,
    val document: Long,
    val plan: Long,
)

/**
 * A used database: every table holding something, and nothing round about it.
 *
 * Written through the app's own writes rather than by insert statements, so what
 * is in here is what a phone would actually have. Every nullable column is null in
 * one row and filled in another, because null and zero are different answers all
 * through this app.
 *
 * The ids are made awkward on purpose. Three sessions are recorded and two are
 * deleted, which is what a year of use does, and it leaves one session whose id is
 * three in a table of one row. A restore that renumbers would give it id one and
 * leave its movements pointing at a session that no longer exists, and it would
 * report success while doing it.
 */
suspend fun SteadyDatabase.fillEveryTable(): Ids {
    theBody()
    theDays()
    val run = theMoving()
    val check = theChecks()
    val item = theList()
    val document = thePaper()
    val plan = thePlans()
    theChoices()
    theRest()
    return Ids(CHECK_IN_ID, run, check, item, document, plan)
}

private suspend fun SteadyDatabase.theBody() {
    weighIns().upsert(WeighInEntity(DAY, 1_757_000_000_000, 82.4, 82.35, "scale"))
    weighIns().upsert(WeighInEntity(FAR_OFF_DAY, 1_757_100_000_000, -1.5, 0.0, NOTHING))
    body().upsertWaist(WaistEntity(DAY + 1, 1_757_000_000_001, 94.5))
    body().upsertBloodPressure(BloodPressureEntity(0, DAY + 2, 1_757_000_000_002, 128, 79))
    body().upsertBloodPressure(BloodPressureEntity(0, DAY + 3, 1_757_000_000_003, 131, 84))
    body().upsertPhoto(PhotoEntity(DAY + 4, 1_757_000_000_004, "sunday-20004.jpg"))
}

/**
 * Two days, with ids that are not one and two.
 *
 * There is no way to delete one day in this app, so the gap is written rather than
 * made. It is the same gap: a phone that has been restored once already, or a day
 * removed by a future screen, has exactly this shape.
 */
private suspend fun SteadyDatabase.theDays() {
    checkIns().upsert(CheckInEntity(CHECK_IN_ID, DAY + 5, 1L, SENTENCE, 15, "good", true))
    checkIns().upsert(CheckInEntity(SECOND_CHECK_IN_ID, DAY + 6, 2L, NOTHING, null, null, false))
    checkIns().insertTags(
        listOf(
            CheckInTagEntity(CHECK_IN_ID, "sleep", true),
            CheckInTagEntity(CHECK_IN_ID, "knees", false),
            CheckInTagEntity(SECOND_CHECK_IN_ID, "busy", false),
        ),
    )
}

private suspend fun SteadyDatabase.theMoving(): Long {
    sessions().upsert(
        SessionEntity(0, DAY + 7, 1L, 2L, 600, "walk", 3, "talk", 900, 1.5, "same", DAY),
    )
    sessions().upsert(
        SessionEntity(0, DAY + 8, 3L, 4L, 0, "chair", 0, null, null, null, null, null),
    )

    val first = runs().upsertRun(RunEntity(0, DAY + 9, 5L, 6L, "finished", "easy"))
    val second = runs().upsertRun(RunEntity(0, DAY + 10, 7L, 8L, "hurt", null))
    val kept = runs().upsertRun(
        RunEntity(0, DAY + 11, 9L, 10L, "enough_for_today", "about_right", true, true, "sleep hip"),
    )
    runs().deleteRun(first)
    runs().deleteRun(second)

    runs().upsertMovement(RunMovementEntity(0, kept, "sit_to_stand", 10, 8, true, false, false))
    runs().upsertMovement(RunMovementEntity(0, kept, "heel_raise", 12, 12, false, true, false))
    runs().upsertSore(SoreAreaEntity(0, "knee", DAY + 12, DAY + 13))
    runs().upsertSore(SoreAreaEntity(0, "shoulder", DAY + 14, null))
    return kept
}

private suspend fun SteadyDatabase.theChecks(): Long {
    checks().upsertCheck(CheckEntity(CHECK_ID, DAY + 15, 1_757_000_000_005, "on_feet"))
    checks().upsertMeasure(
        MeasureResultEntity(0, CHECK_ID, "chair_stand", "strength", DAY + 15, 11L, 14.0, "phone"),
    )
    checks().upsertMeasure(
        MeasureResultEntity(0, CHECK_ID, "walk_speed", "walking", DAY + 15, 12L, 1.02, "camera"),
    )
    // Not taken inside a check, which is the case the nullable column is for.
    checks().upsertMeasure(
        MeasureResultEntity(0, null, "walk_speed", "walking", DAY + 16, 13L, -0.25, "person"),
    )
    return CHECK_ID
}

private suspend fun SteadyDatabase.theList(): Long {
    abilities().upsertItem(TrackedItemEntity(ITEM_ID, SENTENCE, "walking", 14L, null))
    abilities().upsertItem(TrackedItemEntity(SECOND_ITEM_ID, NOTHING, "strength", 15L, DAY + 17))
    abilities().upsertRating(ItemRatingEntity(ITEM_ID, DAY + 18, 7, 16L, 4))
    abilities().upsertRating(ItemRatingEntity(ITEM_ID, DAY + 19, 0, 17L, null))
    abilities().upsertRating(ItemRatingEntity(SECOND_ITEM_ID, DAY + 18, 3, 18L, 10))
    return ITEM_ID
}

private suspend fun SteadyDatabase.thePaper(): Long {
    val first = documents().put(DocumentEntity(0, DAY + 20, "letter", "the physio", 19L))
    val second = documents().put(DocumentEntity(0, DAY + 21, "results", NOTHING, 20L))
    val kept = documents().put(DocumentEntity(0, DAY + 22, "letter", NOTHING, 21L))
    documents().delete(first)
    documents().delete(second)

    documents().putPage(DocumentPageEntity(0, kept, 0, byteArrayOf(0, 1, -1, 127, -128), SENTENCE))
    documents().putPage(DocumentPageEntity(0, kept, 1, byteArrayOf(-2, 3), NOTHING))
    return kept
}

private suspend fun SteadyDatabase.thePlans(): Long {
    val first = plans().put(PlanEntity(0, "Old physio", 22L))
    val second = plans().put(PlanEntity(0, NOTHING, 23L))
    val kept = plans().put(PlanEntity(0, "Physio", 24L, DAY + 23, null, DAY + 24))
    plans().delete(first)
    plans().delete(second)

    plans().putItem(
        PlanItemEntity(0, kept, SENTENCE, "sit_to_stand", "reps", 10, 3, "week", 3, true, 25L),
    )
    plans().putItem(PlanItemEntity(0, kept, NOTHING, null, "hold", 30, 1, "day", 1, false, 26L))
    return kept
}

/**
 * The settings, which are every choice somebody made.
 *
 * The real keys rather than invented ones, because losing these loses the ways of
 * getting around, the exclusions, the chair, the reminder switches and what was
 * said about the six places, without losing a single number. That would read as a
 * restore that worked.
 */
private suspend fun SteadyDatabase.theChoices() {
    listOf(
        SettingEntity("onboarding_complete", "true"),
        SettingEntity("getting_around", "stick"),
        SettingEntity("chair", "kitchen"),
        SettingEntity("chair_arms", "true"),
        SettingEntity("chair_height", "46"),
        SettingEntity("units", "metric"),
        SettingEntity("height_cm", "168"),
        SettingEntity("show_numbers", "false"),
        SettingEntity("pacing", "true"),
        SettingEntity("remind_daily", "false"),
        SettingEntity("remind_review", "true"),
        SettingEntity("place_stairs", "not_yet"),
        SettingEntity("place_kitchen", "sorted"),
        SettingEntity("place_bathroom", NOTHING),
        SettingEntity("name", SENTENCE),
    ).forEach { profile().put(it) }
    profile().putExclusion(ExclusionEntity("weight", 1_757_000_000_006))
    profile().putExclusion(ExclusionEntity("photos", 1_757_000_000_007))
    profile().putReadiness(ReadinessEntity("chest_pain", false, 1_757_000_000_008))
    profile().putReadiness(ReadinessEntity("dizzy", true, 1_757_000_000_009))
}

private suspend fun SteadyDatabase.theRest() {
    reminders().record(ReminderSentEntity(0, "walk", 1_757_000_000_010))
    reminders().record(ReminderSentEntity(0, "review", 1_757_000_000_011))
    dailyPrompts().put(DailyPromptEntity(DAY + 25, true))
    dailyPrompts().put(DailyPromptEntity(DAY + 26, false))
    notes().upsertSummary(VisitSummaryEntity(0, 27L, DAY, DAY + 27, SENTENCE, NOTHING, "{}", false))
    notes().upsertNote(WeeklyNoteEntity(DAY + 28, 28L, SENTENCE, true))
    notes().upsertPattern(
        PatternEntity(0, "sleep", "chair_stand", "up", 4, 3, 2, 5, SENTENCE, NOTHING, DAY + 29),
    )
    notes().upsertExperiment(
        ExperimentEntity(
            0, "walk", "chair_stand", DAY + 30, DAY + 44, DAY + 58, 29L, "more", SENTENCE,
        ),
    )
    notes().upsertExperiment(
        ExperimentEntity(0, "sleep", "walk_speed", DAY + 31, DAY + 45, DAY + 59, null, null, null),
    )
    synonyms().upsert(PersonSynonymEntity("done in", "tired", 1_757_000_000_012))
    ladders().upsertName(StepNameEntity("walk", 3, "the postbox and back", 30L))
    ladders().upsertState(LadderStateEntity("walk", 3, DAY + 32, DAY + 33, DAY + 34, DAY + 35))
    ladders().upsertState(LadderStateEntity("chair", 0, null, null, null, null))
    notices().put(NoticeEntity("soft_wall", 1_757_000_000_013))
    notices().put(NoticeEntity("smoothed_weight", 1_757_000_000_014))
}

private const val DAY = 20_000L
private const val CHECK_IN_ID = 7L
private const val SECOND_CHECK_IN_ID = 12L
private const val CHECK_ID = 5L
private const val ITEM_ID = 9L
private const val SECOND_ITEM_ID = 11L
