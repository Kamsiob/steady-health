package com.kamsiob.steadyhealth.plan

/**
 * What the app should say today about an appointment somebody set.
 *
 * A type rather than a sentence, because the same answer is read in two places that
 * word it differently: a notification saying it in under ten words, and the plan
 * screen saying it beside the date it is already showing. Neither of them decides
 * whether it is due, and neither can quietly start deciding.
 */
sealed interface ReviewPrompt {

    /** Send the one prompt, about the appointment on [onDay]. */
    data class Send(val onDay: Long) : ReviewPrompt

    /** Say nothing today. */
    data object Quiet : ReviewPrompt
}

/**
 * The one prompt before the next appointment. ADDENDUM-03 Part 6, "REVIEW DATE".
 *
 * "The person can set when they next see their therapist. Two days before, one
 * prompt." That is the whole feature, and it earns a file of its own because every
 * rule in it is one that breaks quietly: a second prompt about the same appointment, a
 * prompt the morning after it, a prompt about a date that has been and gone. Each of
 * those is the app talking about something the person has already dealt with, which is
 * how somebody decides an app is not worth hearing from.
 *
 * A pure function of a date and today, for the reason DailyPrompt is one: what calls
 * it wakes at seven in the morning with nobody watching, and a scheduler is a thing
 * that can be wrong at that hour while a function cannot.
 *
 * Nothing here reads a day somebody did not do anything. This prompt is about an
 * appointment being close and never about what was done before it. The page is ready
 * whether the plan was followed every day or none, and the words carry no opinion
 * about which it was.
 *
 * WHETHER THIS COUNTS AGAINST THE TWO A WEEK. It does, and whoever wires it treats it
 * as one of the capped kinds. ADDENDUM-03 Part 13 carves out exactly one exception,
 * the daily prompt, and Reminders already holds it; "everything else capped at two a
 * week combined" is a ceiling worth having only while a new prompt cannot argue its
 * way out of it, and this one would have the best argument of any that comes after it.
 *
 * The cost of that is real and is paid for here. This is the only capped prompt that
 * expires: a Sunday write-up is still there on Monday, and an appointment is not. So
 * two things follow. Put this at the head of Reminders.CAPPED, ahead of a longer walk
 * being ready, because it is the only one of them that cannot simply go out tomorrow.
 * And the window below is two days wide rather than one, so that a day lost to the
 * ceiling, to a phone that was switched off, or to a job that did not run is not the
 * feature lost with it.
 */
object ReviewDate {

    /** Two days before, as Part 6 words it. The day the prompt is meant for. */
    const val DAYS_BEFORE = 2L

    /**
     * The last day it may still go out.
     *
     * The day before the appointment, and only ever as the catch up for a prompt that
     * never went. The day of the appointment is not in the window at all: somebody
     * on their way to see their physio does not need the app to mention it, and
     * "your page is ready" arriving in the waiting room is a day late rather than
     * early.
     */
    const val LAST_CHANCE = 1L

    /**
     * Whether the one prompt is due today, for one appointment.
     *
     * [reviewDay] is the appointment as an epoch day, or null when nobody has set one.
     * [sentFor] is every appointment day that has already had its prompt, which is
     * what makes "exactly once per appointment" true across a moved date as well as
     * across a day. The date is the key, so moving the appointment earns the new date
     * its own prompt and leaves the old one with the one it already had.
     */
    fun due(reviewDay: Long?, today: Long, sentFor: Set<Long> = emptySet()): ReviewPrompt {
        if (reviewDay == null || reviewDay in sentFor) return ReviewPrompt.Quiet
        val daysAway = reviewDay - today
        if (daysAway !in LAST_CHANCE..DAYS_BEFORE) return ReviewPrompt.Quiet
        return ReviewPrompt.Send(reviewDay)
    }

    /**
     * The same question asked across every plan that has a date on it.
     *
     * ADDENDUM-03 Part 6: a physio plan and an OT plan coexist, each labelled, each
     * separate, and each with its own appointment. The soonest one that is actually
     * due wins, and at most one answer comes back because only one notification goes
     * out in a day. Two appointments two days apart do not collide: the nearer one is
     * prompted today, and tomorrow the other is one day out and takes its catch up.
     */
    fun due(reviewDays: List<Long>, today: Long, sentFor: Set<Long> = emptySet()): ReviewPrompt =
        reviewDays.sorted()
            .map { due(it, today, sentFor) }
            .filterIsInstance<ReviewPrompt.Send>()
            .firstOrNull()
            ?: ReviewPrompt.Quiet

    /**
     * The next appointment there is, for a screen that wants to name one.
     *
     * A date behind us is not the next anything, so it is left out. A date that is
     * today stays in, because a screen asking what is next would otherwise skip over
     * this afternoon to next month. It is still never a prompt.
     */
    fun soonest(reviewDays: List<Long>, today: Long): Long? =
        reviewDays.filter { it >= today }.minOrNull()
}
