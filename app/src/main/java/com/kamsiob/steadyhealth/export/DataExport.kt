package com.kamsiob.steadyhealth.export

import android.content.Context
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One spreadsheet: its file name and its rows, the first of which is the header. */
data class Sheet(val name: String, val rows: List<List<String>>)

/**
 * Everything the person has, as ordinary files.
 *
 * PRIVACY.md promises this in those words: "Export everything at any time from
 * Settings as ordinary files: spreadsheets, your photos, and a one-page summary."
 * So it is CSV and PDF in a zip, openable by anything, rather than a format only
 * this app can read. An export somebody cannot open is not an export.
 *
 * Nothing is transmitted. The zip is written into the app's own cache and handed
 * to the share sheet, and the person decides where it goes.
 */
object DataExport {

    fun write(context: Context, sheets: List<Sheet>, extras: List<File>, name: String): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            sheets.forEach { sheet ->
                zip.putNextEntry(ZipEntry("${sheet.name}.csv"))
                zip.write(csv(sheet.rows).toByteArray())
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
