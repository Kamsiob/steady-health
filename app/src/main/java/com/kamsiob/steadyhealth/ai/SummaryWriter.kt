package com.kamsiob.steadyhealth.ai

/**
 * The visit summary without the model.
 *
 * AI.md job 6: "Without the model installed: the summary page shows a
 * deterministic version assembled from templates, plainer and shorter, plus the
 * full measures table. The button is never absent and never disabled."
 *
 * The engine already wrote a sentence for every fact, so this is selection and
 * joining rather than generation: three paragraphs in the shape AI.md gives them,
 * each citing exactly the facts it used. Nothing here can invent a number,
 * because nothing here writes one.
 *
 * It is put through the same validator as the model's output, and a test holds it
 * to passing. A template that could not pass the check would be a template
 * nobody should trust either.
 */
object SummaryWriter {

    fun write(brief: VisitBrief): VisitSummary = VisitSummary(
        paragraphs = listOfNotNull(
            whatChanged(brief),
            whatTheySaid(brief),
            whatHasBeenHappening(brief),
        ),
        // The engine's own wording, used as-is. The model's only job would have
        // been to say these more naturally.
        questions = brief.offered.map { SummaryQuestion(it.text, it.id) },
        fromModel = false,
    )

    /**
     * Paragraph one: what changed most, and what stayed the same.
     *
     * Same is stated plainly and never apologised for, which is why the abilities
     * that held are in the same sentence as the ones that did not, in the same
     * words, rather than in a note underneath.
     */
    private fun whatChanged(brief: VisitBrief): SummaryParagraph? {
        val abilities = brief.facts.filter { it.kind == "ability" }
        val measures = brief.facts.filter { it.kind == "measure" }
        if (abilities.isEmpty()) return null
        val used = abilities + measures.take(TWO)
        return SummaryParagraph(
            text = used.joinToString(" ") { it.text },
            cites = used.map { it.id },
        )
    }

    /** Paragraph two: their own words, and what the app noticed alongside them. */
    private fun whatTheySaid(brief: VisitBrief): SummaryParagraph? {
        val items = brief.facts.filter { it.kind == "item" }.take(TWO)
        val mentions = brief.facts.filter { it.kind == "mention" }.take(TWO)
        val used = items + mentions
        if (used.isEmpty()) return null
        return SummaryParagraph(
            text = used.joinToString(" ") { it.text },
            cites = used.map { it.id },
        )
    }

    /** Paragraph three: sessions, gaps, what they are doing now, and the levers. */
    private fun whatHasBeenHappening(brief: VisitBrief): SummaryParagraph? {
        val sessions = brief.facts.filter { it.kind == "sessions" }.takeLast(THREE)
        val levers = brief.facts.filter { it.kind == "lever" }
        val used = sessions + levers
        if (used.isEmpty()) return null
        return SummaryParagraph(
            text = used.joinToString(" ") { it.text },
            cites = used.map { it.id },
        )
    }

    private const val TWO = 2
    private const val THREE = 3
}
