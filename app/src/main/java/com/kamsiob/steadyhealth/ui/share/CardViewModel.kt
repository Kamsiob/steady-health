package com.kamsiob.steadyhealth.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.export.Share
import com.kamsiob.steadyhealth.share.CardCopy
import com.kamsiob.steadyhealth.share.CardImage
import com.kamsiob.steadyhealth.share.CardKind
import com.kamsiob.steadyhealth.share.CardRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the card screen is showing. */
data class CardUiState(
    val kind: CardKind = CardKind.Blank,
    val line: String = "",
    /** Anything in the line that Part 11 says never goes on a card. */
    val worthAWord: List<String> = emptyList(),
    val tooLong: Boolean = false,
) {
    /** A card with nothing on it is not a card. Everything else may be sent. */
    val canSend: Boolean get() = line.isNotBlank() && !tooLong
}

/**
 * One card, from the suggested line to the share sheet. ADDENDUM-03 Part 11.
 *
 * The suggested line is put in the field and is then the person's, which is what
 * "every card is editable before sending, with the suggested line already in the
 * field" means. Nothing is remembered afterwards: the file goes to the cache, the
 * share sheet takes it, and the app does not know or ask where it went.
 */
class CardViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CardUiState())
    val state: StateFlow<CardUiState> = _state.asStateFlow()

    fun open(kind: CardKind, suggested: String) {
        _state.value = CardUiState(kind = kind, line = suggested).checked()
    }

    fun setLine(line: String) {
        _state.update { it.copy(line = line).checked() }
    }

    /**
     * Render and hand it over.
     *
     * The warning about weight or a rating does not stop this. Part 11's list is
     * about what the app puts on a card, and a person's own sentence is theirs; the
     * screen says what it noticed and they decide.
     */
    fun send() = viewModelScope.launch {
        val state = _state.value
        if (!state.canSend) return@launch
        val context = getApplication<Application>()
        val file = CardImage.write(context, CardCopy(state.kind, state.line.trim()))
        Share.file(context, file, "image/png", context.getString(R.string.card_send))
    }

    private fun CardUiState.checked() = copy(
        worthAWord = CardRules.worthAWord(line),
        tooLong = CardRules.tooLong(line),
    )
}
