package com.kamsiob.steadyhealth.ui

import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.GettingAround

/**
 * What one of the four abilities means, in words, for each version of the app.
 *
 * Shown on a tile with nothing measured and nothing on the person's own list yet.
 * Better than an empty tile with one word in it, and it teaches somebody what
 * Carry means here without a page about it.
 *
 * Its own file because it is a table with twelve entries and the view model has
 * enough tables in it already.
 */
object AbilityWords {

    @StringRes
    fun plain(way: GettingAround, domain: AbilityDomain): Int = when (way) {
        GettingAround.Wheelchair -> wheelchair(domain)
        GettingAround.InBed -> inBed(domain)
        else -> onFeet(domain)
    }

    private fun onFeet(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_plain_get_up
        AbilityDomain.Go -> R.string.ability_plain_go
        AbilityDomain.Carry -> R.string.ability_plain_carry
        AbilityDomain.Steady -> R.string.ability_plain_steady
    }

    private fun wheelchair(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_plain_transfer
        AbilityDomain.Go -> R.string.ability_plain_wheel
        AbilityDomain.Carry -> R.string.ability_plain_carry
        AbilityDomain.Steady -> R.string.ability_plain_seated
    }

    private fun inBed(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_plain_bed_up
        AbilityDomain.Go -> R.string.ability_plain_breathe
        AbilityDomain.Carry -> R.string.ability_plain_grip
        AbilityDomain.Steady -> R.string.ability_plain_ankles
    }
}
