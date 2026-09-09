package com.kamsiob.steadyhealth.session

/**
 * How high the chair is, roughly, in the only terms somebody can answer.
 *
 * ADDENDUM-03 Part 15 says "a rough height" and the word rough is the whole design.
 * A chair stand from a dining chair and a chair stand from a low armchair are not the
 * same movement, so the number is meaningless unless the chair holds still. The
 * obvious way to hold it still is to store centimetres, and that is the wrong way:
 * almost nobody will fetch a tape measure, so the figure would be a guess wearing the
 * clothes of a measurement, and the same person would guess differently in January
 * and in June while the stored number went on looking exact.
 *
 * So three named heights, told apart by where the knees end up when somebody sits in
 * the chair, which is the position they are already in when they do the movement.
 * That question can be answered in a second, without standing up, without a tape, and
 * answered the same way a year later by the same person, which is the only property
 * this feature actually needs.
 *
 * The trade-off is real and accepted: these buckets cannot tell a low dining chair
 * from a high one, and one person's Level is another person's High. Neither matters,
 * because the only comparison the app ever makes is a person against their own
 * earlier self, and within one home a bucket boundary is crossed by swapping the
 * furniture rather than by drifting across it.
 *
 * Declared low to high, so a change has a direction without anybody storing one.
 */
enum class ChairHeight(val id: String) {

    /** Knees end up higher than the hips. A sofa, or a soft low armchair. */
    Low("low"),

    /** Thighs roughly level, feet flat on the floor. An ordinary dining chair. */
    Level("level"),

    /** Hips higher than the knees, feet only just down. A tall chair, or the bed. */
    High("high"),
}

/**
 * What a chair stand's setup line has to say about the chair, on top of its own words.
 *
 * Part 15: "show it in the setup line every time". Every time, because the point is
 * not to inform somebody once, it is to put the same chair in the room at the moment
 * they are choosing one. A movement's own setup line already says what to do with the
 * chair; this says which chair, and only that, so a screen can append it.
 */
sealed interface ChairLine {

    /** There is a height on record. Say it, so the same chair gets used again. */
    data class SameAgain(val height: ChairHeight) : ChairLine

    /**
     * Nothing on record yet, so this is the once that Part 15 asks.
     *
     * A screen is free to ask here, or to say only "the same chair as last time" and
     * ask somewhere calmer. Deliberately not a fourth [ChairHeight] called "not sure":
     * a stored "not sure" would be a height that can never be compared to anything,
     * and an unanswered question is more honest than an unusable answer.
     */
    data object AskOnce : ChairLine
}

/**
 * The chair changed, and numbers either side of that were done from different seats.
 *
 * A plain fact, and nothing more than a fact. Part 15 asks for this "without alarm",
 * so there is nothing here to be alarmed by: no severity, no fault, no name that
 * suggests something went wrong, because nothing did. Somebody moved to a different
 * chair, which people do, and the only consequence is that two numbers cannot be put
 * beside each other and read as progress.
 *
 * [from] and [to] are given rather than a single direction because the honest sentence
 * names both chairs, and [lower] is offered for the screens that only need to know
 * which way it went. A lower seat makes a chair stand harder, so a number that fell
 * after a change is not a number that fell.
 */
data class DifferentChair(
    val from: ChairHeight,
    val to: ChairHeight,
    /** The day the chair changed. Everything recorded before it is on [from]. */
    val changedOn: Long,
) {
    /** True when the chair in the room now is the lower of the two, and so the harder. */
    val lower: Boolean get() = to < from
}

/**
 * The chair, and everything the app knows about whether it is still the same one.
 *
 * ADDENDUM-03 Part 15, CHAIR HEIGHT. Two jobs, and only two: tell a setup line which
 * chair to ask for, and tell a screen when a comparison reaches back past a change of
 * chair, once.
 *
 * The once is modelled here rather than left to the caller, because "once" is a fact
 * about the change and not about the screen. If each screen tracked it, a person who
 * looked at Progress and then at a session card would be told twice, and being told
 * twice turns a plain fact into nagging, which is exactly what Part 15 forbids.
 *
 * Only one boundary is remembered. Somebody who changes chairs twice before anything
 * has been said gets one note covering both, and somebody who changes again after
 * being told loses the older boundary. A full history of chairs would be more precise
 * and no more useful: the sentence a person needs is that the old numbers were from a
 * different chair, and a list of every chair they have owned does not improve it.
 */
