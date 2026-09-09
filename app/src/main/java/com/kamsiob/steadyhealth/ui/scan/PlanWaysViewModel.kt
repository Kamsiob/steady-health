package com.kamsiob.steadyhealth.ui.scan

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.plan.HowMany
import com.kamsiob.steadyhealth.plan.HowOften
import com.kamsiob.steadyhealth.plan.PlanItem
import com.kamsiob.steadyhealth.plan.PlanMatching
import com.kamsiob.steadyhealth.plan.Sureness
import com.kamsiob.steadyhealth.session.Movements
import com.kamsiob.steadyhealth.session.Piece
import com.kamsiob.steadyhealth.ui.TodayCards
import com.kamsiob.steadyhealth.ui.screens.LibraryRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Where the three ways into a plan that have no page behind them have got to. */
data class PlanWaysUiState(
    /** The typed way, as it stands in the box. */
    val typed: String = "",
    /** Everything this person could be asked for, for the picked way. */
    val library: List<LibraryRow> = emptyList(),
    val picked: Set<String> = emptySet(),
    /** Whether this phone can hear a sentence without sending it anywhere. */
    val canHear: Boolean = false,
    val microphone: Boolean = false,
    val listening: Boolean = false,
    /** The sentence that was heard, shown back before anything is read into items. */
    val heard: String = "",
    val nothingHeard: Boolean = false,
)

/**
 * The three ways into a therapist's plan that are not the camera. ADDENDUM-03 Part 6.
 *
 * Part 6 has four ways in and all four end at one confirmation screen. That screen and
 * the draft behind it belong to [ScanViewModel], which owns saving a plan, so this
 * holds only what the three screens are working on and hands finished items across.
 * One draft rather than two is what keeps "nothing is saved unconfirmed" a property of
 * the app instead of a property of each way in.
 *
 * Nothing here reads a plan differently from the way the photographed sheet is read.
 * Typing and speaking both go through [PlanMatching], which was written to take a
 * block of text from any of them, and picking from the library goes through none of it
 * because there is nothing to guess: the person named the movement by tapping it.
 */
class PlanWaysViewModel(application: Application) : AndroidViewModel(application) {

    private val db get() = SteadyDatabase.get(getApplication())
    private val profile get() = ProfileRepository(db)
    private val cards get() = TodayCards(getApplication(), db)

    private val voice = PlanVoice(application)

    // Both answers are here before the first frame rather than filled in by [open].
    // Read a moment later, the screen would show the sentence about this phone not
    // being able to listen, on a phone that can, for as long as it takes to correct
    // itself, and that flash is the app appearing to change its mind about somebody.
    private val _state = MutableStateFlow(
        PlanWaysUiState(canHear = voice.available(), microphone = microphone()),
    )
    val state: StateFlow<PlanWaysUiState> = _state.asStateFlow()

    /**
     * What the screen needs before anybody touches it.
     *
     * Whether the phone can hear is asked here rather than held, because a person can
     * install a language pack between one visit and the next and the answer would
     * otherwise be wrong until the app was restarted.
     *
     * It adds to what is here and never clears it. Each way in holds its own view
     * model, so there is nothing stale to reset, and this runs again every time the
     * screen comes back from the confirmation screen: somebody who went forward to
     * look and pressed back would otherwise find their picks gone.
     */
    fun open() {
        _state.update { it.copy(canHear = voice.available(), microphone = microphone()) }
        loadLibrary()
    }

    private fun loadLibrary() = viewModelScope.launch {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        _state.update {
            it.copy(
                library = cards.library(
                    way = profile.gettingAround(),
                    exclusions = profile.exclusions(),
                    today = today,
                    pieces = Piece.entries.toSet(),
                ),
            )
        }
    }

    // --- Type it -------------------------------------------------------------

    fun setTyped(text: String) = _state.update { it.copy(typed = text) }

    /**
     * What was typed, read the way a scanned sheet is read.
     *
     * One movement to a line is what the screen asks for, and the reader takes a block
     * of text either way, so somebody who types the whole thing as one sentence gets
     * the same answer the spoken way in gets.
     */
    fun typedItems(): List<PlanItem> = PlanMatching.readAll(_state.value.typed)

    // --- Pick from the library ------------------------------------------------

    fun togglePicked(id: String) = _state.update { state ->
        state.copy(picked = if (id in state.picked) state.picked - id else state.picked + id)
    }

    /**
     * One item per movement tapped, in the order the library lists them.
     *
     * The line is the movement's own name because that is what the person picked, and
     * the confirmation screen shows every line as it was given. The sureness is
     * [Sureness.Named] for the same reason: nothing was matched, so there is nothing
     * to be unsure about. Both numbers are left unsaid, because a movement chosen off
     * a list carries no reps and no frequency, and filling either in would be the app
     * putting a number into somebody else's plan.
     */
    fun pickedItems(): List<PlanItem> {
        val state = _state.value
        return state.library
            .filter { it.id in state.picked }
            .mapNotNull { row -> Movements.byId(row.id) }
            .map { movement ->
                PlanItem(
                    line = movement.name,
                    movement = movement,
                    howMany = HowMany.Unsaid,
                    howOften = HowOften.Unsaid,
                    sureness = Sureness.Named,
                )
            }
    }

    // --- Say it out loud ------------------------------------------------------

    fun microphoneNow() = _state.update { it.copy(microphone = microphone()) }

    private fun microphone(): Boolean = ContextCompat.checkSelfPermission(
        getApplication(),
        android.Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Listen for one sentence.
     *
     * Every ending that is not a sentence comes back the same way, as nothing heard,
     * because the screen says one plain thing for all of them. There is deliberately
     * no second attempt with the networked recogniser: [PlanVoice] says why.
     */
    fun listen() {
        // Clearing what was heard is what makes this "say it again" as well as
        // "start talking". Two functions for that would be one screen state apart.
        _state.update { it.copy(listening = true, nothingHeard = false, heard = "") }
        voice.listen(
            onHeard = { said -> _state.update { it.copy(listening = false, heard = said) } },
            onNothing = { _state.update { it.copy(listening = false, nothingHeard = true) } },
        )
    }

    /** The person says that is the whole sentence. The result still arrives. */
    fun stopListening() = voice.stop()

    /**
     * The screen has gone, and the microphone goes with it.
     *
     * Stopping and letting go are not the same thing, and only one of them is a
     * promise. A recogniser that has been told to stop is still a recogniser this app
     * built and still holds, which is what the phone's own microphone light is
     * reporting on, and somebody who turned their screen off part way through a
     * sentence should not have to take the app's word for what it is doing with the
     * microphone while they are not looking. So the screen going away destroys it.
     * Coming back builds a new one, and nothing is lost by that.
     */
    fun release() {
        voice.close()
        _state.update { it.copy(listening = false) }
    }

    /** What was heard, read exactly as a typed block or a scanned page is read. */
    fun heardItems(): List<PlanItem> = PlanMatching.readAll(_state.value.heard)

    /** The last of it, for the way out that [release] does not cover. */
    override fun onCleared() {
        voice.close()
    }
}
