package com.kamsiob.steadyhealth.ai

/**
 * One word the document used, and the one plain line that explains it.
 *
 * AI.md job 9 calls this "the part people will use most", and ADDENDUM-03 Part 7
 * agrees: the whole difficulty of a therapist's letter is the vocabulary. [term]
 * is the document's own word and never the app's, which is why the validator can
 * require it to appear in the extracted text, and why it is the one place in a
 * reading where a word from the banned list is allowed to be printed at all.
 *
 * [plain] is one line. Not a paragraph, and never a second opinion about what the
 * word means for this person, which is the sentence Part 7 exists to prevent.
 */
data class ReadingTerm(val term: String, val plain: String)

/**
 * What job 9 returns, before anything has checked it.
 *
 * ADDENDUM-03 Part 7 and AI.md job 9. Exactly four things and nothing else: what
 * kind of document it is, what it says in plain words, the words from the document
 * explained, and things you might ask. The type is closed on purpose. A fifth
 * field is how a reading turns into an opinion, and there is no field here for
 * what any of it means for the person, because that is the question Part 7 sends
 * back to whoever wrote the document.
 *
 * There is no `fromModel` flag of the kind job 6's summary carries, because there
 * is no template fallback for this job. Without MedGemma there is no reading at
 * all, only the photograph, and Part 7 says that is a complete outcome rather than
 * a degraded one.
 *
 * Pure data. It holds no strings a screen shows around it: the heading over each
 * section, the standing disclaimer, and the line for when only the terms survived
 * are all the screen's, so that this type can be unit tested on the JVM and so
 * that the words the app says live where the rest of the app's words live.
 */
data class Reading(
    /** From the document itself, never a guess. Blank when it was dropped. */
    val kind: String,
    /** Three to five short paragraphs, second person, every sentence traceable. */
    val paragraphs: List<String>,
    /** The clinical terms and abbreviations that actually appeared. */
    val terms: List<ReadingTerm>,
    /** Drawn from the document, never from inference, and always a question. */
    val questions: List<String>,
) {
    companion object {

        /**
         * The ceiling on paragraphs. ADDENDUM-03 Part 7 says three to five.
         *
         * The ceiling is enforced and the floor is not. A reading that came back
         * with two honest paragraphs is short, not wrong, and throwing it away
         * would cost the person the only plain-words account of their own letter.
         * A sixth paragraph, on the other hand, is past what anybody reads on a
         * phone, so it is trimmed rather than faulted.
         */
        const val MOST_PARAGRAPHS = 5
    }
}
