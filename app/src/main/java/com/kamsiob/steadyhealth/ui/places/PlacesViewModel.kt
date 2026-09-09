package com.kamsiob.steadyhealth.ui.places

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.data.ContextRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.places.Said
import com.kamsiob.steadyhealth.ui.screens.PlacesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val context get() = ContextRepository(SteadyDatabase.get(getApplication()))

    private val _state = MutableStateFlow(PlacesUiState())
    val state: StateFlow<PlacesUiState> = _state.asStateFlow()

    fun open() = viewModelScope.launch {
        _state.value = PlacesUiState(answers = context.places())
    }

    fun said(id: String, said: Said) = viewModelScope.launch {
        context.setPlace(id, said)
        _state.value = PlacesUiState(answers = context.places())
    }

    fun startAgain() = viewModelScope.launch {
        context.forgetPlaces()
        _state.value = PlacesUiState()
    }
}
