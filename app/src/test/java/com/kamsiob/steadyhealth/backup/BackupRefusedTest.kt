package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Test

/**
 * Everything the app says no to, and nothing thrown at anybody.
 *
 * Each of these ends in a sentence on a screen, so each of them has to be a value
 * the screen can read rather than an exception somewhere underneath it.
 */
class BackupRefusedTest {

    private val ours = BackupFile.write(everything())

    /**
     * A backup from a later version is refused outright.
     *
     * A later version can have a column this one has nowhere to put. Reading it
     * anyway would put back most of a person's history and drop the rest, and the
     * dropped part is invisible: they would find out by noticing something missing
     * months later, if ever. Refusing is visible immediately and is recoverable by
     * updating the app.
     */
    @Test
    fun aBackupFromALaterVersionIsRefused() {
        val later = withSchema(Backup.SCHEMA_VERSION + 1)
        assertThat(BackupFile.read(later.toByteArray()))
            .isEqualTo(BackupRead.FromLater(Backup.SCHEMA_VERSION + 1))
    }

    @Test
    fun aBackupFromThisVersionIsNotRefused() {
        assertThat(BackupFile.read(ours.toByteArray())).isInstanceOf(BackupRead.Ready::class.java)
    }

    @Test
    fun anEmptyFileIsRefused() {
        assertThat(BackupFile.read(ByteArray(0))).isEqualTo(BackupRead.NotOurs)
    }

    @Test
    fun aFileThatIsNotJsonAtAllIsRefused() {
        assertThat(BackupFile.read("hello".toByteArray())).isEqualTo(BackupRead.NotOurs)
    }

    @Test
    fun somebodyElsesJsonIsRefused() {
        val other = """{"name":"a different app","rows":[1,2,3]}"""
        assertThat(BackupFile.read(other.toByteArray())).isEqualTo(BackupRead.NotOurs)
    }

    @Test
    fun oneOfOursCutInHalfIsRefusedAsDamaged() {
        val half = ours.take(ours.length / 2)
        assertThat(BackupFile.read(half.toByteArray())).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun oneOfOursWithARowOfTheWrongShapeIsRefusedAsDamaged() {
        val bent = ours.replace("\"rawKg\":82.4", "\"rawKg\":\"eighty two\"")
        assertThat(BackupFile.read(bent.toByteArray())).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun anExportWithNoBackupInItIsRefused() {
        val zip = zipOf("days.csv" to "date,what_you_said\n2026-09-09,Walked")
        assertThat(BackupFile.read(zip)).isEqualTo(BackupRead.NotOurs)
    }

    @Test
    fun anEmptyZipIsRefused() {
        assertThat(BackupFile.read(zipOf())).isEqualTo(BackupRead.NotOurs)
    }

    @Test
    fun aZipThatIsNotAZipIsRefused() {
        val nonsense = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4, 9, 9, 9)
        assertThat(BackupFile.read(nonsense)).isEqualTo(BackupRead.NotOurs)
    }

    private fun withSchema(version: Int): String {
        val root = Json.parseToJsonElement(ours).jsonObject
        val header = root.getValue("header").jsonObject + ("schema" to JsonPrimitive(version))
        return JsonObject(root + ("header" to JsonObject(header))).toString()
    }
}
