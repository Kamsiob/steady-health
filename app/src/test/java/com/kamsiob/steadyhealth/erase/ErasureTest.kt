package com.kamsiob.steadyhealth.erase

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The parts of "there is no copy anywhere else" a laptop can hold.
 *
 * Most of deleting everything needs a phone: a Keystore entry, an encrypted file
 * and the lock file beside it, a background job. Two parts do not, and both are
 * the parts that were actually wrong.
 *
 * The first is the cache. Every file this app hands to the share sheet is written
 * into it first, and the export zip is the whole history in plain CSV. Deleting
 * everything left it there. Nothing in the app was ever going to remove it.
 *
 * The second is a rule about the source itself: deleting everything can only reach
 * the places it knows about, so the app has to keep to those places. A test is the
 * only thing that can say so, because writing somewhere new compiles, works, and
 * is wrong only on the day somebody deletes.
 */
class ErasureTest {

    @Test
    fun theExportIsNotStillSittingInTheCache() {
        val cache = File.createTempFile("cache", "").let { file ->
            file.delete()
            file.apply { mkdirs() }
        }
        val shared = File(cache, "shared").apply { mkdirs() }
        val export = File(shared, "steady-health-export.zip").apply { writeText("everything") }
        File(shared, "summary.pdf").writeText("a page")
        File(cache, "steady-card.png").writeText("a card")

        Erase.emptyOut(cache)

        assertWithMessage("somebody's whole history, in a file they deleted")
            .that(export.exists())
            .isFalse()
        assertThat(cache.listFiles()).isEmpty()
        assertWithMessage("the cache itself has to survive; the app writes into it")
            .that(cache.isDirectory)
            .isTrue()
        cache.deleteRecursively()
    }

    @Test
    fun emptyingSomethingThatIsNotThereIsNotAFailure() {
        Erase.emptyOut(null)
        Erase.emptyOut(File("no-such-directory-anywhere"))
    }

    /**
     * Nothing keeps anything in preferences, because deleting cannot reach one.
     *
     * There is no SharedPreferences and no DataStore in this app today, and that is
     * the only reason delete-everything does not mention them. The day somebody adds
     * one for something small, it survives deletion, and nobody finds out. This
     * fails on that day instead.
     */
    @Test
    fun nothingIsKeptInPreferences() {
        val offenders = kotlinFiles()
            .filter { file ->
                Regex("""getSharedPreferences|SharedPreferences|PreferenceManager|DataStore""")
                    .containsMatchIn(withoutComments(file))
            }
            .map { it.name }

        assertWithMessage("delete-everything does not know how to empty a preferences file")
            .that(offenders)
            .isEmpty()
    }

    /**
     * The app writes into two places, and deleting everything empties both.
     *
     * The cache, which Erase empties whole, and the database directory, which
     * SteadyDatabase.destroy clears of every file whose name starts with the
     * database name. Anything written anywhere else outlives a deletion.
     *
     * The two exceptions are named rather than pattern-matched. DatabaseKey writes
     * the wrapped passphrase there and deletes it itself, which is what makes the
     * key part of the promise true. ModelsViewModel reads the free space at that
     * path and writes nothing.
     */
    @Test
    fun nothingWritesWhereDeletingCannotReach() {
        val offenders = kotlinFiles()
            .filter { file ->
                Regex("""filesDir|getExternalFilesDir|openFileOutput|getExternalStorage""")
                    .containsMatchIn(withoutComments(file))
            }
            .map { it.name }
            .filterNot { it in allowed }

        assertWithMessage("written outside the cache and the database, so a deletion misses it")
            .that(offenders)
            .isEmpty()
    }

    private val allowed = setOf("DatabaseKey.kt", "ModelsViewModel.kt")

    /** Comments say what the app does not do, so they are not evidence that it does. */
    private fun withoutComments(file: File): String = file.readText()
        .lineSequence()
        .map { it.substringBefore("//") }
        .filterNot { it.trimStart().startsWith("*") }
        .joinToString("\n")

    private fun kotlinFiles(): List<File> =
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()
}
