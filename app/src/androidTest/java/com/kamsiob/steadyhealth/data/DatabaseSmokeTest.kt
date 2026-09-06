package com.kamsiob.steadyhealth.data

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.data.entity.SettingEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

/**
 * The database opens, holds a row, and can be destroyed completely.
 *
 * This is the Phase 0 gate for the data layer, and it has to run on a device
 * because the parts that can fail are the parts a JVM test cannot reach: the
 * Android Keystore, the StrongBox fallback, and SQLCipher's native library.
 *
 * The last test is the one worth having. "Gone. There is no copy anywhere else."
 * is on a screen, and a delete that leaves the key in the Keystore or a stray
 * write-ahead log beside the database is tidy rather than complete.
 */
class DatabaseSmokeTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theEncryptedDatabaseOpensAndKeepsWhatIsWrittenToIt() = runBlocking {
        val db = SteadyDatabase.get(context)

        db.weighIns().upsert(
            WeighInEntity(
                epochDay = 20_000,
                recordedAt = 1_700_000_000_000,
                rawKg = 80.0,
                smoothedKg = 80.0,
                source = "entered",
            ),
        )

        assertThat(db.weighIns().forDay(20_000)?.smoothedKg).isEqualTo(80.0)
    }

    @Test
    fun theFileOnDiskIsNotReadableAsPlainSqlite() = runBlocking {
        SteadyDatabase.get(context).profile().put(SettingEntity("units", "imperial"))

        val file = context.getDatabasePath("steady.db")
        assertThat(file.exists()).isTrue()

        // An unencrypted SQLite file begins with "SQLite format 3". This one must
        // not, or the encryption is not doing anything.
        val header = file.inputStream().use { input ->
            ByteArray(HEADER).also { input.read(it) }
        }
        assertThat(String(header, Charsets.US_ASCII)).doesNotContain("SQLite format 3")
    }

    @Test
    fun destroyLeavesNoFileAndNoKey() {
        SteadyDatabase.get(context)
        val databaseDir = context.getDatabasePath("steady.db").parentFile
        val keyFile = File(context.filesDir, "steady_db.key")
        assertThat(keyFile.exists()).isTrue()

        SteadyDatabase.destroy(context)

        val leftBehind = databaseDir?.listFiles()
            ?.filter { it.name.startsWith("steady.db") }
            ?.map { it.name }
            .orEmpty()
        assertThat(leftBehind).isEmpty()
        assertThat(keyFile.exists()).isFalse()
    }

    private companion object {
        const val HEADER = 16
    }
}
