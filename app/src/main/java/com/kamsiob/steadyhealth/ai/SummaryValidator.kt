package com.kamsiob.steadyhealth.ai

import kotlin.math.abs

/** Why a paragraph was rejected. One per check in AI.md job 6. */
enum class Fault {
    /** A number with no matching fact. */
    InventedNumber,

    /** No citations, or a citation to a fact that is not in the brief. */
    BadCitation,

    /** A month or date not in any cited fact. */
    InventedDate,

    /** A quotation the person never wrote. */
    InventedQuote,

    /** A word from DESIGN.md section 6, or a clinical term. */
    ForbiddenWord,

    /** "because", "suggests", and the rest: the model reaching past the data. */
    CausalClaim,
}

/** One paragraph's verdict, with everything that was wrong with it. */
data class ParagraphVerdict(
    val paragraph: SummaryParagraph,
    val faults: List<Fault>,
    val detail: List<String>,
) {
    val passed: Boolean get() = faults.isEmpty()
}

/** What survived, and what did not. */
data class SummaryVerdict(
    val paragraphs: List<ParagraphVerdict>,
    val questions: List<SummaryQuestion>,
    val droppedQuestions: Int,
) {
    val kept: List<SummaryParagraph> get() = paragraphs.filter { it.passed }.map { it.paragraph }
    val allFailed: Boolean get() = paragraphs.isNotEmpty() && paragraphs.none { it.passed }
    val faults: List<Fault> get() = paragraphs.flatMap { it.faults }.distinct()
}

/**
 * The validator, from AI.md job 6.
 *
 * Built before the model was wired in, which ADDENDUM-01 requires and which is
 * the right order for a reason worth stating: this is the only feature in the app
 * where a sentence somebody else might act on comes out of a language model. A
 * clinician reading an invented number in a summary a patient brought them is the
 * one failure that actually matters, and it is not a failure a prompt instruction
 * prevents. A prompt is a request. This is a check.
 *
 * Deterministic, offline, and unforgiving. It has no model in it and no
 * randomness, so its behaviour on any output is the same today and in five years.
 *
 * Seven checks, in AI.md's order. Where a rule says "or directly entailed by",
 * this reads that strictly: a number is allowed when it appears in a cited fact's
 * numbers, and a quantity word when it appears in a cited fact's own text. A
 * looser reading would need the validator to do arithmetic, and a validator that
 * computes is a second thing that can be wrong.
 */
object SummaryValidator {

    /** Three paragraphs, at most ninety words. AI.md job 6. */
    const val MOST_WORDS = 90
    const val MOST_PARAGRAPHS = 3

    /** How close a number has to be to a fact's number to count as the same one. */
    private const val CLOSE_ENOUGH = 0.051

    fun check(summary: VisitSummary, brief: VisitBrief): SummaryVerdict {
        val paragraphs = summary.paragraphs.take(MOST_PARAGRAPHS).map { check(it, brief) }
        val valid = brief.candidates.map { it.id }.toSet()
        val questions = summary.questions.filter { it.candidateId in valid }
        return SummaryVerdict(
            paragraphs = paragraphs,
            questions = questions.take(VisitBrief.MOST_QUESTIONS),
            droppedQuestions = summary.questions.size - questions.size,
        )
    }

    fun check(paragraph: SummaryParagraph, brief: VisitBrief): ParagraphVerdict {
        val cited = paragraph.cites.mapNotNull(brief::fact)
        val found = buildList {
            addAll(citations(paragraph, brief, cited))
            addAll(numbers(paragraph, cited))
            addAll(dates(paragraph, cited))
            addAll(quotes(paragraph, brief))
            addAll(forbiddenWords(paragraph))
            addAll(claimShape(paragraph))
        }
        return ParagraphVerdict(
            paragraph = paragraph,
            faults = found.map { it.first }.distinct(),
            detail = found.map { it.second },
        )
    }

    /** 2. Every paragraph cites at least one fact, and every id exists. */
    private fun citations(
        paragraph: SummaryParagraph,
        brief: VisitBrief,
        cited: List<Fact>,
    ): List<Pair<Fault, String>> =
        if (paragraph.cites.isEmpty() || cited.size != paragraph.cites.size) {
            listOf(
                Fault.BadCitation to
                    "cites ${paragraph.cites} but the brief has ${brief.facts.map { it.id }}",
            )
        } else {
            emptyList()
        }

