package com.kamsiob.steadyhealth.export

import android.content.Context
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One spreadsheet: its file name and its rows, the first of which is the header. */
data class Sheet(val name: String, val rows: List<List<String>>)

/** One file in the zip that is already text, written in as it is. */
data class TextFile(val name: String, val text: String)

/**
 * Everything the person has, as ordinary files.
 *
 * PRIVACY.md promises this in those words: "Export everything at any time from
 * Settings as ordinary files: spreadsheets, your photos, and a one-page summary."
 * So it is CSV, JPEG and PDF in a zip, openable by anything, rather than a format
 * only this app can read. An export somebody cannot open is not an export, and a
 * photograph that comes out only as Base64 inside the backup is not a photograph.
 *
 * One more file goes in beside them, the backup, which is the same data in the
 * shape this app can read back. The spreadsheets are for a person and are lossy on
 * purpose: they format the dates, leave out the ids that join one table to
 * another, and flatten the tags into a column. Nothing could be rebuilt from them.
 * Keeping both in one zip means somebody who kept their export can open it and can
 * also put it back, without having had to know in advance which of those they were
 * going to want.
 *
 * Nothing is transmitted. The zip is written into the app's own cache and handed
 * to the share sheet, and the person decides where it goes.
 */
object DataExport {

    fun write(
        context: Context,
        sheets: List<Sheet>,
        extras: List<File>,
        name: String,
        files: List<TextFile> = emptyList(),
        pictures: List<Picture> = emptyList(),
    ): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            sheets.forEach { sheet ->
                zip.putNextEntry(ZipEntry("${sheet.name}.csv"))
                zip.write(csv(sheet.rows).toByteArray())
                zip.closeEntry()
            }
            // Straight from memory into the zip, never onto the disk on the way. The
            // backup holds everything the database holds, and a second unencrypted
            // copy of it sitting in the cache afterwards is not something this app
            // should leave behind.
            files.forEach { text ->
                zip.putNextEntry(ZipEntry(text.name))
                zip.write(text.text.toByteArray())
                zip.closeEntry()
            }
            // The scanned pages, under the names the paperwork sheet printed for
            // them, so a row and its photograph are found together.
            pictures.forEach { picture ->
                zip.putNextEntry(ZipEntry(picture.name))
                zip.write(picture.bytes)
                zip.closeEntry()
            }
            extras.forEach { extra ->
                zip.putNextEntry(ZipEntry(extra.name))
                extra.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return file
    }

    /**
     * Rows as CSV, quoted the way every spreadsheet expects.
     *
     * A person's own sentence can contain a comma, a quotation mark and a
     * newline, and all three have to survive the round trip, because those
     * sentences are the part of this export that is actually theirs.
     */
    fun csv(rows: List<List<String>>): String =
        rows.joinToString("\n") { row -> row.joinToString(",") { cell(it) } }

    private fun cell(value: String): String =
        if (value.any(::needsQuoting)) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun needsQuoting(char: Char): Boolean =
        char == ',' || char == '"' || char == '\n' || char == '\r'

    /**
     * Read a CSV back, with the same quoting rules. For import.
     *
     * Written as a small state machine rather than a split, because a person's
     * own sentence can hold a comma, a quotation mark and a newline, and every
     * one of those breaks a naive parser on exactly the rows worth keeping.
     */
    fun parse(text: String): List<List<String>> {
        val reader = Reader()
        text.forEach(reader::accept)
        return reader.finish()
    }

    /** One pass over the characters, holding the little state a CSV needs. */
    private class Reader {
        private val rows = mutableListOf<List<String>>()
        private var row = mutableListOf<String>()
        private val cell = StringBuilder()
        private var quoted = false
        private var lastWasQuote = false
        private var lastWasCarriage = false

        fun accept(char: Char) {
            if (lastWasQuote) {
                lastWasQuote = false
                if (char == '"') {
                    cell.append('"')
                    return
                }
                quoted = false
            }
            when {
                char == '"' && quoted -> lastWasQuote = true
                char == '"' -> quoted = true
                quoted -> cell.append(char)
                char == ',' -> endCell()
                char == '\n' || char == '\r' -> endRow(char)
                else -> cell.append(char)
            }
        }

        fun finish(): List<List<String>> {
            if (cell.isNotEmpty() || row.isNotEmpty()) endCellAndRow()
            return rows
        }

        private fun endCell() {
            row.add(cell.toString())
            cell.clear()
        }

        private fun endRow(char: Char) {
            if (char == '\n' && lastWasCarriage) {
                lastWasCarriage = false
                return
            }
            lastWasCarriage = char == '\r'
            if (cell.isNotEmpty() || row.isNotEmpty()) endCellAndRow()
        }

        private fun endCellAndRow() {
            endCell()
            rows.add(row)
            row = mutableListOf()
        }
    }
}
