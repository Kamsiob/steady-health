package com.kamsiob.steadyhealth.backup

import androidx.room3.executeSQL
import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import com.kamsiob.steadyhealth.data.SteadyDatabase
import java.time.LocalDate

/**
 * The database, out into a backup and back in from one.
 *
 * Both directions are here so the two halves are read together. Neither of them
 * knows anything about files, zips or screens.
 */
class BackupRepository(private val db: SteadyDatabase) {

    /**
     * Every table, as it is at one moment.
     *
     * One constructor call with one argument per table, and [Backup] gives none of
     * them a default. That is deliberate and it is the point of the whole file:
     * somebody who adds a table to the database and does not add it here cannot
     * compile, rather than shipping a backup that is quietly missing something
     * nobody will look for until they are restoring it.
     *
     * All of it inside one read transaction, because thirty-one queries are
     * otherwise thirty-one different moments. The app writes a session and then
     * writes its movements, so a backup that read the sessions before that and the
     * movements after it holds movements belonging to a session it does not carry.
     * Nothing would be wrong with the file and nothing would be wrong with the
     * phone. It would go wrong later, on a different phone, where that work would
     * either belong to no session or belong to whichever session ends up with that
     * id. Putting a backup back is already one transaction for the same reason;
     * taking one has to be as well, or the file is right about no moment at all.
     */
    suspend fun read(appVersion: String, writtenOn: LocalDate): Backup =
        db.withReadTransaction {
            val history = db.backupHistory()
            val state = db.backupState()
            Backup(
                header = BackupHeader(
                    app = Backup.MARK,
                    appVersion = appVersion,
                    schema = Backup.SCHEMA_VERSION,
                    writtenOn = writtenOn.toString(),
                ),
                weighIns = history.weighIns(),
                waist = history.waist(),
                bloodPressure = history.bloodPressure(),
                photos = history.photos(),
                checkIns = history.checkIns(),
                checkInTags = history.checkInTags(),
                sessions = history.sessions(),
                runs = history.runs(),
                runMovements = history.runMovements(),
                soreAreas = history.soreAreas(),
                checks = history.checks(),
                measureResults = history.measureResults(),
                itemRatings = history.itemRatings(),
                remindersSent = history.remindersSent(),
                dailyPrompts = history.dailyPrompts(),
                visitSummaries = history.visitSummaries(),
                weeklyNotes = history.weeklyNotes(),
                patterns = history.patterns(),
                experiments = history.experiments(),
                synonyms = state.synonyms(),
                stepNames = state.stepNames(),
                ladderState = state.ladderState(),
                trackedItems = state.trackedItems(),
                documents = state.documents(),
                documentPages = state.documentPages(),
                plans = state.plans(),
                planItems = state.planItems(),
                notices = state.notices(),
                settings = state.settings(),
                exclusions = state.exclusions(),
                readiness = state.readiness(),
            )
        }

    /**
     * Put a backup back, in place of everything that is here.
     *
     * It replaces rather than merges, and that is a decision rather than a
     * shortcut. Two histories of the same person cannot be joined honestly: the
     * same day can hold two different sentences, a weight from each, two ratings
     * of the same thing a fortnight apart, and there is no rule that picks between
     * them that is not the app inventing what somebody meant. Whatever such a rule
     * chose, nobody could check it afterwards, because the two versions would be
     * mixed and the join would be invisible. Replacing is the one outcome that can
     * be described in a sentence before it happens, and a person can decide about
     * a sentence. So the screen says everything here will be replaced by what is
     * in the file, and then that is exactly what happens.
     *
     * It is one transaction, so the app is never left holding half of one history
     * and half of another. Every table is emptied and refilled from the same list
     * the file is built from, so a table cannot be emptied and then not refilled.
     */
    suspend fun restore(backup: Backup) {
        db.withWriteTransaction {
            BackupTables.all.forEach { table ->
                executeSQL("DELETE FROM ${table.name}")
                table.restore(db, backup)
            }
        }
    }
}
