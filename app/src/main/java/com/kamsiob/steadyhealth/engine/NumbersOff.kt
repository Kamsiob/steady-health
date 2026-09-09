package com.kamsiob.steadyhealth.engine

/**
 * Which way something went, when the number itself is switched off.
 *
 * The same three words the weight line already uses, because somebody who has turned
 * numbers off should meet one vocabulary and not four.
 */
enum class Went {
    Up,
    Same,
    Down,
    Unknown,
}

/**
 * Numbers off. LOGIC.md section 1 and ADDENDUM-03 Phase 8.
 *
 * "All figures become direction words; charts keep shape and lose axes; the
 * destination is hidden."
 *
 * WHERE THE LINE IS, because the sentence above does not draw it and the wrong reading
 * makes the app unusable. There are two kinds of number in this app.
 *
 * The first is a number the app REPORTS BACK: what you did, how many, how long, how
 * much you weigh, what you rated something, how many days this week. Every one of
 * those is the app telling somebody how they are doing, and every one of those is a
 * figure that becomes a word with this setting off. That is the whole feature, and the
 * reason it exists is that for some people a number is a verdict and a word is not.
 *
 * The second is a number that is part of DOING something right now: the count climbing
 * on the live screen while you stand up out of a chair, the seconds left in a rest,
 * the repetitions a therapist wrote on their sheet, and the settings somebody chose
 * themselves, like three sessions a week. None of those is the app's opinion of
 * anybody. They are instruments and choices, and hiding them does not spare somebody a
 * verdict, it stops them counting. So they stay.
 *
 * A date stays too. Hiding today's date would be hiding the calendar.
 *
 * The line is worth stating this plainly because it is the sort of rule that erodes:
 * every individual number looks harmless enough to keep, and then the setting does
 * nothing. The test for a new number is one question. Is this the app telling somebody
 * how they are doing? Then it goes.
 */
object NumbersOff {

    /**
     * Which way a pair of numbers went, or Unknown when there is no pair.
     *
     * Unknown rather than Same for a first-ever value, because "about the same" said
     * about a number nothing preceded is the app comparing something with nothing.
     */
    fun went(from: Double?, to: Double?, moreIsBetter: Boolean = true): Went = when {
        from == null || to == null -> Went.Unknown
        to == from -> Went.Same
        (to > from) == moreIsBetter -> Went.Up
        else -> Went.Down
    }

    /**
     * Whether a chart still draws itself at its real height. It does.
     *
     * "Charts keep shape and lose axes." The bar keeps its real height, because the
     * shape of four weeks side by side is the information and a shape carries no
     * digits. What goes is the number under it and the number it is measured
     * against, so nothing on the chart can be read off as a place on a scale.
     */
    const val CHARTS_KEEP_THEIR_SHAPE = true
}
