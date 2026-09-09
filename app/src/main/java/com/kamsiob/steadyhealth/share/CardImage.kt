package com.kamsiob.steadyhealth.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.kamsiob.steadyhealth.R
import java.io.File

/**
 * The card, drawn. ADDENDUM-03 Part 11.
 *
 * "A rendered image, 1080x1350, in the app's design language: the sun and hills hero
 * in the app's orange, the person's sentence in Figtree 900 navy, the figure beneath,
 * the app name small at the bottom. It looks like the app, not a screenshot of it."
 *
 * Drawn with the plain Android canvas rather than by capturing a composable. A
 * screenshot of a screen is the wrong size, carries the status bar and whatever
 * happened to be on it, and changes shape every time the screen it copies changes.
 * This is a picture with its own layout, and the only thing it shares with the app is
 * the palette and the typeface.
 *
 * Nothing on it comes from anywhere except the one line the person confirmed. There is
 * no date, no count, no number of any kind unless they typed one, and no identifier.
 * A card cannot leak what it was never given.
 */
object CardImage {

    /** Part 11 names the size. Portrait, and the shape every messaging app accepts. */
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val MARGIN = 96f
    private const val SUN_CENTRE_Y = 430f
    private const val SUN_RADIUS = 190f
    private const val HILL_TOP = 620f
    private const val LINE_SIZE = 76f
    private const val NAME_SIZE = 34f
    private const val FIGURE_TOP = 980f
    private const val LINE_SPACING = 1.12f

    /**
     * Draw one card into the app's own cache, and return the file.
     *
     * The cache rather than anywhere else, because the file exists only to be handed
     * to the share sheet. Nothing keeps it, and a card somebody sent is not part of
     * their history.
     */
    fun write(context: Context, copy: CardCopy, name: String = "steady-card.png"): File {
        val bitmap = draw(context, copy.line)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    fun draw(context: Context, line: String): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(GROUND)

        sunAndHills(canvas)
        val figtree = runCatching { ResourcesCompat.getFont(context, R.font.figtree) }.getOrNull()
        sentence(canvas, line, figtree)
        figure(canvas)
        appName(canvas, context.getString(R.string.app_name), figtree)
        return bitmap
    }

    /**
     * The hero, the same shape the welcome screen draws.
     *
     * The sun sits behind the hills rather than above them, which is what makes it
     * read as the app's picture rather than as a generic sunrise.
     */
    private fun sunAndHills(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = ORANGE
        canvas.drawCircle(WIDTH / 2f, SUN_CENTRE_Y, SUN_RADIUS, paint)

        paint.color = NAVY
        canvas.drawPath(hill(from = HILL_TOP, dip = 90f), paint)

        paint.color = NAVY_LIGHT
        canvas.drawPath(hill(from = HILL_TOP + 110f, dip = -70f), paint)
    }

    private fun hill(from: Float, dip: Float): Path = Path().apply {
        moveTo(0f, from)
        quadTo(WIDTH / 2f, from + dip, WIDTH.toFloat(), from)
        lineTo(WIDTH.toFloat(), HEIGHT.toFloat())
        lineTo(0f, HEIGHT.toFloat())
        close()
    }

    /**
     * The person's sentence, as large as it can be and still fit.
     *
     * The size comes down until the text fits the space rather than the text being
     * cut off, because the sentence is the whole card and a card that clipped somebody
     * mid-word would be worse than one whose type is a little smaller.
     */
    private fun sentence(canvas: Canvas, line: String, font: Typeface?) {
        val width = (WIDTH - MARGIN * 2).toInt()
        var size = LINE_SIZE
        var layout = layoutOf(line, font, size, width)
        while (layout.height > SENTENCE_ROOM && size > LINE_SIZE / 2) {
            size -= 4f
            layout = layoutOf(line, font, size, width)
        }
        canvas.save()
        canvas.translate(MARGIN, HILL_TOP + 150f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun layoutOf(line: String, font: Typeface?, size: Float, width: Int): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size
            typeface = Typeface.create(font ?: Typeface.DEFAULT, Typeface.BOLD)
        }
        return StaticLayout.Builder.obtain(line, 0, line.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, LINE_SPACING)
            .setIncludePad(false)
            .build()
    }

    /**
     * The figure, the same one the app draws everywhere else.
     *
     * A person standing, in strokes, facing the sun. Not a silhouette and not a
     * photograph: nobody sending this card should have to wonder whether the person on
     * it is supposed to be them.
     */
    private fun figure(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BUTTER
            style = Paint.Style.STROKE
            strokeWidth = 16f
            strokeCap = Paint.Cap.ROUND
        }
        val x = WIDTH - MARGIN - 130f
        val top = FIGURE_TOP

        canvas.drawCircle(x, top, 42f, paint)
        canvas.drawLine(x, top + 46f, x, top + 170f, paint)
        canvas.drawLine(x, top + 80f, x - 74f, top + 130f, paint)
        canvas.drawLine(x, top + 80f, x + 74f, top + 40f, paint)
        canvas.drawLine(x, top + 170f, x - 60f, top + 280f, paint)
        canvas.drawLine(x, top + 170f, x + 60f, top + 280f, paint)
    }

    private fun appName(canvas: Canvas, name: String, font: Typeface?) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SAND
            textSize = NAME_SIZE
            typeface = Typeface.create(font ?: Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(name, MARGIN, HEIGHT - MARGIN, paint)
    }

    private const val SENTENCE_ROOM = 380

    private const val GROUND = 0xFFFBF8F3.toInt()
    private const val ORANGE = 0xFFF5843E.toInt()
    private const val NAVY = 0xFF1E2A5A.toInt()
    private const val NAVY_LIGHT = 0xFF2E3D7D.toInt()
    private const val BUTTER = 0xFFFFD766.toInt()
    private const val SAND = 0xFFF6E7D3.toInt()
}
