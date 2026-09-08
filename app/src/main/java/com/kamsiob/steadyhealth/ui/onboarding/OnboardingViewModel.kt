package com.kamsiob.steadyhealth.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.data.AbilityRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.session.Kit
import com.kamsiob.steadyhealth.session.SessionEngine
import com.kamsiob.steadyhealth.session.SessionInputs
import com.kamsiob.steadyhealth.session.SessionPlan
import com.kamsiob.steadyhealth.ui.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Onboarding, rebuilt to ADDENDUM-03 Part 3.
 *
 * Everything is written the moment it is answered rather than at the end. That is
 * what makes the flow resumable, and it is also what makes O4 possible: the first
 * session runs before there is any "finish" to reach, so the app has to already know
 * how the person gets around by then.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val db get() = SteadyDatabase.get(getApplication())
    private val profile get() = ProfileRepository(db)
    private val abilities get() = AbilityRepository(db)

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _firstPlan = MutableStateFlow<SessionPlan?>(null)

    /** The one movement of O4, planned as soon as the way of getting around is known. */
    val firstPlan: StateFlow<SessionPlan?> = _firstPlan.asStateFlow()

    init {
        viewModelScope.launch {
            // Resume exactly where it stopped, with what was already given.
            val step = Step.entries.firstOrNull { it.name == profile.onboardingStep() }
                ?: Step.Hook
            profile.gettingAroundOrNull()?.let { planFirst(it) }
            _state.value = OnboardingState(
                step = step,
                gettingAround = profile.gettingAroundOrNull(),
                wanted = abilities.items().firstOrNull()?.text,
                sessionDone = step == Step.AfterTheSession,
            )
        }
    }

    fun setLanguage(tag: String) {
        _state.update { it.copy(language = tag) }
        Language.set(getApplication(), tag)
    }

    fun go(step: Step) = viewModelScope.launch {
        _state.update { it.copy(step = step) }
        profile.setOnboardingStep(step.name)
    }

    fun setGettingAround(way: GettingAround) = viewModelScope.launch {
        _state.update { it.copy(gettingAround = way) }
        profile.setGettingAround(way)
        profile.setWeighsIn(way != GettingAround.InBed)
        planFirst(way)
        go(Step.TheirWords)
    }

    private fun planFirst(way: GettingAround) {
        _firstPlan.value = SessionEngine.first(SessionInputs(way = way))
    }

    fun setTyped(text: String) = _state.update { it.copy(typed = text) }

    /** Their sentence, exactly as they said it, and one line saying it is kept. */
    fun keep(text: String, domain: AbilityDomain) = viewModelScope.launch {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@launch
        _state.update { it.copy(wanted = trimmed, wantedDomain = domain, typed = "") }
        abilities.add(trimmed, domain, System.currentTimeMillis())
    }

    /**
     * Skipping picks something plain and moves on.
     *
     * ADDENDUM-03 Part 3: the app asks again on day four. Nothing is lost by skipping
     * and nothing anywhere records that it was skipped.
     */
    fun skipWanted(fallback: String) = viewModelScope.launch {
        if (_state.value.wanted == null) {
            abilities.add(fallback, AbilityDomain.Go, System.currentTimeMillis())
            _state.update { it.copy(wanted = fallback) }
        }
        go(Step.FirstSession)
    }

    fun sessionFinished() = viewModelScope.launch {
        _state.update { it.copy(sessionDone = true) }
        go(Step.AfterTheSession)
    }

    fun toggleExclusion(exclusion: Exclusion) = viewModelScope.launch {
        val next = _state.value.exclusions.let {
            if (exclusion in it) it - exclusion else it + exclusion
        }
        _state.update { it.copy(exclusions = next) }
        profile.setExclusions(next, System.currentTimeMillis())
    }

    fun clearExclusions() = viewModelScope.launch {
        _state.update { it.copy(exclusions = emptySet()) }
        profile.setExclusions(emptySet(), System.currentTimeMillis())
    }

    /**
     * What is in the room.
     *
     * A chair with arms is still a chair: it changes which variant is offered, not
     * whether anything is. "No" leaves the chair movements out and the library still
     * has plenty in it.
     */
    fun setChair(answer: ChairAnswer) = viewModelScope.launch {
        _state.update { it.copy(chair = answer) }
        val kit = when (answer) {
            ChairAnswer.None -> setOf(Kit.None, Kit.Wall)
            else -> setOf(Kit.None, Kit.Chair, Kit.Wall)
        }
        profile.setKit(kit)
        profile.setChairHasArms(answer == ChairAnswer.WithArms)
    }

    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        profile.setAnchorDay(LocalDate.now(ZoneId.systemDefault()).toEpochDay())
        profile.setOnboardingComplete()
        onDone()
    }
}
