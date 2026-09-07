package com.kamsiob.steadyhealth.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

/** One line of the page, and how it is set. */
private data class Line(val text: String, val size: Float, val bold: Boolean, val gapBefore: Float)

/** Everything the page prints. Assembled by the caller; this only draws. */
data class SummaryPage(
    val title: String,
    val window: String,
    val paragraphs: List<String>,
    val questionsHeading: String,
    val questions: List<String>,
    val numbersHeading: String,
    val numbers: List<Pair<String, String>>,
    val notTracked: String,
    val provenance: String,
)

/**
 * The visit summary as one PDF page.
 *
 * LOGIC.md section 13: one page, function first. It is drawn rather than
 * templated because Android has no HTML-to-PDF path that does not involve a
 * WebView, and a WebView in a local-first app is a network stack sitting next to
 * somebody's health record for no reason.
 *
 * Nothing is transmitted. The file is written to the app's own cache and handed
 * to the share sheet, and the person decides where it goes from there.
 */
object SummaryPdf {

    /** A4 at 72 dpi, which is what Android's PDF canvas measures in. */
    const val WIDTH = 595
    const val HEIGHT = 842

    private const val MARGIN = 48f
    private const val TITLE = 20f
    private const val HEADING = 13f
    private const val BODY = 11f
    private const val SMALL = 9f
    private const val LEADING = 1.45f

    private const val INK = 0xFF1A1F3D.toInt()
    private const val QUIET = 0xFF6C708F.toInt()

    /**
     * Write the page and return the file.
     *
     * Into `cacheDir/shared` so that nothing lands in a directory the person has
     * to clean up, and so a share that is cancelled leaves a file the system will
     * remove on its own.
     */
    fun write(context: Context, page: SummaryPage, name: String): File {
        val document = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, 1).create()
        val pdfPage = document.startPage(info)
        draw(pdfPage.canvas, page)
        document.finishPage(pdfPage)

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        file.outputStream().use(document::writeTo)
        document.close()
        return file
    }

    private fun draw(canvas: Canvas, page: SummaryPage) {
        val lines = buildList {
            add(Line(page.title, TITLE, bold = true, gapBefore = 0f))
            add(Line(page.window, SMALL, bold = false, gapBefore = 4f))
            page.paragraphs.forEach { add(Line(it, BODY, bold = false, gapBefore = 14f)) }
            if (page.questions.isNotEmpty()) {
                add(Line(page.questionsHeading, HEADING, bold = true, gapBefore = 20f))
                page.questions.forEach { add(Line("- $it", BODY, bold = false, gapBefore = 6f)) }
            }
            add(Line(page.numbersHeading, HEADING, bold = true, gapBefore = 20f))
        }

        val text = Paint().apply {
            isAntiAlias = true
            color = INK
        }
        var y = MARGIN + TITLE

        lines.forEach { line ->
            text.textSize = line.size
            text.typeface = if (line.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            text.color = if (line.size <= SMALL) QUIET else INK
            y += line.gapBefore
            y = wrap(canvas, text, line.text, y)
        }

        // The measures, as two columns, because a clinician reads down a column.
        text.textSize = BODY
        text.typeface = Typeface.DEFAULT
        text.color = INK
        page.numbers.forEach { (name, value) ->
            y += BODY * LEADING
            canvas.drawText(name, MARGIN, y, text)
            val right = Paint(text)
            right.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, WIDTH - MARGIN, y, right)
        }

        text.textSize = SMALL
        text.color = QUIET
        y += SMALL * LEADING * 2
        y = wrap(canvas, text, page.notTracked, y)
        y += SMALL
        wrap(canvas, text, page.provenance, y)
    }

    /** Draw [text] inside the margins, wrapping on words, and return the new y. */
    private fun wrap(canvas: Canvas, paint: Paint, text: String, startY: Float): Float {
        val width = WIDTH - MARGIN * 2
        var y = startY
        var line = StringBuilder()
        text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > width) {
                canvas.drawText(line.toString(), MARGIN, y, paint)
                y += paint.textSize * LEADING
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), MARGIN, y, paint)
            y += paint.textSize * LEADING
        }
        return y
    }
}
