package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.data.entity.MeasureResultEntity
import com.kamsiob.steadyhealth.data.entity.WeighInEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * A restore that does not finish.
 *
 * This is the worst thing this feature can do, and it is what it does by default
 * unless it was written against it. Putting a backup back means emptying every
 * table first. Empty them, then fail, and the person has neither what they had nor
 * what was in the file, and there is nowhere else to get it from, because the whole
 * promise of this app is that there is no copy anywhere else.
 *
 * A restore can stop part way for reasons nobody controls: the phone runs out of
 * room, the file turns out to hold a row this database will not take, the app is
 * killed while it is working. All of them have to come out the same way, which is
 * that nothing happened.
 *
 * The backup used here is an empty one with a single impossible row bolted on, and
 * that is deliberate. A backup holding the same rows the phone already has would
 * hide the failure this test is for: the tables emptied before the failure would be
 * refilled with what was in them anyway, and a restore with no transaction at all
 * would look like it had done no harm.
 */
@RunWith(RobolectricTestRunner::class)
class RestoreInterruptedTest {

    private val phone = aDatabase()
    private val nothing = aDatabase()

    @After
    fun close() {
        phone.close()
        nothing.close()
    }

    /**
     * A row the database refuses, in a table half way down the list.
     *
     * SQLite has no not-a-number, so a value bound as one is stored as null, and the
     * column will not take a null. That makes a row that reads back out of a file
     * perfectly well and cannot be written, which is the shape of the real failures:
     * something the file says that the database will not have.
     *
     * Measures are the twelfth of thirty-one tables, so by the time it fails the
     * weigh-ins, the waist, the blood pressure, the photos, the days, the tags, the
     * sessions, their movements and the sore areas have all been emptied.
     */
    @Test
    fun aRestoreThatFailsPartWayThroughChangesNothing() = runTest {
        phone.fillEveryTable()
        val before = rowsIn(BackupRepository(phone).read(VERSION, WRITTEN_ON))

        val what = runCatching { BackupRepository(phone).restore(anEmptyOneThatCannotBeWritten()) }

        assertThat(what.exceptionOrNull()).isNotNull()
        assertThat(rowsIn(BackupRepository(phone).read(VERSION, WRITTEN_ON))).isEqualTo(before)
    }

    /**
     * The tables emptied before the failure, named one at a time.
     *
     * Kept separate from the comparison above so that a failure here says which
     * history went rather than that one large map is not equal to another.
     */
    @Test
    fun theTablesEmptiedBeforeTheFailureAreStillFull() = runTest {
        phone.fillEveryTable()

        runCatching { BackupRepository(phone).restore(anEmptyOneThatCannotBeWritten()) }

        assertThat(phone.weighIns().allOnce()).hasSize(2)
        assertThat(phone.checkIns().allOnce()).hasSize(2)
        assertThat(phone.checkIns().allTagsOnce()).hasSize(3)
        assertThat(phone.sessions().allOnce()).hasSize(2)
        assertThat(phone.runs().allOnce()).hasSize(1)
        assertThat(phone.runs().allMovementsOnce()).hasSize(2)
        assertThat(phone.body().allWaist()).hasSize(1)
        assertThat(phone.body().allBloodPressure()).hasSize(2)
    }

    /**
     * The tables after the failure are untouched as well.
     *
     * Half a restore is not only about what was emptied. If the earlier tables came
     * from the file and the later ones stayed as they were, the database would hold
     * two different histories with nothing to say which row came from where, and
     * every screen would read it as one.
     */
    @Test
    fun theTablesAfterTheFailureAreUntouchedAsWell() = runTest {
        phone.fillEveryTable()

        runCatching { BackupRepository(phone).restore(anEmptyOneThatCannotBeWritten()) }

        assertThat(phone.profile().allOnce()).hasSize(15)
        assertThat(phone.profile().exclusionsOnce()).hasSize(2)
        assertThat(phone.plans().all()).hasSize(1)
        assertThat(phone.documents().all()).hasSize(1)
        assertThat(phone.notices().all()).hasSize(2)
        assertThat(phone.abilities().allRatingsOnce()).hasSize(3)
    }

