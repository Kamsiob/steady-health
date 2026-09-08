package com.kamsiob.steadyhealth.ui.onboarding

import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround

/**
 * The five steps of ADDENDUM-03 Part 3, replacing the earlier eleven.
 *
 * The order is the whole design: the person does something real at O4 before
 * answering anything optional at O5, and everything else is asked later, in place, at
 * the moment it matters.
 */
enum class Step {
    Hook,
    HowYouGetAround,
    TheirWords,
    FirstSession,
    AfterTheSession,
}

/** Whether the chair in the room is the one the chair stands assume. */
enum class ChairAnswer(val id: String) {
    Sturdy("sturdy"),
    WithArms("with_arms"),
    None("none"),
}

/**
 * What onboarding has collected.
 *
 * Every value is stored as it is answered rather than at the end, so quitting
 * halfway resumes exactly where it stopped with what was already given still there.
 */
data class OnboardingState(
    val step: Step = Step.Hook,
    val language: String = "en",
    val gettingAround: GettingAround? = null,
    val typed: String = "",
    val wanted: String? = null,
    val wantedDomain: AbilityDomain = AbilityDomain.Go,
    val exclusions: Set<Exclusion> = emptySet(),
    val chair: ChairAnswer? = null,
    /** True once the first session has been run, so O5 is reachable. */
    val sessionDone: Boolean = false,
)

/**
 * The six starter chips, and what each one is about.
 *
 * Deliberately not all maintenance. Two of the six are about being stronger than
 * today, because ADDENDUM-03's opening says the app was written as though everyone is
 * holding on to what they have, and that is wrong for many of the people using it.
 */
data class Starter(val id: String, val domain: AbilityDomain)

object Starters {
    val all = listOf(
        Starter("floor", AbilityDomain.GetUp),
        Starter("stairs", AbilityDomain.Go),
        Starter("shopping", AbilityDomain.Carry),
        Starter("grandkids", AbilityDomain.Go),
        Starter("further", AbilityDomain.Go),
        Starter("stronger", AbilityDomain.Carry),
    )
}
