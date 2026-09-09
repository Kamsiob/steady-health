package com.kamsiob.steadyhealth.export

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.backup.Schemas
import org.junit.Test

/**
 * The test that holds the word "everything" to its meaning.
 *
 * PRIVACY.md says a person can "export everything at any time from Settings as
 * ordinary files". A table that is in the database and in no spreadsheet makes
 * that sentence false, and it makes it false silently: the export runs, the zip
 * opens, every file in it is correct, and the thing that is missing is missing
 * only from the point of view of somebody who knew it was there.
 *
 * That is exactly how the export got to five sheets carrying seven of thirty-one
 * tables. Nobody removed anything. Tables arrived and sheets did not.
 *
 * So the sheets are held against Room's own description of the database, the same
 * schema files the backup is held against, for the same reason: Room's annotations
 * are dropped from the built code, so nothing at runtime can be asked what tables
 * exist, and these files are generated from the one `@Database` the app runs on.
 */
class EverySheetTest {

    private val schema = Schemas.newest()
    private val carried = EverySheet.all.flatMap { it.from }

    @Test
    fun everyTableInTheDatabaseIsInASpreadsheet() {
        assertWithMessage("in the database and in no spreadsheet")
            .that(schema.tables.keys - carried.toSet())
            .isEmpty()
    }

    @Test
    fun noSheetClaimsATableTheDatabaseDoesNotHave() {
        assertWithMessage("named by a sheet and not in the database")
            .that(carried.toSet() - schema.tables.keys)
            .isEmpty()
    }

    /**
     * One table, one sheet.
     *
     * Not tidiness. A table read twice is a table a person meets twice under two
     * sets of column names, and the second one is the copy nobody maintains.
     */
    @Test
    fun noTableIsInTwoSheets() {
        assertThat(carried).containsNoDuplicates()
    }

    @Test
    fun everySheetHasItsOwnName() {
        assertThat(EverySheet.all.map { it.name }).containsNoDuplicates()
    }

    /**
     * A sheet name becomes a file name inside the zip, so it has to be one.
     *
     * A slash would make a folder, a backslash or a colon would make a file some
     * systems refuse to unpack, and either is found by the person unzipping rather
     * than by anybody here.
     */
    @Test
    fun everySheetNameIsAFileName() {
        EverySheet.all.forEach { sheet ->
            assertWithMessage(sheet.name)
                .that(sheet.name.none { it in "/\\:*?\"<>|" })
                .isTrue()
        }
    }

    @Test
    fun everySheetHasAHeading() {
        EverySheet.all.forEach { sheet ->
            assertWithMessage(sheet.name).that(sheet.heading).isNotEmpty()
        }
    }

    /**
     * Two pages never come out under one name.
     *
     * The paperwork sheet prints these names beside the text read off each page. If
     * two pages shared one, the zip would hold one picture where the spreadsheet
     * says there are two, and the row pointing at the missing one would look right.
     */
    @Test
    fun everyPageGetsItsOwnPictureName() {
        val names = (1L..THREE).flatMap { paper ->
            (0 until THREE.toInt()).map { page -> Pictures.nameOf(paper, page) }
        }
        assertThat(names).containsNoDuplicates()
    }

    @Test
    fun aPictureNameSaysWhichPageItIs() {
        assertThat(Pictures.nameOf(4, 0)).endsWith("page-1.jpg")
    }

    private companion object {
        const val THREE = 3L
    }
}
