package com.kamsiob.steadyhealth.ui.help

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Which sand blocks have been read.
 *
 * One view model for the whole app rather than one per screen, because "shown once"
 * has to mean once across the app's life and a per-screen owner would have to be told
 * about every screen anyway.
 *
 * Null means not loaded yet, and nothing is drawn until it is known. A sand block that
 * flashes up and vanishes when the answer arrives is worse than one that appears a
 * frame late.
 */
class HelpViewModel(application: Application) : AndroidViewModel(application) {

    private val profile get() = ProfileRepository(SteadyDatabase.get(getApplication()))

    private val _seen = MutableStateFlow<Set<String>?>(null)
    val seen: StateFlow<Set<String>?> = _seen.asStateFlow()

    init {
        viewModelScope.launch {
            _seen.value = Place.entries.filter { profile.seen(it.seenKey) }.map { it.seenKey }.toSet()
        }
    }

    fun dismiss(place: Place) = viewModelScope.launch {
        _seen.update { (it ?: emptySet()) + place.seenKey }
        profile.markSeen(place.seenKey)
    }
}
