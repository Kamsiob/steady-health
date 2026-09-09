package com.kamsiob.steadyhealth.backup

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.data.DatabaseKey
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.entity.CheckInEntity
import com.kamsiob.steadyhealth.data.entity.CheckInTagEntity
import com.kamsiob.steadyhealth.data.entity.DocumentEntity
import com.kamsiob.steadyhealth.data.entity.DocumentPageEntity
import com.kamsiob.steadyhealth.data.entity.SettingEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import java.time.LocalDate

/**
 * A backup goes into a real database and comes back out of it.
 *
 * The format is proved on a laptop; this is the half that cannot be. Restore
 * empties every table and refills it inside one write transaction, calling the
 * ordinary writes from inside that transaction, and whether Room hands those
 * writes the connection it is already holding is a question only a real database
 * can answer. The failure it would be hiding is a restore that waits forever.
 *
 * Ids are the other thing that can only be checked here. Every join in this
 * database is by id, and an id survives being written back only because Room's
 * insert keeps the one it is given. A day whose tags came back attached to
 * nothing would still look like a successful restore.
 *
 * Its own database and its own key, like every other data-affecting test here.
 */
class RestoreDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val db by lazy { SteadyDatabase.forTesting(context, NAME, DatabaseKey.TEST_ALIAS) }

    private val backups by lazy { BackupRepository(db) }

    @After
    fun tidyUp() {
        db.close()
        SteadyDatabase.destroyTesting(context, NAME, DatabaseKey.TEST_ALIAS)
    }

    @Test
    fun whatIsInTheBackupIsWhatIsInTheDatabaseAfterwards() = runBlocking {
        backups.restore(full())

        val back = backups.read("0.1.0", WRITTEN_ON)
        assertThat(back.weighIns).isEqualTo(full().weighIns)
        assertThat(back.checkIns).isEqualTo(full().checkIns)
        assertThat(back.checkInTags).isEqualTo(full().checkInTags)
        assertThat(back.settings).isEqualTo(full().settings)
        assertThat(back.documents).isEqualTo(full().documents)
    }

    @Test
    fun aDayKeepsItsTagsBecauseTheIdsComeBackToo() = runBlocking {
        backups.restore(full())

        val back = backups.read("0.1.0", WRITTEN_ON)
        assertThat(back.checkIns.single().id).isEqualTo(DAY)
        assertThat(back.checkInTags.map { it.checkInId }).containsExactly(DAY, DAY)
        assertThat(back.documentPages.single().documentId).isEqualTo(PAPER)
    }

    @Test
    fun aPhotographedPageComesBackByteForByte() = runBlocking {
        backups.restore(full())

        assertThat(backups.read("0.1.0", WRITTEN_ON).documentPages.single().image)
            .isEqualTo(IMAGE)
    }

    /**
     * Restore replaces; it never merges.
     *
     * A row that is in the app and not in the file has to be gone afterwards. If
     * it survived, the person would be looking at two histories joined by nothing
     * they agreed to.
     */
    @Test
    fun whatWasHereBeforeIsGone() = runBlocking {
        db.weighIns().upsert(WeighInEntity(19_000, 1L, 99.0, 99.0, "entered"))
        db.profile().put(SettingEntity("units", "imperial"))

        backups.restore(full())

        val back = backups.read("0.1.0", WRITTEN_ON)
        assertThat(back.weighIns.map { it.epochDay }).containsExactly(20_000L)
        assertThat(back.settings).isEqualTo(full().settings)
    }

    @Test
    fun anEmptyBackupLeavesAnEmptyApp() = runBlocking {
        backups.restore(full())

        backups.restore(backups.read("0.1.0", WRITTEN_ON).let { emptyOf(it) })

        val back = backups.read("0.1.0", WRITTEN_ON)
        assertThat(back.weighIns).isEmpty()
        assertThat(back.checkIns).isEmpty()
        assertThat(back.checkInTags).isEmpty()
        assertThat(back.documentPages).isEmpty()
        assertThat(back.settings).isEmpty()
    }

    /** The same backup with nothing in any of its tables. */
    private fun emptyOf(backup: Backup): Backup = backup.copy(
        weighIns = emptyList(),
        checkIns = emptyList(),
        checkInTags = emptyList(),
        documents = emptyList(),
        documentPages = emptyList(),
        settings = emptyList(),
    )

    /**
     * A backup with something in a few tables, built from an empty one.
     *
     * Reading the empty database first means this does not have to name all
     * thirty-one lists, and it cannot go stale when a table is added.
     */
    private fun full(): Backup = runBlocking {
        backups.read("0.1.0", WRITTEN_ON).copy(
            weighIns = listOf(WeighInEntity(20_000, 1_757_000_000_000, 82.4, 82.35, "scale")),
            checkIns = listOf(
                CheckInEntity(DAY, 20_000, 1_757_000_000_000, SAID, 15, "good", true),
            ),
            checkInTags = listOf(
                CheckInTagEntity(DAY, "sleep", true),
                CheckInTagEntity(DAY, "knees", false),
            ),
            documents = listOf(DocumentEntity(PAPER, 20_000, "letter", "", 1L)),
            documentPages = listOf(DocumentPageEntity(1, PAPER, 0, IMAGE, SAID)),
            settings = listOf(SettingEntity("units", "metric")),
        )
    }

    private companion object {
        const val NAME = "steady-restore-test.db"
        const val DAY = 7L
        const val PAPER = 3L
        const val SAID = "Walked to the shop, said \"not today\" to the hill,\nand went anyway 🙂"
        val IMAGE = byteArrayOf(0, 1, -1, 127, -128)
        val WRITTEN_ON: LocalDate = LocalDate.of(2026, 9, 9)
    }
}
