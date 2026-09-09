package com.kamsiob.steadyhealth.plan

import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.session.Area
import com.kamsiob.steadyhealth.session.Felt

/** Who changed a line. The only thing the app knows about a change, and it is enough. */
enum class ChangedBy { ThePerson, TheirTherapist }

/**
 * A line that no longer reads the way it was first given.
 *
 * This is on the page because the alternative is a therapist reading numbers they
 * did not set and taking them for their own. LOGIC.md 17: reps and frequency change
 * only when the person changes them or marks that their therapist did, so the app can
 * always say which of the two it was, and [was] carries the wording it replaced.
 */
data class Changed(val onDay: Long, val by: ChangedBy, val was: String)

/**
 * One confirmed line of a therapist's plan, with the history the app keeps about it.
 *
 * [PlanItem] is deliberately a type with nothing on it that moves, so the things that
 * do move sit out here around it rather than inside it: an id to hang the record of
 * what was done on, a note of a change, and a flag when the line asks for something
 * the person said they avoid. LOGIC.md 17: a conflicting line is flagged and left in,
 * never dropped, and the flag points at the therapist rather than at the app's own
 * judgement.
 */
data class PlannedLine(
    val id: String,
    val item: PlanItem,
    val conflictsWith: Exclusion? = null,
    val changed: Changed? = null,
)

/**
 * One therapist's plan, labelled by whose it is.
 *
 * ADDENDUM-03 Part 6: a physio plan and an OT plan coexist, each labelled, each
 * separate. That is why the page is built from one of these rather than from all of
 * them at once. Somebody sitting in front of their physio wants their physio's page,
 * and an occupational therapist's lines in that room are noise.
 */
data class TherapistPlan(
    val id: String,
    /** Whose plan this is, in the person's own words: "physio", "hand therapist". */
    val label: String,
    val givenOnDay: Long,
    val reviewDay: Long? = null,
    val lines: List<PlannedLine> = emptyList(),
)

/**
 * One time one plan line was done.
 *
 * A line that was skipped is not one of these. It is simply absent, and the page then
 * shows a line with no dates and no numbers, which is the truth without a nought
 * standing in for it.
 */
data class PlanDone(
    val lineId: String,
    val epochDay: Long,
    val result: Int,
    val target: Int,
    /** True when the person typed the number rather than the phone counting it. */
    val selfCounted: Boolean = false,
    /** True when they took the easier version of the movement and said so. */
    val madeEasier: Boolean = false,
)

/**
 * Something hurt, and when, in the person's own words.
 *
 * ADDENDUM-03 Part 6 puts this on the page because it is the one thing a therapist
 * cannot get any other way: by the appointment nobody remembers which day the knee
 * was bad or what they were doing at the time. [words] is never paraphrased and never
 * summarised, and [duringLineId] is what turns "my knee hurt" into something a
 * therapist can act on.
 */
data class SaidHurt(
    val area: Area,
    val onDay: Long,
    val words: String? = null,
    val duringLineId: String? = null,
)

/** How one session felt, on the day it was done. The person's own rating, untouched. */
data class Rated(val epochDay: Long, val felt: Felt)

/**
 * Everything the page is built from. Plain lists, so this is a JVM unit test away.
 *
 * [otherSessionDays] counts days with a session the app suggested rather than one
 * from this plan. It is here because a therapist reading the page should know what
 * else the person has been doing, and it is a number of its own rather than part of
 * the plan's because LOGIC.md 17 says the two are never merged.
 */
data class BriefInputs(
    val plan: TherapistPlan,
    val today: Long,
    val done: List<PlanDone> = emptyList(),
    val hurt: List<SaidHurt> = emptyList(),
    val ratings: List<Rated> = emptyList(),
    /** The day of the last appointment, when the person set one. Starts the window. */
    val sinceDay: Long? = null,
    val otherSessionDays: Int = 0,
)

/** The fewest, the usual and the most somebody managed on one line. */
data class Counts(val fewest: Int, val usual: Int, val most: Int)

/** The two marks the person put on their own numbers, counted rather than weighed. */
data class Marks(val madeEasier: Int, val selfCounted: Int)

/**
 * What happened on one line, which is the second half of every row on the page.
 *
 * [onDays] is the whole of the "when": every day the line was done, ascending, so a
 * renderer can print the dates or draw a row of marks across the window without
 * asking anything back. [times] is kept apart from the number of days because a plan
 * asking for something twice a day is not reported honestly by either number alone.
 */
data class WhatHappened(
    val onDays: List<Long>,
    val times: Int,
    val counts: Counts?,
    val marks: Marks,
) {
    val days: Int get() = onDays.size
    val firstDay: Long? get() = onDays.firstOrNull()
    val lastDay: Long? get() = onDays.lastOrNull()

    /** False when this line was never done, which the page says plainly and once. */
    val ever: Boolean get() = times > 0
}

