package com.kamsiob.steadyhealth.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Out and back, field for field.
 *
 * A backup that loses one column is worse than no backup, because it is only
 * found by the person who needed it. So this writes a row into every table, reads
 * the file back, and compares the whole thing rather than a sample of it.
 */
class BackupRoundTripTest {

    private val written = BackupFile.write(everything())
    private val read = BackupFile.read(written.toByteArray()) as BackupRead.Ready

    @Test
    fun everythingComesBackExactly() {
        assertThat(read.backup).isEqualTo(everything())
    }

    @Test
    fun theHeaderSaysWhatWroteItAndWhen() {
        assertThat(read.backup.header.app).isEqualTo(Backup.MARK)
        assertThat(read.backup.header.appVersion).isEqualTo("0.1.0")
        assertThat(read.backup.header.schema).isEqualTo(Backup.SCHEMA_VERSION)
        assertThat(read.backup.header.writtenOn).isEqualTo("2026-09-09")
    }

    @Test
    fun aSentenceKeepsItsCommaQuotationMarkNewlineAndEmoji() {
        assertThat(read.backup.checkIns.first().sentence).isEqualTo(SENTENCE)
        assertThat(read.backup.checkIns.first().sentence).contains(",")
        assertThat(read.backup.checkIns.first().sentence).contains("\"")
        assertThat(read.backup.checkIns.first().sentence).contains("\n")
        assertThat(read.backup.checkIns.first().sentence).contains("🙂")
    }

    @Test
    fun anEmptyStringIsNotANull() {
        assertThat(read.backup.checkIns[1].sentence).isEqualTo(NOTHING)
        assertThat(read.backup.documents.first().fromWho).isEqualTo(NOTHING)
        assertThat(read.backup.weighIns[1].source).isEqualTo(NOTHING)
    }

    @Test
    fun everyNullStaysNull() {
        assertThat(read.backup.checkIns[1].sleepHalfHours).isNull()
        assertThat(read.backup.checkIns[1].dayRating).isNull()
        assertThat(read.backup.sessions[1].talkTest).isNull()
        assertThat(read.backup.sessions[1].steps).isNull()
        assertThat(read.backup.sessions[1].distanceMetres).isNull()
        assertThat(read.backup.sessions[1].nextDayFeel).isNull()
        assertThat(read.backup.sessions[1].nextDayAskedOnDay).isNull()
        assertThat(read.backup.runs[1].felt).isNull()
        assertThat(read.backup.soreAreas[1].clearedOnDay).isNull()
        assertThat(read.backup.measureResults[1].checkId).isNull()
        assertThat(read.backup.itemRatings[1].sureness).isNull()
        assertThat(read.backup.experiments[1].stoppedAt).isNull()
        assertThat(read.backup.experiments[1].resultHeadline).isNull()
        assertThat(read.backup.experiments[1].resultDetail).isNull()
        assertThat(read.backup.ladderState[1].lastOfferedDay).isNull()
        assertThat(read.backup.ladderState[1].offerDeclinedUntilDay).isNull()
        assertThat(read.backup.ladderState[1].easingUntilDay).isNull()
        assertThat(read.backup.ladderState[1].lastSessionDay).isNull()
        assertThat(read.backup.trackedItems[1].archivedAt).isNull()
        assertThat(read.backup.plans[1].reviewDay).isNull()
        assertThat(read.backup.plans[1].archivedAt).isNull()
        assertThat(read.backup.plans[1].reviewPromptedFor).isNull()
        assertThat(read.backup.planItems[1].movementId).isNull()
    }

    @Test
    fun aNegativeNumberIsStillNegative() {
        assertThat(read.backup.weighIns[1].rawKg).isEqualTo(-1.5)
        assertThat(read.backup.measureResults[1].value).isEqualTo(-0.25)
        assertThat(read.backup.bloodPressure.first().diastolic).isEqualTo(-1)
    }

    @Test
    fun aVeryLargeDayIsStillThatDay() {
        assertThat(read.backup.weighIns[1].epochDay).isEqualTo(FAR_OFF_DAY)
    }

    /**
     * A page image is compared byte by byte on purpose.
     *
     * `DocumentPageEntity` compares itself by id, which is right for a row and
     * useless here: the whole-backup comparison above would pass with every
     * photograph replaced by a different one.
     */
    @Test
    fun aPhotographedPageComesBackByteForByte() {
        val page = read.backup.documentPages.first()
        assertThat(page.image).isEqualTo(byteArrayOf(0, 1, -1, 127, -128))
        assertThat(page.text).isEqualTo(SENTENCE)
        assertThat(page.documentId).isEqualTo(1)
    }

    /** Ids are what joins one table to another, so they are not decoration. */
    @Test
    fun idsComeBackSoTheRowsStillPointAtEachOther() {
        assertThat(read.backup.checkInTags.first().checkInId)
            .isEqualTo(read.backup.checkIns.first().id)
        assertThat(read.backup.runMovements.first().runId).isEqualTo(read.backup.runs.first().id)
        assertThat(read.backup.planItems.first().planId).isEqualTo(read.backup.plans.first().id)
        assertThat(read.backup.documentPages.first().documentId)
            .isEqualTo(read.backup.documents.first().id)
        assertThat(read.backup.itemRatings.first().itemId)
            .isEqualTo(read.backup.trackedItems.first().id)
    }

    /** The whole file inside a zip, which is how it is actually handed out. */
    @Test
    fun theSameFileInsideAZipReadsTheSame() {
        val zip = zipOf(Backup.FILE_NAME to written, "days.csv" to "date,what_you_said")
        assertThat(BackupFile.read(zip)).isEqualTo(read)
    }

    /** Unzipped and zipped up again by hand, which puts it one level down. */
    @Test
    fun theSameFileInAFolderInsideAZipReadsTheSame() {
        val zip = zipOf(("steady-health-export/" + Backup.FILE_NAME) to written)
        assertThat(BackupFile.read(zip)).isEqualTo(read)
    }
}
