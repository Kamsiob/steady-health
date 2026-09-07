package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.kamsiob.steadyhealth.ai.Card
import com.kamsiob.steadyhealth.ai.CardSearch
import com.kamsiob.steadyhealth.ai.Cards
import com.kamsiob.steadyhealth.ui.screens.AskUiState
import com.kamsiob.steadyhealth.ui.screens.CardRow
import com.kamsiob.steadyhealth.ui.screens.CardUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ask a question, on its own.
 *
 * Separate from [SteadyViewModel] because it shares nothing with it: no
 * repository, no database, no coroutine. The cards are hand-written constants and
 * the search is a word match, so the whole feature is a pure function of what
 * somebody typed. Keeping it here says that.
 */
class AskViewModel(application: Application) : AndroidViewModel(application) {

    private val _ask = MutableStateFlow(AskUiState())
    val ask: StateFlow<AskUiState> = _ask.asStateFlow()

    private val _card = MutableStateFlow(CardUiState())
    val card: StateFlow<CardUiState> = _card.asStateFlow()

    fun openAsk() {
        _ask.value = AskUiState(all = cardRows())
    }

    /**
     * Look for a card, without the model.
     *
     * The search runs on every keystroke because it is a word match over thirteen
     * hand-written rows, and there is nothing to wait for. When the question is
     * long enough to be a question and nothing matches, the screen says so.
     */
    fun setQuestion(text: String) {
        val titles = Cards.all.associate { it.id to string(it.title) }
        val matches = CardSearch.search(text, titles).take(Cards.CLOSEST).map { it.card }
        _ask.value = AskUiState(
            question = text,
            matches = matches.map(::cardRow),
            all = cardRows(),
            nothingFits = matches.isEmpty() && text.trim().length >= A_REAL_QUESTION,
        )
    }

    fun openCard(id: String) {
        val card = Cards.byId(id) ?: return
        _card.value = CardUiState(
            title = string(card.title),
            body = string(card.body),
            source = string(card.source),
        )
    }

    private fun cardRows(): List<CardRow> = Cards.all.map(::cardRow)

    private fun cardRow(card: Card) =
        CardRow(id = card.id, title = string(card.title), summary = string(card.summary))

    private fun string(@StringRes id: Int): String = getApplication<Application>().getString(id)

    private companion object {
        /** Shorter than this is somebody still typing, not a question. */
        const val A_REAL_QUESTION = 3
    }
}
