package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * A used phone, backed up, and put back on an empty one.
 *
 * This is the only moment the feature exists for, and it is the one nobody can
 * undo. Everything else in the app has a second chance somewhere: a number can be
 * entered again, a sentence can be written again. A restore that quietly drops a
 * column replaces the only copy there is with a worse one, and the person who
 * finds out is the one who has already lost the phone.
 *
 * So this compares column by column rather than by spot check, and it does it
 * through the file's own JSON rather than through the row types. Comparing rows
 * would go through their `equals`, and one of them, `DocumentPageEntity`, compares
 * only its id: every photographed page could come back blank and a comparison by
 * row would still be green.
 */
@RunWith(RobolectricTestRunner::class)
class RestoreLosesNothingTest {

    private val phone = aDatabase()
    private val newPhone = aDatabase()

    @After
    fun close() {
        phone.close()
        newPhone.close()
    }

    @Test
    fun everyTableHasSomethingInItOrNoneOfThisProvesAnything() = onANewPhone { before, _ ->
        rowsIn(before).forEach { (table, rows) ->
            assertWithMessage("nothing in $table, so this test would pass empty")
                .that(rows)
                .isNotEmpty()
        }
        assertThat(rowsIn(before).keys).containsExactlyElementsIn(BackupTables.all.map { it.name })
    }

    /**
     * Every row of every table, every column of every row.
     *
     * Order is not asserted because no query here has an ORDER BY and none should:
     * a table is a set of rows. Multiplicity is asserted, so a row arriving twice
     * or not at all is still a failure.
     */
    @Test
    fun everyColumnOfEveryRowIsThereAfterwards() = onANewPhone { before, after ->
        val was = rowsIn(before)
        val now = rowsIn(after)
        was.forEach { (table, rows) ->
            assertWithMessage(table).that(now.getValue(table)).containsExactlyElementsIn(rows)
        }
    }

    /** The bytes of a photographed page, which no comparison by row would check. */
    @Test
    fun aPhotographedPageComesBackByteForByte() = onANewPhone { before, after ->
        val was = before.documentPages.sortedBy { it.at }
        val now = after.documentPages.sortedBy { it.at }
        assertThat(now.map { it.image.toList() }).isEqualTo(was.map { it.image.toList() })
        assertThat(now.map { it.text }).isEqualTo(was.map { it.text })
    }

    /**
     * The settings, one by one.
     *
     * Every choice somebody made is in this one table as strings: the ways of
     * getting around, the exclusions, the chair, the reminder switches, what was
     * said about the six places. Losing it loses all of that without losing a
     * single number, so the restore would look like it worked.
     */
    @Test
    fun everySettingComesBackWithItsValue() = onANewPhone { before, after ->
        val was = before.settings.associate { it.key to it.value }
        val now = after.settings.associate { it.key to it.value }
        assertThat(now).containsExactlyEntriesIn(was)
        assertThat(now["place_bathroom"]).isEqualTo(NOTHING)
        assertThat(now["remind_daily"]).isEqualTo("false")
    }

    /** Doing it twice is doing it once, so a second tap cannot double anything. */
    @Test
    fun restoringTheSameFileTwiceLeavesTheSameDatabase() = runTest {
        phone.fillEveryTable()
        val backup = BackupRepository(phone).read(VERSION, WRITTEN_ON)
        BackupRepository(newPhone).restore(backup)
        val once = BackupRepository(newPhone).read(VERSION, WRITTEN_ON)
        BackupRepository(newPhone).restore(backup)
        val twice = BackupRepository(newPhone).read(VERSION, WRITTEN_ON)
        assertThat(rowsIn(twice)).isEqualTo(rowsIn(once))
    }

    /**
     * Fill one, back it up, put it on an empty one, and hand both back.
     *
     * The second database is genuinely empty rather than the first one emptied,
     * because that is the case this feature is for: a phone that is gone and a new
     * one that has never seen any of it.
     */
    private fun onANewPhone(check: suspend (Backup, Backup) -> Unit) = runTest {
        phone.fillEveryTable()
        val before = BackupRepository(phone).read(VERSION, WRITTEN_ON)
        BackupRepository(newPhone).restore(before)
        check(before, BackupRepository(newPhone).read(VERSION, WRITTEN_ON))
    }

    private companion object {
        const val VERSION = "0.1.0"
        val WRITTEN_ON: LocalDate = LocalDate.of(2026, 9, 9)
    }
}

/**
 * A backup as its tables, each one a list of rows with every column spelled out.
 *
 * Through the file rather than through the objects on purpose. The file is what a
 * person keeps, so it is the thing that has to hold everything, and a column that
 * the row type has and the file does not would pass any comparison made of rows.
 */
fun rowsIn(backup: Backup): Map<String, JsonArray> =
    Json.parseToJsonElement(BackupFile.write(backup)).jsonObject
        .filterValues { it is JsonArray }
        .mapValues { (_, rows) -> rows.jsonArray }
