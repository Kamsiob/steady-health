package com.kamsiob.steadyhealth.backup

import com.kamsiob.steadyhealth.data.SteadyDatabase

/**
 * One table: what it is called, where its rows are in a [Backup], and how a row
 * goes back in.
 *
 * The rows go back one at a time through the same writes the app uses every day,
 * so a restored row is written exactly the way it was written the first time. The
 * ids come back with them: `nullif(?, 0)` in Room's insert keeps an id somebody
 * supplied, and every join in this database is by id, so a day keeps its tags and
 * a plan keeps its lines.
 */
class BackupTable<E : Any>(
    /** The name in the database, which is also the name of its list in the file. */
    val name: String,
    /** Where this table's rows are in a backup. Public so a test can walk them. */
    val rows: (Backup) -> List<E>,
    private val put: suspend (SteadyDatabase, E) -> Unit,
) {
    suspend fun restore(db: SteadyDatabase, backup: Backup) {
        rows(backup).forEach { put(db, it) }
    }
}

/**
 * Every table, once, in the only list that puts a backup back.
 *
 * Restore walks this list and nothing else, so there is no second place a table
 * could be handled differently or not at all. BackupCompletenessTest holds the
 * list against the schema Room writes at build time, which is what makes adding a
 * table to the database and forgetting it here a failing test rather than a
 * discovery somebody makes on a new phone with an empty app.
 */
object BackupTables {

    val all: List<BackupTable<*>> = listOf(
        BackupTable("weigh_ins", Backup::weighIns) { db, row -> db.weighIns().upsert(row) },
        BackupTable("waist", Backup::waist) { db, row -> db.body().upsertWaist(row) },
        BackupTable("blood_pressure", Backup::bloodPressure) { db, row ->
            db.body().upsertBloodPressure(row)
        },
        BackupTable("photos", Backup::photos) { db, row -> db.body().upsertPhoto(row) },
        BackupTable("check_ins", Backup::checkIns) { db, row -> db.checkIns().upsert(row) },
        BackupTable("check_in_tags", Backup::checkInTags) { db, row ->
            db.checkIns().insertTags(listOf(row))
        },
        BackupTable("sessions", Backup::sessions) { db, row -> db.sessions().upsert(row) },
        BackupTable("runs", Backup::runs) { db, row -> db.runs().upsertRun(row) },
        BackupTable("run_movements", Backup::runMovements) { db, row ->
            db.runs().upsertMovement(row)
        },
        BackupTable("sore_areas", Backup::soreAreas) { db, row -> db.runs().upsertSore(row) },
        BackupTable("checks", Backup::checks) { db, row -> db.checks().upsertCheck(row) },
        BackupTable("measure_results", Backup::measureResults) { db, row ->
            db.checks().upsertMeasure(row)
        },
        BackupTable("item_ratings", Backup::itemRatings) { db, row ->
            db.abilities().upsertRating(row)
        },
        BackupTable("reminders_sent", Backup::remindersSent) { db, row ->
            db.reminders().record(row)
        },
        BackupTable("daily_prompts", Backup::dailyPrompts) { db, row ->
            db.dailyPrompts().put(row)
        },
        BackupTable("visit_summaries", Backup::visitSummaries) { db, row ->
            db.notes().upsertSummary(row)
        },
        BackupTable("weekly_notes", Backup::weeklyNotes) { db, row -> db.notes().upsertNote(row) },
        BackupTable("patterns", Backup::patterns) { db, row -> db.notes().upsertPattern(row) },
        BackupTable("experiments", Backup::experiments) { db, row ->
            db.notes().upsertExperiment(row)
        },
        BackupTable("person_synonyms", Backup::synonyms) { db, row -> db.synonyms().upsert(row) },
        BackupTable("step_names", Backup::stepNames) { db, row -> db.ladders().upsertName(row) },
        BackupTable("ladder_state", Backup::ladderState) { db, row ->
            db.ladders().upsertState(row)
        },
        BackupTable("tracked_items", Backup::trackedItems) { db, row ->
            db.abilities().upsertItem(row)
        },
        BackupTable("documents", Backup::documents) { db, row -> db.documents().put(row) },
        BackupTable("document_pages", Backup::documentPages) { db, row ->
            db.documents().putPage(row)
        },
        BackupTable("plans", Backup::plans) { db, row -> db.plans().put(row) },
        BackupTable("plan_items", Backup::planItems) { db, row -> db.plans().putItem(row) },
        BackupTable("notices", Backup::notices) { db, row -> db.notices().put(row) },
        BackupTable("settings", Backup::settings) { db, row -> db.profile().put(row) },
        BackupTable("exclusions", Backup::exclusions) { db, row ->
            db.profile().putExclusion(row)
        },
        BackupTable("readiness", Backup::readiness) { db, row -> db.profile().putReadiness(row) },
    )
}
