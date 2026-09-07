package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.PemAnswer
import com.kamsiob.steadyhealth.engine.Envelope
import com.kamsiob.steadyhealth.engine.PacingEngine
import com.kamsiob.steadyhealth.engine.WaysOfGettingAround
import com.kamsiob.steadyhealth.ui.screens.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Settings, on its own.
 *
 * Separate from [SteadyViewModel] because settings writes and the rest reads:
 * every other tab loads the profile again when it comes back into view, so
 * nothing here has to reach across and tell them. That is also why changing how
 * somebody gets around is safe from here. It writes one row, and the app is a
 * different app the next time Today is drawn.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy { SteadyDatabase.get(application) }
    private val profile by lazy { ProfileRepository(db) }

    private val _settings = MutableStateFlow(SettingsUiState())
    val settings: StateFlow<SettingsUiState> = _settings.asStateFlow()

    private val _pattern = MutableStateFlow<PemAnswer?>(null)

    /** The pattern question's current answer, for its own screen. */
    val pattern: StateFlow<PemAnswer?> = _pattern.asStateFlow()

    private var pacing = false
    private var envelope: Envelope = PacingEngine.DEFAULT
    private var exclusions: Set<Exclusion> = emptySet()

    fun openSettings() = viewModelScope.launch { refreshSettings() }

    private suspend fun refreshSettings() {
        val context = getApplication<Application>()
        val chosen = profile.exclusions()
        exclusions = chosen
        pacing = profile.pacing()
        envelope = profile.envelope()
        _pattern.value = profile.pem()
        _settings.value = SettingsUiState(
            gettingAround = profile.gettingAround(),
            gettingAroundLabel = context.getString(labelFor(profile.gettingAround())),
            withTherapist = profile.withTherapist(),
            weighsIn = profile.weighsIn(),
            showNumbers = profile.showNumbers(),
            units = profile.units(),
            exclusions = chosen,
            exclusionsLabel = if (chosen.isEmpty()) {
                context.getString(R.string.settings_leave_out_none)
            } else {
                chosen.joinToString(", ") { context.getString(labelFor(it)) }
            },
            pacing = pacing,
            pemLabel = context.getString(labelFor(_pattern.value)),
            envelopeMinutes = envelope.minutes,
            envelopeDays = envelope.daysPerWeek,
        )
    }

    /**
     * Change how somebody gets around, after setup.
     *
     * Everything they have done stays where it is: sessions carry the ladder they
     * were done on, so a walk from before a wheelchair is still a walk. What
     * changes is what the app offers from here.
     */
    fun setGettingAround(value: GettingAround) = viewModelScope.launch {
        profile.setGettingAround(value)
        profile.setWeighsIn(WaysOfGettingAround.forWay(value).weighsIn)
        refresh()
    }

    fun setTherapist(value: Boolean) = viewModelScope.launch {
        profile.setWithTherapist(value)
        refreshSettings()
    }

    fun setWeighsIn(value: Boolean) = viewModelScope.launch {
        profile.setWeighsIn(value)
        refreshSettings()
    }

    fun setShowNumbers(value: Boolean) = viewModelScope.launch {
        profile.setShowNumbers(value)
        refreshSettings()
    }

    fun toggleExclusion(value: Exclusion) = viewModelScope.launch {
        val next = if (value in exclusions) exclusions - value else exclusions + value
        profile.setExclusions(next, System.currentTimeMillis())
        refresh()
    }

    fun setEnvelopeMinutes(value: Int) = viewModelScope.launch {
        setEnvelope(envelope.copy(minutes = value.coerceIn(PacingEngine.FLOOR_MINUTES, MAX_ENVELOPE_MINUTES)))
    }

    fun setEnvelopeDays(value: Int) = viewModelScope.launch {
        setEnvelope(envelope.copy(daysPerWeek = value.coerceIn(1, DAYS_IN_WEEK)))
    }

    private suspend fun setEnvelope(value: Envelope) {
        profile.setEnvelope(value)
        envelope = value
        refreshSettings()
    }

    /** Leaving pacing mode. The person's decision, and nothing else's. */
    fun stopPacing() = viewModelScope.launch {
        profile.setPacing(false)
        pacing = false
        refresh()
    }

    /**
     * Read everything back after a change.
     *
     * Only this screen's own state: Today, Move and Abilities each read the
     * profile again when they come back into view, which is the whole reason
     * settings can live in a view model of its own.
     */
    fun refresh() = viewModelScope.launch {
        pacing = profile.pacing()
        envelope = profile.envelope()
        refreshSettings()
    }

    private fun labelFor(value: PemAnswer?) = when (value) {
        PemAnswer.Yes -> R.string.start_pem_yes
        PemAnswer.Sometimes -> R.string.start_pem_sometimes
        else -> R.string.start_pem_no
    }

    /**
     * The pattern question, answered again.
     *
     * Yes turns pacing mode on, because LOGIC.md section 7 makes it a trigger.
     * Changing the answer back does not turn pacing mode off: leaving is its own
     * decision, made on its own screen, and the app does not make it for anybody.
     */
    fun setPattern(value: PemAnswer) = viewModelScope.launch {
        profile.setPem(value)
        if (value == PemAnswer.Yes && !pacing) {
            profile.setPacing(true)
            profile.setEnvelope(envelope)
        }
        refresh()
    }

    private fun labelFor(value: GettingAround) = when (value) {
        GettingAround.OnFeet -> R.string.around_on_feet
        GettingAround.Walker -> R.string.around_walker
        GettingAround.Wheelchair -> R.string.around_wheelchair
        GettingAround.InBed -> R.string.around_in_bed
    }

    fun labelFor(value: Exclusion) = when (value) {
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

    private companion object {
        const val DAYS_IN_WEEK = 7

        /** Anything above this is not an envelope any more. */
        const val MAX_ENVELOPE_MINUTES = 120
    }
}