/**
 * One row: the line as it was given, and what happened. In that order, always.
 *
 * The line as given is carried whole rather than copied field by field, so the words
 * on the page are the words on the sheet and there is nowhere for a paraphrase to get
 * in between them.
 */
data class PlanLine(val given: PlannedLine, val happened: WhatHappened) {
    val lineId: String get() = given.id

    /** The line as the therapist wrote it, which is what a renderer prints first. */
    val text: String get() = given.item.line
}

/** One sentence the person wrote, on the day they wrote it. */
data class Said(val onDay: Long, val words: String)

/**
 * One part of the body, and everything the person said about it in the window.
 *
 * Grouped by area rather than listed by date, because a therapist reads this by body
 * part and then looks at the dates, and a flat diary of eleven entries is the sort of
 * thing that gets skimmed past in a corridor.
 */
data class HurtEntry(
    val area: Area,
    /** Days it was mentioned, counted in days rather than in mentions. */
    val days: Int,
    val lastDay: Long,
    /** The plan lines being done when it was said, in the therapist's own words. */
    val during: List<String>,
    /** Their own sentences, most recent first, at most [TherapistBriefs.MOST_SAID_EACH]. */
    val said: List<Said>,
)

/** How many sessions felt one way. A count of the person's own answer, and no more. */
data class RatingCount(val felt: Felt, val sessions: Int)

/**
 * The top of the page: whose plan, over which stretch of days, and how many of them.
 *
 * [daysWithPlan] is a count of days and never a share of anything. There is nowhere
 * in this type to put a percentage, a grade or a run of days, which is deliberate:
 * ADDENDUM-03 Part 6 says what was done and what was not is reported without nagging
 * and without scoring, and the way to keep a promise like that is to leave the field
 * out rather than to remember not to fill it in.
 */
data class BriefHeading(
    val planLabel: String,
    val givenOnDay: Long,
    val fromDay: Long,
    val toDay: Long,
    val reviewDay: Long?,
    val daysWithPlan: Int,
    /** Days with a session of the app's own, kept beside the plan and never inside it. */
    val otherSessionDays: Int,
)

/**
 * One page for the next appointment. ADDENDUM-03 Part 6, LOGIC.md 17, MASTER_SPEC 6.19.
 *
 * The order of the fields is the order of the page and it is the order Part 6 sets:
 * what was asked for, what was done and when, the numbers, what hurt and when, and
 * the person's own ratings. A clinician reads this standing up, in under a minute,
 * with the next appointment already waiting, so it is arranged to be read from the top
 * and abandoned at any point without the important part having been below the fold.
 *
 * What is on it: every line of the plan, in the order the therapist wrote them, kept
 * whole even when a line was never done, because dropping a line a therapist set is
 * the app editing their plan. Beside each line, the days it was done and the fewest,
 * the usual and the most the person managed. Then what hurt, grouped by part of the
 * body, with the person's own sentences and the line they were doing at the time.
 * Then how the sessions felt, in the person's own three words.
 *
 * What is deliberately not on it, and none of these is an oversight: a share of the
 * plan done, a grade, a run of days, a colour, a trend line, weight, blood pressure,
 * anything the app inferred, and any sentence drawing a conclusion from any of it.
 * COMPLIANCE.md: here the app is a record keeper and not a clinician. It states what
 * was done and what was not and draws nothing from it, and the person holding the
 * page is the one qualified to do that.
 */
data class TherapistBrief(
    val heading: BriefHeading,
    val lines: List<PlanLine>,
    val hurt: List<HurtEntry>,
    val ratings: List<RatingCount>,
    /** The days rated hard, most recent first, for reading against the days that hurt. */
    val hardDays: List<Long>,
    /** Sentences that did not fit, counted so that nothing is quietly dropped. */
    val moreSaid: Int,
)

/**
 * Building the page, from plain lists and nothing else.
 *
 * Pure, and given lists rather than a repository so that every rule here is a JVM unit
 * test. Nothing in this file renders anything: what it produces is finished, and a
 * renderer lays it out with no decision left to take.
 */
object TherapistBriefs {

    /**
     * Twelve weeks, and the page never reaches further back than that.
     *
     * Longer than the gap between appointments in nearly every course of treatment,
     * and a page covering more has stopped being a page about this stretch of it.
     * Whole weeks, so a renderer drawing the days as a grid gets even rows of seven.
     */
    const val LONGEST_WINDOW = 84

    /** Sentences kept for each part of the body. Two recent ones read; six get skimmed. */
    const val MOST_SAID_EACH = 2

    /** Hard days named. Enough to read beside the days that hurt, and not a diary. */
    const val MOST_HARD_DAYS = 6

