package com.kamsiob.steadyhealth.ui.nav

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R

/**
 * The four tabs, and there is never a fifth.
 *
 * MASTER_SPEC section 5, from ADDENDUM-03 Part 20, replacing the earlier three.
 * Today is what to do now; Sessions is everything you can do and everything you
 * have done; Progress is what changed; You is your list, your settings and how
 * the app works. Nothing else is a destination.
 *
 * Settings moved out from behind a gear on Today, because a tab called You is
 * where somebody looks for their own things and a gear is where somebody looks
 * for a switch they already know exists.
 */
enum class Tab(
    val route: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    Today("today", R.string.tab_today, R.drawable.ic_today),
    Sessions("sessions", R.string.tab_sessions, R.drawable.ic_move),
    Progress("progress", R.string.tab_progress, R.drawable.ic_abilities),
    You("you", R.string.tab_you, R.drawable.ic_settings),
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
    const val LOG_PAST = "log-past"
    const val PHONE_FREE = "phone-free"
    const val PAST_SESSION = "past-session"
}