    /**
     * 1. Every number traces back to a cited fact.
     *
     * A number written as a word passes either way: as the value, or as the word
     * itself in a fact the engine wrote. "One foot" is the name of a measure, not
     * a claim that something happened once, and the engine's own sentence says so.
     */
    private fun numbers(paragraph: SummaryParagraph, cited: List<Fact>): List<Pair<Fault, String>> {
        val allowed = cited.flatMap { it.numbers }
        val allowedWords = cited.joinToString(" ") { it.text }
        val digits = digitsIn(paragraph.text)
            .filter { number -> allowed.none { abs(it - number) < CLOSE_ENOUGH } }
            .map { Fault.InventedNumber to "$it is in no cited fact" }
        val words = (spelledIn(paragraph.text) + quantitiesIn(paragraph.text))
            .filterNot { (word, value) ->
                (value != null && allowed.any { abs(it - value) < CLOSE_ENOUGH }) ||
                    Words.says(allowedWords, word)
            }
            .map { Fault.InventedNumber to "\"${it.first}\" is in no cited fact" }
        return digits + words
    }

    /** 3. Every month or season matches a date in a cited fact. */
    private fun dates(paragraph: SummaryParagraph, cited: List<Fact>): List<Pair<Fault, String>> {
        val allowed = cited.flatMap { it.months }
        return MONTHS
            .filter { Words.says(paragraph.text, it) && allowed.none { m -> m.equals(it, true) } }
            .map { Fault.InventedDate to "$it is in no cited fact" }
    }

    /** 4. Every quotation is a sentence the person actually wrote. */
    private fun quotes(paragraph: SummaryParagraph, brief: VisitBrief): List<Pair<Fault, String>> =
        quotesIn(paragraph.text)
            .filter { quote -> brief.quotable.none { it.trim().equals(quote.trim(), true) } }
            .map { Fault.InventedQuote to "\"$it\" is not something the person wrote" }

    /** 6. The banned list, plus the clinical vocabulary this job adds. */
    private fun forbiddenWords(paragraph: SummaryParagraph): List<Pair<Fault, String>> {
        val bad = Words.bannedWordsIn(paragraph.text) + Words.clinicalWordsIn(paragraph.text)
        return if (bad.isEmpty()) emptyList() else listOf(Fault.ForbiddenWord to "uses $bad")
    }

    /** 7. "because", "suggests", and the rest: reaching past the data. */
    private fun claimShape(paragraph: SummaryParagraph): List<Pair<Fault, String>> {
        val causal = Words.causalPhrasesIn(paragraph.text)
        return if (causal.isEmpty()) {
            emptyList()
        } else {
            listOf(Fault.CausalClaim to "reaches past the data with $causal")
        }
    }

    /**
     * Every number in a sentence, including the ones written as words.
     *
     * "Fourteen" and "14" are the same claim and both have to be traceable, so
     * both are extracted. Ordinals are not numbers about the person ("the first
     * time") and are left alone, which is why the list stops where it does.
     */
    fun numbersIn(text: String): List<Double> =
        digitsIn(text) + spelledIn(text).mapNotNull { it.second }

    fun digitsIn(text: String): List<Double> = Regex("""(?<![\w.])(\d+(?:\.\d+)?)""")
        .findAll(text)
        .mapNotNull { it.groupValues[1].toDoubleOrNull() }
        .toList()

    /** Numbers written as words, with the value each one carries. */
    fun spelledIn(text: String): List<Pair<String, Double?>> =
        SPELLED.filterKeys { Words.says(text, it) }.map { it.key to it.value.toDouble() }

    /** "most", "all", "twice": a quantity with no single value behind it. */
    private fun quantitiesIn(text: String): List<Pair<String, Double?>> =
        Words.quantities.filter { Words.says(text, it) }.map { it to null }

    /** Everything inside straight or curly double quotes. */
    fun quotesIn(text: String): List<String> =
        Regex("""[“"]([^”"]+)[”"]""").findAll(text).map { it.groupValues[1] }.toList()

    fun words(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    private val SPELLED = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
        "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
        "nineteen" to 19, "twenty" to 20, "thirty" to 30, "forty" to 40,
        "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80,
        "ninety" to 90, "hundred" to 100,
    )

    private val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December",
        "spring", "summer", "autumn", "winter", "fall",
    )
}
