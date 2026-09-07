package com.kamsiob.steadyhealth.ai

/** A card and why it came back, so the screen can show the ones that nearly fit. */
data class CardMatch(val card: Card, val score: Int)

/**
 * Finding a card without the model.
 *
 * AI.md job 5 hands the model a question and a list of titles and summaries and
 * takes back an id or null. This does the same job by matching words, and it is
 * the path most people will be on, because the model is an optional download.
 *
 * It is deliberately dumb. It matches whole words against hand-written keywords
 * and against the card's own title, and it never guesses: below the threshold it
 * returns nothing, and the screen says there is not a card for that yet rather
 * than showing something close and hoping.
 */
object CardSearch {

    /** Below this, nothing is a match, and saying so is the honest answer. */
    const val ENOUGH = 1

    /**
     * The cards that match [question], best first.
     *
     * [titles] supplies each card's title text, because the words live in
     * resources and this object does not have a context. Passing them in also
     * makes the whole thing testable without one.
     */
    fun search(question: String, titles: Map<String, String>): List<CardMatch> {
        val asked = words(question)
        if (asked.isEmpty()) return emptyList()
        return Cards.all
            .map { card -> CardMatch(card, score(asked, card, titles[card.id].orEmpty())) }
            .filter { it.score >= ENOUGH }
            .sortedByDescending { it.score }
    }

    /** The one card to show, or null when nothing fits well enough. */
    fun best(question: String, titles: Map<String, String>): Card? =
        search(question, titles).firstOrNull()?.card

    private fun score(asked: Set<String>, card: Card, title: String): Int {
        // A hand-written keyword is worth more than a word that happens to be in
        // the title, because somebody wrote the keyword down meaning exactly this.
        val fromKeywords = card.keywords.count { keyword ->
            val parts = words(keyword)
            parts.isNotEmpty() && parts.all { it in asked }
        } * KEYWORD_WORTH
        val fromTitle = words(title).count { it in asked && it !in COMMON }
        return fromKeywords + fromTitle
    }

    private fun words(text: String): Set<String> = text.lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(" ")
        .filter { it.isNotBlank() }
        .toSet()

    private const val KEYWORD_WORTH = 3

    /**
     * Words too common to mean anything on their own.
     *
     * Without this "why does the app do that" matches every card whose title
     * starts with why, which is most of them, and the ordering becomes noise.
     */
    private val COMMON = setOf(
        "the", "a", "an", "is", "it", "to", "of", "and", "why", "what", "how",
        "does", "do", "my", "i", "me", "in", "on", "at", "for", "that", "this",
        "there", "are", "no", "not", "you", "your", "be", "able", "app", "here",
        "really", "goes", "get", "gets", "with", "when", "if", "so", "same",
        "much", "many", "about", "can", "will", "should", "would", "did", "does",
        "health", "steady", "works", "work",
    )
}
