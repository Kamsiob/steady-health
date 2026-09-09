package com.kamsiob.steadyhealth.backup

import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * What came back when the app looked at a file somebody chose.
 *
 * Four answers and no exceptions, because every one of them is something the
 * screen has to say out loud in a sentence, and a thrown error is not a sentence.
 */
sealed interface BackupRead {

    /** Ours, this build can store it, and it parsed. */
    data class Ready(val backup: Backup) : BackupRead

    /** Nothing in it says it came from this app. */
    data object NotOurs : BackupRead

    /**
     * Written by a later version of Steady Health.
     *
     * Refused rather than read as far as it goes. A later version can have a
     * column this build has nowhere to put, and putting back most of somebody's
     * history while quietly dropping the rest is worse than putting back none of
     * it, because only one of those two is visible.
     */
    data class FromLater(val schema: Int) : BackupRead

    /** Ours, and damaged. */
    data object Damaged : BackupRead
}

/**
 * The backup file: writing one, and deciding whether one can be read.
 *
 * The file lives inside the export zip beside the spreadsheets, so this reads a
 * zip as well as a bare file. Somebody who unzipped the export and picked the
 * backup out of it should not be told they picked the wrong thing.
 */
object BackupFile {

    /**
     * Defaults are written out rather than left implied, because the file is the
     * record: a column that is missing from it should mean the version that wrote
     * it did not have that column, and nothing else.
     *
     * Unknown keys are allowed through. Within one schema version there is nothing
     * an unknown key can be except somebody's edit, and the case that matters, a
     * file from a later version carrying a column this build cannot store, is
     * refused by its header long before this.
     */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun write(backup: Backup): String = json.encodeToString(Backup.serializer(), backup)

    /**
     * Read a file somebody picked, and never throw.
     *
     * The header is read on its own first. A file that does not say it is ours is
     * turned away by name, which is both faster and kinder than a parser failing
     * two thousand rows in and the app saying something about JSON.
     */
    fun read(bytes: ByteArray): BackupRead {
        val text = textIn(bytes) ?: return BackupRead.NotOurs
        val root = parse(text) ?: return unreadable(text)
        val header = header(root) ?: return unreadable(text)
        return when {
            header.app != Backup.MARK -> BackupRead.NotOurs
            header.schema > Backup.SCHEMA_VERSION -> BackupRead.FromLater(header.schema)
            else -> rows(root)
        }
    }

    private fun parse(text: String): JsonObject? =
        runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()

    private fun header(root: JsonObject): BackupHeader? = runCatching {
        root["header"]?.let { json.decodeFromJsonElement(BackupHeader.serializer(), it) }
    }.getOrNull()

    /**
     * A file that would not parse, named as plainly as we can name it.
     *
     * If our own word is in there then it is one of ours and it is damaged. If it
     * is not, it was never ours, and saying so is more use to somebody who picked
     * the wrong file than telling them their file is broken.
     */
    private fun unreadable(text: String): BackupRead =
        if (text.contains(Backup.MARK)) BackupRead.Damaged else BackupRead.NotOurs

    private fun rows(root: JsonObject): BackupRead =
        runCatching { json.decodeFromJsonElement(Backup.serializer(), withEveryTable(root)) }
            .fold({ BackupRead.Ready(it) }, { BackupRead.Damaged })

    /**
     * Every table this build knows about, whether or not the file has it.
     *
     * A backup from an older version has no list for a table that did not exist
     * yet, and that is not damage: it is a file from before the table. Those come
     * back empty, which is what an app that had never seen that table had in it.
     */
    private fun withEveryTable(root: JsonObject): JsonObject {
        val shape = Backup.serializer().descriptor
        val missing = (0 until shape.elementsCount)
            .filter { shape.getElementDescriptor(it).kind == StructureKind.LIST }
            .map { shape.getElementName(it) }
            .filter { it !in root }
            .associateWith { JsonArray(emptyList()) }
        return JsonObject(root + missing)
    }

    /**
     * The text of the backup, out of whatever somebody picked.
     *
     * Two bytes decide it: every zip starts PK. Anything else is treated as the
     * file itself, so both the export and the backup taken out of it work.
     */
    private fun textIn(bytes: ByteArray): String? = when {
        bytes.size < 2 -> null
        bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() -> inZip(bytes)
        else -> bytes.decodeToString()
    }

    /**
     * The backup out of a zip, by its name and not by where it sits.
     *
     * Somebody who unzipped the export and zipped the folder back up has the same
     * file one level down. That is the same backup and there is no reason to
     * refuse it.
     */
    private fun inZip(bytes: ByteArray): String? = runCatching {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }
                .firstOrNull { it.name.substringAfterLast('/') == Backup.FILE_NAME }
                ?.let { zip.readBytes().decodeToString() }
        }
    }.getOrNull()
}
