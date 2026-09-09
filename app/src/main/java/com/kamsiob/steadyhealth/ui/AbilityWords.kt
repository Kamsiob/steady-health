package com.kamsiob.steadyhealth.ui

import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.GettingAround

/**
 * What one of the four abilities is called, and what it means, in each version of
 * the app.
 *
 * Two tables. [name] is the word on the tile, which differs because Get up is
 * Transfer in a wheelchair and Sit up in bed. [plain] is the line under it on a tile
 * with nothing measured and nothing on the person's own list yet, which is better
 * than an empty tile and teaches somebody what Carry means here without a page about
 * it.
 *
 * Both tables live here rather than in a view model because two screens ask the same
 * question. The ability page used to answer it with the on-feet words while Today
 * answered it with the person's own, so somebody in bed had a tile called Ankles that
 * opened a page headed Steady. Nothing here says one set is a smaller version of
 * another, because it is not.
 */
object AbilityWords {

    @StringRes
    fun name(way: GettingAround, domain: AbilityDomain): Int = when (way) {
        GettingAround.Wheelchair -> when (domain) {
            AbilityDomain.GetUp -> R.string.ability_transfer
            else -> onFeetName(domain)
        }

        GettingAround.InBed -> when (domain) {
            AbilityDomain.GetUp -> R.string.ability_sit_up
            AbilityDomain.Go -> R.string.ability_breathe
            AbilityDomain.Carry -> R.string.ability_grip
            AbilityDomain.Steady -> R.string.ability_ankles
        }

        else -> onFeetName(domain)
    }

    @StringRes
    fun plain(way: GettingAround, domain: AbilityDomain): Int = when (way) {
        GettingAround.Wheelchair -> wheelchair(domain)
        GettingAround.InBed -> inBed(domain)
        else -> onFeet(domain)
    }

    private fun onFeetName(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_get_up
        AbilityDomain.Go -> R.string.ability_go
        AbilityDomain.Carry -> R.string.ability_carry
        AbilityDomain.Steady -> R.string.ability_steady
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

    /**
     * Carry is the same line as on feet on purpose. Lifting a bottle and holding a
     * cup is lifting and holding, and the earlier "holding on" was the one line in
     * the table that described surviving rather than doing.
     */
    private fun inBed(domain: AbilityDomain) = when (domain) {
        AbilityDomain.GetUp -> R.string.ability_plain_bed_up
        AbilityDomain.Go -> R.string.ability_plain_breathe
        AbilityDomain.Carry -> R.string.ability_plain_carry
        AbilityDomain.Steady -> R.string.ability_plain_ankles
    }
}
