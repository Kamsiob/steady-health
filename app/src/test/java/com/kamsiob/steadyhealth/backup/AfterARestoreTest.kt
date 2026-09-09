package com.kamsiob.steadyhealth.backup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.data.entity.CheckInEntity
import com.kamsiob.steadyhealth.data.entity.RunEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * The day after, when somebody uses the app again.
 *
 * A restore puts back rows with the ids they had, which is what keeps a session
 * attached to its movements. The next thing somebody does is record a new one, and
 * the database hands out the next id for it. If that id were one a restored row
 * already has, the write would land on top of a real session and take its
 * movements with it, and there would be nothing to see: the count would be right,
 * the date would be today's, and a walk from last spring would be gone.
 *
 * SQLite is what stops that, because these keys are AUTOINCREMENT and the highest
 * ever handed out survives emptying the table. That is a property of the schema
 * rather than of the restore, which is exactly why it is worth a test: nothing in
 * the backup code would notice if it changed.
 */
@RunWith(RobolectricTestRunner::class)
class AfterARestoreTest {

    private val phone = aDatabase()
    private val newPhone = aDatabase()

    @After
    fun close() {
        phone.close()
        newPhone.close()
    }

    @Test
    fun aSessionRecordedAfterARestoreDoesNotLandOnARestoredOne() = runTest {
        val ids = phone.fillEveryTable()
        BackupRepository(newPhone).restore(BackupRepository(phone).read(VERSION, WRITTEN_ON))

        val next = newPhone.runs().upsertRun(RunEntity(0, TODAY, 1L, 2L, "finished", "easy"))

        assertThat(next).isNotEqualTo(ids.run)
        assertThat(newPhone.runs().movementsFor(ids.run)).hasSize(2)
        assertThat(newPhone.runs().allOnce().map { it.id }).containsExactly(ids.run, next)
    }

    @Test
    fun aDayWrittenAfterARestoreDoesNotLandOnARestoredOne() = runTest {
        val ids = phone.fillEveryTable()
        BackupRepository(newPhone).restore(BackupRepository(phone).read(VERSION, WRITTEN_ON))

        val today = CheckInEntity(0, TODAY, 1L, "today", null, null, false)
        val next = newPhone.checkIns().upsert(today)

        assertThat(next).isNotEqualTo(ids.checkIn)
        assertThat(newPhone.checkIns().tagsFor(ids.checkIn)).hasSize(2)
        assertThat(newPhone.checkIns().allOnce().first { it.id == ids.checkIn }.sentence)
            .isEqualTo(SENTENCE)
    }

    /**
     * The other direction, which is the one that looks dangerous.
     *
     * A phone that has been used for a year has ids far above anything in an old
     * backup. Putting that backup back empties the tables, and the next id has to
     * carry on from where the phone had got to rather than start again at the
     * bottom of a range the restored rows are sitting in.
     */
    @Test
    fun aPhoneWithHigherIdsThanTheBackupCarriesOnAboveThem() = runTest {
        phone.fillEveryTable()
        val old = BackupRepository(phone).read(VERSION, WRITTEN_ON)
        repeat(A_YEAR_OR_SO) {
            phone.runs().upsertRun(RunEntity(0, TODAY + it, 1L, 2L, "finished", null))
        }

        BackupRepository(phone).restore(old)
        val next = phone.runs().upsertRun(RunEntity(0, TODAY, 1L, 2L, "finished", "easy"))

        assertThat(phone.runs().allOnce().map { it.id }).containsExactly(old.runs.single().id, next)
        assertThat(phone.runs().movementsFor(old.runs.single().id)).hasSize(2)
    }

    /**
     * A screen watching the database sees what was put back.
     *
     * Restore empties the tables with plain SQL rather than through the queries,
     * and what tells an open screen that its rows have changed is a trigger Room
     * puts on the table rather than the call that changed them. If that ever came
     * apart, a restore would work and every open screen would go on showing what
     * was there before, which somebody would read as the restore having done
     * nothing, and the reasonable next thing to try is deleting everything.
     */
    @Test
    fun aScreenWatchingTheDatabaseSeesWhatWasPutBack() = runTest {
        phone.fillEveryTable()
        val backup = BackupRepository(phone).read(VERSION, WRITTEN_ON)

        newPhone.weighIns().all().test {
            assertThat(awaitItem()).isEmpty()
            BackupRepository(newPhone).restore(backup)
            assertThat(awaitItem()).hasSize(2)
        }
    }

    private companion object {
        const val TODAY = 20_700L
        const val A_YEAR_OR_SO = 40
        const val VERSION = "0.1.0"
        val WRITTEN_ON: LocalDate = LocalDate.of(2026, 9, 9)
    }
}
