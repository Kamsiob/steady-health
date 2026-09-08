package com.kamsiob.steadyhealth.ui.nav

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R

/**
 * The three tabs, and there is never a fourth.
 *
 * MASTER_SPEC section 5 makes this a rule rather than a layout: Today, Move,
 * Abilities, a question mark and a gear on Today, and nothing else is a
 * destination. The visit summary is reached from inside Abilities for exactly
 * this reason.
 */
enum class Tab(
    val route: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    Today("today", R.string.tab_today, R.drawable.ic_today),
    Move("move", R.string.tab_move, R.drawable.ic_move),
    Abilities("abilities", R.string.tab_abilities, R.drawable.ic_abilities),
}

/**
 * Every destination that is not a tab root.
 *
 * The offer is not here on purpose. It is a thing the app is saying rather than a
 * place somebody went, so it covers the screen from outside the graph and has no
 * route to navigate to or back from.
 */
object Route {
    const val TABS = "tabs"
    const val WEIGH_IN = "weigh-in"
    const val SAY_HOW = "say-how"
    const val WALKING = "walking"
    const val WALK_DONE = "walk-done"
    const val SETTINGS = "settings"
    const val GETTING_AROUND = "getting-around"
    const val LEAVE_OUT = "leave-out"
    const val PACING = "pacing"
    const val PATTERN = "pattern"
    const val ASK = "ask"
    const val CARD = "card"
    const val CHECK = "check"
    const val CHECK_MEASURE = "check-measure"
    const val CHECK_RATE = "check-rate"
    const val CHECK_DONE = "check-done"
    const val QUIETER = "quieter"
    const val ABILITY = "ability"
    const val WEIGHT = "weight"
    const val SUMMARY = "summary"
    const val DATA = "data"
    const val REMINDERS = "reminders"
    const val TRY_OFFER = "try-offer"
    const val TRY_RESULT = "try-result"
    const val SESSION = "session"
}
