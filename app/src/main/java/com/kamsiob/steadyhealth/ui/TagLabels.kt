package com.kamsiob.steadyhealth.ui

import android.content.Context
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ai.TagGroup
import com.kamsiob.steadyhealth.ai.Tags
import com.kamsiob.steadyhealth.ui.screens.TagChip
import com.kamsiob.steadyhealth.ui.screens.TagSection

/**
 * The words for the twenty-four tags, and the grid they are laid out in.
 *
 * Its own file because it is a table rather than behaviour, and because the view
 * model has enough to do. The ids never change and the words are resources, which
 * is the same split as everywhere else in this app.
 */
object TagLabels {

    fun sections(
        context: Context,
        chosen: Set<String>,
        suggested: Set<String>,
    ): List<TagSection> = TagGroup.entries.map { group ->
        TagSection(
            heading = context.getString(headingFor(group)),
            chips = Tags.inGroup(group).map { tag ->
                TagChip(
                    id = tag.id,
                    label = context.getString(labelFor(tag.id)),
                    chosen = tag.id in chosen,
                    suggested = tag.id in suggested,
                )
            },
        )
    }

    fun headingFor(group: TagGroup) = when (group) {
        TagGroup.Food -> R.string.tag_group_food
        TagGroup.Sleep -> R.string.tag_group_sleep
        TagGroup.Mood -> R.string.tag_group_mood
        TagGroup.Movement -> R.string.tag_group_movement
        TagGroup.Body -> R.string.tag_group_body
        TagGroup.Life -> R.string.tag_group_life
    }

    /**
     * The word for one tag.
     *
     * Written out rather than looked up by resource name, because a resource
     * found by name is one lint cannot see and a translator can silently lose.
     * Twenty-four lines, once, and the compiler holds them.
     */
    @Suppress("CyclomaticComplexMethod") // Twenty-four constants, not twenty-four decisions.
    fun labelFor(id: String) = when (id) {
        "ate_out" -> R.string.tag_ate_out
        "cooked" -> R.string.tag_cooked
        "ate_light" -> R.string.tag_ate_light
        "ate_a_lot" -> R.string.tag_ate_a_lot
        "late_night" -> R.string.tag_late_night
        "snacked" -> R.string.tag_snacked
        "slept_well" -> R.string.tag_slept_well
        "slept_badly" -> R.string.tag_slept_badly
        "short_sleep" -> R.string.tag_short_sleep
        "rested" -> R.string.tag_rested
        "stressed" -> R.string.tag_stressed
        "calm" -> R.string.tag_calm
        "low" -> R.string.tag_low
        "good" -> R.string.tag_good
        "walked" -> R.string.tag_walked
        "active" -> R.string.tag_active
        "sat_a_lot" -> R.string.tag_sat_a_lot
        "sore" -> R.string.tag_sore
        "pain" -> R.string.tag_pain
        "unwell" -> R.string.tag_unwell
        "busy" -> R.string.tag_busy
        "travel" -> R.string.tag_travel
        "social" -> R.string.tag_social
        else -> R.string.tag_family
    }
}
