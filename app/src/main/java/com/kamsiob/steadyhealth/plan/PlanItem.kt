package com.kamsiob.steadyhealth.plan

import com.kamsiob.steadyhealth.session.Movement

/**
 * How sure the app is that a line names the movement it put beside it.
 *
 * ADDENDUM-03 Part 6 says every item is shown with what the app matched it to and
 * nothing is saved unconfirmed, so this is not a filter and nothing is ever dropped
 * for being weak. It exists so the screen can show a guess as a guess. The order of
 * the four is the order of how much of the person's attention the line needs, and a
 * wrong confident match on a therapist's plan is the worst outcome available here,
 * worse than a line the person has to fill in by hand.
 */
enum class Sureness {

    /** The words on the line mean this movement and nothing else in the library. */
    Named,

    /** The line named something this movement is one kind of. "Push ups" is the case. */
    Likely,

    /** The line named more than one movement, so the app proposes none of them. */
    MoreThanOne,

    /** Nothing on the line names anything in the library. */
    Unmatched,
}

/**
 * The number a line asks for, in the unit it was written in.
 *
 * Separate cases rather than an integer and a unit flag, because the unit decides
 * what the live screen counts, and a hold written as reps would be counted wrong all
 * the way through a session. [Unsaid] is a real and common answer: plenty of sheets
 * name a movement and leave the number to the appointment.
 */
sealed interface HowMany {

    /** The line named a movement and no number. The screen asks for one. */
    data object Unsaid : HowMany

    /** Repetitions, and how many rounds of them. One round unless the line said. */
    data class Reps(val reps: Int, val sets: Int = 1) : HowMany

    /** A hold, in seconds, and how many rounds of it. */
    data class Hold(val seconds: Int, val sets: Int = 1) : HowMany

    /** Minutes of walking or wheeling. */
    data class Minutes(val minutes: Int) : HowMany
}

/**
 * How often a line asks for the movement.
 *
 * Only the shapes therapists actually write. Anything else stays [Unsaid] rather
 * than being squeezed into the nearest case, because the person can see the line
 * they were given sitting right above it and can say what it means.
 */
sealed interface HowOften {

    /** The line said nothing about how often. */
    data object Unsaid : HowOften

    /** "Twice a day", "daily", "three times a day". Daily is one time. */
    data class ADay(val times: Int) : HowOften

    /** "Three times a week", "weekly". Weekly is one time. */
    data class AWeek(val times: Int) : HowOften

    /** "Every other day", which is neither of the two above and is written a lot. */
    data object EveryOtherDay : HowOften
}

/**
 * One line of a therapist's plan, with what the app thinks it means.
 *
 * ADDENDUM-03 Part 6. [line] is the line as it was given and is never rewritten,
 * corrected or tidied: it is what the therapist wrote or what the person said, it is
 * what the confirmation screen shows above the app's guess, and it is what the export
 * back to the appointment quotes. Everything else on this type is the app's proposal
 * about that line and can be wrong.
 *
 * There is deliberately nothing here that progresses or adapts. Part 6: the app runs
 * the plan exactly as given, and reps and frequency change only when the person
 * changes them. A field the engine could move would be a field the engine eventually
 * moves.
 */
data class PlanItem(
    val line: String,
    val movement: Movement?,
    val howMany: HowMany,
    val howOften: HowOften,
    val sureness: Sureness,
    /** The line said each side, each leg, or both sides. */
    val eachSide: Boolean = false,
    /**
     * The movements a [Sureness.MoreThanOne] line named, for the screen to offer.
     *
     * Empty otherwise. The app holds all of them rather than picking the first,
     * because on a line naming two movements the first one is not more likely to be
     * right, it is just earlier.
     */
    val couldBe: List<Movement> = emptyList(),
) {

    /**
     * True when the app is guessing rather than reading.
     *
     * The screen shows these differently. It is not a warning and not an error: a
     * guessed line is an ordinary outcome and the person confirming it is the point
     * of the screen.
     */
    val guessed: Boolean get() = sureness != Sureness.Named

    /** True when there is enough here to run without asking the person anything. */
    val ready: Boolean get() = movement != null && howMany != HowMany.Unsaid
}
