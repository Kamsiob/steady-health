package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.data.entity.CheckInEntity
import com.kamsiob.steadyhealth.data.entity.RunEntity
import com.kamsiob.steadyhealth.data.entity.SettingEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * A backup put back onto a phone that is already being used.
 *
 * This is the ordinary case, not the new phone: somebody has the app, something
 * has gone wrong with it, and they put back the copy they kept. The screen says
 * everything here will be replaced by what is in the file, so that is what has to
 * happen. Anything of theirs that survives is a day they did not have, a session
 * they did not do or a setting they turned off and finds turned on, and none of
 * those announces itself.
 *
 * The rows in the file share their keys with the rows already here, which is the
 * other half of it: writing them has to overwrite rather than fail or double.
 */
@RunWith(RobolectricTestRunner::class)
class RestoreOverTheTopTest {

    private val phone = aDatabase()

    @After
    fun close() = phone.close()

    @Test
    fun nothingThatIsHereNowSurvivesARestore() = runTest {
        val ids = phone.fillEveryTable()
        val kept = BackupRepository(phone).read(VERSION, WRITTEN_ON)
        val since = sinceTheBackup(ids.run)

        BackupRepository(phone).restore(kept)

        assertThat(phone.runs().allOnce().map { it.id }).doesNotContain(since)
        assertThat(phone.checkIns().allOnce().map { it.epochDay }).doesNotContain(NEW_DAY)
        assertThat(phone.profile().get("remind_walk")).isNull()
        val after = BackupRepository(phone).read(VERSION, WRITTEN_ON)
        assertThat(rowsIn(after)).isEqualTo(rowsIn(kept))
    }

    /**
     * A row that is in both, changed here since, comes back as the file has it.
     *
     * The keys collide on every row of a restore onto a used phone, so this is the
     * write that actually happens thirty-one times over. If it did nothing on a
     * collision, a restore would leave every changed row as it was and only add the
     * missing ones, which is a merge, and a merge is the one outcome nobody can
     * describe in a sentence beforehand.
     */
    @Test
    fun aRowChangedSinceTheBackupComesBackAsTheBackupHasIt() = runTest {
        val ids = phone.fillEveryTable()
        val kept = BackupRepository(phone).read(VERSION, WRITTEN_ON)
        val day = phone.checkIns().allOnce().first { it.id == ids.checkIn }
        phone.checkIns().upsert(day.copy(sentence = "written over", sleepHalfHours = 2))
        phone.profile().put(SettingEntity("chair", "the other one"))

        BackupRepository(phone).restore(kept)

        val back = phone.checkIns().allOnce().first { it.id == ids.checkIn }
        assertThat(back.sentence).isEqualTo(SENTENCE)
        assertThat(back.sleepHalfHours).isEqualTo(15)
        assertThat(phone.profile().get("chair")).isEqualTo("kitchen")
    }

    /** Nothing is doubled, which an insert that ignored the collision would do. */
    @Test
    fun nothingIsThereTwiceAfterwards() = runTest {
        phone.fillEveryTable()
        val kept = BackupRepository(phone).read(VERSION, WRITTEN_ON)

        BackupRepository(phone).restore(kept)

        val after = BackupRepository(phone).read(VERSION, WRITTEN_ON)
        rowsIn(after).forEach { (table, rows) ->
            assertThat(rows.size).isEqualTo(rowsIn(kept).getValue(table).size)
        }
    }

    /** Two days of use after the backup was taken, so there is something to lose. */
    private suspend fun sinceTheBackup(run: Long): Long {
        phone.checkIns().upsert(CheckInEntity(0, NEW_DAY, 99L, "after the backup", 14, "ok", false))
        phone.profile().put(SettingEntity("remind_walk", "true"))
        return phone.runs().upsertRun(RunEntity(run + 1, NEW_DAY, 99L, 100L, "finished", "easy"))
    }

    private companion object {
        const val NEW_DAY = 20_100L
        const val VERSION = "0.1.0"
        val WRITTEN_ON: LocalDate = LocalDate.of(2026, 9, 9)
    }
}
