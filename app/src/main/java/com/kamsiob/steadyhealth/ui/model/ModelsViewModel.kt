package com.kamsiob.steadyhealth.ui.model

import android.app.Application
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.data.ContextRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.model.ModelRoom
import com.kamsiob.steadyhealth.model.OptionalModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * "What the app can read". ADDENDUM-03 Part 7.
 *
 * The decision layer is [ModelRoom] and it is pure. This reads the two things it needs
 * from the phone, the free space and which models are already here, and writes back
 * which ones the person chose.
 *
 * Nothing downloads yet. Agreeing to a licence records the agreement and nothing else,
 * and the screen says so, because a button that looks like it starts a download and
 * does not is worse than a button that says it is not ready.
 */
class ModelsViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = ContextRepository(SteadyDatabase.get(getApplication()))

    private val _state = MutableStateFlow(ModelsUiState())
    val state: StateFlow<ModelsUiState> = _state.asStateFlow()

    fun open() = viewModelScope.launch {
        _state.value = ModelsUiState(
            room = ModelRoom(freeBytes = freeBytes(), installed = here()),
        )
    }

    /** Agreeing is recorded; the download itself is not built. */
    fun agree(model: OptionalModel) = viewModelScope.launch {
        context.setAgreedToTerms(model.id, true)
        _state.update { it.copy(showTermsFor = null) }
    }

    fun showTerms(model: OptionalModel) = _state.update { it.copy(showTermsFor = model) }

    fun closeTerms() = _state.update { it.copy(showTermsFor = null) }

    /**
     * Take one away. Everything already produced stays, which is Part 7's promise.
     *
     * Nothing is deleted here beyond the record that the model is present, because
     * there is nothing on disk to delete until downloading exists.
     */
    fun remove(model: OptionalModel) = viewModelScope.launch {
        val removal = ModelRoom.remove(model, here())
        context.setModelHere(model.id, false)
        _state.update {
            it.copy(
                room = ModelRoom(freeBytes = freeBytes(), installed = removal.installed),
                fellBackTo = removal.nowByHand,
            )
        }
    }

    fun byHandSeen() = _state.update { it.copy(fellBackTo = emptySet()) }

    private suspend fun here(): Set<OptionalModel> =
        OptionalModel.entries.filter { context.modelHere(it.id) }.toSet()

    /**
     * Room left on the phone, from the data directory rather than from external
     * storage, because that is where a downloaded model would actually go.
     */
    private fun freeBytes(): Long = runCatching {
        val path = getApplication<Application>().filesDir
        StatFs(path.absolutePath).availableBytes
    }.getOrDefault(0L)
}
