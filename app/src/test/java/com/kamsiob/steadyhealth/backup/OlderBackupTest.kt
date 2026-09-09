package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test

/**
 * A backup from an older version of the app still restores.
 *
 * That is the whole point of having one. The file somebody needs is the one they
 * took before the phone went in the river, and it was written by whatever version
 * they had then.
 *
 * What an older file cannot carry is anything added since, and this is the
 * decision, written down where it is tested. Room's own migrations do not apply
 * here: they turn a database on disk into a newer one, and this is not a database,
 * it is a file. So a table added since comes back empty, and a column added since
 * comes back as the value a new row would get, which is the property's default in
 * the entity. That is not a guess. It is exactly what Room's own auto migration put
 * into the rows that were already on the phone when the column arrived, so the
 * restored rows and the migrated rows say the same thing.
 *
 * The older file here is built from the schema Room wrote for version 1, rather
 * than from a fixture typed by hand, so it stays honest about what version 1 was.
 */
class OlderBackupTest {

    private val newest = Schemas.newest()
    private val first = Schemas.all().first()
    private val ours = BackupFile.write(everything())
    private val read = BackupFile.read(asVersion(first).toByteArray())
    private val restored = (read as BackupRead.Ready).backup

    @Test
    fun anOlderBackupIsAccepted() {
        assertThat(read).isInstanceOf(BackupRead.Ready::class.java)
    }

    @Test
    fun itSaysWhichVersionItCameFrom() {
        assertThat(restored.header.schema).isEqualTo(first.version)
    }

    @Test
    fun aTableThatExistedThenComesBackWhole() {
        val unchanged = first.tables.filterKeys { newest.tables[it] == first.tables[it] }
        assertThat(unchanged).isNotEmpty()
        unchanged.keys.forEach { table ->
            assertWithMessage(table).that(rowsOf(table, restored))
                .isEqualTo(rowsOf(table, everything()))
        }
    }

    @Test
    fun aTableAddedSinceComesBackEmpty() {
        val added = newest.tables.keys - first.tables.keys
        assertThat(added).isNotEmpty()
        added.forEach { table ->
            assertWithMessage(table).that(rowsOf(table, restored)).isEmpty()
        }
    }

    /**
     * The one column added since version 1, and what becomes of it.
     *
     * `item_ratings.sureness` arrived with the second monthly question. An older
     * file has ratings without it, and they come back with it null, which is what
     * null means everywhere else in this app: the question was not put. It is not
     * zero. Zero would be somebody saying they felt not at all sure.
     */
    @Test
    fun aColumnAddedSinceComesBackAsTheValueANewRowWouldGet() {
        val added = newest.tables.getValue("item_ratings") - first.tables.getValue("item_ratings")
        assertThat(added).containsExactly("sureness")
        assertThat(restored.itemRatings.map { it.sureness }).containsExactly(null, null)
        assertThat(restored.itemRatings.map { it.rating })
            .isEqualTo(everything().itemRatings.map { it.rating })
    }

    private fun rowsOf(table: String, backup: Backup): List<Any> =
        BackupTables.all.first { it.name == table }.rows(backup)

    /** The same backup as this app writes, cut back to what that version knew about. */
    private fun asVersion(was: Schema): String {
        val root = Json.parseToJsonElement(ours).jsonObject
        val header = root.getValue("header").jsonObject + ("schema" to JsonPrimitive(was.version))
        val tables = was.tables.mapValues { (table, columns) ->
            JsonArray(
                root.getValue(table).jsonArray.map { row ->
                    JsonObject(row.jsonObject.filterKeys { it in columns })
                },
            )
        }
        return JsonObject(tables + ("header" to JsonObject(header))).toString()
    }
}