data class TheChair(
    /** The height on record, or null when nobody has been asked yet. */
    val height: ChairHeight? = null,
    /** The last change of chair, when there has been one. */
    val change: Change? = null,
) {

    /**
     * One change of chair, and whether the app has mentioned it yet.
     *
     * [said] lives on the change rather than on [TheChair] so that a later change
     * cannot inherit the silence earned by an earlier one.
     */
    data class Change(val from: ChairHeight, val on: Long, val said: Boolean = false)

    /** True once somebody has answered, which is what stops the app asking again. */
    val known: Boolean get() = height != null

    /**
     * A new answer, from O5 or from You.
     *
     * The first answer is not a change and never produces a note: there is nothing
     * before it to compare against. Answering the same height again is not a change
     * either, and in particular does not move the boundary, because numbers from
     * before today were still done from this same chair.
     *
     * A second change while the first is still unsaid keeps the older boundary. The
     * note is owed for the oldest chair the person has not been told about, and a
     * second move does not earn a second sentence.
     */
    fun changedTo(newHeight: ChairHeight, today: Long): TheChair {
        val was = height ?: return copy(height = newHeight)
        if (newHeight == was) return this
        val stillOwed = change?.takeIf { !it.said }
        return copy(height = newHeight, change = stillOwed ?: Change(was, today))
    }

    /** Record that the note has been said, so it is not said again for this change. */
    fun noted(): TheChair = copy(change = change?.copy(said = true))

    /**
     * What a setup line should say about the chair, or nothing when it does not matter.
     *
     * Null for every movement that only leans on a chair or sits in one. The chair's
     * height changes the work only where the whole body rises off the seat, and
     * putting "the same chair as last time" on a seated stretch would be noise on the
     * one line somebody has to follow with the phone face down.
     */
    fun lineFor(movement: Movement): ChairLine? = when {
        !standsFromTheChair(movement) -> null
        height != null -> ChairLine.SameAgain(height)
        else -> ChairLine.AskOnce
    }

    /**
     * Whether numbers going back to [since] were recorded from a different chair.
     *
     * The plain fact, said or not. It stays true forever, because it is a property of
     * those numbers and not of what the app has got round to mentioning, which is what
     * a Progress screen drawing an old chart needs to know.
     */
    fun differentChair(since: Long): DifferentChair? {
        val last = change ?: return null
        val now = height ?: return null
        if (since >= last.on) return null
        return DifferentChair(from = last.from, to = now, changedOn = last.on)
    }

    /**
     * The same fact, but only the first time anybody would be told it.
     *
     * Pair every non-null answer with [noted]. Callers that only want to know whether
     * two numbers are comparable, without spending the one mention, should ask
     * [differentChair] instead.
     */
    fun noteToSay(since: Long): DifferentChair? {
        val last = change ?: return null
        if (last.said) return null
        return differentChair(since)
    }

    /**
     * The same question asked of real history, which is how a screen will have it.
     *
     * Only the movements somebody stands up out of are counted. A year of heel raises
     * spanning a change of chair is a year of comparable numbers, and saying otherwise
     * would be the app apologising for something that did not happen.
     */
    fun differentChair(history: List<Done>): DifferentChair? =
        oldestFromTheChair(history)?.let { differentChair(it) }

    /** [noteToSay], asked of real history. See [differentChair]. */
    fun noteToSay(history: List<Done>): DifferentChair? =
        oldestFromTheChair(history)?.let { noteToSay(it) }

    private fun oldestFromTheChair(history: List<Done>): Long? = history
        .filter { done -> Movements.byId(done.movementId)?.let { standsFromTheChair(it) } == true }
        .minOfOrNull { it.epochDay }

    companion object {

        /**
         * Whether this is a movement the chair's height decides the work of.
         *
         * [Sensed.Stands] is the library's own mark for a movement where the whole
         * body rises off a seat, which is exactly the set where the seat height sets
         * how far it has to rise, so it is read here rather than a second list of ids
         * being kept in step with the first. The chair is required as well, so a
         * future stand from something else does not quietly inherit this.
         *
         * Mini squats touch a chair and are left out on purpose: the chair is a marker
         * to reach for there, not a seat to rise from, and the person chooses the
         * depth.
         */
        fun standsFromTheChair(movement: Movement): Boolean =
            movement.sensedBy == Sensed.Stands && Kit.Chair in movement.kit
    }
}
