package com.kamsiob.steadyhealth.ui

import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.entity.SettingEntity

/**
 * Whether a session speaks, held in one place because two screens set it.
 *
 * ADDENDUM-03 Part 1: on by default, with the switch in the session top bar and in
 * You. Two switches over one setting only works if there is one setting, so the
 * session writes here when somebody uses the one in the top bar and reads here when
 * a session starts. Otherwise turning the voice off mid session and coming back
 * tomorrow would find it on again, and the switch in You would be describing
 * something that had already changed under it.
 *
 * It lives beside the view models rather than in a repository because the
 * repositories are somebody else's file this week; the key and the default are
 * written once here so neither can drift.
 */
object Audio {

    private const val KEY = "speaks_in_sessions"

    /** On unless somebody turned it off. Part 1 makes the default the answer. */
    suspend fun on(db: SteadyDatabase): Boolean = db.profile().get(KEY)?.toBoolean() ?: true

    suspend fun set(db: SteadyDatabase, value: Boolean) =
        db.profile().put(SettingEntity(KEY, value.toString()))
}
