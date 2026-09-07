package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.NextDayFeel
import com.kamsiob.steadyhealth.domain.TalkTest

/** One finished session, as the progression rules see it. */
data class DoneSession(
    val epochDay: Long,
    val stepIndex: Int,
    val durationSeconds: Int,
    val talkTest: TalkTest?,
    val nextDayFeel: NextDayFeel?,
)

/** What the engine decided to do about the next session. */
sealed interface Progression {
    /** Stay where you are. The ordinary answer, and never framed as a failure. */
    data object Stay : Progression

    /** The next step is ready. Offered, never assigned. */
    data class Offer(val toStepIndex: Int) : Progression

    /** One step back, with the reason, shown once. */
    data class StepBack(val toStepIndex: Int, val because: Because) : Progression

    enum class Because { WorseTheNextDay, TalkTestTwice, TimeAway }
}

/**
 * Progression, from LOGIC.md section 6.
 *
 * Every rule here is deterministic and every threshold is a named constant, so
 * the document and the behaviour are linked by the name rather than by somebody
 * remembering. The engine takes the day it is being run on rather than reading a
 * clock, so a test can ask what happens on any day without pretending.
 *
 * The shape of it is that going up is hard and coming down is easy, which is the
 * opposite of most exercise apps and is the point. An offer needs two easy
 * sessions and no bad next day; a single bad next day drops a step.
 */
object ProgressionEngine {

    /** Two sessions at the current step, both easy enough, before an offer. */
    const val SESSIONS_BEFORE_AN_OFFER = 2

    /** "Not yet" suppresses the offer for this long. */
    const val OFFER_DECLINED_DAYS = 14

    /**
     * An offered walk may not exceed this share of the longest walk in the last
     * month. Precautionary default from Frandsen et al. 2025: what predicts
     * injury is one session much longer than anything recently, not a steady
     * climb.
     */
    const val SPIKE_CAP = 1.10

    const val SPIKE_WINDOW_DAYS = 30

    /** No sessions for this long: one step back. */
    const val SHORT_GAP_DAYS = 8

    /** This long: two steps back, and a fortnight where nothing is offered. */
    const val LONG_GAP_DAYS = 29

    const val EASING_DAYS = 14

    private const val SECONDS_PER_MINUTE = 60

    /**
     * What to do next, given everything that has happened on this ladder.
     *
     * Order matters: a step back beats an offer, because somebody who felt worse
     * yesterday should not be asked whether they want to go up today.
     */
    /**
     * What to do next, given everything that has happened on this ladder.
     *
     * Written as a chain because the order is the rule: a step back beats an
     * offer, since somebody who felt worse yesterday should not be asked whether
     * they want to go up today. Each link answers one question from LOGIC.md
     * section 6 and returns nothing when that rule does not apply.
     */
    fun decide(
        currentStepIndex: Int,
        sessions: List<DoneSession>,
        stepAmounts: List<Int>,
        today: Long,
        offerDeclinedUntilDay: Long? = null,
    ): Progression {
        val atThisStep = sessions.filter { it.stepIndex == currentStepIndex }.sortedBy { it.epochDay }
        val lastTwo = atThisStep.takeLast(SESSIONS_BEFORE_AN_OFFER)

        return afterAWorseDay(currentStepIndex, atThisStep)
            ?: afterTwoHardSessions(currentStepIndex, lastTwo)
            ?: afterTimeAway(currentStepIndex, sessions, today)
            ?: offerIfReady(currentStepIndex, lastTwo, sessions, stepAmounts, today, offerDeclinedUntilDay)
            ?: Progression.Stay
    }

    /** One bad next day drops a step. Going down is easy, on purpose. */
    private fun afterAWorseDay(current: Int, atThisStep: List<DoneSession>): Progression? {
        val worse = atThisStep.lastOrNull()?.nextDayFeel == NextDayFeel.Worse
        return if (worse && current > 0) {
            Progression.StepBack(current - 1, Progression.Because.WorseTheNextDay)
        } else {
            null
        }
    }

