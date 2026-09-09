package com.kamsiob.steadyhealth.ai

/**
 * The one line at the end of a session. AI.md job 7.
 *
 * "Optional, offered at the end of a session, one line, spoken or typed. Its only
 * extra purpose is to feed what the app noticed. Without the model the person picks
 * from the grid, and most will skip it entirely, which is fine."
 *
 * This is the without-the-model half, which is the half almost everybody uses, and it
 * is built first for the same reason the validators were: the fallback is the real
 * product and the model is the shortcut.
 *
 * The vocabulary is job 2's twenty-four tags with nothing added, because the point of
 * this line is to feed "what the app noticed", and a tag the noticing engine has never
 * heard of feeds nothing. It is also what keeps this from becoming a diary: there are
 * twenty-four things somebody can say here and no free text at all in the fallback.
 *
 * Which ones are offered is the only decision worth making. All twenty-four at the end
 * of a session is a wall of choices for somebody who has just stood up out of a chair
 * eleven times, and most of them are about a day rather than about the last four
 * minutes. So the grid is the ones that answer "how was that", and the rest stay where
 * they belong, on the daily check-in.
 */
object AfterASession {

    /** At most this many, because Part 14 says a session ends and does not continue. */
    const val MOST_CHOSEN = 3

    /**
     * The tags offered after a session, in the order they are shown.
     *
     * Body first, because it is the one that changes what the app does next: sore
     * and pain feed the seven day suppression. Movement second, mood third. Nothing
     * about food and nothing about sleep, which are questions about a day and are
     * asked once a day where they belong.
     */
    val offered: List<Tag> get() = here.flatMap { group -> Tags.all.filter { it.group == group } }

    /**
     * The three groups that answer "how was that" rather than "how was today", in
     * the order they are shown, which is not the order the daily grid uses.
     */
    private val here = listOf(TagGroup.Body, TagGroup.Movement, TagGroup.Mood)

    /**
     * What a set of chosen tags comes to, capped and in the library's own order.
     *
     * Order comes from [Tags.all] rather than from the order somebody tapped, so that
     * the same three tags are the same three tags however they were picked, and so
     * nothing downstream can read an order into them that nobody meant.
     */
    fun chosen(picked: Set<String>): List<Tag> =
        offered.filter { it.id in picked }.take(MOST_CHOSEN)

    /** Whether one more may be picked, for a grid that stops rather than warns. */
    fun roomForMore(picked: Set<String>): Boolean = picked.size < MOST_CHOSEN
}
