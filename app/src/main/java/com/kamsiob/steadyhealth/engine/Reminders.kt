package com.kamsiob.steadyhealth.engine

/** The four things the app may ever send a notification about. LOGIC.md section 12. */
enum class ReminderKind(val id: String) {
    /** At the habit the person named, and never about a day they missed. */
    Walk("walk"),

    /** Sunday, if they take photos. */
    Photo("photo"),

    /** Sunday, when the write-up is there to read. */
    WeekNote("week_note"),

    /** A longer walk is ready. The only one that is ever good news by itself. */
    StepReady("step_ready"),
}

/**
 * The reminder ceiling, from LOGIC.md section 12.
 *
 * "Hard ceiling of two notifications in any rolling seven days regardless of
 * switches." Regardless is the word that matters: somebody who turns all four on
 * still gets two, and there is no combination of settings that produces a third.
 *
 * The ceiling is here rather than in the scheduler because a scheduler is a thing
 * that can be wrong at three in the morning and a pure function is not. Whatever
 * wakes up to send a reminder asks this first.
 */
object Reminders {

    /** Two in any rolling seven days. Not per type, not per week: two. */
    const val MOST_IN_A_WEEK = 2

    const val WINDOW_DAYS = 7

    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * True when one more may be sent right now.
     *
     * [sentAt] is every reminder already sent, of any type, in any order.
     */
    fun maySend(sentAt: List<Long>, now: Long): Boolean = usedThisWeek(sentAt, now) < MOST_IN_A_WEEK

    /** How many of the two are gone, for the line the settings screen shows. */
    fun usedThisWeek(sentAt: List<Long>, now: Long): Int =
        sentAt.count { now - it < WINDOW_DAYS * MILLIS_PER_DAY }

    fun leftThisWeek(sentAt: List<Long>, now: Long): Int =
        (MOST_IN_A_WEEK - usedThisWeek(sentAt, now)).coerceAtLeast(0)

    /**
     * Which one to send when more than one is due at the same moment.
     *
     * A longer walk being ready is the only one of the four that is good news on
     * its own, so it goes first. The walk reminder is next because it is the one
     * somebody asked for by naming a habit. The two Sunday ones are last, and
     * losing one of them costs nothing: the write-up is still there tomorrow.
     */
    fun pick(due: Set<ReminderKind>): ReminderKind? = ORDER.firstOrNull { it in due }

    private val ORDER = listOf(
        ReminderKind.StepReady,
        ReminderKind.Walk,
        ReminderKind.WeekNote,
        ReminderKind.Photo,
    )
}
