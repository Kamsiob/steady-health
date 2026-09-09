package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertThat
import com.kamsiob.steadyhealth.data.entity.DocumentEntity
import com.kamsiob.steadyhealth.data.entity.DocumentPageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

/**
 * The photographed pages, which are the largest thing this app keeps.
 *
 * Two questions, and they have different answers. Do the bytes survive being
 * written as text and read back, which they have to, because the page is the thing
 * the app promises to keep whatever it made of it. And what does the file become,
 * because a backup somebody cannot write is not a backup.
 *
 * The pages here are the size a real one is. A page is photographed at whatever the
 * camera gives and kept as JPEG at quality eighty, which for a sheet of typed text
 * off a twelve megapixel sensor is between one and two megabytes. The bytes are
 * random because JPEG bytes are already compressed and behave like random ones,
 * and a test filled with zeroes would measure nothing.
 */
@RunWith(RobolectricTestRunner::class)
class PageImagesTest {

    private val phone = aDatabase()
    private val newPhone = aDatabase()

    @After
    fun close() {
        phone.close()
        newPhone.close()
    }

    /** A page of a real size, through the database, the file, and back. */
    @Test
    fun aRealSizedPageComesBackByteForByte() = runTest {
        val pages = pagesOnThePhone(FOUR_PAGES)
        val backup = BackupRepository(phone).read(VERSION, WRITTEN_ON)

        BackupRepository(newPhone).restore(BackupFile.readyOf(BackupFile.write(backup)))

        val back = newPhone.documents().pagesOf(1).sortedBy { it.at }
        assertThat(back).hasSize(FOUR_PAGES)
        back.forEachIndexed { at, page ->
            assertThat(page.image.contentEquals(pages[at])).isTrue()
        }
    }

    /**
     * What a page costs in the file, and what fifty of them come to.
     *
     * Bytes have no shape of their own in JSON, so they are written as base64,
     * which is four characters for every three bytes. That is the number below: a
     * page costs about a third more in the file than it does in the database, and
     * nothing else in the file is big enough to matter beside it.
     *
     * Fifty pages, which is a person who has photographed everything they have been
     * given over a couple of years, is on the order of ninety megabytes of
     * backup.json.
     */
    @Test
    fun aPageCostsAThirdMoreInTheFileThanItDoesOnThePhone() = runTest {
        pagesOnThePhone(FOUR_PAGES)
        val written = BackupFile.write(BackupRepository(phone).read(VERSION, WRITTEN_ON))
        val images = FOUR_PAGES.toLong() * PAGE_BYTES

        val cost = written.length.toDouble() / images
        assertThat(cost).isGreaterThan(BASE64)
        assertThat(cost).isLessThan(BASE64 + A_LITTLE)
    }

    /**
     * The zip gives most of it back, and that is the part worth knowing.
     *
     * base64 spends eight bits saying six, and deflate finds that again, so the
     * file inside the export is close to the size of the photographs themselves.
     * The size of the thing somebody keeps is therefore not the problem here.
     */
    @Test
    fun theZipTakesMostOfThatBackAgain() = runTest {
        pagesOnThePhone(FOUR_PAGES)
        val written = BackupFile.write(BackupRepository(phone).read(VERSION, WRITTEN_ON))
        val images = FOUR_PAGES.toLong() * PAGE_BYTES

        val zipped = deflatedSize(written)
        assertThat(zipped.toDouble() / images).isLessThan(BASE64)
    }

    /**
     * The whole backup is one string in memory before anything is written.
     *
     * This is the number that decides whether a person can back up at all. The text
     * is built whole, held as characters, which is two bytes each on this runtime,
     * and then copied again into bytes for the zip. Fifty pages is around ninety
     * megabytes of text, so something like three hundred and fifty megabytes has to
     * be free at once, and an ordinary Android heap is smaller than that.
     *
     * The test states the shape rather than the failure, because the failure only
     * happens on a phone. It goes red if the file ever stops being built whole,
     * which is the fix, and at that point this should be replaced by a test of the
     * streaming.
     */
    @Test
    fun theWholeFileIsBuiltInMemoryAtOnce() = runTest {
        pagesOnThePhone(FOUR_PAGES)
        val written = BackupFile.write(BackupRepository(phone).read(VERSION, WRITTEN_ON))

        assertThat(written.length.toLong()).isAtLeast(FOUR_PAGES.toLong() * PAGE_BYTES * 4 / 3)
    }

    private suspend fun pagesOnThePhone(many: Int): List<ByteArray> {
        val id = phone.documents().put(DocumentEntity(0, 20_000, "letter", "the physio", 1L))
        val random = Random(SEED)
        return (0 until many).map { at ->
            random.nextBytes(PAGE_BYTES).also {
                phone.documents().putPage(DocumentPageEntity(0, id, at, it, "page $at"))
            }
        }
    }

    /** The same compression the export uses, so the number means the export's zip. */
    private fun deflatedSize(text: String): Int {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.setLevel(Deflater.DEFAULT_COMPRESSION)
            zip.putNextEntry(ZipEntry(Backup.FILE_NAME))
            zip.write(text.toByteArray())
            zip.closeEntry()
        }
        return out.size()
    }

    private companion object {
        /** A sheet of typed text, photographed and kept as JPEG at quality eighty. */
        const val PAGE_BYTES = 1_400_000
        const val FOUR_PAGES = 4
        const val SEED = 20_260_909
        const val BASE64 = 4.0 / 3.0
        const val A_LITTLE = 0.01
        const val VERSION = "0.1.0"
        val WRITTEN_ON: LocalDate = LocalDate.of(2026, 9, 9)
    }
}

/** The backup out of its own file, or a failure that says which of the four it was. */
fun BackupFile.readyOf(text: String): Backup =
    (read(text.toByteArray()) as? BackupRead.Ready)?.backup
        ?: error("the file this test just wrote did not read back")
