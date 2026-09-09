package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.data.entity.RunEntity
import com.kamsiob.steadyhealth.data.entity.RunMovementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * A backup taken while the app is still writing.
 *
 * Reading thirty-one tables is thirty-one queries, and unless they are held
 * together they are thirty-one different moments. A session is written and then
 * its movements are written, so a backup that read the sessions before that and
 * the movements after it carries movements belonging to a session it does not
 * have. Nothing is wrong with the file and nothing is wrong with the phone. The
 * damage appears later, on a different phone, as a session's worth of work with no
 * session, or worse, attached to whichever session ends up with that id.
 *
 * The window is small and the export is started by hand, but it is not closed by
 * anything: the reminder worker writes while the app is open, and reading the
 * photographed pages out of a full database is not quick. A backup is the one file
 * that has to be right about a moment, so it is taken as one.
 */
@RunWith(RobolectricTestRunner::class)
class BackupWhileUsingItTest {

    private val phone = aDatabase()

    @After
    fun close() = phone.close()

    @Test
    fun aBackupTakenWhileSomethingIsBeingWrittenIsWholeInItself() = runTest {
        phone.fillEveryTable()
        val writing = launch(Dispatchers.Default) { keepRecordingSessions() }

        val orphans = withContext(Dispatchers.Default) {
            (0 until TRIES).flatMap { movementsWithNoSession() }
        }

        writing.cancelAndJoin()
        assertWithMessage("movements in the backup whose session is not in it")
            .that(orphans)
            .isEmpty()
    }

    /** One session and then its movements, which is the order the app writes them. */
    private suspend fun keepRecordingSessions() {
        var day = 30_000L
        while (currentCoroutineContext().isActive) {
            val id = phone.runs().upsertRun(RunEntity(0, day++, 1L, 2L, "finished", null))
            phone.runs().upsertMovement(
                RunMovementEntity(0, id, "sit_to_stand", 10, 10, true, false, false),
            )
        }
    }

    private suspend fun movementsWithNoSession(): List<Long> {
        val backup = BackupRepository(phone).read(VERSION, WRITTEN_ON)
        return backup.runMovements.map { it.runId } - backup.runs.map { it.id }.toSet()
    }

    private companion object {
        const val TRIES = 60
        const val VERSION = "0.1.0"
        val WRITTEN_ON: LocalDate = LocalDate.of(2026, 9, 9)
    }
}