    /**
     * The page, from one plan and the history around it.
     *
     * The window is the honest one: it starts at the last appointment when the person
     * set one, otherwise at the day the plan was given, and it never reaches further
     * back than [LONGEST_WINDOW]. Anything outside it is left out rather than folded
     * in, because a number covering a different stretch of days than the one printed
     * at the top of the page is a number that will be misread.
     */
    fun of(inputs: BriefInputs): TherapistBrief {
        val from = windowStart(inputs)
        val to = inputs.today
        val done = inputs.done.filter { it.epochDay in from..to }
        val hurt = inputs.hurt.filter { it.onDay in from..to }
        val ratings = inputs.ratings.filter { it.epochDay in from..to }
        val byLine = done.groupBy { it.lineId }
        val entries = hurtEntries(hurt, inputs.plan)
        val heading = BriefHeading(
            planLabel = inputs.plan.label,
            givenOnDay = inputs.plan.givenOnDay,
            fromDay = from,
            toDay = to,
            reviewDay = inputs.plan.reviewDay,
            daysWithPlan = done.map { it.epochDay }.distinct().size,
            otherSessionDays = inputs.otherSessionDays,
        )
        return TherapistBrief(
            heading = heading,
            lines = inputs.plan.lines.map { row(it, byLine[it.id].orEmpty()) },
            hurt = entries,
            ratings = ratingCounts(ratings),
            hardDays = hardDays(ratings),
            moreSaid = hurt.count { !it.words.isNullOrBlank() } - entries.sumOf { it.said.size },
        )
    }

    /**
     * The first day the page covers.
     *
     * Held at or before today so that a plan dated tomorrow, which happens when
     * somebody sets one up sitting in front of their therapist, produces a page rather
     * than an inverted window that quietly reports nothing.
     */
    private fun windowStart(inputs: BriefInputs): Long {
        val longest = inputs.today - LONGEST_WINDOW + 1
        return maxOf(inputs.plan.givenOnDay, inputs.sinceDay ?: longest, longest)
            .coerceAtMost(inputs.today)
    }

    private fun row(line: PlannedLine, done: List<PlanDone>): PlanLine = PlanLine(
        given = line,
        happened = WhatHappened(
            onDays = done.map { it.epochDay }.distinct().sorted(),
            times = done.size,
            counts = counts(done.map { it.result }),
            marks = Marks(
                madeEasier = done.count { it.madeEasier },
                selfCounted = done.count { it.selfCounted },
            ),
        ),
    )

    /**
     * The fewest, the usual and the most, or nothing at all when it was never done.
     *
     * The usual one is the lower of the two middles on an even number of days, rather
     * than an average. One good Tuesday of thirty pulls an average somewhere the
     * person never actually was, and rounding the middle upward would flatter them on
     * a page a therapist is going to plan the next six weeks from.
     */
    private fun counts(results: List<Int>): Counts? {
        if (results.isEmpty()) return null
        val sorted = results.sorted()
        return Counts(
            fewest = sorted.first(),
            usual = sorted[(sorted.size - 1) / 2],
            most = sorted.last(),
        )
    }

    private fun hurtEntries(hurt: List<SaidHurt>, plan: TherapistPlan): List<HurtEntry> = hurt
        .groupBy { it.area }
        .map { (area, mentions) -> entry(area, mentions, plan) }
        .sortedByDescending { it.lastDay }

    private fun entry(area: Area, mentions: List<SaidHurt>, plan: TherapistPlan) = HurtEntry(
        area = area,
        days = mentions.map { it.onDay }.distinct().size,
        lastDay = mentions.maxOf { it.onDay },
        during = mentions
            .mapNotNull { it.duringLineId }
            .distinct()
            .mapNotNull { id -> plan.lines.firstOrNull { line -> line.id == id }?.item?.line },
        said = mentions
            .mapNotNull { said -> said.words?.takeIf { it.isNotBlank() }?.let { Said(said.onDay, it) } }
            .sortedByDescending { it.onDay }
            .take(MOST_SAID_EACH),
    )

    /**
     * The three ratings, counted, and only the ones that happened.
     *
     * In the order they are offered, easiest first, and a rating nobody gave is left
     * off rather than printed as a nought, so a renderer prints the list as it arrives
     * and has nothing to decide.
     */
    private fun ratingCounts(ratings: List<Rated>): List<RatingCount> = Felt.entries
        .mapNotNull { felt ->
            val sessions = ratings.count { it.felt == felt }
            if (sessions > 0) RatingCount(felt, sessions) else null
        }

    private fun hardDays(ratings: List<Rated>): List<Long> = ratings
        .filter { it.felt == Felt.Hard }
        .map { it.epochDay }
        .distinct()
        .sortedDescending()
        .take(MOST_HARD_DAYS)
}
