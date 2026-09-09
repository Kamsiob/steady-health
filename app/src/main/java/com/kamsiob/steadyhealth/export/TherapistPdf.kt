package com.kamsiob.steadyhealth.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.plan.BriefHeading
import com.kamsiob.steadyhealth.plan.ChangedBy
import com.kamsiob.steadyhealth.plan.HurtEntry
import com.kamsiob.steadyhealth.plan.PlanLine
import com.kamsiob.steadyhealth.plan.TherapistBrief
import com.kamsiob.steadyhealth.plan.WhatHappened
import com.kamsiob.steadyhealth.session.Felt
import com.kamsiob.steadyhealth.ui.Labels
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One thing on the page, and how it is set.
 *
 * Three kinds and no more, because a page a clinician reads standing up is words,
 * dates, and one row of days per line of the plan. Anything else would be the app
 * drawing a conclusion in ink, which ADDENDUM-03 Part 6 and COMPLIANCE.md both put
 * out of bounds.
 */
private sealed interface Mark {

    /** Words, wrapped inside the margins. [gap] is the air above them. */
    data class Words(val text: String, val size: Float, val bold: Boolean, val gap: Float) : Mark

    /**
     * The window, one square per day, filled on the days a line was done.
     *
     * A day either happened or it did not, so a square is either there or it is not,
     * and there is no shade in between for a printer to turn into a judgement.
     */
    data class Days(val done: List<Boolean>) : Mark

    /** A hairline across the page, which is the only furniture on it. */
    object Rule : Mark
}

/**
 * The page somebody takes to their next appointment. ADDENDUM-03 Part 6.
 *
 * Drawn rather than templated for the reason [SummaryPdf] gives: the only
 * HTML-to-PDF path on Android runs through a WebView, and a WebView is a network
 * stack standing next to somebody's health record for no reason. The two files share
 * the approach and little else, because they are different documents: that one is
 * written for the person, this one is read by a clinician who did not ask for it and
 * has four minutes.
 *
 * The page is 595 by 792 points, which is the width of A4 and the height of US
 * Letter. A page inside both prints on either without a printer scaling it down, and
 * scaling is what turns a nine point line into a smudge on the office laser printer
 * this will actually come out of. The cost is roughly two centimetres of A4 left
 * blank at the foot, which nobody has ever complained about.
 *
 * It is black on white throughout, and there is no grey. A tired toner cartridge
 * renders a mid grey as a smear, so the hierarchy is carried by size and weight
 * alone, which survives a bad photocopy of a bad print.
 *
 * The order is Part 6's order and it is also the order of [TherapistBrief]: what was
 * asked for, what was done and when, the numbers, what hurt and when, and the
 * person's own ratings. It is arranged to be read from the top and abandoned partway
 * without the important part having been below the fold, which is why the plan and
 * its days sit above everything else and why the page never runs to a second sheet.
 */
object TherapistPdf {

    /** A4's width and US Letter's height, at the 72 dpi Android's PDF canvas uses. */
    const val WIDTH = 595
    const val HEIGHT = 792

    private const val MARGIN = 44f
    private const val TITLE = 17f
    private const val HEADING = 12f
    private const val LINE = 11f
    private const val BODY = 10f
    private const val SMALL = 9f

    /** Tight, because the page is a document and not a screen, and it has to fit. */
    private const val LEADING = 1.35f

    private const val GAP_STAMP = 3f
    private const val GAP_TIGHT = 2f
    private const val GAP_ROW = 9f
    private const val GAP_RULE = 11f
    private const val RULE_AFTER = 7f
    private const val GAP_FOOTER = 16f

    /** One square per day, sitting on a hairline, with a tick at each week. */
    private const val MARK = 5f
    private const val TICK = 2.5f
    private const val DAY_GAP = 1.5f
    private const val STRIP = MARK + TICK
    private const val DAYS_IN_WEEK = 7
    private const val HAIRLINE = 0.8f

    private const val INK = 0xFF000000.toInt()

    /** Phrases the app joins on one line. Punctuation, and never a sentence. */
    private const val DOT = " · "
    private const val COMMA = ", "

    private const val FILE_NAME = "steady-health-plan.pdf"

