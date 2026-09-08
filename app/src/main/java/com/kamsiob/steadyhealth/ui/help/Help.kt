package com.kamsiob.steadyhealth.ui.help

import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R

/** One question somebody actually asks, and the answer, both hand written. */
data class Question(@param:StringRes val ask: Int, @param:StringRes val answer: Int)

/**
 * What the help dot says on one screen.
 *
 * [says] is the L1 sand block, shown once on first open and never again. The rest is
 * L2, behind the question mark, and it is always available.
 */
data class Topic(
    @param:StringRes val says: Int?,
    @param:StringRes val what: Int,
    val points: List<Int>,
    val questions: List<Question>,
)

/**
 * The screens that explain themselves. ADDENDUM-03 Part 4, DESIGN.md 4b.
 *
 * A place is added here at the same time as the screen it describes, so a dot never
 * opens onto nothing. A screen with no entry has no dot, which is honest, and the
 * screens still missing one are listed in DECISIONS.md rather than given a placeholder.
 */
enum class Place {
    Today,
    Session,
    Move,
    Abilities,
    Settings,
    ;

    /** The key the sand block's one dismissal is stored under. */
    val seenKey: String get() = "says_${name.lowercase()}"
}

object Help {

    @Suppress("LongMethod") // A table of hand written copy. There is no logic in it.
    fun topic(place: Place): Topic = when (place) {
        Place.Today -> Topic(
            says = R.string.says_today,
            what = R.string.help_today_what,
            points = listOf(R.string.help_today_p1, R.string.help_today_p2, R.string.help_today_p3),
            questions = listOf(
                Question(R.string.help_today_q1, R.string.help_today_a1),
                Question(R.string.help_today_q2, R.string.help_today_a2),
                Question(R.string.help_today_q3, R.string.help_today_a3),
            ),
        )

        Place.Session -> Topic(
            says = null,
            what = R.string.help_session_what,
            points = listOf(
                R.string.help_session_p1,
                R.string.help_session_p2,
                R.string.help_session_p3,
            ),
            questions = listOf(
                Question(R.string.help_session_q1, R.string.help_session_a1),
                Question(R.string.help_session_q2, R.string.help_session_a2),
                Question(R.string.help_session_q3, R.string.help_session_a3),
            ),
        )

        Place.Move -> Topic(
            says = R.string.says_sessions,
            what = R.string.help_move_what,
            points = listOf(R.string.help_move_p1, R.string.help_move_p2),
            questions = listOf(
                Question(R.string.help_move_q1, R.string.help_move_a1),
                Question(R.string.help_move_q2, R.string.help_move_a2),
            ),
        )

        Place.Abilities -> Topic(
            says = R.string.says_progress,
            what = R.string.help_abilities_what,
            points = listOf(R.string.help_abilities_p1, R.string.help_abilities_p2),
            questions = listOf(
                Question(R.string.help_abilities_q1, R.string.help_abilities_a1),
                Question(R.string.help_abilities_q2, R.string.help_abilities_a2),
            ),
        )

        Place.Settings -> Topic(
            says = R.string.says_you,
            what = R.string.help_settings_what,
            points = listOf(R.string.help_settings_p1, R.string.help_settings_p2),
            questions = listOf(
                Question(R.string.help_settings_q1, R.string.help_settings_a1),
                Question(R.string.help_settings_q2, R.string.help_settings_a2),
            ),
        )
    }
}
