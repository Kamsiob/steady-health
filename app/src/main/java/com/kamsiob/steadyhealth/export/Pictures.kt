package com.kamsiob.steadyhealth.export

import com.kamsiob.steadyhealth.data.SteadyDatabase

/** One picture in the zip: its file name, and the bytes exactly as they were taken. */
class Picture(val name: String, val bytes: ByteArray)

/**
 * The photographs, as picture files anything can open.
 *
 * PRIVACY.md promises "spreadsheets, your photos, and a one-page summary", and the
 * photographs this app holds are the pages somebody scanned. They live in the
 * database as bytes, which is right for keeping them encrypted and wrong for an
 * export: a person who opens the zip should see the letter from their physio, not
 * a column of Base64 inside a file only this app reads.
 *
 * They come out under the same names the paperwork spreadsheet prints in its
 * `picture_file` column, so the row and the picture can be read side by side. JPEG
 * because that is what was stored; nothing is re-encoded on the way out, so what
 * comes out of the zip is the photograph that was taken.
 */
object Pictures {

    suspend fun of(db: SteadyDatabase): List<Picture> = db.backupState()
        .documentPages()
        .sortedWith(compareBy({ it.documentId }, { it.at }))
        .map { Picture(nameOf(it.documentId, it.at), it.image) }

    /**
     * The name one page goes into the zip under.
     *
     * In its own function because two places need to agree on it: this one, which
     * writes the file, and the paperwork sheet, which prints the name beside the
     * text read off that page. Two copies of this rule would drift and the
     * spreadsheet would point at files that are not there.
     */
    fun nameOf(documentId: Long, at: Int): String = "photos/paper-$documentId-page-${at + 1}.jpg"
}