    /** Two sessions in a row where they could not talk. */
    private fun afterTwoHardSessions(current: Int, lastTwo: List<DoneSession>): Progression? {
        val bothHard = lastTwo.size == SESSIONS_BEFORE_AN_OFFER &&
            lastTwo.all { it.talkTest == TalkTest.No }
        return if (bothHard && current > 0) {
            Progression.StepBack(current - 1, Progression.Because.TalkTestTwice)
        } else {
            null
        }
    }

    /** Gap decay, and never framed as a loss on the screen that shows it. */
    private fun afterTimeAway(
        current: Int,
        sessions: List<DoneSession>,
        today: Long,
    ): Progression? {
        val lastDay = sessions.maxOfOrNull { it.epochDay } ?: return null
        val gap = today - lastDay
        val back = when {
            gap >= LONG_GAP_DAYS -> 2
            gap >= SHORT_GAP_DAYS -> 1
            else -> 0
        }
        return if (back == 0 || current == 0) {
            null
        } else {
            Progression.StepBack((current - back).coerceAtLeast(0), Progression.Because.TimeAway)
        }
    }

    /** Two easy sessions, nothing worse the next day, and inside the cap. */
    private fun offerIfReady(
        current: Int,
        lastTwo: List<DoneSession>,
        sessions: List<DoneSession>,
        stepAmounts: List<Int>,
        today: Long,
        offerDeclinedUntilDay: Long?,
    ): Progression? {
        if (offerDeclinedUntilDay != null && today < offerDeclinedUntilDay) return null

        val next = current + 1
        if (next >= stepAmounts.size) return null

        val ready = lastTwo.size == SESSIONS_BEFORE_AN_OFFER &&
            lastTwo.all { it.talkTest == TalkTest.YesEasily || it.talkTest == TalkTest.JustAbout } &&
            lastTwo.none { it.nextDayFeel == NextDayFeel.Worse }

        if (!ready) return null
        if (!withinSpikeCap(next, sessions, stepAmounts, today, current)) return null
        return Progression.Offer(next)
    }

    /**
     * True when the next step is not a jump past what this person has actually
     * been doing.
     *
     * LOGIC.md sets the cap at 110% of the longest session in the last month, and
     * taken as arithmetic against the next rung that rule makes the ladder in the
     * same document unclimbable: its steps are 2, 4, 6, 8, 11, 14, 16, 20, 25, 30,
     * and every one of those is more than 110% of the one before it. The smallest
     * jump on the whole ladder is 14%.
     *
     * So the cap is read as what it is for. Frandsen et al. found that what
     * predicts injury is a single session much longer than anything done recently,
     * not a steady climb, and the ladder is the steady climb. The dangerous case
     * is somebody sitting at step seven whose actual walks have been eight
     * minutes, being offered twenty.
     *
     * The rule that catches that and leaves the ladder climbable: if they have
     * genuinely been doing the step they are on, the next rung is the ladder's own
     * design and it passes. If their recent walks are below their current step,
     * the 110% arithmetic applies against what they have really been doing.
     *
     * Recorded in DECISIONS.md, because it resolves a contradiction rather than
     * implementing a sentence.
     */
    fun withinSpikeCap(
        nextIndex: Int,
        sessions: List<DoneSession>,
        stepAmounts: List<Int>,
        today: Long,
        currentStepIndex: Int = nextIndex - 1,
    ): Boolean {
        val longest = sessions
            .filter { today - it.epochDay <= SPIKE_WINDOW_DAYS }
            .maxOfOrNull { it.durationSeconds / SECONDS_PER_MINUTE }
            ?: return true
        if (longest <= 0) return true

        val currentAmount = stepAmounts.getOrNull(currentStepIndex) ?: return true
        if (longest >= currentAmount) return true

        return stepAmounts[nextIndex] <= longest * SPIKE_CAP
    }
}
