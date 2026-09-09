package com.kamsiob.steadyhealth.ui.places

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.data.ContextRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.places.Said
import com.kamsiob.steadyhealth.ui.screens.PlacesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The places walkthrough, holding six answers.
 *
 * Each answer is written the moment it is tapped rather than at the end, so somebody
 * who answers two questions and puts the phone down has answered two questions.
 * Part 8 calls this optional and light, and a walkthrough that loses four answers
 * because it was not finished is neither.
 */
class PlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val db get() = SteadyDatabase.get(getApplication())
    private val context get() = ContextRepository(db)
    private val profile get() = ProfileRepository(db)

    private val _state = MutableStateFlow(PlacesUiState())
    val state: StateFlow<PlacesUiState> = _state.asStateFlow()

    fun open() = viewModelScope.launch {
        _state.value = PlacesUiState(answers = context.places(), way = profile.gettingAround())
    }

    fun said(id: String, said: Said) = viewModelScope.launch {
        context.setPlace(id, said)
        val answers = context.places()
        _state.update { it.copy(answers = answers) }
    }

    fun startAgain() = viewModelScope.launch {
        context.forgetPlaces()
        _state.update { it.copy(answers = emptyMap()) }
    }
}
