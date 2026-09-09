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
 * One starter chip, and what it is about.
 *
 * Deliberately not all maintenance. Two of every six are about being stronger than
 * today, because ADDENDUM-03's opening says the app was written as though everyone is
 * holding on to what they have, and that is wrong for many of the people using it.
 */
data class Starter(val id: String, val domain: AbilityDomain)

/**
 * Six chips for each way of getting around.
 *
 * O2 is answered before O3, so by the time these are shown the app knows whose life
 * it is asking about. Four of the original six ("get off the floor", "stairs without
 * stopping", "walk further than I do now", "carry the shopping in one trip") only
 * mean anything on two feet, and offering them to somebody in a wheelchair is the app
 * describing a life that is not theirs and calling it the list of things worth
 * wanting.
 *
 * So there are three sets, all the same shape: one about getting up, three about
 * going or keeping up with people, two about being stronger than today. None of them
 * is shorter than another and none of them is the same list with things crossed off.
 * "Get stronger than I am" is in all three, because it is in all three.
 */
object Starters {

    val onFeet = listOf(
        Starter("floor", AbilityDomain.GetUp),
        Starter("stairs", AbilityDomain.Go),
        Starter("shopping", AbilityDomain.Carry),
        Starter("grandkids", AbilityDomain.Go),
        Starter("further", AbilityDomain.Go),
        Starter("stronger", AbilityDomain.Carry),
    )

    val wheelchair = listOf(
        Starter("transfer", AbilityDomain.GetUp),
        Starter("block", AbilityDomain.Go),
        Starter("lap", AbilityDomain.Carry),
        Starter("grandkids", AbilityDomain.Go),
        Starter("further_wheel", AbilityDomain.Go),
        Starter("stronger", AbilityDomain.Carry),
    )

    val inBed = listOf(
        Starter("sit_up", AbilityDomain.GetUp),
        Starter("chair_back", AbilityDomain.GetUp),
        Starter("cup", AbilityDomain.Carry),
        Starter("breath", AbilityDomain.Go),
        Starter("edge", AbilityDomain.Steady),
        Starter("stronger", AbilityDomain.Carry),
    )

    /** A walker is walking, so it takes the same six as feet. */
    fun forWay(way: GettingAround?): List<Starter> = when (way) {
        GettingAround.Wheelchair -> wheelchair
        GettingAround.InBed -> inBed
        else -> onFeet
    }

    val all: List<Starter> = (onFeet + wheelchair + inBed).distinctBy { it.id }
}
