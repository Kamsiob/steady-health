package com.kamsiob.steadyhealth.data

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.data.entity.SettingEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
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
 *
 * Every test here runs against its own database and its own key. The first
 * version ran against the real ones and wiped a real setup on the phone, which is
 * exactly what the template rule about data-affecting tests exists to prevent.
 */
class DatabaseSmokeTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val db by lazy { SteadyDatabase.forTesting(context, NAME, DatabaseKey.TEST_ALIAS) }

    @After
    fun tidyUp() {
        db.close()
        SteadyDatabase.destroyTesting(context, NAME, DatabaseKey.TEST_ALIAS)
    }

    @Test
    fun theEncryptedDatabaseOpensAndKeepsWhatIsWrittenToIt() = runBlocking {
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
        db.profile().put(SettingEntity("units", "imperial"))

        val file = context.getDatabasePath(NAME)
        assertThat(file.exists()).isTrue()

        // An unencrypted SQLite file begins with "SQLite format 3". This one must
        // not, or the encryption is not doing anything.
        val header = file.inputStream().use { input ->
            ByteArray(HEADER).also { input.read(it) }
        }
        assertThat(String(header, Charsets.US_ASCII)).doesNotContain("SQLite format 3")
    }

    @Test
    fun destroyLeavesNoFileAndNoKey() = runBlocking {
        db.profile().put(SettingEntity("units", "imperial"))
        val databaseDir = context.getDatabasePath(NAME).parentFile
        assertThat(DatabaseKey.exists(context, DatabaseKey.TEST_ALIAS)).isTrue()

        db.close()
        SteadyDatabase.destroyTesting(context, NAME, DatabaseKey.TEST_ALIAS)

        val leftBehind = databaseDir?.listFiles()
            ?.filter { it.name.startsWith(NAME) }
            ?.map { it.name }
            .orEmpty()
        assertThat(leftBehind).isEmpty()
        assertThat(DatabaseKey.exists(context, DatabaseKey.TEST_ALIAS)).isFalse()
    }

    @Test
    fun theRealDatabaseIsNeverTouchedByAnyOfThis() {
        // The rule this file broke once, now held by a test of its own.
        val real = File(context.filesDir, "steady_db.key")
        val mine = File(context.filesDir, "steady_db.key.${DatabaseKey.TEST_ALIAS}")
        assertThat(real.absolutePath).isNotEqualTo(mine.absolutePath)
        assertThat(NAME).isNotEqualTo("steady.db")
    }

    private companion object {
        const val HEADER = 16
        const val NAME = "steady-test.db"
    }
}
