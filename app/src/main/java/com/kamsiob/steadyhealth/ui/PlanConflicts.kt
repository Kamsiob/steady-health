package com.kamsiob.steadyhealth.ui

import android.content.Context
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.plan.PlanItem
import com.kamsiob.steadyhealth.plan.PlanMatching

/**
 * "Your plan has wall push ups, and you said you avoid pushing." ADDENDUM-03 Part 6.
 *
 * One place rather than two, because the sentence is now said twice: on the screen
 * where the plan is confirmed, and on Today's card afterwards. Two copies of it would
 * eventually be two sentences, and the second one somebody read would look like a
 * different problem rather than the same one.
 *
 * It is said at confirmation as well as on the card because that is the moment the
 * person can still do something about it, and because a movement picked off the
 * library is picked by hand: somebody who taps wall push ups having told the app they
 * avoid pushing has said two things that disagree, and finding out the next morning is
 * finding out too late to ask about it while the sheet is still in their hand.
 *
 * It never removes anything. Part 6 is explicit that a clash is flagged and left in,
 * and the app has no standing to overrule whoever wrote the plan.
 */
object PlanConflicts {

    fun of(
        context: Context,
        items: List<PlanItem>,
        exclusions: Set<Exclusion>,
        way: GettingAround,
    ): List<String> = PlanMatching.toAskAbout(items, exclusions, way).mapNotNull { item ->
        val movement = item.movement ?: return@mapNotNull null
        val avoided = movement.excludedBy.firstOrNull { it in exclusions } ?: return@mapNotNull null
        context.getString(
            R.string.plan_conflict,
            movement.name.lowercase(),
            context.getString(Labels.forExclusion(avoided)).lowercase(),
        )
    }
}
