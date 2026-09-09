package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import org.junit.Test

/**
 * The test that makes forgetting a table impossible.
 *
 * DECISIONS.md already says what goes wrong without it: a table that arrives in a
 * later phase "is exactly the kind that gets left out of the export and is not
 * noticed until somebody tries to restore". Nobody notices, because everything
 * works. The person who notices is the one who has just lost their phone.
 *
 * So the backup is held against Room's own description of the database, the
 * schema files it writes at build time from `@Database`. Reflection cannot do
 * this: Room's annotations are kept only until the code is built, so at runtime
 * `SteadyDatabase` does not know it has entities. The schema files do, they are
 * generated rather than written by hand, and they are checked in.
 *
 * Add an entity to `SteadyDatabase` and every one of these fails.
 */
class BackupCompletenessTest {

    private val schema = Schemas.newest()

    /** The lists in the backup, by the table name each one is written under. */
    private val rows: Map<String, SerialDescriptor> = Backup.serializer().descriptor.let { shape ->
        (0 until shape.elementsCount)
            .filter { shape.getElementDescriptor(it).kind == StructureKind.LIST }
            .associate { shape.getElementName(it) to shape.getElementDescriptor(it) }
            .mapValues { (_, list) -> list.getElementDescriptor(0) }
    }

    @Test
    fun theBackupCarriesEveryTableInTheDatabase() {
        assertThat(rows.keys).containsExactlyElementsIn(schema.tables.keys)
    }

    @Test
    fun restorePutsEveryTableBack() {
        assertThat(BackupTables.all.map { it.name })
            .containsExactlyElementsIn(schema.tables.keys)
    }

    @Test
    fun everyTableIsPutBackOnce() {
        val names = BackupTables.all.map { it.name }
        assertThat(names).hasSize(names.toSet().size)
    }

    @Test
    fun everyColumnOfEveryTableIsInTheBackup() {
        schema.tables.forEach { (table, columns) ->
            val row = checkNotNull(rows[table]) { "nothing in the backup for $table" }
            val carried = (0 until row.elementsCount).map { row.getElementName(it) }
            assertWithMessage(table).that(carried).containsExactlyElementsIn(columns)
        }
    }

    /**
     * The number written into every backup is the number the database is at.
     *
     * Nothing at runtime can read the version off `@Database`, so the backup keeps
     * its own copy of it, and a copy is a thing that drifts. Room names its schema
     * files after that same annotation, so this is the check that the two agree.
     */
    @Test
    fun theSchemaVersionInTheBackupIsTheSchemaVersionOfTheDatabase() {
        assertThat(Backup.SCHEMA_VERSION).isEqualTo(schema.version)
    }

    /**
     * Anything added to a table after it existed has to be optional here.
     *
     * This is what makes an older backup restorable. A file written before a column
     * existed does not carry it, so the row type has to be able to say what that
     * column means when it is absent, which in Kotlin is a default value. Without a
     * default, decoding an older file throws and the backup is refused as damaged,
     * which is exactly the case a backup exists for.
     */
    @Test
    fun aColumnAddedLaterCanBeMissingFromAnOlderBackup() {
        val older = Schemas.all().filter { it.version < schema.version }
        older.forEach { was ->
            was.tables.forEach { (table, columns) ->
                val added = schema.tables[table].orEmpty() - columns.toSet()
                added.forEach { column ->
                    assertWithMessage("$table.$column, added since version ${was.version}")
                        .that(optional(rows.getValue(table), column))
                        .isTrue()
                }
            }
        }
    }

    private fun optional(row: SerialDescriptor, column: String): Boolean =
        (0 until row.elementsCount)
            .filter { row.getElementName(it) == column }
            .any { row.isElementOptional(it) }
}
