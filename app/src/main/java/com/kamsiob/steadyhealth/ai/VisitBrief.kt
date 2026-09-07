package com.kamsiob.steadyhealth.ai

/**
 * One fact the engine established, with an id.
 *
 * Every number in the summary has to trace back to one of these, which is what
 * makes the validator possible. The engine did the arithmetic; [numbers] is what
 * it worked out and [text] is the sentence it would write itself.
 *
 * [months] and [quotes] exist for the same reason: a month the model mentions has
 * to be a month in a fact it cited, and a sentence in quotation marks has to be a
 * sentence the person actually wrote.
 */
data class Fact(
    val id: String,
    /** What this fact is about. For the prompt only; nothing branches on it. */
    val kind: String,
    /** The engine's own rendering, and what the template summary uses verbatim. */
    val text: String,
    val numbers: List<Double> = emptyList(),
    val months: List<String> = emptyList(),
    /** Sentences the person wrote, which the model may quote and nothing else may. */
    val quotes: List<String> = emptyList(),
)

/**
 * One thing worth asking about, found by the engine.
 *
 * LOGIC.md 13b: the engine, not the model, decides what is worth asking about.
 * Seven rules produce these and nothing else does. The model may reword one; it
 * may not add one, and anything in its output without a [id] here is dropped.
 */
data class QuestionCandidate(
    val id: String,
    /** Which of the seven rules in LOGIC.md 13b produced it. Decides the order. */
    val rule: Int,
    /** The engine's own wording, used as-is when there is no model. */
    val text: String,
    /** The facts that triggered it. */
    val evidence: List<String>,
)

/** How far back the summary looks. */
data class VisitWindow(val fromDay: Long, val toDay: Long, val label: String)

/**
 * Everything the model is allowed to know about somebody's last six months.
 *
 * The engine assembles this and the model receives exactly it: no database, no
 * tool use, no second turn. Anything not in here did not happen, as far as the
 * summary is concerned, and the validator enforces that rather than trusting it.
 *
 * ADDENDUM-01, folded into LOGIC.md 13b: "The engine does all selection, all
 * arithmetic, and all thresholding. It produces a structured brief. The model
 * receives only that brief."
 */
data class VisitBrief(
    val window: VisitWindow,
    val facts: List<Fact>,
    val candidates: List<QuestionCandidate>,
    /** Flags only, never with a reason attached. LOGIC.md 13b. */
    val pacing: Boolean = false,
    val anyExclusions: Boolean = false,
    val wayChanged: Boolean = false,
) {
    fun fact(id: String): Fact? = facts.firstOrNull { it.id == id }

    /** Every sentence the person wrote that may be quoted back. */
    val quotable: List<String> get() = facts.flatMap { it.quotes }

    /** At most four go to the model, in rule order. LOGIC.md 13b. */
    val offered: List<QuestionCandidate>
        get() = candidates.sortedBy { it.rule }.take(MOST_QUESTIONS)

    companion object {
        const val MOST_QUESTIONS = 4

        /** Below this there is not enough to say anything. LOGIC.md 13b. */
        const val LEAST_WEEKS = 8
        const val LEAST_CHECKS = 2
    }
}

/** One paragraph the model wrote, and the facts it says it used. */
data class SummaryParagraph(val text: String, val cites: List<String>)

/** One question, and the candidate it rewords. */
data class SummaryQuestion(val text: String, val candidateId: String)

/** What job 6 returns, before anything has checked it. */
data class VisitSummary(
    val paragraphs: List<SummaryParagraph>,
    val questions: List<SummaryQuestion>,
    val fromModel: Boolean,
)
