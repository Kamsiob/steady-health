package com.kamsiob.steadyhealth.plan

import com.kamsiob.steadyhealth.session.Movement

/**
 * One therapist's plan, kept whole.
 *
 * ADDENDUM-03 Part 6: a physio plan and an OT plan can coexist, each labelled, each
 * separate. That is only true if the plan a line came from travels with the line, so
 * the label lives here beside the items rather than being looked up again later. The
 * app used to hold every live line in one flat list and read the label off whichever
 * plan happened to be oldest, which put the OT's movements under the physio's name.
 */
data class TheirPlan(val label: String, val items: List<PlanItem>) {

    /**
     * Their label, or the plain word for a therapist when the box was left empty.
     *
     * Somebody will leave it empty, and "From your" with nothing after it is worse
     * than a plan that does not name whose it is. [theWord] is passed in rather than
     * read from resources here, because this file is the reasoning and not the copy.
     */
    fun labelOr(theWord: String): String = label.trim().ifBlank { theWord }
}

/**
 * What the app may say about more than one plan at a time.
 *
 * All of it is reading. Nothing here merges two plans, reorders one, or drops a line
 * from either, because Part 6 makes the app a record keeper for anything a therapist
 * wrote and the moment two plans become one list there is no way back to whose was
 * whose.
 */
object TheirPlans {

    /**
     * Every line of every plan, in the order the plans were given.
     *
     * This is what the session runs and what the card counts. It is deliberately not
     * how the card lists them: a session is one thing to do and the card that offers
     * it keeps the plans apart.
     */
    fun lines(plans: List<TheirPlan>): List<PlanItem> = plans.flatMap { it.items }

    /**
     * Whose plan names this movement, for the one sentence about a movement on both
     * lists.
     *
     * The first plan that names it, when two therapists asked for the same thing. The
     * sentence exists to say the person has already done it today, and doing it once
     * answers both, so naming one of them is enough and naming both would be the app
     * making a point of two people agreeing.
     */
    fun whose(plans: List<TheirPlan>, movement: Movement, theWord: String): String? =
        plans.firstOrNull { plan -> plan.items.any { it.movement?.id == movement.id } }
            ?.labelOr(theWord)
}