    /**
     * Write the page and return the file.
     *
     * Into `cacheDir/shared` by way of the same directory [SummaryPdf] uses, so that
     * a share somebody changes their mind about leaves a file the system clears up.
     */
    fun write(context: Context, brief: TherapistBrief, name: String = FILE_NAME): File {
        val document = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, 1).create()
        val page = document.startPage(info)
        draw(page.canvas, context, brief)
        document.finishPage(page)

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        file.outputStream().use(document::writeTo)
        document.close()
        return file
    }

    /**
     * Write the page and hand it to the share sheet.
     *
     * The person chooses where it goes, prints it, or mails it to themselves. The app
     * never sends it anywhere and has no network permission with which to try.
     */
    fun share(context: Context, brief: TherapistBrief, name: String = FILE_NAME) {
        Share.file(
            context = context,
            file = write(context, brief, name),
            mimeType = "application/pdf",
            title = context.getString(R.string.plan_export),
        )
    }

    /**
     * Lay the page out from the top, and keep the foot of it whatever happens.
     *
     * The footer is measured first and its room is taken off the bottom before
     * anything else is drawn, because the sentence saying nothing was transmitted and
     * nothing was interpreted is the one line that may not fall off the page.
     *
     * Content is drawn a group at a time and a group is never split, so a plan line
     * cannot end up on one side of the fold from the days it was done. If a group
     * will not fit, the page says so in one line and stops rather than crowding the
     * rest in at a size nobody can read.
     */
    private fun draw(canvas: Canvas, context: Context, brief: TherapistBrief) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = INK
        }
        val foot = footer(context)
        val floor = HEIGHT - MARGIN - height(paint, foot) - GAP_FOOTER
        var y = MARGIN
        var room = true

        groups(context, brief).forEach { group ->
            if (!room) return@forEach
            if (y + height(paint, group) > floor) {
                room = false
                place(canvas, paint, listOf(more(context)), y)
            } else {
                y = place(canvas, paint, group, y)
            }
        }
        place(canvas, paint, foot, floor + GAP_FOOTER)
    }

    /** The whole page, as groups that are each kept together or left off. */
    private fun groups(context: Context, brief: TherapistBrief): List<List<Mark>> = buildList {
        val label = brief.heading.planLabel.ifBlank { context.getString(R.string.plan_them) }
        add(header(context, brief.heading, label))
        brief.lines.forEach { add(planLine(context, it, brief.heading, label)) }
        if (brief.hurt.isNotEmpty()) {
            add(section(context.getString(R.string.therapist_hurt)))
            brief.hurt.forEach { add(hurt(context, it)) }
            if (brief.moreSaid > 0) {
                add(listOf(small(quantity(context, R.plurals.therapist_more_said, brief.moreSaid))))
            }
        }
        if (brief.ratings.isNotEmpty()) {
            add(section(context.getString(R.string.therapist_felt)))
            add(felt(context, brief))
        }
    }

    /**
     * The top of the page: whose plan, over which days, and how many of them.
     *
     * The app's name and the dates are on the second line rather than the first
     * because a clinician picking the sheet up wants to know what it is about before
     * they care where it came from, and both are on it because a page on a desk with
     * neither is a page nobody can place.
     *
     * The count of days is a count and never a share of anything. There is nothing to
     * divide it by anywhere on this page.
     */
    private fun header(context: Context, heading: BriefHeading, label: String): List<Mark> = buildList {
        add(Mark.Words(context.getString(R.string.therapist_title, label), TITLE, true, 0f))
        val stamp = context.getString(
            R.string.therapist_stamp,
            context.getString(R.string.app_name),
            day(heading.fromDay),
            day(heading.toDay),
        )
        add(Mark.Words(stamp, SMALL, false, GAP_STAMP))
        val dates = listOfNotNull(
            context.getString(R.string.therapist_given, day(heading.givenOnDay)),
            heading.reviewDay?.let { context.getString(R.string.therapist_review, day(it)) },
        )
        add(Mark.Words(dates.joinToString(DOT), SMALL, false, GAP_TIGHT))
        add(Mark.Rule)
        val days = listOfNotNull(
            quantity(context, R.plurals.therapist_plan_days, heading.daysWithPlan),
            heading.otherSessionDays
                .takeIf { it > 0 }
                ?.let { quantity(context, R.plurals.therapist_other_days, it) },
        )
        add(Mark.Words(days.joinToString(DOT), BODY, false, RULE_AFTER))
        val legend = context.getString(
            R.string.therapist_legend,
            short(heading.fromDay),
            short(heading.toDay),
        )
        add(Mark.Words(legend, SMALL, false, GAP_TIGHT))
    }

    /**
     * One line of the plan: their words, then what became of them.
     *
     * Their words are printed whole and first, in bold, and never paraphrased. A line
     * that was never done keeps its place and says so in a sentence, because dropping
     * a line a therapist set would be the app quietly editing their plan, and a nought
     * standing in for it would be a number where there was never a measurement.
     */
    private fun planLine(
        context: Context,
        line: PlanLine,
        heading: BriefHeading,
        label: String,
    ): List<Mark> = buildList {
        add(Mark.Words(line.text, LINE, true, GAP_ROW))
        val happened = line.happened
        if (happened.ever) {
            add(Mark.Days(strip(happened, heading)))
            add(body(whenDone(context, happened)))
            numbers(context, happened)?.let { add(body(it)) }
        } else {
            add(body(context.getString(R.string.therapist_not_done)))
        }
        changed(context, line, label)?.let { add(small(it)) }
        avoided(context, line)?.let { add(small(it)) }
    }

    /** Every day of the window, said done or not, in the order they happened. */
    private fun strip(happened: WhatHappened, heading: BriefHeading): List<Boolean> {
        val done = happened.onDays.toSet()
        return (heading.fromDay..heading.toDay).map { it in done }
    }

    /**
     * When a line was done, as days, times, and the two ends of it.
     *
     * The number of times is left off when it equals the number of days, because
     * "done on 24 days, 24 times" says one thing twice and the page has no room for
     * that. A plan asking for something twice a day is the case the second number is
     * for, and it appears exactly then.
     */
    private fun whenDone(context: Context, happened: WhatHappened): String {
        val first = happened.firstDay
        val last = happened.lastDay
        val span = when {
            first == null || last == null -> null
            first == last -> short(first)
            else -> context.getString(R.string.therapist_between, short(first), short(last))
        }
        return listOfNotNull(
            quantity(context, R.plurals.therapist_days, happened.days),
            quantity(context, R.plurals.therapist_times, happened.times)
                .takeIf { happened.times != happened.days },
            span,
        ).joinToString(COMMA)
    }

    /**
     * The numbers on one line: what was asked for, what was managed, what was marked.
     *
     * The fewest, the usual and the most are printed as three numbers and nothing is
     * done to them. Where every day was the same number, that is said once instead,
     * which is both shorter and truer than printing it three times.
     */
    private fun numbers(context: Context, happened: WhatHappened): String? {
        val counts = happened.counts
        val parts = listOfNotNull(
            happened.asked
                .takeIf { it.isNotEmpty() }
                ?.let { context.getString(R.string.therapist_asked, it.joinToString(COMMA)) },
            when {
                counts == null -> null
                counts.fewest == counts.most -> context.getString(R.string.therapist_same, counts.most)
                else -> context.getString(
                    R.string.therapist_counts,
                    counts.fewest,
                    counts.usual,
                    counts.most,
                )
            },
            happened.marks.madeEasier
                .takeIf { it > 0 }
                ?.let { quantity(context, R.plurals.therapist_easier, it) },
            happened.marks.selfCounted
                .takeIf { it > 0 }
                ?.let { quantity(context, R.plurals.therapist_counted, it) },
        )
        return parts.joinToString(DOT).ifBlank { null }
    }

    /** A line that changed, with the wording it replaced and which of them changed it. */
    private fun changed(context: Context, line: PlanLine, label: String): String? {
        val change = line.given.changed ?: return null
        return when (change.by) {
            ChangedBy.ThePerson ->
                context.getString(R.string.therapist_changed_you, day(change.onDay), change.was)

            ChangedBy.TheirTherapist ->
                context.getString(R.string.therapist_changed_them, day(change.onDay), label, change.was)
        }
    }

    /**
     * A line asking for something the person said they avoid.
     *
     * On the page because the therapist is the one who can settle it, and worded so
     * that it reports what the person said rather than what the app thinks of the
     * line. LOGIC.md 17: flagged, and left in as it was set.
     */
    private fun avoided(context: Context, line: PlanLine): String? {
        val clash = line.given.conflictsWith ?: return null
        return context.getString(
            R.string.therapist_avoid,
            context.getString(Labels.forExclusion(clash)).lowercase(Locale.getDefault()),
        )
    }

    /**
     * One part of the body, and everything said about it.
     *
     * Their own sentences are printed in quotation marks and are not touched, because
     * the sentence is the thing a therapist cannot get any other way. The line being
     * done at the time sits above them, which is what turns "my knee hurt" into
     * something a therapist can act on.
     */
    private fun hurt(context: Context, entry: HurtEntry): List<Mark> = buildList {
        add(Mark.Words(context.getString(Labels.forArea(entry.area)), LINE, true, GAP_ROW))
        val days = listOf(
            quantity(context, R.plurals.therapist_hurt_days, entry.days),
            context.getString(R.string.therapist_hurt_last, day(entry.lastDay)),
        )
        add(body(days.joinToString(COMMA)))
        if (entry.during.isNotEmpty()) {
            add(body(context.getString(R.string.therapist_hurt_during, entry.during.joinToString(COMMA))))
        }
        entry.said.forEach { said ->
            add(body(context.getString(R.string.therapist_said, day(said.onDay), said.words)))
        }
    }

    /**
     * The person's own rating of their own sessions, counted.
     *
     * The days rated hard are named under the counts so that they can be read against
     * the days something hurt, which is a comparison for the clinician to make and
     * not one the app makes for them.
     */
    private fun felt(context: Context, brief: TherapistBrief): List<Mark> = buildList {
        val counts = brief.ratings.joinToString(DOT) { rating ->
            context.getString(
                R.string.therapist_felt_count,
                context.getString(word(rating.felt)),
                rating.sessions,
            )
        }
        add(body(counts))
        if (brief.hardDays.isNotEmpty()) {
            val days = brief.hardDays.joinToString(COMMA) { short(it) }
            add(body(context.getString(R.string.therapist_hard, days)))
        }
    }

    private fun footer(context: Context): List<Mark> = listOf(
        Mark.Rule,
        small(context.getString(R.string.therapist_usual)),
        small(context.getString(R.string.therapist_footer)),
    )

    private fun section(title: String): List<Mark> =
        listOf(Mark.Rule, Mark.Words(title, HEADING, true, RULE_AFTER))

    private fun body(text: String) = Mark.Words(text, BODY, false, GAP_TIGHT)

    private fun small(text: String) = Mark.Words(text, SMALL, false, GAP_TIGHT)

    private fun more(context: Context) =
        Mark.Words(context.getString(R.string.therapist_more), SMALL, false, GAP_ROW)

    @StringRes
    private fun word(felt: Felt) = when (felt) {
        Felt.Easy -> R.string.felt_easy
        Felt.AboutRight -> R.string.felt_about_right
        Felt.Hard -> R.string.felt_hard
    }

    /** How tall a group is, measured the same way it is drawn, and by the same code. */
    private fun height(paint: Paint, marks: List<Mark>): Float =
        marks.fold(0f) { total, mark -> total + tall(paint, mark) }

    private fun tall(paint: Paint, mark: Mark): Float = when (mark) {
        is Mark.Words -> mark.gap + fold(paint, mark).size * mark.size * LEADING
        is Mark.Days -> GAP_ROW + STRIP
        Mark.Rule -> GAP_RULE
    }

    private fun place(canvas: Canvas, paint: Paint, marks: List<Mark>, startY: Float): Float =
        marks.fold(startY) { y, mark -> put(canvas, paint, mark, y) }

    private fun put(canvas: Canvas, paint: Paint, mark: Mark, startY: Float): Float = when (mark) {
        is Mark.Words -> words(canvas, paint, mark, startY)
        is Mark.Days -> days(canvas, paint, mark.done, startY + GAP_ROW)
        Mark.Rule -> rule(canvas, paint, startY)
    }

    private fun words(canvas: Canvas, paint: Paint, mark: Mark.Words, startY: Float): Float {
        var y = startY + mark.gap
        fold(paint, mark).forEach { line ->
            y += mark.size * LEADING
            canvas.drawText(line, MARGIN, y, paint)
        }
        return y
    }

    /**
     * The window as a row of days, with a filled square on the days it was done.
     *
     * A row of dates in words would take four lines for a plan somebody kept up, and
     * the shape of it, three weeks on and then a fortnight of nothing, is the thing a
     * therapist wants and the thing sentences hide. The ticks under the line are
     * weeks, so the row can be read as a length of time rather than as a picture.
     */
    private fun days(canvas: Canvas, paint: Paint, done: List<Boolean>, startY: Float): Float {
        val cell = (WIDTH - MARGIN * 2) / done.size
        val base = startY + MARK
        paint.strokeWidth = HAIRLINE
        canvas.drawLine(MARGIN, base, WIDTH - MARGIN, base, paint)
        done.forEachIndexed { at, wasDone ->
            val x = MARGIN + cell * at
            if (at % DAYS_IN_WEEK == 0) canvas.drawLine(x, base, x, base + TICK, paint)
            if (wasDone) canvas.drawRect(x, base - MARK, x + (cell - DAY_GAP).coerceAtMost(MARK), base, paint)
        }
        return startY + STRIP
    }

    private fun rule(canvas: Canvas, paint: Paint, startY: Float): Float {
        val y = startY + GAP_RULE / 2
        paint.strokeWidth = HAIRLINE
        canvas.drawLine(MARGIN, y, WIDTH - MARGIN, y, paint)
        return startY + GAP_RULE
    }

    /** The lines [mark] takes at its own size and weight, wrapped on words. */
    private fun fold(paint: Paint, mark: Mark.Words): List<String> {
        paint.textSize = mark.size
        paint.typeface = if (mark.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val width = WIDTH - MARGIN * 2
        val lines = mutableListOf<String>()
        var line = StringBuilder()
        mark.text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > width && line.isNotEmpty()) {
                lines.add(line.toString())
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) lines.add(line.toString())
        return lines
    }

    private fun quantity(context: Context, @PluralsRes id: Int, count: Int): String =
        context.resources.getQuantityString(id, count, count)

    /** The long form, for the two or three dates that identify the page. */
    private fun day(epochDay: Long): String = LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))

    /** The short form, for dates in a list, where the year is on the page already. */
    private fun short(epochDay: Long): String = LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}
