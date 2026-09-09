package com.kamsiob.steadyhealth.session

/**
 * The four answers to the one question asked after time away. ADDENDUM-03 Part 15.
 *
 * Four answers rather than a scale, because a scale invites somebody to grade
 * themselves and this app never grades anybody. No free text, because free text about
 * why a person was away is health information the app would then be holding, and these
 * four carry everything the engine actually acts on.
 *
 * [id] is what gets stored, so the wording on the screen can change without the stored
 * answers coming to mean something else.
 */
enum class WhyAway(val id: String) {
    Busy("busy"),
    Unwell("unwell"),
    Away("away"),
    RatherNotSay("rather_not_say"),
    ;

    /**
     * What the engine acts on, which is not always what was tapped.
     *
     * "I'd rather not say" is treated as busy, because busy is the gentler of the two
     * guesses and guessing gently is the only defensible thing to do with an answer
     * somebody deliberately withheld. Everything downstream reads this rather than the
     * raw answer, so there is exactly one place where that decision lives.
     */
    val counts: WhyAway get() = if (this == RatherNotSay) Busy else this
}

/**
 * A sentence the screen may owe somebody, named rather than written.
 *
 * Both belong to "I was unwell" and nothing else. [WorthAWord] is the one line in this
 * whole feature that points at a doctor, so it is said once in a person's life and
 * never repeated: a second showing reads as the app deciding something about them.
 */
enum class Aside {
    /** "We'll take it slowly for a couple of weeks. That's what bodies need." */
    TakingItSlowly,

    /** The hospital or fall line. Once, ever. */
    WorthAWord,
}

/** An answer already given, and which gap it was given about. */
data class Answered(
    val why: WhyAway,
    /** The last session before the gap. This is what makes one gap a different gap. */
    val afterLastSessionDay: Long,
    /** The day they came back and answered. The ramp is counted from here. */
    val onDay: Long,
)

/** Everything the engine needs to decide what coming back means today. */
data class Gap(
    val lastSessionDay: Long?,
    val today: Long,
    /** Set once this gap has been answered, which is what stops it being asked twice. */
    val answered: Answered? = null,
    /** True once the hospital or fall line has been shown, ever, to anybody's history. */
    val saidWorthAWordBefore: Boolean = false,
)

/**
 * What a gap does to the sessions that follow it.
 *
 * Two different things get smaller and they are deliberately kept apart. [stepsSmaller]
 * is rungs on the ladder, the same steps [com.kamsiob.steadyhealth.engine.ProgressionEngine]
 * counts. [rampingOn] is the session itself, and it feeds
 * [SessionInputs.rampingAfterUnwell] verbatim, which is why there is no number here for
 * how much lighter a session gets. SessionEngine already owns that number twice over,
 * once as its four day rule and once as the ramp, and a second copy of it in this file
 * would be a second answer to a question that already has one.
 */
data class Easing(
    /** How long they were away, measured on the day they answered. */
    val days: Int,
    /** What they said, kept as tapped. Read [WhyAway.counts] for what it means. */
    val why: WhyAway,
    /** Rungs back on the ladder. */
    val stepsSmaller: Int,
    /**
     * How many sessions the smaller step lasts, when the answer bounds it in sessions.
     * Null when it does not: "I was away" leaves the ordinary ladder rules to bring the
     * step back, and a ramp is counted in days instead.
     */
    val forSessions: Int?,
    /** The last day of the ramp, or null when there is no ramp. */
    val rampUntilDay: Long?,
    /** Sentences the screen owes, in the order they should be read. */
    val asides: List<Aside>,
    /** Re-run O2 and O3 with the previous answers filled in. */
    val startAgain: Boolean,
) {
    /**
     * Whether the session planned for [day] is still inside the ramp.
     *
     * Pass the result straight into [SessionInputs.rampingAfterUnwell]. A caller that
     * works out its own answer here has forked the rule.
     */
    fun rampingOn(day: Long): Boolean = rampUntilDay != null && day <= rampUntilDay
}

/** What the app does when somebody opens it, before it offers them anything. */
sealed interface ComingBack {

    /** They were here recently enough that there is nothing to ask and nothing to do. */
    data object Ordinary : ComingBack

    /**
     * The question is due. [startAgain] says to re-run O2 and O3 with the previous
     * answers filled in, which happens alongside the question rather than instead of it.
     */
    data class Ask(val days: Int, val startAgain: Boolean) : ComingBack

    /** This gap has been answered already. Apply the easing, and do not ask again. */
    data class Settled(val easing: Easing) : ComingBack
}

