package com.kamsiob.steadyhealth.erase

import android.content.Context
import androidx.work.WorkManager
import androidx.work.await
import com.kamsiob.steadyhealth.data.SteadyDatabase
import java.io.File

/**
 * Everything gone, and nothing left anywhere else.
 *
 * PRIVACY.md says "Deletion is immediate and complete, and there is no copy
 * anywhere else to delete." The database was never the hard part of that sentence.
 * The hard part is everywhere the app has put a copy of the same rows on the way
 * to somewhere else, because each one is a file somebody thinks they deleted.
 *
 * There are four such places and they are all here.
 *
 * The rows, through [SteadyDatabase.clearAllTables], which Room generates from the
 * same `@Database` the app runs on. It used to be a hand-written list of eighteen
 * calls, which covered twenty of the thirty-one tables and read like a complete
 * list. Waist, blood pressure, photographs, documents and their pages, plans and
 * their lines, visit summaries, experiments, reminders and daily prompts were all
 * in the database and none of them were in that list. A generated call cannot be
 * short by eleven tables.
 *
 * The cache, which is where every file this app has ever handed to the share sheet
 * still is: the export zip with the whole history in it, the summary, the page
 * for a therapist, the card. A person who exported in March, deleted in June and
 * believed the sentence would have left their entire history sitting in
 * `cache/shared/steady-health-export.zip` in plain CSV. That is the worst of these
 * by a distance, because it is unencrypted, it is complete, and nothing was ever
 * going to remove it except the system reclaiming space at some unknowable time.
 *
 * The jobs, because a daily reminder that keeps waking a phone after somebody has
 * deleted everything is the app still being there. Cancelled and pruned rather
 * than left to fail quietly on an empty database.
 *
 * The file and the key, last, through [SteadyDatabase.destroy]. Last because it
 * closes the database, and nothing above it can run afterwards.
 */
object Erase {

    suspend fun everything(context: Context, db: SteadyDatabase) {
        stopEveryJob(context)
        db.clearAllTables()
        emptyOut(context.cacheDir)
        emptyOut(context.externalCacheDir)
        SteadyDatabase.destroy(context)
    }

    /**
     * Everything inside a directory, deepest first. The directory itself stays.
     *
     * The whole cache rather than the one folder the exports go in, for the same
     * reason [SteadyDatabase.destroy] deletes every file whose name starts with the
     * database name rather than a list of suffixes it knows about: a list of places
     * to look is a list somebody has to remember to add to, and the one it is
     * missing is the one that matters.
     *
     * Its own function, and public, because it is the only part of this that a test
     * on a laptop can run.
     */
    fun emptyOut(dir: File?) {
        dir?.listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * Every background job stopped, and its record of having run pruned.
     *
     * Awaited rather than fired off, so the daily job cannot wake up between here
     * and the database being closed. It holds no health data; what it holds is that
     * this phone was running this app, which is the kind of thing somebody deleting
     * everything means to be rid of.
     *
     * Wrapped because none of it is worth failing the deletion over. If WorkManager
     * is not there to be asked, there is no job to cancel.
     */
    private suspend fun stopEveryJob(context: Context) {
        runCatching {
            val work = WorkManager.getInstance(context)
            work.cancelAllWork().await()
            work.pruneWork().await()
        }
    }
}
