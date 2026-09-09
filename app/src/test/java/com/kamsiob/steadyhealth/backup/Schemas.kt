package com.kamsiob.steadyhealth.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** One version of the database, as Room wrote it down. */
data class Schema(val version: Int, val tables: Map<String, List<String>>)

/**
 * The schema files Room writes at build time, read back.
 *
 * Room's own annotations are dropped from the built code, so nothing can ask the
 * database at runtime what tables it has. These files can, they are generated from
 * the same `@Database` the app runs on, and they are checked in, so they are the
 * one description of this database that a test on a laptop can hold the backup
 * against.
 */
object Schemas {

    private val json = Json { ignoreUnknownKeys = true }

    fun all(): List<Schema> = folder().listFiles().orEmpty()
        .filter { it.name.endsWith(".json") }
        .map { read(it) }
        .sortedBy { it.version }

    fun newest(): Schema = all().maxBy { it.version }

    private fun read(file: File): Schema {
        val database = json.parseToJsonElement(file.readText()).jsonObject
            .getValue("database").jsonObject
        return Schema(
            version = database.getValue("version").jsonPrimitive.content.toInt(),
            tables = database.getValue("entities").jsonArray.associate { entity ->
                entity.jsonObject.getValue("tableName").jsonPrimitive.content to columns(
                    entity.jsonObject,
                )
            },
        )
    }

    private fun columns(entity: JsonObject): List<String> =
        entity.getValue("fields").jsonArray.map {
            it.jsonObject.getValue("columnName").jsonPrimitive.content
        }

    /**
     * Where the schema files are, whichever directory the tests were started from.
     *
     * It fails rather than passing quietly when it cannot find them: a test that
     * skips itself when it cannot see the thing it is checking is worse than no
     * test, because it reports green.
     */
    private fun folder(): File {
        val here = listOf("schemas", "app/schemas", "../app/schemas").map(::File)
        val root = here.firstOrNull { it.isDirectory }
        checkNotNull(root) { "No schemas directory beside ${File("").absolutePath}" }
        return File(root, "com.kamsiob.steadyhealth.data.SteadyDatabase")
    }
}
