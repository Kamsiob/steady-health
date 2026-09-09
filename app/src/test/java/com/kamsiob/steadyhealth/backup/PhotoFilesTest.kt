package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The one thing in the database that is only half in the backup.
 *
 * A Sunday photograph is two things: a row in `photos`, and a JPEG in the app's own
 * storage that the row names. The backup carries the row. It cannot carry the file,
 * because the file is not in the database, and nothing else in the export carries
 * it either. Restore that on a new phone and every photograph is a name pointing at
 * nothing, with the app reporting that it worked.
 *
 * Nothing writes a photo row today, so there is nothing to lose yet, and the
 * pages of a scanned document are safe because their bytes are in the database
 * rather than beside it. But PRIVACY.md already promises "spreadsheets, your
 * photos, and a one-page summary", and the day somebody builds the Sunday photo
 * that promise is false and a restore starts losing something.
 *
 * So this fails on the day the feature arrives rather than on the day somebody
 * restores. It is written the way VoiceTest is written and for the same reason: a
 * promise nothing checks is a promise that quietly stops being true.
 */
class PhotoFilesTest {

    @Test
    fun nothingWritesAPhotoUntilTheExportCarriesTheFileItNames() {
        val writes = sources()
            .filter { it.readText().let { text -> WRITES.any(text::contains) } }
            .map { it.name }

        assertWithMessage(
            "these write photo rows, so the JPEG they name has to go into the export " +
                "and come back out of it, or a restore loses every Sunday photograph",
        ).that(writes).isEmpty()
    }

    /**
     * Everything but the places a row type has to be mentioned.
     *
     * The table, its queries and the backup's own list of tables all name
     * `PhotoEntity` and none of them writes one.
     */
    private fun sources(): List<File> = main().walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .filterNot { it.parentFile?.name in setOf("dao", "entity", "backup") }
        .filterNot { it.name == "SteadyDatabase.kt" }
        .toList()

    /**
     * The app's own source, from whichever directory the tests were started in.
     *
     * It fails rather than passing quietly when it cannot find it, because a test
     * that skips itself when it cannot see the thing it is checking is worse than
     * no test: it reports green.
     */
    private fun main(): File {
        val here = WHERE.map(::File).firstOrNull { it.isDirectory }
        return checkNotNull(here) { "No app sources beside " + File("").absolutePath }
    }

    private companion object {
        val WHERE = listOf(
            "src/main/java/com/kamsiob/steadyhealth",
            "app/src/main/java/com/kamsiob/steadyhealth",
            "../app/src/main/java/com/kamsiob/steadyhealth",
        )
        val WRITES = listOf("upsertPhoto(", "PhotoEntity(")
    }
}