    /** The other way round: a full file that fails, onto a phone with nothing on it. */
    @Test
    fun aFailedRestoreOntoAnEmptyPhoneLeavesItEmpty() = runTest {
        phone.fillEveryTable()
        val full = BackupRepository(phone).read(VERSION, WRITTEN_ON)

        runCatching { BackupRepository(nothing).restore(withAnImpossibleMeasure(full)) }

        val after = rowsIn(BackupRepository(nothing).read(VERSION, WRITTEN_ON))
        assertThat(after.filterValues { it.isNotEmpty() }).isEmpty()
    }

    /**
     * The app is killed in the middle of it.
     *
     * The likeliest interruption of the lot, and the one that is not an error
     * anywhere: somebody starts a restore and then leaves, or the system takes the
     * app back while it is working. The screen's own coroutine is cancelled, and
     * cancelling in the middle of emptying thirty-one tables has to leave either
     * everything or nothing.
     *
     * Whichever side of the commit the cancellation lands on, the answer below is
     * one of two whole states. That is what makes this a test rather than a race: a
     * restore that is neither is the failure, and it fails the same way every time
     * it happens.
     */
    @Test
    fun aRestoreCancelledPartWayThroughIsAllOrNothing() = runTest {
        phone.fillEveryTable()
        val before = rowsIn(BackupRepository(phone).read(VERSION, WRITTEN_ON))
        val long = aLongOne()

        val job = launch(Dispatchers.Default) { BackupRepository(phone).restore(long) }
        withContext(Dispatchers.Default) { delay(A_MOMENT) }
        job.cancelAndJoin()

        val after = rowsIn(BackupRepository(phone).read(VERSION, WRITTEN_ON))
        assertWithMessage("neither what was here nor what was in the file")
            .that(after == before || after == rowsIn(long))
            .isTrue()
    }

    /**
     * Every table empty afterwards, when the file says every table is empty.
     *
     * A backup taken before somebody had entered anything is still a backup, and
     * putting it back means what it says. It is also the one restore that shows
     * whether every table is emptied, because a table whose rows were not cleared
     * would be the only thing left standing.
     */
    @Test
    fun restoringAnEmptyBackupEmptiesEveryTable() = runTest {
        phone.fillEveryTable()

        BackupRepository(phone).restore(BackupRepository(nothing).read(VERSION, WRITTEN_ON))

        val after = rowsIn(BackupRepository(phone).read(VERSION, WRITTEN_ON))
        assertThat(after.filterValues { it.isNotEmpty() }).isEmpty()
    }

    /** Enough rows that cancelling lands somewhere in the middle of writing them. */
    private suspend fun aLongOne(): Backup =
        BackupRepository(nothing).read(VERSION, WRITTEN_ON).copy(
            weighIns = (0 until MANY).map {
                WeighInEntity(30_000L + it, it.toLong(), 80.0, 80.0, "scale")
            },
        )

    /** An empty backup that cannot be put back, so it can only wipe or do nothing. */
    private suspend fun anEmptyOneThatCannotBeWritten(): Backup =
        withAnImpossibleMeasure(BackupRepository(nothing).read(VERSION, WRITTEN_ON))

    private fun withAnImpossibleMeasure(backup: Backup): Backup = backup.copy(
        measureResults = backup.measureResults +
            MeasureResultEntity(
                0, null, "chair_stand", "strength", 20_050, 1L, Double.NaN, "phone",
            ),
    )

    private companion object {
        const val MANY = 6_000
        const val A_MOMENT = 40L
        const val VERSION = "0.1.0"
        val WRITTEN_ON: LocalDate = LocalDate.of(2026, 9, 9)
    }
}
