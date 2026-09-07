package com.kamsiob.steadyhealth.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.data.AbilityRepository
import com.kamsiob.steadyhealth.data.MovementRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.data.WeightRepository
import com.kamsiob.steadyhealth.domain.Anchor
import com.kamsiob.steadyhealth.domain.ChairEase
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.FloorAccess
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.Ladder
import com.kamsiob.steadyhealth.domain.PemAnswer
import com.kamsiob.steadyhealth.domain.ReadinessFlag
import com.kamsiob.steadyhealth.domain.Stairs
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.domain.WalkTolerance
import com.kamsiob.steadyhealth.engine.PacingEngine
import com.kamsiob.steadyhealth.ui.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Setup, held in memory until the end.
 *
 * Nothing is written to the database until the person finishes, with one
 * deliberate exception: the first weigh-in saves as soon as it is entered,
 * because ONBOARDING.md calls it the first real action and an action that is only
 * provisional is not one.
 *
 * The measured target the specification sets is the first weigh-in and the first
 * tracked ability both saved inside two minutes, which is why every screen here
 * is one tap and why nothing asks twice.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * The database, resolved on every use rather than held.
     *
     * Deleting everything closes the database and destroys its key, and anything
     * holding the old instance then throws "Database is closed" on its next
     * write. That happened on the phone, on the first screen of setup, right
     * after somebody had deleted everything, which is the worst possible moment
     * for this app to crash.
     *
     * The repositories are stateless wrappers, so resolving them per call costs
     * an object allocation and removes the whole class of bug.
     */
    private val db get() = SteadyDatabase.get(getApplication())
    private val profile get() = ProfileRepository(db)
    private val weight get() = WeightRepository(db)
    private val abilities get() = AbilityRepository(db)
    private val movement get() = MovementRepository(db)

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    /**
     * Switch the app's language, now, rather than storing a preference for later.
     *
     * Per-app locales, so the system remembers it and the system settings show
     * it, and so the rest of setup is in the language somebody just picked
     * instead of the one after it.
     */
    fun setLanguage(tag: String) {
        _state.update { it.copy(language = tag) }
        Language.set(getApplication(), tag)
    }

    fun setGettingAround(value: GettingAround) = _state.update { it.copy(gettingAround = value) }

    fun setWithTherapist(value: Boolean) = _state.update { it.copy(withTherapist = value) }

    fun setUnits(value: Units) = _state.update { it.copy(units = value) }

    fun setHeight(cm: Double) = _state.update {
        it.copy(heightCm = cm.coerceIn(MIN_HEIGHT_CM, MAX_HEIGHT_CM))
    }

    fun setAge(years: Int?) = _state.update {
        if (years == null) {
            it.copy(age = null, ageSkipped = true)
        } else {
            it.copy(age = years.coerceIn(MIN_AGE, MAX_AGE), ageSkipped = false)
        }
    }

    fun setChair(value: ChairEase) = _state.update { it.copy(chair = value) }

    fun setStairs(value: Stairs) = _state.update { it.copy(stairs = value) }

    fun setWalk(value: WalkTolerance) = _state.update { it.copy(walkTolerance = value) }

    fun setFloor(value: FloorAccess) = _state.update { it.copy(floor = value) }

    fun setPem(value: PemAnswer) = _state.update { it.copy(pem = value) }

    fun toggleExclusion(value: Exclusion) = _state.update {
        it.copy(
            exclusions = if (value in it.exclusions) it.exclusions - value else it.exclusions + value,
        )
    }

    fun clearExclusions() = _state.update { it.copy(exclusions = emptySet()) }

    fun setReadiness(flag: ReadinessFlag, yes: Boolean) = _state.update {
        it.copy(readiness = it.readiness + (flag to yes))
    }

    fun setTyped(text: String) = _state.update { it.copy(typed = text) }

    /**
     * Add what somebody typed, or one of the starters.
     *
     * Without the model this is the whole of "your words become what is tracked":
     * the person's own sentence, kept verbatim, and a domain they can see. The
     * model's job in Phase 3 is to guess the domain and split a long sentence into
     * a few items; the words stay theirs either way.
     */
    fun addWanted(text: String, domain: com.kamsiob.steadyhealth.domain.AbilityDomain) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        _state.update {
            if (it.wanted.size >= MAX_ITEMS || it.wanted.any { item -> item.text.equals(cleaned, true) }) {
                it
            } else {
                it.copy(wanted = it.wanted + WantedItem(cleaned, domain), typed = "")
            }
        }
    }

    fun removeWanted(text: String) = _state.update {
        it.copy(wanted = it.wanted.filterNot { item -> item.text == text })
    }

    fun rateWanted(text: String, rating: Int) = _state.update {
        it.copy(
            wanted = it.wanted.map { item ->
                if (item.text == text) item.copy(rating = rating.coerceIn(0, MAX_RATING)) else item
            },
        )
    }

    fun setAnchor(value: Anchor) = _state.update { it.copy(anchor = value) }

    fun setFirstWeight(kg: Double) = _state.update { it.copy(firstWeightKg = kg) }

    fun setFirstWalkName(name: String) = _state.update { it.copy(firstWalkName = name) }

    fun go(step: OnboardingStep) = _state.update { it.copy(step = step) }

    /** The first real action, saved the moment it happens. */
    fun saveFirstWeighIn(onSaved: () -> Unit) = viewModelScope.launch {
        val current = _state.value
        weight.save(epochDay = today(), kg = current.firstWeightKg, at = System.currentTimeMillis())
        onSaved()
    }

    /**
     * Everything else, written in one go at the end.
     *
     * Held in memory until here so that somebody who backs out of setup halfway
     * has left nothing behind, and so that a person changing an answer three
     * screens later does not produce a row that has to be found and corrected.
     */
    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        val current = _state.value
        val now = System.currentTimeMillis()

        current.gettingAround?.let { profile.setGettingAround(it) }
        profile.setWithTherapist(current.withTherapist)
        profile.setUnits(current.units)
        profile.setHeightCm(current.heightCm)
        profile.setAge(current.age)
        current.chair?.let { profile.setChair(it) }
        current.stairs?.let { profile.setStairs(it) }
        current.walkTolerance?.let { profile.setWalkTolerance(it) }
        current.floor?.let { profile.setFloorAccess(it) }
        current.pem?.let {
            profile.setPem(it)
            // The pattern question is one of the two triggers for pacing mode.
            // Saying yes here means the app never offers anybody a longer
            // anything, which for a post-exertional pattern is the whole point.
            if (it == PemAnswer.Yes) {
                profile.setPacing(true)
                profile.setEnvelope(PacingEngine.DEFAULT)
            }
        }
        current.anchor?.let { profile.setAnchor(it) }
        profile.setExclusions(current.exclusions, now)
        current.readiness.forEach { (flag, yes) -> profile.setReadiness(flag, yes, now) }

        current.wanted.forEach { item ->
            val id = abilities.add(item.text, item.domain, now)
            abilities.rate(id, today(), item.rating, now)
        }

        // Weighing in is off for somebody mostly in bed, and it is a setting
        // rather than a fact about them, so it can be turned back on.
        if (current.gettingAround == GettingAround.InBed) profile.setWeighsIn(false)

        if (current.firstWalkName.isNotBlank()) {
            val ladder = if (current.gettingAround == GettingAround.Wheelchair) {
                Ladder.Wheeling
            } else {
                Ladder.Walking
            }
            movement.name(ladder, 0, current.firstWalkName, now)
        }

        profile.setOnboardingComplete()
        onDone()
    }

    private fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private companion object {
        const val MIN_HEIGHT_CM = 120.0
        const val MAX_HEIGHT_CM = 220.0
        const val MIN_AGE = 18
        const val MAX_AGE = 100
        const val MAX_ITEMS = 4
        const val MAX_RATING = 10
    }
}