/**
 * The interruption rules. ADDENDUM-03 Part 15, folded into LOGIC.md 15b, replacing the
 * gap decay in LOGIC.md 6 entirely.
 *
 * The old rule guessed. It read the length of a gap and took rungs off the ladder on
 * the strength of that number alone, so somebody who spent a fortnight on a beach was
 * treated exactly like somebody who spent it in a hospital bed. The number is the same
 * and the two bodies are not, so the app asks instead of guessing, and the answer
 * decides. The cost is one screen between a person and their session, which is why it
 * is one question with no free text and why it is asked once per gap.
 *
 * Run when the app opens rather than after a session, because somebody who has been
 * away for a month meets it before they do anything.
 *
 * Everything here is deterministic and every result is typed. The two sentences come
 * back as [Aside] values for a screen to turn into words, so they are written once, in
 * one place, and this engine cannot drift from them.
 */
object ComingBackEngine {

    /** A gap this long or longer gets the question. ADDENDUM-03 Part 15. */
    const val ASK_AFTER_DAYS = 7

    /** A gap this long re-runs the first two onboarding questions and starts further back. */
    const val START_AGAIN_AFTER_DAYS = 60

    /** The ramp is a fortnight, which is what "a couple of weeks" is in days. */
    const val RAMP_DAYS = 14

    /** Busy, away, or an answer withheld: one rung. */
    const val ONE_STEP = 1

    /** Unwell: two rungs. */
    const val UNWELL_STEPS = 2

    /** Sixty days or more: three rungs, whatever the answer was. */
    const val START_AGAIN_STEPS = 3

    /** How many sessions "one step smaller" lasts when the reason was a busy life. */
    const val BUSY_SESSIONS = 2

    /**
     * What today is, given a gap.
     *
     * The question is answered once per gap for all four answers, not only for "I'd
     * rather not say". The spec attaches the phrase to that answer because it is the
     * one somebody would be tempted to re-ask, but re-asking any of them would be the
     * app failing to hear an answer it already has.
     */
    fun of(gap: Gap): ComingBack {
        val last = gap.lastSessionDay ?: return ComingBack.Ordinary
        val given = gap.answered?.takeIf { it.afterLastSessionDay == last }
        if (given != null) {
            return ComingBack.Settled(easing(given.why, given.onDay, last, gap.saidWorthAWordBefore))
        }
        val days = (gap.today - last).toInt()
        if (days < ASK_AFTER_DAYS) return ComingBack.Ordinary
        return ComingBack.Ask(days, days >= START_AGAIN_AFTER_DAYS)
    }

    /**
     * The answer, at the moment somebody taps it.
     *
     * The caller stores the [Answered] this implies so that [of] returns Settled from
     * here on, and applies the [Easing] that comes back.
     */
    fun answer(why: WhyAway, gap: Gap): Easing =
        easing(why, gap.today, gap.lastSessionDay ?: gap.today, gap.saidWorthAWordBefore)

    /**
     * The easing itself.
     *
     * The gap is measured on the day they came back and answered, not on whatever day
     * this is being recomputed. Otherwise a person who answers and then does not manage
     * a session for another week finds the ramp moving away from them and the rules
     * getting stricter while they were doing nothing, which is the opposite of the
     * point. The cost is that a very long silence after an answer keeps reading as the
     * shorter gap it was answered about, and that is the right way round.
     */
    private fun easing(why: WhyAway, onDay: Long, lastSessionDay: Long, saidBefore: Boolean): Easing {
        val days = (onDay - lastSessionDay).toInt()
        val counts = why.counts
        val startAgain = days >= START_AGAIN_AFTER_DAYS
        val unwell = counts == WhyAway.Unwell
        val ramping = unwell || startAgain
        return Easing(
            days = days,
            why = why,
            stepsSmaller = stepsSmaller(unwell, startAgain),
            // Two durations at once would be two rules, so the ramp wins where they
            // meet: it is the longer of the two and the one somebody was told about.
            forSessions = if (counts == WhyAway.Busy && !ramping) BUSY_SESSIONS else null,
            rampUntilDay = if (ramping) onDay + RAMP_DAYS else null,
            asides = listOfNotNull(
                Aside.TakingItSlowly.takeIf { unwell },
                Aside.WorthAWord.takeIf { unwell && !saidBefore },
            ),
            startAgain = startAgain,
        )
    }

    /**
     * Rungs back, taking whichever rule asks for more.
     *
     * The sixty day rule is about the length of the silence and the unwell rule is
     * about what happened in it, so they are not alternatives. Somebody who was unwell
     * for three months gets the three the long gap asks for, not the two.
     */
    private fun stepsSmaller(unwell: Boolean, startAgain: Boolean): Int = maxOf(
        if (unwell) UNWELL_STEPS else ONE_STEP,
        if (startAgain) START_AGAIN_STEPS else 0,
    )
}
