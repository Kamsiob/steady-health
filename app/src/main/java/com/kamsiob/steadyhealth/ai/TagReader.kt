package com.kamsiob.steadyhealth.ai

/** A tag the reader proposed, and how sure it was. */
data class ReadTag(val tag: String, val confidence: Double)

/** What the person has taught the app one of their phrases means. */
data class Synonym(val phrase: String, val tag: String)

/**
 * The strict validator around job 2, from AI.md.
 *
 * "Constrained decoding or a strict validator so no tag outside the vocabulary
 * can be returned." This is the strict validator, and it runs whether or not
 * constrained decoding is available, because the guarantee is not allowed to
 * depend on a runtime feature of whatever engine is installed.
 *
 * It is deliberately unforgiving. Anything not exactly one of the twenty-four is
 * dropped rather than corrected, because a near miss corrected by guesswork is
 * how a vocabulary quietly grows.
 */
object TagReader {

    /**
     * Keep only what the app is allowed to say.
     *
     * Drops anything outside the vocabulary, drops duplicates keeping the more
     * confident one, sorts by confidence, and cuts to three.
     */
    fun validate(proposed: List<ReadTag>): List<ReadTag> = proposed
        .filter { it.tag in Tags.ids }
        .filter { it.confidence in 0.0..1.0 }
        .groupBy { it.tag }
        .map { (_, same) -> same.maxBy { it.confidence } }
        .sortedByDescending { it.confidence }
        .take(Tags.MOST_PER_SENTENCE)

    /**
     * What the person taught the app, applied before the model is asked.
     *
     * A phrase somebody corrected once is not a guess any more, so it comes back
     * at full confidence and the model does not get a vote on it. Matching is on
     * whole words, lowercased, so "ate at the diner" matches inside a sentence
     * but "diner" does not match "dinner".
     */
    fun fromSynonyms(text: String, synonyms: List<Synonym>): List<ReadTag> {
        val haystack = normalise(text)
        return synonyms
            .filter { it.tag in Tags.ids }
            .filter { haystack.contains(normalise(it.phrase)) }
            .map { ReadTag(it.tag, CERTAIN) }
            .distinctBy { it.tag }
    }

    /**
     * Everything the app is willing to show for one sentence: what the person has
     * taught it first, then what the reader proposed, up to three in total.
     */
    fun combine(fromPerson: List<ReadTag>, fromReader: List<ReadTag>): List<ReadTag> =
        validate(fromPerson).let { taught ->
            (taught + validate(fromReader).filterNot { it.tag in taught.map(ReadTag::tag) })
                .take(Tags.MOST_PER_SENTENCE)
        }

    /**
     * What to store when the person corrects a tag.
     *
     * The phrase, not the sentence: a whole diary entry as a synonym would match
     * nothing ever again. Short enough to be a phrase and long enough not to be a
     * single common word.
     */
    fun learnable(phrase: String): Boolean {
        val words = normalise(phrase).trim().split(" ").filter { it.isNotBlank() }
        return words.size in 1..LONGEST_SYNONYM_WORDS && words.joinToString("").length >= SHORTEST
    }

    private fun normalise(text: String): String =
        " " + text.lowercase().map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") + " "

    private const val CERTAIN = 1.0
    private const val LONGEST_SYNONYM_WORDS = 5
    private const val SHORTEST = 3
}
