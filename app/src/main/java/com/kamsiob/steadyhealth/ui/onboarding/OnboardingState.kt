package com.kamsiob.steadyhealth.ui.onboarding

import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Anchor
import com.kamsiob.steadyhealth.domain.ChairEase
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.FloorAccess
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.PemAnswer
import com.kamsiob.steadyhealth.domain.ReadinessFlag
import com.kamsiob.steadyhealth.domain.Stairs
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.domain.WalkTolerance

/** The screens of setup, in the order ONBOARDING.md puts them. */
enum class OnboardingStep {
    Welcome,
    ThreeThings,
    HowYouGetAround,
    AboutYou,
    WhereYouAreStarting,
    LeaveOut,
    Readiness,
    WhatYouWant,
    RateThem,
    Anchor,
    FirstWeighIn,
    NameFirstWalk,
}

/** One thing the person said they would like to be able to do, before it is saved. */
data class WantedItem(
    val text: String,
    val domain: AbilityDomain,
    val rating: Int = DEFAULT_RATING,
)

/**
 * Everything setup has collected so far.
 *
 * Every value a screen displays is seeded here rather than left null until
 * somebody touches the control. That is a rule and not a convenience: a stepper
 * that draws 5 ft 7 and stores nothing unless it is pressed shows somebody a
 * number, lets them agree with it, and saves nothing, which is the one outcome
 * nobody expects. Skipping is a separate, deliberate answer with its own button.
 */
data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,

    val language: String = "en",
    val gettingAround: GettingAround? = null,
    val withTherapist: Boolean = false,

    val units: Units = Units.Imperial,
    val heightCm: Double = DEFAULT_HEIGHT_CM,
    val age: Int? = DEFAULT_AGE,
    val ageSkipped: Boolean = false,

    val chair: ChairEase? = null,
    val stairs: Stairs? = null,
    val walkTolerance: WalkTolerance? = null,
    val floor: FloorAccess? = null,
    val pem: PemAnswer? = null,

    val exclusions: Set<Exclusion> = emptySet(),
    val readiness: Map<ReadinessFlag, Boolean> = emptyMap(),

    val typed: String = "",
    val wanted: List<WantedItem> = emptyList(),

    val anchor: Anchor? = null,

    /** Seeded with the dial's own starting value, for the reason above. */
    val firstWeightKg: Double = DEFAULT_FIRST_WEIGHT_KG,
    val firstWalkName: String = "",
) {
    /** Every capability question answered. The Continue button waits for this. */
    val startingAnswered: Boolean
        get() = chair != null && stairs != null && walkTolerance != null &&
            floor != null && pem != null

    val readinessAnswered: Boolean
        get() = ReadinessFlag.entries.all { it in readiness }

    val anyReadinessFlag: Boolean
        get() = readiness.values.any { it }
}

/** What the height stepper starts on, and therefore what it stores unless changed. */
const val DEFAULT_HEIGHT_CM = 170.0

/** What the age stepper starts on. Skipping is a separate, deliberate answer. */
const val DEFAULT_AGE = 45

/** Where the dial starts, in kilos. */
const val DEFAULT_FIRST_WEIGHT_KG = 80.0

/** The middle of the scale, so a rating is a move rather than a starting point. */
const val DEFAULT_RATING = 5
