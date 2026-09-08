package com.kamsiob.steadyhealth.ui

import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround

/**
 * The words for the two enums that appear in more than one place.
 *
 * These used to live inside SettingsViewModel, which was fine while settings was the
 * only screen that named an exclusion. Onboarding now asks the same question, and two
 * copies of the same mapping is how a list ends up saying "Lifting overhead" in one
 * place and "Reaching overhead" in another.
 */
object Labels {

    fun forExclusion(value: Exclusion) = when (value) {
        Exclusion.Pushing -> R.string.leave_out_pushing
        Exclusion.StomachStrain -> R.string.leave_out_stomach
        Exclusion.GettingOnTheFloor -> R.string.leave_out_floor
        Exclusion.Impact -> R.string.leave_out_impact
        Exclusion.DeepKneeBending -> R.string.leave_out_knee
        Exclusion.LiftingOverhead -> R.string.leave_out_overhead
        Exclusion.TwistingBack -> R.string.leave_out_twisting
        Exclusion.DeepForwardBending -> R.string.leave_out_forward
        Exclusion.ArchingBack -> R.string.leave_out_arching
        Exclusion.LyingFlat -> R.string.leave_out_lying
        Exclusion.BreathHolding -> R.string.leave_out_breath
    }

    fun forGettingAround(value: GettingAround) = when (value) {
        GettingAround.OnFeet -> R.string.around_on_feet
        GettingAround.Walker -> R.string.around_walker
        GettingAround.Wheelchair -> R.string.around_wheelchair
        GettingAround.InBed -> R.string.around_in_bed
    }

    fun forAbility(value: AbilityDomain) = when (value) {
        AbilityDomain.GetUp -> R.string.ability_get_up
        AbilityDomain.Go -> R.string.ability_go
        AbilityDomain.Carry -> R.string.ability_carry
        AbilityDomain.Steady -> R.string.ability_steady
    }

    fun forLanguage(tag: String) = when (tag) {
        "es" -> R.string.language_spanish
        "zh" -> R.string.language_chinese
        "ar" -> R.string.language_arabic
        else -> R.string.language_english
    }
}
